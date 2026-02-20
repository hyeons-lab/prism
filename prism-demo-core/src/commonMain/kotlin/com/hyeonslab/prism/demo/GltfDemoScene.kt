package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.assets.GltfAsset
import com.hyeonslab.prism.assets.GltfLoader
import com.hyeonslab.prism.assets.ImageDecoder
import com.hyeonslab.prism.assets.upload
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
import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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
 *   scope while the scene renders immediately with placeholder materials. IBL is also computed
 *   asynchronously at reduced resolution so the render loop starts immediately. When null, the
 *   function blocks until all textures and IBL are fully initialized before returning.
 * @param nativeGlbBuffer Platform-native source buffer (e.g. the JS `ArrayBuffer` from the GLB
 *   fetch on WASM). When non-null and [progressiveScope] is non-null, texture image bytes are
 *   sliced directly from this buffer instead of copying through Kotlin, eliminating ~125K JS
 *   interop calls per texture. Pass null on platforms that do not retain a native buffer.
 */
suspend fun createGltfDemoScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  glbData: ByteArray,
  surfacePreConfigured: Boolean = false,
  progressiveScope: CoroutineScope? = null,
  nativeGlbBuffer: Any? = null,
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = surfacePreConfigured)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  renderer.hdrEnabled = true

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
        val texture = asset.textures.getOrNull(i) ?: continue
        // Use zero-copy path when native buffer + byte range are both available (WASM only).
        val range = result.rawTextureByteRanges.getOrNull(i)
        val imageData =
          try {
            if (nativeGlbBuffer != null && range != null) {
              ImageDecoder.decodeFromNativeBuffer(nativeGlbBuffer, range.first, range.second)
            } else {
              val rawBytes = result.rawTextureImageBytes[i] ?: continue
              ImageDecoder.decode(rawBytes, unpremultiply = true)
            }
          } catch (e: Exception) {
            log.w { "Progressive texture $i decode failed: ${e.message}" }
            null
          } ?: continue
        // Update descriptor to real image dimensions before GPU allocation.
        texture.descriptor =
          texture.descriptor.copy(width = imageData.width, height = imageData.height)
        renderer.initializeTexture(texture)
        uploadDecodedImage(renderer, texture, imageData)
        // Evict cached bind groups so the next draw call rebuilds them with the real texture.
        texToMaterials[texture]?.forEach { renderer.invalidateMaterial(it) }
        // Yield to the render loop so the next texture upload starts on a clean frame boundary.
        yield()
      }
      log.i { "Progressive texture loading complete ($nodeCount primitives)" }
    }
    // Compute IBL asynchronously at reduced resolution — completes in < one frame (~25ms) so the
    // render loop runs immediately with the default env bind group until IBL is ready.
    // Wrapped in try-catch: if the user switches scenes before IBL finishes, surface.detach()
    // closes the device and initializeIbl() will throw. Without a handler the unhandled exception
    // would terminate the WASM coroutine runtime, preventing the new scene from launching.
    progressiveScope.launch {
      try {
        renderer.initializeIbl(
          brdfLutSize = 64,
          brdfLutSamples = 32,
          irradianceSize = 8,
          prefilteredSize = 16,
          prefilteredMipLevels = 4,
        )
        log.i { "glTF async IBL ready" }
      } catch (e: Throwable) {
        log.d { "glTF async IBL aborted (scene switched before completion): ${e.message}" }
      }
    }
  } else {
    // Non-progressive: block on full image decode and IBL before returning.
    renderer.initializeIbl()
    val asset = GltfLoader().load("model.glb", glbData)
    nodeCount = asset.renderableNodes.size
    renderer.upload(asset)
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
