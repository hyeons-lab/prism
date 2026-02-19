package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.Entity
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val log = Logger.withTag("DemoScene")

private const val SPHERE_COLS = 7
private const val SPHERE_ROWS = 7
private const val SPHERE_SPACING = 1.5f

/** Holds the engine, ECS world, renderer, and camera entity for the PBR sphere grid demo scene. */
class DemoScene(
  val engine: Engine,
  val world: World,
  val renderer: WgpuRenderer,
  val cameraEntity: Entity,
) {
  private var orbitAzimuth = 0f
  private var orbitElevation = 0f
  private val orbitRadius = 12f

  /**
   * Advances the scene by one frame: runs the ECS world update. The PBR sphere grid is a static
   * showcase — no per-frame rotation is applied to individual spheres.
   */
  fun tick(deltaTime: Float, elapsed: Float, frameCount: Long) {
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    world.update(time)
  }

  /**
   * Advances the scene with an explicit angle value. Kept for cross-platform compatibility with iOS
   * and macOS demos that manage their own angle accumulation. The angle is unused in the static
   * sphere grid scene.
   */
  fun tickWithAngle(deltaTime: Float, elapsed: Float, frameCount: Long, angle: Float) {
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    world.update(time)
  }

  /** Updates the camera's aspect ratio. Call this when the rendering surface is resized. */
  fun updateAspectRatio(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val cameraComponent = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cameraComponent.camera.aspectRatio = width.toFloat() / height.toFloat()
  }

  /**
   * Rotates the orbit camera by the given deltas (in radians). Horizontal drag maps to azimuth
   * (rotation around Y), vertical drag maps to elevation (tilt up/down). Elevation is clamped to
   * avoid gimbal lock at the poles.
   */
  fun orbitBy(deltaAzimuth: Float, deltaElevation: Float) {
    orbitAzimuth += deltaAzimuth
    orbitElevation =
      (orbitElevation + deltaElevation).coerceIn(
        -PI.toFloat() / 2f + 0.05f,
        PI.toFloat() / 2f - 0.05f,
      )
    val ca = cos(orbitElevation)
    val x = orbitRadius * ca * sin(orbitAzimuth)
    val y = orbitRadius * sin(orbitElevation)
    val z = orbitRadius * ca * cos(orbitAzimuth)
    val cameraComponent = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cameraComponent.camera.position = Vec3(x, y, z)
  }

  /**
   * Overrides metallic and roughness on all sphere entities. Called each frame from the Flutter
   * render loop to apply DemoStore slider values to the rendered materials.
   */
  fun setMaterialOverride(metallic: Float, roughness: Float) {
    for ((_, matComp) in world.query<MaterialComponent>()) {
      val current = matComp.material ?: continue
      matComp.material = current.copy(metallic = metallic, roughness = roughness)
    }
  }

  /** Updates the IBL environment intensity on the renderer. */
  fun setEnvIntensity(intensity: Float) {
    renderer.setEnvIntensity(intensity)
  }

  fun shutdown() {
    world.shutdown()
    engine.shutdown()
  }
}

/**
 * Creates the PBR sphere grid demo scene: a 7×7 grid of spheres with metallic varying on the X axis
 * (left = dielectric, right = metallic) and roughness varying on the Y axis (top = smooth, bottom =
 * rough). Two lights illuminate the scene; IBL with HDR tone mapping is enabled.
 *
 * @param wgpuContext The WebGPU context to render into.
 * @param width Initial surface width in pixels.
 * @param height Initial surface height in pixels.
 * @param surfacePreConfigured When true, the renderer skips its own surface configuration (used for
 *   AWT Canvas integration where the surface is pre-configured externally).
 */
fun createDemoScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
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

  // Camera — positioned to view the full 7×7 grid (spans ~9 units across)
  val cameraEntity = world.createEntity()
  val camera = Camera()
  camera.position = Vec3(0f, 0f, 12f)
  camera.target = Vec3.ZERO
  camera.fovY = 45f
  camera.aspectRatio = width.toFloat() / height.toFloat()
  camera.nearPlane = 0.1f
  camera.farPlane = 100f
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

  // Point light — cool white at upper-right for complementary specular highlights
  val pointLight = world.createEntity()
  world.addComponent(pointLight, TransformComponent(position = Vec3(5f, 5f, 5f)))
  world.addComponent(
    pointLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(0.8f, 0.9f, 1.0f),
      intensity = 80f,
      range = 20f,
    ),
  )

  // PBR sphere grid — metallic on X (col 0=dielectric → col 6=metallic),
  // roughness on Y (row 0=top=smooth → row 6=bottom=rough).
  val sphereMesh = Mesh.sphere()
  for (row in 0 until SPHERE_ROWS) {
    for (col in 0 until SPHERE_COLS) {
      val metallic = col.toFloat() / (SPHERE_COLS - 1).toFloat()
      val roughness = maxOf(0.04f, row.toFloat() / (SPHERE_ROWS - 1).toFloat())
      val x = (col - (SPHERE_COLS - 1) / 2f) * SPHERE_SPACING
      val y = ((SPHERE_ROWS - 1) / 2f - row) * SPHERE_SPACING

      val sphere = world.createEntity()
      world.addComponent(sphere, TransformComponent(position = Vec3(x, y, 0f)))
      world.addComponent(sphere, MeshComponent(mesh = sphereMesh))
      world.addComponent(
        sphere,
        MaterialComponent(
          material = Material(baseColor = Color.WHITE, metallic = metallic, roughness = roughness)
        ),
      )
    }
  }

  world.initialize()
  log.i { "PBR sphere grid initialized: ${SPHERE_COLS}×${SPHERE_ROWS} (${width}×${height})" }
  return DemoScene(engine, world, renderer, cameraEntity)
}
