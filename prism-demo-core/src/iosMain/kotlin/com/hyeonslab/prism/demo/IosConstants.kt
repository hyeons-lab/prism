package com.hyeonslab.prism.demo

import kotlinx.coroutines.sync.Mutex
import platform.Foundation.NSOperationQueue
import platform.QuartzCore.CACurrentMediaTime

/** Default surface dimensions used when MTKView's drawableSize is not yet computed at init time. */
internal const val IOS_DEFAULT_WIDTH = 800
internal const val IOS_DEFAULT_HEIGHT = 600

/** Shared [DemoStore] so Native and Compose tabs share pause and state. */
internal val sharedDemoStore: DemoStore = DemoStore()

/**
 * Pause-aware elapsed-time tracker shared between Native and Compose tabs.
 *
 * All state is synchronized via a [Mutex] because both MTKView delegates may run on separate
 * display-link threads. The single [tick] method acquires the lock once per frame to sync pause
 * state and compute elapsed time atomically. When the lock is contended, the cached value from the
 * previous frame is returned — at 60fps the worst case is a single frame of staleness.
 */
internal object SharedDemoTime {
  private val mutex = Mutex()
  private var baseTime = CACurrentMediaTime()
  private var accumulatedElapsed = 0.0
  private var paused = false

  private var cachedElapsed = 0.0

  inline fun tick(isPaused: Boolean, block: (Float) -> Unit) {
    if (!mutex.tryLock()) {
      block(cachedElapsed.toFloat())
      return
    }
    try {
      if (isPaused && !paused) {
        accumulatedElapsed += CACurrentMediaTime() - baseTime
        paused = true
      } else if (!isPaused && paused) {
        baseTime = CACurrentMediaTime()
        paused = false
      }
      val currentElapsed =
        if (paused) accumulatedElapsed else accumulatedElapsed + (CACurrentMediaTime() - baseTime)
      cachedElapsed = currentElapsed
      block(currentElapsed.toFloat())
    } finally {
      mutex.unlock()
    }
  }
}

/**
 * Shared per-frame update logic for both Native and Compose iOS render delegates. Reads the current
 * [DemoStore] state, ticks [SharedDemoTime] for synchronized elapsed values, dispatches smoothed
 * FPS, and runs the ECS world update.
 */
internal fun tickDemoFrame(scene: DemoScene, store: DemoStore, deltaTime: Float, frameCount: Long) {
  val currentState = store.state.value

  var elapsed = 0f
  SharedDemoTime.tick(isPaused = currentState.isPaused) { e -> elapsed = e }

  // Update FPS (smoothed) — dispatch on main queue for thread-safe Compose state updates
  if (deltaTime > 0f) {
    val smoothedFps = currentState.fps * 0.9f + (1f / deltaTime) * 0.1f
    NSOperationQueue.mainQueue.addOperationWithBlock {
      store.dispatch(DemoIntent.UpdateFps(smoothedFps))
    }
  }

  scene.tick(
    deltaTime = if (currentState.isPaused) 0f else deltaTime,
    elapsed = elapsed,
    frameCount = frameCount,
  )
}
