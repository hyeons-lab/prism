@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsExport::class)

package com.hyeonslab.prism.flutter

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.createDemoScene
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.widget.PrismSurface
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

/** Per-canvas engine instance state. */
private class EngineInstance(
  val store: DemoStore,
  var scene: DemoScene? = null,
  var surface: PrismSurface? = null,
  var running: Boolean = false,
  var accumulatedAngle: Float = 0f,
  var startTime: Double = 0.0,
  var lastFrameTime: Double = 0.0,
  var frameCount: Long = 0L,
)

/** Active engine instances keyed by canvas element ID. */
private val instances = mutableMapOf<String, EngineInstance>()

/** WASM entry point — module is loaded, @JsExport functions become available. */
fun main() {
  log.i { "Prism Flutter WASM module loaded" }
}

/**
 * Initialize the Prism engine on the given canvas element by ID. Called from Dart via JS interop.
 *
 * Each canvas ID gets its own independent engine instance with its own DemoStore, DemoScene, and
 * render loop. Multiple canvases can run simultaneously without conflicting.
 */
@OptIn(DelicateCoroutinesApi::class)
@JsExport
fun prismInit(canvasId: String) {
  log.i { "Initializing on canvas '$canvasId'" }

  // Shut down any existing instance on this canvas before re-initializing.
  instances.remove(canvasId)?.let { old ->
    old.running = false
    old.scene?.shutdown()
    old.surface?.detach()
  }

  // Create instance with store before launching the coroutine so control methods
  // (setRotationSpeed, togglePause, setCubeColor) work immediately.
  val instance = EngineInstance(store = DemoStore())
  instances[canvasId] = instance

  val handler = CoroutineExceptionHandler { _, throwable ->
    log.e(throwable) { "Fatal error on '$canvasId': ${throwable.message}" }
    logError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    val canvas = getCanvasById(canvasId) ?: error("Canvas element '$canvasId' not found")
    val width = if (canvas.width > 0) canvas.width else 800
    val height = if (canvas.height > 0) canvas.height else 600

    val surface = createPrismSurface(canvas, width = width, height = height)
    instance.surface = surface
    val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }

    val demoScene =
      createDemoScene(
        wgpuContext,
        width = width,
        height = height,
        initialColor = instance.store.state.value.cubeColor,
      )
    instance.scene = demoScene

    instance.startTime = performanceNow()
    instance.lastFrameTime = instance.startTime
    instance.frameCount = 0L
    instance.accumulatedAngle = 0f
    instance.running = true

    log.i { "WebGPU initialized on '$canvasId' — starting render loop" }

    fun renderFrame(timestamp: Double) {
      if (!instance.running) return

      try {
        val deltaTime = ((timestamp - instance.lastFrameTime) / 1000.0).toFloat()
        instance.lastFrameTime = timestamp
        val elapsed = ((timestamp - instance.startTime) / 1000.0).toFloat()
        instance.frameCount++

        val state = instance.store.state.value

        // Update FPS (smoothed)
        if (deltaTime > 0f) {
          val smoothedFps = state.fps * 0.9f + (1f / deltaTime) * 0.1f
          instance.store.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }

        if (!state.isPaused) {
          val speedRadians = MathUtils.toRadians(state.rotationSpeed)
          instance.accumulatedAngle += speedRadians * deltaTime
        }

        // Update material color when it changes
        val cubeMaterial = demoScene.world.getComponent<MaterialComponent>(demoScene.cubeEntity)
        if (cubeMaterial != null && cubeMaterial.material?.baseColor != state.cubeColor) {
          cubeMaterial.material = Material(baseColor = state.cubeColor)
        }

        demoScene.tickWithAngle(
          deltaTime = if (state.isPaused) 0f else deltaTime,
          elapsed = elapsed,
          frameCount = instance.frameCount,
          angle = instance.accumulatedAngle,
        )

        requestAnimationFrame(::renderFrame)
      } catch (e: Throwable) {
        instance.running = false
        demoScene.shutdown()
        surface.detach()
        instances.remove(canvasId)
        log.e(e) { "Render loop error on '$canvasId': ${e.message}" }
        logError(e.message ?: "Render loop error")
      }
    }

    requestAnimationFrame(::renderFrame)
  }
}

/** Set rotation speed in degrees per second. */
@JsExport
fun prismSetRotationSpeed(canvasId: String, degreesPerSecond: Float) {
  instances[canvasId]?.store?.dispatch(DemoIntent.SetRotationSpeed(degreesPerSecond))
}

/** Toggle pause/resume. */
@JsExport
fun prismTogglePause(canvasId: String) {
  instances[canvasId]?.store?.dispatch(DemoIntent.TogglePause)
}

/** Set cube color (RGB, 0.0 to 1.0). */
@JsExport
fun prismSetCubeColor(canvasId: String, r: Float, g: Float, b: Float) {
  instances[canvasId]?.store?.dispatch(DemoIntent.SetCubeColor(Color(r, g, b)))
}

/** Get current state as a JS-friendly format. Returns JSON string. */
@JsExport
fun prismGetState(canvasId: String): String {
  val state = instances[canvasId]?.store?.state?.value ?: return "{}"
  val fps = state.fps.let { if (it.isFinite()) it else 0f }
  val speed = state.rotationSpeed.let { if (it.isFinite()) it else 0f }
  return """{"rotationSpeed":$speed,"isPaused":${state.isPaused},"fps":$fps}"""
}

/** Check if engine is initialized for the given canvas. */
@JsExport fun prismIsInitialized(canvasId: String): Boolean = instances[canvasId]?.scene != null

/** Shut down and release resources for the given canvas. */
@JsExport
fun prismShutdown(canvasId: String) {
  val instance = instances.remove(canvasId) ?: return
  instance.running = false
  instance.scene?.shutdown()
  instance.surface?.detach()
}
