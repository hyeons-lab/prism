@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.widget

import kotlin.js.ExperimentalWasmJsInterop
import web.html.HTMLCanvasElement

// Internal JS interop primitives. These are implementation details of PrismSurface and WasmUtils —
// consumers interact only with the Kotlin API they power.

@JsFun(
  """(id) => {
  var el = document.getElementById(id);
  if (el && !(el instanceof HTMLCanvasElement)) {
    throw new Error('Element "' + id + '" is not a canvas element');
  }
  return el;
}"""
)
internal external fun jsGetCanvasById(id: String): HTMLCanvasElement?

@JsFun("(canvas, w, h) => { canvas.width = w; canvas.height = h; }")
internal external fun jsSetCanvasSize(canvas: HTMLCanvasElement, w: Int, h: Int)

@JsFun("() => window.innerWidth") internal external fun jsWindowWidth(): Int

@JsFun("() => window.innerHeight") internal external fun jsWindowHeight(): Int

/** Returns `performance.now()` in milliseconds. */
@JsFun("() => performance.now()") internal external fun jsNow(): Double

/** Schedules [callback] for the next animation frame via `requestAnimationFrame`. */
@JsFun("(callback) => requestAnimationFrame((t) => callback(t))")
internal external fun jsNextFrame(callback: (Double) -> Unit): JsAny

@JsFun("(callback) => window.addEventListener('beforeunload', callback)")
internal external fun jsOnBeforeUnload(callback: () -> Unit)

/**
 * Invokes `window.prismHideLoading()` if set. Embed pages assign this to dismiss a loading overlay
 * as soon as the first frame is rendered, without needing any consumer-side `@JsFun` glue.
 */
@JsFun("() => { var f = window.prismHideLoading; if (f) f(); }")
internal external fun jsNotifyFirstFrameReady()

/**
 * Installs pointer-capture drag listeners on [canvas]. Calls [onDelta] with (dx, dy) in CSS pixels
 * for each pointermove while a button is held.
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
internal external fun jsInstallPointerDrag(
  canvas: HTMLCanvasElement,
  onDelta: (Double, Double) -> Unit,
)

/**
 * Installs a ResizeObserver on [canvas]. When the canvas's CSS size changes, updates
 * canvas.width/height to match and calls [onResize] with the new pixel dimensions.
 */
@JsFun(
  """(canvas, onResize) => {
  const ro = new ResizeObserver(() => {
    const w = Math.floor(canvas.clientWidth);
    const h = Math.floor(canvas.clientHeight);
    if (w > 0 && h > 0) { canvas.width = w; canvas.height = h; onResize(w, h); }
  });
  ro.observe(canvas);
}"""
)
internal external fun jsInstallResizeObserver(
  canvas: HTMLCanvasElement,
  onResize: (Int, Int) -> Unit,
)
