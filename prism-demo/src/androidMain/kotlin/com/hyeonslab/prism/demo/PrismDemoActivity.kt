package com.hyeonslab.prism.demo

import android.app.Activity
import android.os.Bundle
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val log = Logger.withTag("PrismDemoActivity")

/** Android demo activity that renders a rotating lit cube using wgpu4k + Vulkan. */
class PrismDemoActivity : Activity(), SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private var scene: DemoScene? = null
  private var surface: com.hyeonslab.prism.widget.PrismSurface? = null
  private var initJob: Job? = null
  private var running = false

  private var startTimeNanos = 0L
  private var lastFrameTimeNanos = 0L
  private var frameCount = 0L

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    log.i { "onCreate" }
    val surfaceView = SurfaceView(this)
    surfaceView.holder.addCallback(this)
    setContentView(surfaceView)
  }

  // -- SurfaceHolder.Callback --

  override fun surfaceCreated(holder: SurfaceHolder) {
    log.i { "surfaceCreated" }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    log.i { "surfaceChanged: ${width}x${height}" }

    if (scene != null) {
      scene?.updateAspectRatio(width, height)
      return
    }
    if (initJob?.isActive == true) return

    initJob =
      scope.launch {
        val prismSurface = createPrismSurface(holder, width, height)
        surface = prismSurface

        val wgpuContext =
          checkNotNull(prismSurface.wgpuContext) { "wgpu context not available on Android" }
        val demoScene = createDemoScene(wgpuContext, width, height)
        scene = demoScene

        startTimeNanos = System.nanoTime()
        lastFrameTimeNanos = startTimeNanos
        frameCount = 0L
        running = true

        log.i { "Render loop starting" }
        Choreographer.getInstance().postFrameCallback(this@PrismDemoActivity)
      }
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    log.i { "surfaceDestroyed" }
    stopRendering()
  }

  // -- Choreographer.FrameCallback --

  override fun doFrame(frameTimeNanos: Long) {
    if (!running) return

    val deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
    lastFrameTimeNanos = frameTimeNanos
    val elapsed = (frameTimeNanos - startTimeNanos) / 1_000_000_000f
    frameCount++

    try {
      scene?.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
    } catch (e: Exception) {
      log.e(e) { "Render error on frame $frameCount" }
      running = false
      return
    }

    if (frameCount % 60 == 0L) log.d { "Frame $frameCount, fps=${1f / deltaTime}" }

    Choreographer.getInstance().postFrameCallback(this)
  }

  // -- Lifecycle --

  override fun onPause() {
    super.onPause()
    if (running) {
      running = false
      Choreographer.getInstance().removeFrameCallback(this)
      log.i { "onPause — render loop paused" }
    }
  }

  override fun onResume() {
    super.onResume()
    if (scene != null && !running) {
      running = true
      lastFrameTimeNanos = System.nanoTime()
      Choreographer.getInstance().postFrameCallback(this)
      log.i { "onResume — render loop resumed" }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    log.i { "onDestroy" }
    stopRendering()
    scope.cancel()
  }

  private fun stopRendering() {
    running = false
    Choreographer.getInstance().removeFrameCallback(this)
    scene?.shutdown()
    scene = null
    surface?.detach()
    surface = null
  }
}
