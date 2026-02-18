@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsExport::class)

package com.hyeonslab.prism.flutter

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.createDemoScene
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.widget.createPrismSurface
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import web.html.HTMLCanvasElement

@JsFun(
  """(id) => {
  var el = document.getElementById(id);
  if (el && !(el instanceof HTMLCanvasElement)) {
    throw new Error('Element "' + id + '" is not a canvas element');
  }
  return el;
}"""
)
private external fun getCanvasById(id: String): HTMLCanvasElement?

@JsFun("(callback) => requestAnimationFrame((timestamp) => callback(timestamp))")
private external fun requestAnimationFrame(callback: (Double) -> Unit): JsAny

@JsFun("() => performance.now()") private external fun performanceNow(): Double

@JsFun("(msg) => console.error('Prism Flutter Web: ' + msg)")
private external fun logError(message: String)

private val log = Logger.withTag("PrismFlutterWeb")

/** WASM entry point — module is loaded, @JsExport functions become available. */
fun main() {
  log.i { "Prism Flutter WASM module loaded" }
}

private var scene: DemoScene? = null
private var store: DemoStore? = null
private var running = false
private var accumulatedAngle = 0f
private var startTime = 0.0
private var lastFrameTime = 0.0
private var frameCount = 0L

/**
 * Initialize the Prism engine on the given canvas element by ID. Called from Dart via JS interop.
 */
@OptIn(DelicateCoroutinesApi::class)
@JsExport
fun prismInit(canvasId: String) {
  log.i { "Initializing on canvas '$canvasId'" }

  val handler = CoroutineExceptionHandler { _, throwable ->
    log.e(throwable) { "Fatal error: ${throwable.message}" }
    logError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    val canvas = getCanvasById(canvasId) ?: error("Canvas element '$canvasId' not found")
    val width = 800
    val height = 600

    val surface = createPrismSurface(canvas, width = width, height = height)
    val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }

    val demoStore = DemoStore()
    store = demoStore

    val demoScene = createDemoScene(wgpuContext, width = width, height = height)
    scene = demoScene

    startTime = performanceNow()
    lastFrameTime = startTime
    frameCount = 0L
    accumulatedAngle = 0f
    running = true

    log.i { "WebGPU initialized — starting render loop" }

    fun renderFrame(timestamp: Double) {
      if (!running) return

      try {
        val deltaTime = ((timestamp - lastFrameTime) / 1000.0).toFloat()
        lastFrameTime = timestamp
        val elapsed = ((timestamp - startTime) / 1000.0).toFloat()
        frameCount++

        val state = demoStore.state.value

        // Update FPS (smoothed)
        if (deltaTime > 0f) {
          val smoothedFps = state.fps * 0.9f + (1f / deltaTime) * 0.1f
          demoStore.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }

        if (!state.isPaused) {
          val speedRadians = MathUtils.toRadians(state.rotationSpeed)
          accumulatedAngle += speedRadians * deltaTime
        }

        demoScene.tickWithAngle(
          deltaTime = if (state.isPaused) 0f else deltaTime,
          elapsed = elapsed,
          frameCount = frameCount,
          angle = accumulatedAngle,
        )

        requestAnimationFrame(::renderFrame)
      } catch (e: Throwable) {
        running = false
        demoScene.shutdown()
        surface.detach()
        log.e(e) { "Render loop error: ${e.message}" }
        logError(e.message ?: "Render loop error")
      }
    }

    requestAnimationFrame(::renderFrame)
  }
}

/** Set rotation speed in degrees per second. */
@JsExport
fun prismSetRotationSpeed(degreesPerSecond: Float) {
  store?.dispatch(DemoIntent.SetRotationSpeed(degreesPerSecond))
}

/** Toggle pause/resume. */
@JsExport
fun prismTogglePause() {
  store?.dispatch(DemoIntent.TogglePause)
}

/** Set cube color (RGB, 0.0 to 1.0). */
@JsExport
fun prismSetCubeColor(r: Float, g: Float, b: Float) {
  store?.dispatch(DemoIntent.SetCubeColor(Color(r, g, b)))
}

/** Get current state as a JS-friendly format. Returns JSON string. */
@JsExport
fun prismGetState(): String {
  val state = store?.state?.value ?: return "{}"
  return """{"rotationSpeed":${state.rotationSpeed},"isPaused":${state.isPaused},"fps":${state.fps}}"""
}

/** Check if engine is initialized. */
@JsExport fun prismIsInitialized(): Boolean = scene != null

/** Shut down and release resources. */
@JsExport
fun prismShutdown() {
  running = false
  scene?.shutdown()
  scene = null
  store = null
}
