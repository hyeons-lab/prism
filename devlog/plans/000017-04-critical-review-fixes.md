## Thinking

AI code review of the `feat/ffi-kmp-bindings` branch surfaced several real bugs in the
macOS bridge and demo code, plus two false positives that are documented here. The fixes
are ordered by severity (C = critical, M = moderate, m = minor).

Two review items were hallucinated:
- `PrismFlutterPlugin.kt` line 67 is correctly `handler = bundle.handler` — no typo.
- `AbstractFlutterMethodHandler.handleMethodCall` line 19 uses the Kotlin `else` keyword,
  not the string `"else"` — the routing is correct.

## Plan

### F1 — `attachMetalLayer` leaks new `MacosContext` when `createScene` throws (C-1)
Wrap the surface-configure + createScene block in try-catch; close `newCtx` on exception.

### F2 — `fread` return value not checked (C-3)
Capture `bytesRead` from `fread`; return `null` if `bytesRead.toLong() != size`.
Files: `DemoMacosMain.kt`, `DemoMacosBridge.kt`.

### F3 — `detachSurface` leaves stale `surfaceConfig` (C-2)
Add `surfaceConfig = null` in `PrismMetalBridge.detachSurface()`.

### F4 — GLFW callback globals: `@Volatile` + comment (C-4)
Add `@Volatile` to `lastMouseX`, `lastMouseY`, `mouseButtonDown` in `DemoMacosMain.kt`.
Add comment explaining GLFW synchronous dispatch model (no mutex needed).

### F5 — `PrismFlutterPlugin.swift` `fatalError` on missing bridge config (C-5)
Replace `fatalError` with `assertionFailure` + `return` guard.

### F6 — `attachScene`/`detachScene` are public (M-1)
Change both to `internal` in `PrismBridge.kt`.

### F7 — Surface Outdated caught by fragile string-matching ISE (M-2)
Add `SurfaceOutdatedException` to wgpu4k fork (commonMain). Update `getCurrentTexture()`
in `Surface.native.kt` to throw it when `status == SurfaceTextureStatus.outdated`.
No version bump; Prism catch-block changes deferred until fork is published (Phase B).

### F8 — `backgroundScope.cancel()` before `scene.shutdown()` (M-3)
Join all backgroundScope children via `runBlocking` before `cancel()` in both
`DemoMacosBridge.shutdown()` and `DemoAndroidBridge.shutdown()`.

### F9 — macOS `getState` returns fewer fields than Kotlin (M-4)
Deferred to Phase B (requires Swift protocol + SKIE export changes).

### F10 — `PrismMetalBridge.renderFrame` advances timing during pause (M-5)
Move `frameCount++` after `if (isPaused) return`. Also extract `FrameTimer` class
(bonus M-8) to eliminate the timing boilerplate duplicated across metal and android bridges.

### F11 — `PrismMacOSPlatformView.deinit` may run off main thread (M-7)
Dispatch `detachSurface()` to main thread in `deinit`.

### F12 — Minor fixes (m-1 through m-4)
- m-1: `PrismBridge.isInitialized` → property (update all callers)
- m-2: `FlutterMethodHandler.setRotationSpeed` → throw `IllegalArgumentException` on missing arg
- m-3: `DemoBridge.dispatchFps()` → removed (dead code)
- m-4: Sensitivity comment → fix to 628 pts/revolution

### New test: `AbstractFlutterMethodHandlerTest`
7 tests in `prism-flutter/commonTest` covering the dispatch contract of
`AbstractFlutterMethodHandler`.
