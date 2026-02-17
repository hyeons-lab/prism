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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val log = Logger.withTag("PrismDemoActivity")

/** Android demo activity that renders a rotating lit cube using wgpu4k + Vulkan. */
class PrismDemoActivity : Activity(), SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  private var scene: DemoScene? = null
  private var surface: com.hyeonslab.prism.widget.PrismSurface? = null
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

    scene?.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)

    Choreographer.getInstance().postFrameCallback(this)
  }

  // -- Lifecycle --

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
