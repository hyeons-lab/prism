package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.WgpuRenderer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val log = Logger.withTag("DemoScene")

/** Holds the engine, ECS world, renderer, and camera entity for a demo scene. */
class DemoScene(
  val engine: Engine,
  val world: World,
  val renderer: WgpuRenderer,
  val cameraEntity: Entity,
  private var orbitRadius: Float = 12f,
) {
  private var orbitAzimuth = 0f
  private var orbitElevation = 0f

  /**
   * Work items to execute one per frame, used for progressive scene setup (e.g. adding entities one
   * at a time so the render loop can display progress between additions).
   */
  internal val pendingSetup: ArrayDeque<() -> Unit> = ArrayDeque()

  /**
   * Advances the scene by one frame: executes the next pending setup item (if any), then ticks the
   * ECS world.
   */
  fun tick(deltaTime: Float, elapsed: Float, frameCount: Long) {
    pendingSetup.removeFirstOrNull()?.invoke()
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    world.update(time)
  }

  /**
   * Updates the orbit radius and immediately recalculates the camera position. Call this when the
   * viewport size changes to ensure the scene content remains fully visible.
   */
  fun setOrbitRadius(radius: Float) {
    orbitRadius = radius
    orbitBy(0f, 0f)
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
   * Overrides metallic and roughness on all material entities. Called each frame from the render
   * loop to apply DemoStore slider values.
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
