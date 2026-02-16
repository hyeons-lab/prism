package com.hyeonslab.prism.demo

import platform.QuartzCore.CACurrentMediaTime

/** Default surface dimensions used when MTKView's drawableSize is not yet computed at init time. */
internal const val IOS_DEFAULT_WIDTH = 800
internal const val IOS_DEFAULT_HEIGHT = 600

/** Shared [DemoStore] so Native and Compose tabs share pause, speed, and color state. */
internal val sharedDemoStore: DemoStore = DemoStore()

/**
 * Pause-aware elapsed-time tracker shared between Native and Compose tabs. Both delegates call
 * [syncPause] each frame and read [elapsed] for rotation angle computation, guaranteeing the cube
 * angle is identical across tabs.
 */
internal object SharedDemoTime {
  private var baseTime = CACurrentMediaTime()
  private var accumulatedElapsed = 0.0
  private var paused = false

  // Angle tracking — accumulates an offset when speed changes so the cube doesn't jump.
  private var angleOffset = 0.0
  private var elapsedAtSpeedChange = 0.0
  private var lastSpeed = Double.NaN

  /** Returns the current pause-aware elapsed time in seconds. */
  fun elapsed(): Double {
    return if (paused) accumulatedElapsed
    else accumulatedElapsed + (CACurrentMediaTime() - baseTime)
  }

  /**
   * Returns the current rotation angle in radians, smoothly handling speed changes. When
   * [speedRadians] changes, the current angle is captured as an offset so the cube continues from
   * its current position at the new speed instead of jumping.
   */
  fun angle(speedRadians: Float): Float {
    val currentElapsed = elapsed()
    val speed = speedRadians.toDouble()
    if (lastSpeed.isNaN()) {
      lastSpeed = speed
      elapsedAtSpeedChange = currentElapsed
    } else if (speed != lastSpeed) {
      angleOffset += (currentElapsed - elapsedAtSpeedChange) * lastSpeed
      elapsedAtSpeedChange = currentElapsed
      lastSpeed = speed
    }
    return (angleOffset + (currentElapsed - elapsedAtSpeedChange) * speed).toFloat()
  }

  /**
   * Synchronizes the pause state. Call each frame with the current [isPaused] value. On pause:
   * freezes elapsed time. On resume: resets the base time so elapsed continues from the frozen
   * value.
   */
  fun syncPause(isPaused: Boolean) {
    if (isPaused && !paused) {
      accumulatedElapsed += CACurrentMediaTime() - baseTime
      paused = true
    } else if (!isPaused && paused) {
      baseTime = CACurrentMediaTime()
      paused = false
    }
  }
}
