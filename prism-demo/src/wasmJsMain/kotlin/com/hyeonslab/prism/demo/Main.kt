@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import io.ygdrasil.webgpu.canvasContextRenderer
import kotlin.math.PI
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
    val canvasContext = canvasContextRenderer(htmlCanvas = canvas, width = width, height = height)
    val scene = createDemoScene(canvasContext.wgpuContext, width = width, height = height)

    var running = true

    onBeforeUnload {
      if (!running) return@onBeforeUnload
      running = false
      scene.shutdown()
    }

    log.i { "WebGPU initialized — starting render loop" }

    val startTime = performanceNow()
    val rotationSpeed = PI.toFloat() / 4f
    var frameCount = 0L
    var lastFrameTime = startTime

    fun renderFrame(timestamp: Double) {
      if (!running) return
      try {
        val deltaTime = ((timestamp - lastFrameTime) / 1000.0).toFloat()
        lastFrameTime = timestamp
        val elapsed = ((timestamp - startTime) / 1000.0).toFloat()
        val angle = elapsed * rotationSpeed

        val cubeTransform = scene.world.getComponent<TransformComponent>(scene.cubeEntity)
        if (cubeTransform != null) {
          cubeTransform.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)
        }

        frameCount++
        val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
        scene.world.update(time)

        requestAnimationFrame(::renderFrame)
      } catch (e: Throwable) {
        running = false
        scene.shutdown()
        log.e(e) { "Render loop error: ${e.message}" }
        showError(e.message ?: "Render loop error")
      }
    }

    requestAnimationFrame(::renderFrame)
  }
}
