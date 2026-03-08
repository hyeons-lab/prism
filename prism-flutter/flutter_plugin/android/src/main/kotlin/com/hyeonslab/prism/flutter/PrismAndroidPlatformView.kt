package com.hyeonslab.prism.flutter

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import co.touchlab.kermit.Logger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

private val log = Logger.withTag("PrismAndroidPlatformView")

class PrismAndroidPlatformViewFactory(
  private val onViewCreated: (PrismAndroidPlatformView) -> Unit,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

  override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
    val creationParams = args as? Map<*, *>
    val engineHandle = (creationParams?.get("engineHandle") as? Number)?.toLong() ?: 0L
    val view = PrismAndroidPlatformView(context, engineHandle)
    onViewCreated(view)
    return view
  }
}

class PrismAndroidPlatformView(
  context: Context,
  private val engineHandle: Long,
) : PlatformView, SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val surfaceView = SurfaceView(context)
  private var running = false
  private var paused = false
  private var attached = false

  init {
    surfaceView.holder.addCallback(this)
    log.i { "PrismAndroidPlatformView created with engineHandle: $engineHandle" }
  }

  override fun getView(): View = surfaceView

  // -- SurfaceHolder.Callback --

  override fun surfaceCreated(holder: SurfaceHolder) {
    log.i { "surfaceCreated" }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    log.i { "surfaceChanged: ${width}x${height}" }
    if (engineHandle != 0L) {
      if (!attached) {
        PrismAndroidNative.nAttachSurface(engineHandle, holder.surface, width, height)
        attached = true
      } else {
        PrismAndroidNative.nResize(engineHandle, width, height)
      }
      if (!running) {
        running = true
        Choreographer.getInstance().postFrameCallback(this)
      }
    }
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    log.i { "surfaceDestroyed" }
    stopRendering()
  }

  // -- Choreographer.FrameCallback --

  override fun doFrame(frameTimeNanos: Long) {
    if (!running || paused) return

    if (engineHandle != 0L) {
      try {
        PrismAndroidNative.nRenderFrame(engineHandle)
      } catch (e: Exception) {
        log.e(e) { "Render error — stopping render loop" }
        stopRendering()
        return
      }
    }

    Choreographer.getInstance().postFrameCallback(this)
  }

  fun pauseRendering() {
    if (!running || paused) return
    paused = true
    Choreographer.getInstance().removeFrameCallback(this)
    log.i { "Render loop paused" }
  }

  fun resumeRendering() {
    if (!running || !paused) return
    paused = false
    Choreographer.getInstance().postFrameCallback(this)
    log.i { "Render loop resumed" }
  }

  override fun dispose() {
    log.i { "dispose" }
    stopRendering()
  }

  private fun stopRendering() {
    running = false
    paused = false
    attached = false
    Choreographer.getInstance().removeFrameCallback(this)
    if (engineHandle != 0L) {
      PrismAndroidNative.nDetachSurface(engineHandle)
    }
  }
}
