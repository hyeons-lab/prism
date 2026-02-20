@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import com.hyeonslab.prism.widget.fetchBytes
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val log = Logger.withTag("Prism")

/**
 * Returns the initial PBR scene name when a page sets `window.prismPbrScene` before loading this
 * module (e.g. pbr.html sets it to "hero" or "cornell"). Null when the page hasn't set it.
 */
@JsFun("() => window.prismPbrScene || null") private external fun getPbrSceneName(): String?

/**
 * Consumes and clears `window.prismNextScene`. The PBR render loop calls this each frame to detect
 * scene-switch requests set by pbr.html's `switchScene()` JS function.
 */
@JsFun("() => { var s = window.prismNextScene || null; window.prismNextScene = null; return s; }")
private external fun consumePendingSceneSwitch(): String?

@OptIn(DelicateCoroutinesApi::class)
fun main() {
  // Pages that embed the PBR demo (pbr.html) set window.prismPbrScene to the initial scene name.
  // Route to the PBR path instead of the glTF demo path.
  val pbrScene = getPbrSceneName()
  if (pbrScene != null) {
    val handler = CoroutineExceptionHandler { _, e -> log.e(e) { "PBR fatal: ${e.message}" } }
    GlobalScope.launch(handler) { startPbrScene("prismCanvas", pbrScene) }
    return
  }

  val handler = CoroutineExceptionHandler { _, throwable ->
    log.e(throwable) { "Fatal error: ${throwable.message}" }
    showFatalError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    // 1. Create a WebGPU surface from the HTML canvas element.
    val surface = createPrismSurface("prismCanvas")

    // 2. Load the glTF model (falls back to PBR sphere-grid if unavailable).
    val glbData = fetchBytes("DamagedHelmet.glb")
    val scene =
      if (glbData != null) {
        createGltfDemoScene(
          surface.wgpuContext!!,
          surface.width,
          surface.height,
          glbData,
          progressiveScope = GlobalScope,
        )
      } else {
        createDemoScene(surface.wgpuContext!!, surface.width, surface.height)
      }

    // 3. Drag to orbit the camera.
    surface.onPointerDrag { dx, dy -> scene.orbitBy(-dx * 0.005f, dy * 0.005f) }

    // 4. Keep rendering resolution in sync with CSS layout.
    surface.onResize { w, h ->
      scene.renderer.resize(w, h)
      scene.updateAspectRatio(w, h)
    }

    // 5. Start the render loop (auto-stops on page unload via surface.detach()).
    surface.startRenderLoop(onError = { e -> showFatalError(e.message ?: "Render loop error") }) {
      dt,
      elapsed,
      frame ->
      scene.tick(dt, elapsed, frame)
    }
  }
}

/**
 * Starts a PBR demo scene on [canvasId]. Each frame, checks `window.prismNextScene` for a
 * scene-switch request. When one is detected the current surface is torn down and a new coroutine
 * restarts this function with the requested scene name.
 */
@OptIn(DelicateCoroutinesApi::class)
private suspend fun startPbrScene(canvasId: String, sceneName: String) {
  log.i { "Starting PBR scene '$sceneName'" }
  val surface = createPrismSurface(canvasId)
  val ctx = checkNotNull(surface.wgpuContext) { "WebGPU context not available" }

  val scene =
    when (sceneName) {
      "cornell" -> createCornellBoxScene(ctx, surface.width, surface.height)
      else -> createMaterialPresetScene(ctx, surface.width, surface.height)
    }

  surface.onPointerDrag { dx, dy -> scene.orbitBy(-dx * 0.005f, dy * 0.005f) }
  surface.onResize { nw, nh ->
    scene.renderer.resize(nw, nh)
    scene.updateAspectRatio(nw, nh)
  }

  surface.startRenderLoop(onError = { e -> log.e(e) { "PBR render loop error: ${e.message}" } }) {
    dt,
    elapsed,
    frame ->
    scene.tick(dt, elapsed, frame)

    // Poll for a scene-switch request from pbr.html's switchScene() JS function.
    val next = consumePendingSceneSwitch()
    if (next != null) {
      surface.detach()
      val handler = CoroutineExceptionHandler { _, e ->
        log.e(e) { "PBR scene switch error: ${e.message}" }
      }
      GlobalScope.launch(handler) { startPbrScene(canvasId, next) }
    }
  }
}

@JsFun(
  """(msg) => {
  console.error('Prism: ' + msg);
  var canvas = document.getElementById('prismCanvas');
  if (canvas) canvas.style.display = 'none';
  var fallback = document.getElementById('fallback');
  if (fallback) { fallback.style.display = 'block'; fallback.textContent = 'Error: ' + msg; }
}"""
)
private external fun showFatalError(message: String)
