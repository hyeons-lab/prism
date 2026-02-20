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
 * on each drag movement.
 *
 * On touch devices, single-finger touch scrolls the page normally (touch-action: pan-x pan-y). A
 * second finger activates orbit — [onDelta] is called only when 2+ touch pointers are active. Mouse
 * and pen input always orbit on any single pointer.
 *
 * A "Use two fingers to rotate" hint fades in briefly on first single touch.
 */
@JsFun(
  """(canvas, onDelta) => {
  canvas.style.touchAction = 'pan-x pan-y'; // single finger scrolls; 2 fingers orbit

  const hint = document.createElement('div');
  hint.textContent = 'Use two fingers to rotate';
  hint.style.cssText =
    'position:absolute;top:50%;left:50%;transform:translate(-50%,-50%);' +
    'background:rgba(0,0,0,0.52);color:#fff;font:13px/1 -apple-system,sans-serif;' +
    'padding:8px 16px;border-radius:20px;pointer-events:none;' +
    'opacity:0;transition:opacity 0.2s;white-space:nowrap;z-index:20;';
  (canvas.parentElement || document.body).appendChild(hint);
  let hintTimer = null;
  const showHint = () => {
    clearTimeout(hintTimer);
    hint.style.opacity = '1';
    hintTimer = setTimeout(() => { hint.style.opacity = '0'; }, 1400);
  };

  const touches = new Set(); // active touch pointer IDs
  let orbitId = null, lastX = 0, lastY = 0;

  canvas.addEventListener('pointerdown', e => {
    if (e.pointerType === 'touch') {
      touches.add(e.pointerId);
      if (touches.size === 1) { showHint(); return; } // single finger — let browser scroll
      if (touches.size >= 2 && orbitId === null) {    // second finger — start orbit
        orbitId = e.pointerId; lastX = e.clientX; lastY = e.clientY;
        canvas.setPointerCapture(e.pointerId); e.preventDefault();
      }
    } else {
      orbitId = e.pointerId; lastX = e.clientX; lastY = e.clientY;
      canvas.setPointerCapture(e.pointerId); e.preventDefault();
    }
  }, { passive: false });

  canvas.addEventListener('pointermove', e => {
    if (e.pointerId !== orbitId) return;
    onDelta(e.clientX - lastX, e.clientY - lastY);
    lastX = e.clientX; lastY = e.clientY;
    e.preventDefault();
  }, { passive: false });

  canvas.addEventListener('pointerup', e => {
    if (e.pointerType === 'touch') touches.delete(e.pointerId);
    if (e.pointerId === orbitId) {
      orbitId = null;
      try { canvas.releasePointerCapture(e.pointerId); } catch (_) {}
    }
  });
  canvas.addEventListener('pointercancel', e => {
    if (e.pointerType === 'touch') touches.delete(e.pointerId);
    if (e.pointerId === orbitId) orbitId = null;
  });
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
