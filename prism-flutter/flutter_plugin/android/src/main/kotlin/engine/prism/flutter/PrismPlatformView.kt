package engine.prism.flutter

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.flutter.PrismAndroidBridge
import com.hyeonslab.prism.widget.createPrismSurface
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val log = Logger.withTag("PrismPlatformView")

/**
 * Factory for creating [PrismPlatformView] instances from Flutter's platform view registry.
 */
class PrismPlatformViewFactory(
  private val bridge: PrismAndroidBridge<*, *>,
  private val onViewCreated: (PrismPlatformView) -> Unit,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

  override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
    val view = PrismPlatformView(context, bridge)
    onViewCreated(view)
    return view
  }
}

/**
 * Android platform view that hosts a [SurfaceView] with wgpu4k rendering.
 * Delegates all scene creation and frame ticking to [PrismAndroidBridge].
 */
class PrismPlatformView(
  context: Context,
  private val bridge: PrismAndroidBridge<*, *>,
) : PlatformView, SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val surfaceView = SurfaceView(context)

  private var surface: com.hyeonslab.prism.widget.PrismSurface? = null
  private var initJob: Job? = null
  private var running = false
  private var paused = false

  init {
    surfaceView.holder.addCallback(this)
  }

  override fun getView(): View = surfaceView

  // -- SurfaceHolder.Callback --

  override fun surfaceCreated(holder: SurfaceHolder) {
    log.i { "surfaceCreated" }
  }

  override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    log.i { "surfaceChanged: ${width}x${height}" }

    if (bridge.isInitialized) {
      bridge.onDimensionsChanged(width, height)
      return
    }
    if (initJob?.isActive == true) return

    initJob = scope.launch {
      val prismSurface = createPrismSurface(holder, width, height)
      surface = prismSurface

      val wgpuContext =
        checkNotNull(prismSurface.wgpuContext) { "wgpu context not available on Android" }

      bridge.onSurfaceReady(wgpuContext, width, height)

      running = true
      log.i { "Render loop starting" }
      Choreographer.getInstance().postFrameCallback(this@PrismPlatformView)
    }
  }

  override fun surfaceDestroyed(holder: SurfaceHolder) {
    log.i { "surfaceDestroyed" }
    stopRendering()
  }

  // -- Choreographer.FrameCallback --

  override fun doFrame(frameTimeNanos: Long) {
    if (!running || paused) return

    try {
      bridge.doFrame()
    } catch (e: Exception) {
      log.e(e) { "Render error — stopping render loop and releasing GPU resources" }
      stopRendering()
      return
    }

    Choreographer.getInstance().postFrameCallback(this)
  }

  // -- Pause/Resume for ActivityAware lifecycle --

  /** Pause the render loop (e.g. when the activity goes to background). */
  fun pauseRendering() {
    if (!running || paused) return
    paused = true
    Choreographer.getInstance().removeFrameCallback(this)
    log.i { "Render loop paused" }
  }

  /** Resume the render loop (e.g. when the activity returns to foreground). */
  fun resumeRendering() {
    if (!running || !paused) return
    paused = false
    bridge.resetFrameTiming()
    Choreographer.getInstance().postFrameCallback(this)
    log.i { "Render loop resumed" }
  }

  // -- PlatformView lifecycle --

  override fun dispose() {
    log.i { "dispose" }
    stopRendering()
    scope.cancel()
  }

  private fun stopRendering() {
    running = false
    paused = false
    initJob?.cancel()
    initJob = null
    Choreographer.getInstance().removeFrameCallback(this)
    bridge.shutdown()
    surface?.detach()
    surface = null
  }
}
