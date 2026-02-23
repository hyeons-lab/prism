# Plan: Code Review Fixes + Test Coverage

## Thinking

Critical review of changes landed this session (drag-to-orbit, Surface Outdated fixes, resize
support, Flutter demo cleanup). Simultaneously mapping untested critical paths so we can add
meaningful coverage.

Four bugs were identified in the review:
- `ctx` leaks if `attachMetalLayer` is called twice (layer recreation path).
- The `IllegalStateException` catch in `renderFrame` is too broad — it silently swallows any
  `IllegalStateException`, including real bugs in scene code.
- The GLFW demo leaks `scene` and `surface` on unexpected exception exit.
- `resize()` early-returns before `onResize()` when surface is not yet configured, so the first
  resize during startup is silently dropped.

For tests, three modules have zero coverage of critical paths:
- `prism-flutter/commonMain` — `PrismBridge` lifecycle
- `prism-flutter-demo/commonMain` — `FlutterMethodHandler` dispatch + `getState`
- `prism-flutter-demo/commonMain+macosMain` — FPS EMA smoothing, zoom clamping

All Phase 1 tests use pure logic (no platform APIs) so they compile for all targets via
`commonTest`.

## Plan

### A1 — ctx leak on re-attachment
Add `ctx?.close()` at the very top of `attachMetalLayer`, before early-returning on null ptr.
This ensures any previous wgpu context is released even if `layerPtr` is null this call.

### A2 — Broad `IllegalStateException` catch
Replace the catch body with a message-content check; rethrow anything that doesn't look like a
Surface Outdated error. Mirror the GLFW demo's log call.

### A3 — Render loop resource leak on unexpected exception
Wrap the `while` loop in `try/finally`; move `scene.shutdown()` and `surface.detach()` into the
`finally` block so they always execute. Rename file `MacosDemoMain.kt` → `DemoMacosMain.kt` while
touching it.

### A4 — `resize()` drops `onResize` before first configure
Replace `val config = surfaceConfig ?: return` with `surfaceConfig?.let { configure }`.
`onResize` is now unconditional — its null-safe `scene?.updateAspectRatio` call is already
safe when no scene is attached.

### B1 — FlutterMethodHandlerTest (commonTest, prism-flutter-demo)
Use `DemoBridge` (commonMain) as the bridge; no scene needed. Verify all domain dispatch
branches update `DemoStore` state; verify `getState` keys and values; verify unknown method
throws `MethodNotImplementedException`.

### B2 — PrismBridgeTest (commonTest, prism-flutter)
Use a hand-rolled `FakeStore` (implements `Store<Unit, Nothing>`) and `String` as scene type.
Tests: `isInitialized` before/after `attachScene`; `detachScene`; `shutdown` (including
double-shutdown and uninitialised-shutdown edge cases).

### B3 — FpsSmoothingTest (commonTest, prism-flutter-demo)
Mirror the EMA formula from `DemoMacosBridge.tickScene`/`DemoAndroidBridge.tickScene` in a
helper `tickFps(dt)` that writes directly to a `DemoStore`. Covers: cold start, convergence,
zero/negative dt guard, slow-frame damping, very-small dt spike.

### B4 — DemoInputTest (commonTest, prism-flutter-demo)
Extract and test the `coerceIn(2f, 40f)` zoom formula; verify min/max clamp, mid-range, and
boundary cases. Sign-preservation tests for `orbitBy` double→float cast.

### B5 — Replace placeholder Dart widget test
Use `debugDefaultTargetPlatformOverride = TargetPlatform.iOS` to prevent `AppKitView` /
`AndroidView` platform view creation (which requires a registered factory). Tests: fps chip text,
loading overlay presence, overlay persistence when `isInitialized` stays false.
