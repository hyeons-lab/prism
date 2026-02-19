package engine.prism.flutter

import android.annotation.SuppressLint
import android.content.Context
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.createDemoScene
import com.hyeonslab.prism.demo.createGltfDemoScene
import com.hyeonslab.prism.flutter.PrismBridge
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
  private val bridge: PrismBridge,
  private val onViewCreated: (PrismPlatformView) -> Unit,
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

  override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
    val view = PrismPlatformView(context, bridge)
    onViewCreated(view)
    return view
  }
}

/**
 * Android platform view that hosts a [SurfaceView] with wgpu4k rendering. The glTF DamagedHelmet
 * model is loaded from Flutter assets. Drag-to-rotate is handled via an [OnTouchListener] on the
 * SurfaceView.
 */
@SuppressLint("ClickableViewAccessibility")
class PrismPlatformView(
  context: Context,
  private val bridge: PrismBridge,
) : PlatformView, SurfaceHolder.Callback, Choreographer.FrameCallback {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val surfaceView = SurfaceView(context)

  private var scene: com.hyeonslab.prism.demo.DemoScene? = null
  private var surface: com.hyeonslab.prism.widget.PrismSurface? = null
  private var initJob: Job? = null
  private var running = false
  private var paused = false

  private var startTimeNanos = 0L
  private var lastFrameTimeNanos = 0L
  private var frameCount = 0L

  // Drag-to-rotate state
  private var lastTouchX = 0f
  private var lastTouchY = 0f

  init {
    surfaceView.holder.addCallback(this)
    surfaceView.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          lastTouchX = event.x
          lastTouchY = event.y
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = event.x - lastTouchX
          val dy = event.y - lastTouchY
          lastTouchX = event.x
          lastTouchY = event.y
          scene?.orbitBy(dx * 0.005f, dy * 0.005f)
          true
        }
        else -> false
      }
    }
  }

  override fun getView(): View = surfaceView

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

    initJob = scope.launch {
      val prismSurface = createPrismSurface(holder, width, height)
      surface = prismSurface

      val wgpuContext =
        checkNotNull(prismSurface.wgpuContext) { "wgpu context not available on Android" }

      // Try to load the GLB from Flutter assets (stored at flutter_assets/ in the APK).
      val glbBytes =
        try {
          surfaceView.context.assets
            .open("flutter_assets/assets/DamagedHelmet.glb")
            .use { it.readBytes() }
        } catch (e: Exception) {
          log.w(e) { "DamagedHelmet.glb not found in assets — falling back to sphere-grid demo" }
          null
        }

      val demoScene =
        if (glbBytes != null) {
          createGltfDemoScene(wgpuContext, width, height, glbBytes)
        } else {
          createDemoScene(wgpuContext, width, height)
        }
      scene = demoScene
      bridge.attachScene(demoScene)

      startTimeNanos = System.nanoTime()
      lastFrameTimeNanos = startTimeNanos
      frameCount = 0L
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

    val deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
    lastFrameTimeNanos = frameTimeNanos
    val elapsed = (frameTimeNanos - startTimeNanos) / 1_000_000_000f
    frameCount++

    val currentScene = scene ?: return
    val state = bridge.state.value

    // Update FPS (smoothed EMA)
    if (deltaTime > 0f) {
      val smoothedFps = state.fps * 0.9f + (1f / deltaTime) * 0.1f
      bridge.store.dispatch(DemoIntent.UpdateFps(smoothedFps))
    }

    try {
      currentScene.tick(
        deltaTime = if (state.isPaused) 0f else deltaTime,
        elapsed = elapsed,
        frameCount = frameCount,
      )
    } catch (e: Exception) {
      log.e(e) {
        "Render error on frame $frameCount — stopping render loop and releasing GPU resources"
      }
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
    lastFrameTimeNanos = System.nanoTime()
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
    scene?.shutdown()
    scene = null
    bridge.detachScene()
    surface?.detach()
    surface = null
  }
}
