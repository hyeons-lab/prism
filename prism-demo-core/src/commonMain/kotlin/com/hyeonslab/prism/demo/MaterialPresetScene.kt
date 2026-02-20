package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.LightComponent
import com.hyeonslab.prism.ecs.components.LightType
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private val log = Logger.withTag("MaterialPresetScene")

/** PBR material presets displayed as 5 labeled spheres in a row. */
private val MATERIAL_PRESETS =
  listOf(
    Material(baseColor = Color(1.0f, 0.77f, 0.33f), metallic = 1.0f, roughness = 0.2f),
    Material(baseColor = Color(0.95f, 0.95f, 0.95f), metallic = 1.0f, roughness = 0.05f),
    Material(baseColor = Color(0.7f, 0.65f, 0.6f), metallic = 0.9f, roughness = 0.7f),
    Material(baseColor = Color(0.95f, 0.9f, 0.85f), metallic = 0.0f, roughness = 0.3f),
    Material(baseColor = Color(0.05f, 0.05f, 0.08f), metallic = 0.0f, roughness = 0.15f),
  )

private const val PRESET_SPACING = 1.5f
private const val PRESET_ORBIT_RADIUS = 5f

/**
 * Creates a scene showing 5 PBR material presets as spheres in a row: Gold, Chrome, Worn Metal,
 * Ceramic, and Obsidian. The orbit camera starts from the front at [PRESET_ORBIT_RADIUS] units.
 *
 * @param progressiveScope When non-null, spheres are added one per frame and IBL is computed
 *   asynchronously at reduced resolution so the render loop starts immediately. When null, all
 *   spheres and full-quality IBL are ready before the function returns.
 */
fun createMaterialPresetScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  progressiveScope: CoroutineScope? = null,
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  renderer.hdrEnabled = true

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera
  val cameraEntity = world.createEntity()
  val camera =
    Camera().apply {
      position = Vec3(0f, 0f, PRESET_ORBIT_RADIUS)
      target = Vec3.ZERO
      fovY = 45f
      aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1f
      nearPlane = 0.1f
      farPlane = 100f
    }
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Warm directional key light
  val dirLight = world.createEntity()
  world.addComponent(dirLight, TransformComponent())
  world.addComponent(
    dirLight,
    LightComponent(
      lightType = LightType.DIRECTIONAL,
      color = Color(1.0f, 0.95f, 0.8f),
      intensity = 2.5f,
      direction = Vec3(-0.5f, -1.0f, -0.5f),
    ),
  )

  // Cool fill light from upper-right
  val fillLight = world.createEntity()
  world.addComponent(fillLight, TransformComponent(position = Vec3(4f, 4f, 3f)))
  world.addComponent(
    fillLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(0.7f, 0.85f, 1.0f),
      intensity = 50f,
      range = 20f,
    ),
  )

  val sphereMesh = Mesh.sphere()
  val count = MATERIAL_PRESETS.size

  world.initialize()

  val scene = DemoScene(engine, world, renderer, cameraEntity, orbitRadius = PRESET_ORBIT_RADIUS)

  if (progressiveScope != null) {
    // Queue each sphere for addition on successive frames so the render loop can show progress.
    MATERIAL_PRESETS.forEachIndexed { i, mat ->
      val x = (i - (count - 1) / 2f) * PRESET_SPACING
      scene.pendingSetup.addLast {
        val sphere = world.createEntity()
        world.addComponent(sphere, TransformComponent(position = Vec3(x, 0f, 0f)))
        world.addComponent(sphere, MeshComponent(mesh = sphereMesh))
        world.addComponent(sphere, MaterialComponent(material = mat))
      }
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
        log.i { "Async IBL ready" }
      } catch (e: Throwable) {
        log.d { "Async IBL aborted (scene switched before completion): ${e.message}" }
      }
    }
  } else {
    // Non-progressive: full-quality IBL + all spheres before returning.
    renderer.initializeIbl()
    MATERIAL_PRESETS.forEachIndexed { i, mat ->
      val x = (i - (count - 1) / 2f) * PRESET_SPACING
      val sphere = world.createEntity()
      world.addComponent(sphere, TransformComponent(position = Vec3(x, 0f, 0f)))
      world.addComponent(sphere, MeshComponent(mesh = sphereMesh))
      world.addComponent(sphere, MaterialComponent(material = mat))
    }
  }

  log.i { "Material preset scene initialized: $count spheres (${width}×${height})" }
  return scene
}
