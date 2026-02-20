@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.CanvasContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.canvasContextRenderer
import kotlin.js.ExperimentalWasmJsInterop
import web.html.HTMLCanvasElement

private val log = Logger.withTag("PrismSurface.WASM")

actual class PrismSurface(canvasContext: CanvasContext? = null, canvas: HTMLCanvasElement? = null) {
  private var _canvasContext: CanvasContext? = canvasContext
  private var _canvas: HTMLCanvasElement? = canvas
  private var _width: Int = 0
  private var _height: Int = 0
  private var _running: Boolean = false
  private var engine: Engine? = null

  /**
   * The WebGPU context created from the canvas. Available when constructed via
   * [createPrismSurface].
   */
  val wgpuContext: WGPUContext?
    get() = _canvasContext?.wgpuContext

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching" }
    _running = false
    _canvasContext?.close()
    _canvasContext = null
    _canvas = null
    engine = null
  }

  actual fun resize(width: Int, height: Int) {
    _width = width
    _height = height
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height

  /**
   * Installs a pointer-drag listener on the canvas. [onDelta] is called with (dx, dy) in CSS pixels
   * on each drag movement while a pointer button is held.
   */
  fun onPointerDrag(onDelta: (dx: Float, dy: Float) -> Unit) {
    val canvas = _canvas ?: return
    jsInstallPointerDrag(canvas) { dx, dy -> onDelta(dx.toFloat(), dy.toFloat()) }
  }

  /**
   * Installs a ResizeObserver on the canvas. [onResize] is called with the new (width, height) in
   * pixels whenever the canvas's CSS size changes. Also updates [PrismSurface.width]/[height].
   */
  fun onResize(onResize: (width: Int, height: Int) -> Unit) {
    val canvas = _canvas ?: return
    jsInstallResizeObserver(canvas) { w, h ->
      _width = w
      _height = h
      onResize(w, h)
    }
  }

  /**
   * Starts a `requestAnimationFrame` render loop. [onFrame] is called each frame with (deltaTime in
   * seconds, elapsed seconds since start, frameCount).
   *
   * [onFirstFrame] is called once after the very first rendered frame. Prism also automatically
   * invokes `window.prismHideLoading` (if set) at that point — embed pages can set this to dismiss
   * a loading overlay without writing any `@JsFun` glue code.
   *
   * The loop stops automatically when [detach] is called, including on page unload. If [onFrame]
   * throws, [onError] is invoked (default: log the error), the loop stops, and [detach] is called.
   */
  fun startRenderLoop(
    onError: (Throwable) -> Unit = { log.e(it) { "Render loop error: ${it.message}" } },
    onFirstFrame: (() -> Unit)? = null,
    onFrame: (deltaTime: Float, elapsed: Float, frameCount: Long) -> Unit,
  ) {
    if (_running) return
    _running = true
    jsOnBeforeUnload { detach() }

    val startTime = jsNow()
    var lastTime = startTime
    var frameCount = 0L
    var firstFrameFired = false

    fun frame(timestamp: Double) {
      if (!_running) return
      try {
        val deltaTime = ((timestamp - lastTime) / 1000.0).toFloat()
        lastTime = timestamp
        val elapsed = ((timestamp - startTime) / 1000.0).toFloat()
        frameCount++
        onFrame(deltaTime, elapsed, frameCount)
        if (!firstFrameFired) {
          firstFrameFired = true
          onFirstFrame?.invoke()
          jsNotifyFirstFrameReady()
        }
        jsNextFrame(::frame)
      } catch (e: Throwable) {
        detach()
        onError(e)
      }
    }

    jsNextFrame(::frame)
  }
}

/**
 * Creates a [PrismSurface] from the canvas element with [canvasId]. Sizes the canvas to fill the
 * window before creating the WebGPU context.
 */
suspend fun createPrismSurface(canvasId: String): PrismSurface {
  val canvas = jsGetCanvasById(canvasId) ?: error("Canvas element '$canvasId' not found")
  val width = jsWindowWidth().coerceAtLeast(1)
  val height = jsWindowHeight().coerceAtLeast(1)
  jsSetCanvasSize(canvas, width, height)
  return createPrismSurface(canvas, width, height)
}

/**
 * Creates a [PrismSurface] backed by [canvas] with explicitly specified initial dimensions. Use
 * [createPrismSurface] with a canvas ID for the common case.
 */
suspend fun createPrismSurface(canvas: HTMLCanvasElement, width: Int, height: Int): PrismSurface {
  log.i { "Creating WebGPU surface: ${width}x${height}" }
  val ctx = canvasContextRenderer(htmlCanvas = canvas, width = width, height = height)
  log.i { "wgpu surface ready (WASM)" }
  return PrismSurface(ctx, canvas).apply { resize(width, height) }
}
