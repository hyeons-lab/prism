# 000020 — feat/ios-ffi-bridge-prismview

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ prism feat/ios-ffi-bridge-prismview
**Intent:** Implement iOS FFI bridge in `prism-native`, add `onSurfaceReady` callback to `PrismView`, implement the apple actual (iOS) with UIKitView + MTKView, update Flutter iOS plugin to pass MTKView pointer, and refactor `ComposeIosEntry.kt` to use `PrismView`.

---

## Progress

- [x] Part 1: iOS FFI bridge (`prism-native/src/iosMain/IosBridge.kt` + build.gradle.kts)
- [x] Part 2a: `PrismView` expect + all actuals — add `onSurfaceReady` parameter
- [x] Part 2b: `PrismView.ios.kt` — full UIKitView + MTKViewDelegateProtocol implementation
- [x] Part 2c: `PrismView.macos.kt` — macOS stub with updated signature
- [x] Part 3a: Refactor `ComposeIosEntry.kt` to use `PrismView(onSurfaceReady = ...)`
- [x] Part 3b: Update `PrismFlutterPlugin.swift` to pass MTKView pointer

---

## What Changed

- `2026-02-23T13:07-08:00` `prism-native/build.gradle.kts` — Added `iosMain.dependencies { wgpu4k.toolkit + coroutines }` inside `if (isMac)` block; also added `iosArm64()` + `iosSimulatorArm64()` already present via `if (isMac)` targets.
- `2026-02-23T13:07-08:00` `prism-native/src/iosMain/kotlin/engine/prism/native/IosBridge.kt` — **NEW** iOS FFI bridge exporting `prism_attach_metal_layer`, `prism_render_frame`, `prism_resize`, `prism_detach_surface` via `@CName`. Uses `interpretObjCPointerOrNull<MTKView>` to reconstruct the MTKView from opaque C pointer, then `iosContextRenderer(mtkView, w, h)` to create a wgpu `IosContext`. Clear-color render pass per frame.
- `2026-02-23T13:07-08:00` `prism-compose/src/nonNativeMain/kotlin/com/hyeonslab/prism/compose/PrismView.kt` — Added `onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null` parameter to `expect fun PrismView`.
- `2026-02-23T13:07-08:00` `prism-compose/src/appleMain/kotlin/com/hyeonslab/prism/compose/PrismView.apple.kt` — Removed `actual` declaration; now empty (actuals moved to iosMain + macosMain).
- `2026-02-23T13:07-08:00` `prism-compose/src/iosMain/kotlin/com/hyeonslab/prism/compose/PrismView.ios.kt` — **NEW** Full iOS actual: `UIKitView(MTKView)` + `MTKViewDelegateProtocol` drives `gameLoop.tick()`, calls `onSurfaceReady` once after surface init, dispatches `FrameTick` + `SurfaceResized` events on main queue.
- `2026-02-23T13:07-08:00` `prism-compose/src/macosMain/kotlin/com/hyeonslab/prism/compose/PrismView.macos.kt` — **NEW** macOS stub actual; logs warning, no-op (macOS Compose/Metal not yet implemented).
- `2026-02-23T13:07-08:00` `prism-compose/src/jvmMain/kotlin/com/hyeonslab/prism/compose/PrismView.jvm.kt` — Added `onSurfaceReady` parameter; invoked after JVM surface is configured.
- `2026-02-23T13:07-08:00` `prism-compose/src/androidMain/kotlin/com/hyeonslab/prism/compose/PrismView.android.kt` — Added `onSurfaceReady` parameter; invoked after Android surface and wgpu context are ready.
- `2026-02-23T13:07-08:00` `prism-compose/src/wasmJsMain/kotlin/com/hyeonslab/prism/compose/PrismView.wasmJs.kt` — Added `onSurfaceReady` parameter (stub — WASM doesn't call it yet).
- `2026-02-23T13:07-08:00` `prism-demo-core/src/iosMain/kotlin/com/hyeonslab/prism/demo/ComposeIosEntry.kt` — Refactored from manual `UIKitView + ComposeRenderDelegate` to `PrismView(onSurfaceReady = { ctx, w, h -> ... })`. Scene init in `LaunchedEffect(surfaceCtx)`; per-frame logic via `engineStore.engine.gameLoop.onRender`.
- `2026-02-23T13:07-08:00` `prism-flutter/flutter_plugin/ios/prism_flutter/Sources/prism_flutter/PrismFlutterPlugin.swift` — Changed from `CAMetalLayer` sublayer to embedded `MTKView` subview; passes `Unmanaged.passUnretained(mtkView).toOpaque()` pointer to `prism_attach_metal_layer`.

---

## Decisions

- `2026-02-23T13:07-08:00` Split `appleMain/PrismView.apple.kt` into `iosMain/PrismView.ios.kt` (full UIKitView implementation) and `macosMain/PrismView.macos.kt` (stub). Having both an `appleMain` actual AND an `iosMain` actual for the same `expect` would cause duplicate-actual compile errors; splitting ensures iOS gets the full impl while macOS keeps the stub.
- `2026-02-23T13:07-08:00` iOS FFI bridge (`IosBridge.kt`) uses `interpretObjCPointerOrNull<MTKView>` to reconstruct the MTKView from the opaque C pointer, matching the macOS bridge pattern (which uses `NativeAddress(ptr)` for a CAMetalLayer). This is necessary because `iosContextRenderer` takes an `MTKView`, not a raw layer pointer.
- `2026-02-23T13:07-08:00` Flutter iOS plugin changes to pass MTKView pointer rather than CAMetalLayer pointer so the Kotlin IosBridge can reconstruct the full MTKView object.

---

## Issues

- `2026-02-23T13:07-08:00` **`val iosMain by getting` fails in `prism-native/build.gradle.kts`** — `prism-native` does not call `applyDefaultHierarchyTemplate()`, so the `by getting` delegate syntax throws "KotlinSourceSet with name 'iosMain' not found" at configuration time. Fix: use property-style `iosMain.dependencies { ... }` which matches how `macosMain.dependencies { ... }` is accessed in the same file.

---

## Commits

- 8a999d0b — chore: add devlog and plan for ios-ffi-bridge-prismview
- HEAD — feat: iOS FFI bridge, PrismView onSurfaceReady, Compose iOS refactor

---

## Next Steps

_After implementation: run `./gradlew :prism-native:linkDebugSharedIosSimulatorArm64` and `nm -gU ... | grep prism_` to verify FFI symbols._
