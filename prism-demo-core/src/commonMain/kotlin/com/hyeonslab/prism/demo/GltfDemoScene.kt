package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.assets.GltfAsset
import com.hyeonslab.prism.assets.GltfLoader
import com.hyeonslab.prism.assets.ImageDecoder
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.LightComponent
import com.hyeonslab.prism.ecs.components.LightType
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Renderer
import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val log = Logger.withTag("GltfDemoScene")

private const val GLTF_ORBIT_RADIUS = 3.5f

/**
 * Creates a glTF-model demo scene from raw GLB bytes. Renders the first scene in the GLB, centered
 * at the origin, with PBR lighting and IBL enabled.
 *
 * @param wgpuContext The WebGPU context to render into.
 * @param width Initial surface width in pixels.
 * @param height Initial surface height in pixels.
 * @param glbData Raw bytes of the GLB file to load.
 * @param surfacePreConfigured When true, the renderer skips its own surface configuration.
 * @param progressiveScope When non-null, textures are decoded and uploaded asynchronously in this
 *   scope while the scene renders immediately with placeholder materials. When null, the function
 *   blocks until all textures are uploaded before returning.
 */
suspend fun createGltfDemoScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  glbData: ByteArray,
  surfacePreConfigured: Boolean = false,
  progressiveScope: CoroutineScope? = null,
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = surfacePreConfigured)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  renderer.hdrEnabled = true
  renderer.initializeIbl()

  val world = World()
  world.addSystem(RenderSystem(renderer))

  val nodeCount: Int
  if (progressiveScope != null) {
    // Progressive: parse structure fast (no image decode), render with placeholder materials,
    // then decode and upload each texture in the background.
    val result = GltfLoader().loadStructure("model.glb", glbData)
    val asset = result.asset
    nodeCount = asset.renderableNodes.size
    for (node in asset.renderableNodes) {
      renderer.uploadMesh(node.mesh)
    }
    asset.instantiateInWorld(world)

    val texToMaterials = buildTexToMaterialsMap(asset)
    progressiveScope.launch {
      for (i in result.rawTextureImageBytes.indices) {
        val rawBytes = result.rawTextureImageBytes[i] ?: continue
        val texture = asset.textures.getOrNull(i) ?: continue
        val imageData =
          try {
            ImageDecoder.decode(rawBytes, unpremultiply = true)
          } catch (e: Exception) {
            log.w { "Progressive texture $i decode failed: ${e.message}" }
            null
          } ?: continue
        // Update descriptor to real image dimensions before GPU allocation.
        texture.descriptor =
          texture.descriptor.copy(width = imageData.width, height = imageData.height)
        renderer.initializeTexture(texture)
        renderer.uploadTextureData(texture, imageData.pixels)
        // Evict cached bind groups so the next draw call rebuilds them with the real texture.
        texToMaterials[texture]?.forEach { renderer.invalidateMaterial(it) }
      }
      log.i { "Progressive texture loading complete ($nodeCount primitives)" }
    }
  } else {
    // Non-progressive: block on full image decode, then upload all textures before returning.
    val asset = GltfLoader().load("model.glb", glbData)
    uploadGltfTextures(renderer, asset)
    nodeCount = asset.renderableNodes.size
    for (node in asset.renderableNodes) {
      renderer.uploadMesh(node.mesh)
    }
    asset.instantiateInWorld(world)
  }

  // Camera positioned to view the model at orbit distance
  val cameraEntity = world.createEntity()
  val camera = Camera()
  camera.position = Vec3(0f, 0f, GLTF_ORBIT_RADIUS)
  camera.target = Vec3.ZERO
  camera.fovY = 45f
  camera.aspectRatio = width.toFloat() / height.toFloat()
  camera.nearPlane = 0.1f
  camera.farPlane = 50f
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Directional light — warm white from upper-left
  val dirLight = world.createEntity()
  world.addComponent(dirLight, TransformComponent())
  world.addComponent(
    dirLight,
    LightComponent(
      lightType = LightType.DIRECTIONAL,
      color = Color(1.0f, 0.95f, 0.8f),
      intensity = 2.0f,
      direction = Vec3(-0.5f, -1.0f, -0.5f),
    ),
  )

  // Point light — cool white for complementary specular highlights
  val pointLight = world.createEntity()
  world.addComponent(pointLight, TransformComponent(position = Vec3(3f, 3f, 3f)))
  world.addComponent(
    pointLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(0.8f, 0.9f, 1.0f),
      intensity = 20f,
      range = 15f,
    ),
  )

  world.initialize()
  log.i { "glTF demo scene initialized: $nodeCount primitives (${width}x${height})" }
  return DemoScene(engine, world, renderer, cameraEntity, orbitRadius = GLTF_ORBIT_RADIUS)
}

/** Uploads all textures in a [GltfAsset] to the GPU via the given [Renderer]. */
private fun uploadGltfTextures(renderer: Renderer, asset: GltfAsset) {
  for ((assetTexture, imageData) in asset.textures.zip(asset.imageData)) {
    if (imageData == null) continue
    renderer.initializeTexture(assetTexture)
    renderer.uploadTextureData(assetTexture, imageData.pixels)
  }
}

/**
 * Builds a reverse map from [Texture] to all [Material]s that reference it. Used during progressive
 * loading to invalidate the renderer's bind group cache when a texture is uploaded.
 */
private fun buildTexToMaterialsMap(asset: GltfAsset): Map<Texture, List<Material>> {
  val map = mutableMapOf<Texture, MutableList<Material>>()
  for (node in asset.renderableNodes) {
    val mat = node.material ?: continue
    listOfNotNull(
        mat.albedoTexture,
        mat.normalTexture,
        mat.metallicRoughnessTexture,
        mat.occlusionTexture,
        mat.emissiveTexture,
      )
      .forEach { tex -> map.getOrPut(tex) { mutableListOf() }.add(mat) }
  }
  return map
}
