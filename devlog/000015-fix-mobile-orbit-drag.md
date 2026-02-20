# 000015 — fix/mobile-orbit-drag

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ prism branch fix/mobile-orbit-drag

## Intent

Fix mobile drag-to-orbit on the docs/index.html WebGPU demos. On mobile (iOS Safari, Android Chrome),
touching the demo canvas doesn't rotate the scene because `touch-action: none` is missing from the
canvas element.

## Decisions

2026-02-20T00:00-08:00 Fix in two places — (1) `docs/wasm/pbr.html` CSS for immediate effect without
a WASM rebuild; (2) `WasmInterop.kt` source so future WASM builds include the fix automatically.

2026-02-20T00:00-08:00 No WASM rebuild attempted — the HTML CSS fix alone covers the deployed
GitHub Pages site. The Kotlin source fix ensures correctness for any future rebuild.

## What Changed

2026-02-20T00:00-08:00 docs/wasm/pbr.html — added `touch-action: none` to canvas CSS rule so the
browser delivers pointer events immediately without scroll interception.

2026-02-20T00:00-08:00 prism-native-widgets/src/wasmJsMain/kotlin/com/hyeonslab/prism/widget/WasmInterop.kt —
added `canvas.style.touchAction = 'none'` at the top of `jsInstallPointerDrag` so all future WASM
builds set the property programmatically.

## Issues

Root cause: without `touch-action: none`, mobile browsers intercept touch events to handle
native scroll/zoom gestures. Even though `e.preventDefault()` is called in the `pointerdown`
listener, the browser may not deliver the event at all (or delay it) if it thinks the element
could scroll. `touch-action: none` declares up front that the element handles all touch input
itself, so pointer events fire immediately.

## Commits

HEAD — fix: enable mobile drag-to-orbit by adding touch-action: none to canvas
