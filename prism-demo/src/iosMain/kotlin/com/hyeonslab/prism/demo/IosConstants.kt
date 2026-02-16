package com.hyeonslab.prism.demo

import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Material
import kotlinx.coroutines.sync.Mutex
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CACurrentMediaTime

/** Default surface dimensions used when MTKView's drawableSize is not yet computed at init time. */
internal const val IOS_DEFAULT_WIDTH = 800
internal const val IOS_DEFAULT_HEIGHT = 600

/** Shared [DemoStore] so Native and Compose tabs share pause, speed, and color state. */
internal val sharedDemoStore: DemoStore = DemoStore()

/**
 * Pause-aware elapsed-time tracker shared between Native and Compose tabs.
 *
 * All state is synchronized via a [Mutex] because both MTKView delegates may run on separate
 * display-link threads. The single [tick] method acquires the lock once per frame to sync pause
 * state, compute elapsed time, and compute the rotation angle atomically. When the lock is
 * contended, cached values from the previous frame are returned — at 60fps the worst case is a
 * single frame of staleness, which is imperceptible.
 */
internal object SharedDemoTime {
  private val mutex = Mutex()
  private var baseTime = CACurrentMediaTime()
  private var accumulatedElapsed = 0.0
  private var paused = false

  // Angle tracking — accumulates an offset when speed changes so the cube doesn't jump.
  private var angleOffset = 0.0
  private var elapsedAtSpeedChange = 0.0
  private var lastSpeed = Double.NaN

  // Cached last-known-good values returned when the lock is contended. At 60fps, a single
  // frame of staleness is imperceptible, so no @Volatile annotation is needed.
  private var cachedElapsed = 0.0
  private var cachedAngle = 0f

  private fun elapsedUnsafe(): Double {
    return if (paused) accumulatedElapsed
    else accumulatedElapsed + (CACurrentMediaTime() - baseTime)
  }

  /**
   * Combined per-frame tick: syncs pause state, computes elapsed time, and computes the rotation
   * angle — all atomically under a single lock acquisition. When the lock is contended, [block]
   * receives cached values from the previous frame instead.
   *
   * @param isPaused current pause state from the store
   * @param speedRadians rotation speed in radians per second
   * @param block receives (elapsed seconds, rotation angle in radians)
   */
  inline fun tick(isPaused: Boolean, speedRadians: Float, block: (Float, Float) -> Unit) {
    if (!mutex.tryLock()) {
      block(cachedElapsed.toFloat(), cachedAngle)
      return
    }
    try {
      // Sync pause state
      if (isPaused && !paused) {
        accumulatedElapsed += CACurrentMediaTime() - baseTime
        paused = true
      } else if (!isPaused && paused) {
        baseTime = CACurrentMediaTime()
        paused = false
      }

      // Compute elapsed
      val currentElapsed = elapsedUnsafe()

      // Compute angle (smooth speed transitions via offset accumulation)
      val speed = speedRadians.toDouble()
      if (lastSpeed.isNaN()) {
        lastSpeed = speed
        elapsedAtSpeedChange = currentElapsed
      } else if (speed != lastSpeed) {
        angleOffset += (currentElapsed - elapsedAtSpeedChange) * lastSpeed
        elapsedAtSpeedChange = currentElapsed
        lastSpeed = speed
      }
      val angle = (angleOffset + (currentElapsed - elapsedAtSpeedChange) * speed).toFloat()

      cachedElapsed = currentElapsed
      cachedAngle = angle
      block(currentElapsed.toFloat(), angle)
    } finally {
      mutex.unlock()
    }
  }
}

/**
 * Shared per-frame update logic for both Native and Compose iOS render delegates. Reads the current
 * [DemoStore] state, ticks [SharedDemoTime] for synchronized elapsed/angle values, updates the cube
 * transform and material, dispatches smoothed FPS, and runs the ECS world update.
 */
internal fun tickDemoFrame(scene: DemoScene, store: DemoStore, deltaTime: Float, frameCount: Long) {
  val currentState = store.state.value

  var elapsed = 0f
  var angle = 0f
  SharedDemoTime.tick(
    isPaused = currentState.isPaused,
    speedRadians = MathUtils.toRadians(currentState.rotationSpeed),
  ) { e, a ->
    elapsed = e
    angle = a
  }

  // Update rotation
  val cubeTransform = scene.world.getComponent<TransformComponent>(scene.cubeEntity)
  cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)

  // Update material color only when it actually changes to avoid per-frame allocation
  val cubeMaterial = scene.world.getComponent<MaterialComponent>(scene.cubeEntity)
  if (cubeMaterial != null && cubeMaterial.material?.baseColor != currentState.cubeColor) {
    cubeMaterial.material = Material(baseColor = currentState.cubeColor)
  }

  // Update FPS (smoothed) — dispatch on main queue for thread-safe Compose state updates
  if (deltaTime > 0f) {
    val smoothedFps = currentState.fps * 0.9f + (1f / deltaTime) * 0.1f
    NSOperationQueue.mainQueue.addOperationWithBlock {
      store.dispatch(DemoIntent.UpdateFps(smoothedFps))
    }
  }

  // Run ECS update (triggers RenderSystem)
  val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
  scene.world.update(time)
}
