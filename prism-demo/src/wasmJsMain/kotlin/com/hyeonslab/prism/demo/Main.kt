@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
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

private val log = Logger.withTag("Prism")

@OptIn(DelicateCoroutinesApi::class)
fun main() {
  log.i { "Starting Prism WebGPU Demo..." }

  val handler = CoroutineExceptionHandler { _, throwable ->
    log.e(throwable) { "Fatal error: ${throwable.message}" }
    showError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    val canvas = getCanvasById("prismCanvas") ?: error("Canvas element 'prismCanvas' not found")
    val width = 800
    val height = 600
    val surface = createPrismSurface(canvas, width = width, height = height)
    val scene = createDemoScene(surface.wgpuContext!!, width = width, height = height)

    var running = true

    onBeforeUnload {
      if (!running) return@onBeforeUnload
      running = false
      scene.shutdown()
      surface.detach()
    }

    log.i { "WebGPU initialized — starting render loop" }

    val startTime = performanceNow()
    var frameCount = 0L
    var lastFrameTime = startTime

    fun renderFrame(timestamp: Double) {
      if (!running) return
      try {
        val deltaTime = ((timestamp - lastFrameTime) / 1000.0).toFloat()
        lastFrameTime = timestamp
        val elapsed = ((timestamp - startTime) / 1000.0).toFloat()

        frameCount++
        scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)

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
