@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
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

@JsFun(
  """(msg) => {
  console.error('Prism: ' + msg);
  var canvas = document.getElementById('prismCanvas');
  if (canvas) canvas.style.display = 'none';
  var fallback = document.getElementById('fallback');
  if (fallback) {
    fallback.style.display = 'block';
    fallback.textContent = 'Error: ' + msg;
  }
}"""
)
private external fun showError(message: String)

@JsFun("(callback) => window.addEventListener('beforeunload', callback)")
private external fun onBeforeUnload(callback: () -> Unit)

@JsFun("() => window.innerWidth") private external fun windowInnerWidth(): Int

@JsFun("() => window.innerHeight") private external fun windowInnerHeight(): Int

@JsFun("(canvas, w, h) => { canvas.width = w; canvas.height = h; }")
private external fun setCanvasSize(canvas: HTMLCanvasElement, w: Int, h: Int)

/**
 * Installs a ResizeObserver on [canvas]. When the canvas's CSS size changes, updates
 * [canvas.width]/[canvas.height] to match and calls [callback] with the new pixel dimensions.
 */
@JsFun(
  """(canvas, callback) => {
  const ro = new ResizeObserver(() => {
    const w = Math.floor(canvas.clientWidth);
    const h = Math.floor(canvas.clientHeight);
    if (w > 0 && h > 0) { canvas.width = w; canvas.height = h; callback(w, h); }
  });
  ro.observe(canvas);
}"""
)
private external fun observeCanvasResize(canvas: HTMLCanvasElement, callback: (Int, Int) -> Unit)

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

@JsFun(
  "(arr, i) => (arr[i] & 0xFF) | ((arr[i+1] & 0xFF) << 8) | ((arr[i+2] & 0xFF) << 16) | ((arr[i+3] & 0xFF) << 24)"
)
private external fun int8ArrayReadInt32LE(arr: JsAny, i: Int): Int

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

private val log = Logger.withTag("Prism")

private suspend fun fetchGlbBytes(url: String): ByteArray? = suspendCancellableCoroutine { cont ->
  fetchGlbJs(
    url,
    onSuccess = { jsArr ->
      val len = int8ArrayLength(jsArr)
      val bytes = ByteArray(len)
      val chunks = len / 4
      for (c in 0 until chunks) {
        val i = c * 4
        val v = int8ArrayReadInt32LE(jsArr, i)
        bytes[i] = (v and 0xFF).toByte()
        bytes[i + 1] = ((v ushr 8) and 0xFF).toByte()
        bytes[i + 2] = ((v ushr 16) and 0xFF).toByte()
        bytes[i + 3] = ((v ushr 24) and 0xFF).toByte()
      }
      for (i in chunks * 4 until len) {
        bytes[i] = int8ArrayByte(jsArr, i)
      }
      cont.resume(bytes)
    },
    onError = { error ->
      log.w { "GLB fetch failed: $error" }
      cont.resume(null)
    },
  )
}

@OptIn(DelicateCoroutinesApi::class)
fun main() {
  log.i { "Starting Prism WebGPU Demo..." }

  val store = DemoStore()

  val handler = CoroutineExceptionHandler { _, throwable ->
    log.e(throwable) { "Fatal error: ${throwable.message}" }
    showError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    val canvas = getCanvasById("prismCanvas") ?: error("Canvas element 'prismCanvas' not found")
    // Size the canvas to fill the window before creating the WebGPU surface.
    val width = windowInnerWidth().coerceAtLeast(1)
    val height = windowInnerHeight().coerceAtLeast(1)
    setCanvasSize(canvas, width, height)

    val surface = createPrismSurface(canvas, width = width, height = height)
    val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }

    val glbData = fetchGlbBytes("DamagedHelmet.glb")
    val scene =
      if (glbData != null) {
        log.i { "Loaded GLB (${glbData.size} bytes) — initializing glTF scene" }
        createGltfDemoScene(
          wgpuContext,
          width = width,
          height = height,
          glbData = glbData,
          progressiveScope = GlobalScope,
        )
      } else {
        log.w { "GLB not available — falling back to sphere-grid demo" }
        createDemoScene(wgpuContext, width = width, height = height)
      }

    addPointerDragListeners(canvas) { dx, dy ->
      scene.orbitBy(-dx.toFloat() * 0.005f, dy.toFloat() * 0.005f)
    }

    observeCanvasResize(canvas) { newWidth, newHeight ->
      scene.renderer.resize(newWidth, newHeight)
      scene.updateAspectRatio(newWidth, newHeight)
    }

    var running = true
    onBeforeUnload {
      if (!running) return@onBeforeUnload
      running = false
      scene.shutdown()
      surface.detach()
    }

    log.i { "WebGPU initialized — starting render loop" }

    val startTime = performanceNow()
    var lastFrameTime = startTime
    var frameCount = 0L

    fun renderFrame(timestamp: Double) {
      if (!running) return
      try {
        val deltaTime = ((timestamp - lastFrameTime) / 1000.0).toFloat()
        lastFrameTime = timestamp
        val elapsed = ((timestamp - startTime) / 1000.0).toFloat()
        frameCount++

        val state = store.state.value
        val effectiveDt = if (state.isPaused) 0f else deltaTime
        val rotRadPerSec = state.rotationSpeed * (PI.toFloat() / 180f)
        scene.orbitBy(rotRadPerSec * effectiveDt, 0f)
        scene.tick(deltaTime = effectiveDt, elapsed = elapsed, frameCount = frameCount)

        requestAnimationFrame(::renderFrame)
      } catch (e: Throwable) {
        running = false
        scene.shutdown()
        surface.detach()
        log.e(e) { "Render loop error: ${e.message}" }
        showError(e.message ?: "Render loop error")
      }
    }

    requestAnimationFrame(::renderFrame)
  }
}
