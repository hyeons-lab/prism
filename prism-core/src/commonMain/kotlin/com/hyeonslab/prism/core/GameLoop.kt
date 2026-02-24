package com.hyeonslab.prism.core

class GameLoop {
  var isRunning: Boolean = false
    private set

  var fixedTimeStep: Float = 1f / 60f

  var onUpdate: ((Time) -> Unit)? = null
  var onFixedUpdate: ((Time) -> Unit)? = null
  var onRender: ((Time) -> Unit)? = null

  /**
   * Optional single-fire callback invoked at the start of [stop], before the loop is marked as
   * stopped. The callback is cleared immediately after it fires to prevent re-entrancy.
   *
   * Use this to release resources that must be freed while the render surface is still alive (e.g.
   * scene GPU resource cleanup) before the platform closes the surface after [stop] returns.
   */
  var onStop: (() -> Unit)? = null

  private var accumulator: Float = 0f
  private var lastTimeMillis: Long = 0L
  private var totalTime: Float = 0f
  private var frameCount: Long = 0L

  fun start() {
    if (isRunning) return
    isRunning = true
    accumulator = 0f
    totalTime = 0f
    frameCount = 0L
    lastTimeMillis = Platform.currentTimeMillis()
    loop()
  }

  fun stop() {
    val cb = onStop
    onStop = null // clear before invoking to prevent re-entrancy if stop() is called recursively
    cb?.invoke()
    isRunning = false
  }

  private fun loop() {
    while (isRunning) {
      val currentTimeMillis = Platform.currentTimeMillis()
      val elapsedMillis = currentTimeMillis - lastTimeMillis
      lastTimeMillis = currentTimeMillis

      // Convert to seconds and clamp to avoid spiral of death
      val deltaTime = (elapsedMillis / 1000f).coerceAtMost(0.25f)
      totalTime += deltaTime
      frameCount++

      val time =
        Time(
          deltaTime = deltaTime,
          totalTime = totalTime,
          frameCount = frameCount,
          fixedDeltaTime = fixedTimeStep,
        )

      // Accumulate time for fixed updates
      accumulator += deltaTime

      // Run fixed updates at a consistent rate
      while (accumulator >= fixedTimeStep) {
        val fixedTime = time.copy(deltaTime = fixedTimeStep)
        onFixedUpdate?.invoke(fixedTime)
        accumulator -= fixedTimeStep
      }

      // Variable-rate update
      onUpdate?.invoke(time)

      // Render
      onRender?.invoke(time)
    }
  }

  /**
   * Advances the loop by a single frame. Useful for platforms that manage their own run-loop (e.g.,
   * browser requestAnimationFrame).
   */
  fun tick() {
    if (!isRunning) return

    val currentTimeMillis = Platform.currentTimeMillis()
    val elapsedMillis = currentTimeMillis - lastTimeMillis
    lastTimeMillis = currentTimeMillis

    val deltaTime = (elapsedMillis / 1000f).coerceAtMost(0.25f)
    totalTime += deltaTime
    frameCount++

    val time =
      Time(
        deltaTime = deltaTime,
        totalTime = totalTime,
        frameCount = frameCount,
        fixedDeltaTime = fixedTimeStep,
      )

    accumulator += deltaTime

    while (accumulator >= fixedTimeStep) {
      val fixedTime = time.copy(deltaTime = fixedTimeStep)
      onFixedUpdate?.invoke(fixedTime)
      accumulator -= fixedTimeStep
    }

    onUpdate?.invoke(time)
    onRender?.invoke(time)
  }

  /**
   * Prepares the loop for external tick-based usage without entering the internal while-loop. Call
   * [tick] each frame after this.
   */
  fun startExternal() {
    if (isRunning) return
    isRunning = true
    accumulator = 0f
    totalTime = 0f
    frameCount = 0L
    lastTimeMillis = Platform.currentTimeMillis()
  }
}
