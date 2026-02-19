@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsExport::class)

package com.hyeonslab.prism.flutter

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.createDemoScene
import com.hyeonslab.prism.demo.createGltfDemoScene
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

/**
 * Attaches pointer-drag listeners to [canvas] for rotate-camera control. Calls [onDelta] with
 * (deltaX, deltaY) in CSS pixels on each pointermove while a pointer button is held.
 */
@JsFun(
  """(canvas, onDelta) => {
  let active = false, lastX = 0, lastY = 0;
  canvas.addEventListener('pointerdown', e => {
    active = true; lastX = e.clientX; lastY = e.clientY;
    canvas.setPointerCapture(e.pointerId); e.preventDefault();
  }, { passive: false });
  canvas.addEventListener('pointermove', e => {
    if (!active) return;
    onDelta(e.clientX - lastX, e.clientY - lastY);
    lastX = e.clientX; lastY = e.clientY; e.preventDefault();
  }, { passive: false });
  canvas.addEventListener('pointerup',     () => { active = false; });
  canvas.addEventListener('pointercancel', () => { active = false; });
}"""
)
private external fun addPointerDragListeners(
  canvas: HTMLCanvasElement,
  onDelta: (Double, Double) -> Unit,
)

@JsFun("(msg) => console.error('Prism Flutter Web: ' + msg)")
private external fun logError(message: String)

@JsFun(
  """(url, onSuccess, onError) => {
  fetch(url)
    .then(r => r.arrayBuffer())
    .then(buf => onSuccess(new Int8Array(buf)))
    .catch(e => onError(String(e)));
}"""
)
private external fun fetchGlbJs(url: String, onSuccess: (JsAny) -> Unit, onError: (String) -> Unit)

@JsFun("(arr) => arr.length") private external fun int8ArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]") private external fun int8ArrayByte(arr: JsAny, i: Int): Byte

private val log = Logger.withTag("PrismFlutterWeb")

/** Per-canvas engine instance state. */
private class EngineInstance(
  val store: DemoStore,
  var scene: DemoScene? = null,
  var surface: PrismSurface? = null,
  var running: Boolean = false,
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
 * Fetches a binary resource via JS fetch and returns its bytes. Returns null on failure. Sensitive
 * to same-origin policy — relative URLs are resolved against the page origin.
 */
private suspend fun fetchGlbBytes(url: String): ByteArray? = suspendCoroutine { cont ->
  fetchGlbJs(
    url,
    onSuccess = { jsArr ->
      val len = int8ArrayLength(jsArr)
      val bytes = ByteArray(len) { i -> int8ArrayByte(jsArr, i) }
      cont.resume(bytes)
    },
    onError = { error ->
      log.w { "GLB fetch failed: $error" }
      cont.resume(null)
    },
  )
}

/**
 * Initialize the Prism engine on the given canvas element by ID. Called from Dart via JS interop.
 *
 * Fetches [glbUrl] (relative to the page root) and renders the glTF model; falls back to the PBR
 * sphere-grid demo if loading fails.
 */
@OptIn(DelicateCoroutinesApi::class)
@JsExport
fun prismInit(canvasId: String, glbUrl: String = "DamagedHelmet.glb") {
  log.i { "Initializing on canvas '$canvasId'" }

  // Shut down any existing instance on this canvas before re-initializing.
  instances.remove(canvasId)?.let { old ->
    old.running = false
    old.scene?.shutdown()
    old.surface?.detach()
  }

  // Create instance with store before launching the coroutine so control methods work immediately.
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

    // Fetch the GLB and build the glTF scene; fall back to sphere-grid on failure.
    val glbData = fetchGlbBytes(glbUrl)
    val demoScene =
      if (glbData != null) {
        log.i { "Loaded GLB (${glbData.size} bytes) — initializing glTF scene" }
        createGltfDemoScene(wgpuContext, width = width, height = height, glbData = glbData)
      } else {
        log.w { "GLB not available — falling back to sphere-grid demo" }
        createDemoScene(wgpuContext, width = width, height = height)
      }
    instance.scene = demoScene

    // Drag-to-rotate: pointer drag on the canvas.
    // dx/dy are in CSS pixels; multiply by sensitivity (radians per pixel).
    addPointerDragListeners(canvas) { dx, dy ->
      demoScene.orbitBy(dx.toFloat() * 0.005f, dy.toFloat() * 0.005f)
    }

    instance.startTime = performanceNow()
    instance.lastFrameTime = instance.startTime
    instance.frameCount = 0L
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

        demoScene.tick(
          deltaTime = if (state.isPaused) 0f else deltaTime,
          elapsed = elapsed,
          frameCount = instance.frameCount,
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

/** Toggle pause/resume. */
@JsExport
fun prismTogglePause(canvasId: String) {
  instances[canvasId]?.store?.dispatch(DemoIntent.TogglePause)
}

/** Get current state as a JSON string. */
@JsExport
fun prismGetState(canvasId: String): String {
  val state = instances[canvasId]?.store?.state?.value ?: return "{}"
  val fps = state.fps.let { if (it.isFinite()) it else 0f }
  return """{"isPaused":${state.isPaused},"fps":$fps}"""
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
