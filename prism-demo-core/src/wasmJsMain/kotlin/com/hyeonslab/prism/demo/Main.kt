@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalJsExport::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import com.hyeonslab.prism.widget.fetchBytes
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.tan
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

private val log = Logger.withTag("Prism")

/** Cached GLB bytes — populated on first glTF scene load, reused on subsequent switches. */
private var cachedGlbData: ByteArray? = null

/**
 * Pending scene switch set by pbr.html calling `globalThis["prism-demo-core"].prismSwitchScene`.
 * Written from JS (event-driven on button click); read + cleared by the render loop each frame with
 * no JS interop crossing.
 */
private var pendingSceneSwitch: String? = null

/**
 * Exported entry point callable from JS — pbr.html calls
 * `globalThis["prism-demo-core"].prismSwitchScene(name)` on tab click. The active render loop reads
 * [pendingSceneSwitch] once at the end of the current frame and restarts with the new scene. No
 * per-frame JS interop is needed.
 */
@JsExport
fun prismSwitchScene(name: String) {
  pendingSceneSwitch = name
}

/**
 * Returns the initial PBR scene name when a page sets `window.prismPbrScene` before loading this
 * module (e.g. pbr.html sets it to "hero" or "cornell"). Null when the page hasn't set it.
 */
@JsFun("() => window.prismPbrScene || null") private external fun getPbrSceneName(): String?

/** Updates the FPS counter element in pbr.html with the current smoothed frame rate. */
@JsFun(
  "(fps) => { var el = document.getElementById('fps-counter'); if (el) el.textContent = fps + ' fps'; }"
)
private external fun updateFpsDisplay(fps: Int)

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
 * Computes the orbit radius (camera distance) for the Material Preset scene so that all 5 spheres
 * fit horizontally across the viewport with a small margin, regardless of aspect ratio.
 *
 * The 5 spheres span ±3.5 units in X (including sphere radius). With fovY = 45° and the given
 * aspect ratio, the required distance is `halfSpan / (tan(fovY/2) * aspect)`.
 */
private fun materialPresetOrbitRadius(width: Int, height: Int): Float {
  val aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
  // halfSpan = 3.5 (sphere edges) + 0.5 margin = 4.0 units from centre
  val halfSpan = 4.0f
  // tan(fovY/2) for fovY = 45°
  val tanHalfFov = tan(PI / 8.0).toFloat()
  return (halfSpan / (tanHalfFov * aspect)).coerceIn(5f, 20f)
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
      else -> {
        val s =
          createMaterialPresetScene(
            ctx,
            surface.width,
            surface.height,
            progressiveScope = GlobalScope,
          )
        // Override default orbit radius with a viewport-aware value so all 5 spheres are visible.
        s.setOrbitRadius(materialPresetOrbitRadius(surface.width, surface.height))
        s
      }
    }

  surface.onPointerDrag { dx, dy -> scene.orbitBy(-dx * 0.005f, dy * 0.005f) }
  surface.onResize { nw, nh ->
    scene.renderer.resize(nw, nh)
    scene.updateAspectRatio(nw, nh)
    if (sceneName != "cornell") {
      scene.setOrbitRadius(materialPresetOrbitRadius(nw, nh))
    }
  }

  var smoothedFps = 60f
  surface.startRenderLoop(
    onError = { e -> log.e(e) { "PBR render loop error: ${e.message}" } },
    onFirstFrame = { log.i { "PBR first frame rendered" } },
  ) { dt, elapsed, frame ->
    scene.tick(dt, elapsed, frame)

    // Smoothed FPS — EMA with α=0.05; updated in the DOM every 20 frames (~3 Hz at 60 fps).
    if (frame > 0L && dt > 0f) {
      smoothedFps = 0.05f * (1f / dt) + 0.95f * smoothedFps
      if (frame % 20L == 0L) updateFpsDisplay(smoothedFps.roundToInt())
    }

    // Check for a scene-switch request (set event-driven by prismSwitchScene(); no JS interop).
    val next = pendingSceneSwitch
    if (next != null) {
      pendingSceneSwitch = null
      surface.detach()
      val handler = CoroutineExceptionHandler { _, e ->
        log.e(e) { "Scene switch error: ${e.message}" }
      }
      GlobalScope.launch(handler) {
        if (next == "gltf") startGltfScene(canvasId) else startPbrScene(canvasId, next)
      }
    }
  }
}

/**
 * Starts the glTF DamagedHelmet scene on [canvasId]. The GLB file is fetched lazily on first call
 * and cached for subsequent scene switches. Each frame, checks `window.prismNextScene` for a
 * scene-switch request back to a PBR scene.
 */
@OptIn(DelicateCoroutinesApi::class)
private suspend fun startGltfScene(canvasId: String) {
  log.i { "Starting glTF scene (DamagedHelmet)" }
  val surface = createPrismSurface(canvasId)
  val ctx = checkNotNull(surface.wgpuContext) { "WebGPU context not available" }

  if (cachedGlbData == null) {
    cachedGlbData = fetchBytes("DamagedHelmet.glb")
  }
  val scene =
    if (cachedGlbData != null) {
      createGltfDemoScene(
        ctx,
        surface.width,
        surface.height,
        cachedGlbData!!,
        progressiveScope = GlobalScope,
      )
    } else {
      createDemoScene(ctx, surface.width, surface.height)
    }

  surface.onPointerDrag { dx, dy -> scene.orbitBy(-dx * 0.005f, dy * 0.005f) }
  surface.onResize { w, h ->
    scene.renderer.resize(w, h)
    scene.updateAspectRatio(w, h)
  }

  var smoothedFps = 60f
  surface.startRenderLoop(
    onError = { e -> log.e(e) { "glTF render loop error: ${e.message}" } },
    onFirstFrame = { log.i { "glTF first frame rendered" } },
  ) { dt, elapsed, frame ->
    scene.tick(dt, elapsed, frame)

    // Smoothed FPS — EMA with α=0.05; updated in the DOM every 20 frames (~3 Hz at 60 fps).
    if (frame > 0L && dt > 0f) {
      smoothedFps = 0.05f * (1f / dt) + 0.95f * smoothedFps
      if (frame % 20L == 0L) updateFpsDisplay(smoothedFps.roundToInt())
    }

    // Check for a scene-switch request (set event-driven by prismSwitchScene(); no JS interop).
    val next = pendingSceneSwitch
    if (next != null) {
      pendingSceneSwitch = null
      surface.detach()
      val handler = CoroutineExceptionHandler { _, e ->
        log.e(e) { "Scene switch error: ${e.message}" }
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
