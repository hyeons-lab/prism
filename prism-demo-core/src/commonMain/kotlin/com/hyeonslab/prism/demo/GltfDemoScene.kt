package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.assets.GltfAsset
import com.hyeonslab.prism.assets.GltfLoader
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
import com.hyeonslab.prism.renderer.Renderer
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext

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
 */
suspend fun createGltfDemoScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  glbData: ByteArray,
  surfacePreConfigured: Boolean = false,
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = surfacePreConfigured)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  renderer.hdrEnabled = true
  renderer.initializeIbl()

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Parse GLB and upload textures before instantiating entities
  val asset = GltfLoader().load("model.glb", glbData)
  uploadGltfTextures(renderer, asset)
  for (node in asset.renderableNodes) {
    renderer.uploadMesh(node.mesh)
  }
  asset.instantiateInWorld(world)

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
  log.i {
    "glTF demo scene initialized: ${asset.renderableNodes.size} primitives (${width}x${height})"
  }
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
