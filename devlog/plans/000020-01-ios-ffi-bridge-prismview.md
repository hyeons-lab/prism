# 000020-01 — iOS FFI Bridge + PrismView.apple.kt + Compose Tab Refactor

## Thinking

The plan has three interconnected parts:

**Part 1 (prism-native IosBridge):** macOS already has `MacosBridge.kt` with 4 `@CName` exported C functions using `macosContextRendererFromLayer(NativeAddress(ptr), ...)`. iOS needs the same 4 symbols but using `iosContextRenderer(mtkView, ...)`. The key difference is the API expects an `MTKView` object (not a raw layer pointer). We reconstruct the `MTKView` from the opaque C pointer via `interpretObjCPointerOrNull<MTKView>(ptr.rawValue)`. The Flutter Swift plugin currently passes a `CAMetalLayer` pointer — this must change to an MTKView pointer.

**Part 2 (PrismView):** The current `PrismView.apple.kt` is a shared stub for all Apple platforms (iOS + macOS). The new iOS implementation uses `UIKitView` from `androidx.compose.ui.interop`, which is iOS-only and not available in macOS Compose. Therefore, we MUST split the single `appleMain` actual into separate `iosMain` and `macosMain` actuals — having both would cause duplicate-actual compile errors. The `expect` signature gains `onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null`. `WGPUContext` is available in `nonNativeMain` via `prism-renderer → wgpu4k` (which is in wgpu4k's commonMain).

**Part 3 (ComposeIosEntry refactor):** Instead of manually managing UIKitView + MTKView lifecycle with `ComposeRenderDelegate`, we delegate to `PrismView`. The `DemoScene` initialization and per-frame logic wire into `engineStore.engine.gameLoop.onRender`. The `DemoScene.shutdown()` call is replaced by just `scene.world.shutdown()` to avoid calling `engine.shutdown()` on the wrong engine.

Key source-set change: `appleMain/PrismView.apple.kt` loses its `actual fun PrismView` (becomes empty/package-only), replaced by `iosMain/PrismView.ios.kt` and `macosMain/PrismView.macos.kt`.

## Plan

1. **prism-native/build.gradle.kts** — Add `iosMain` source set (inside `if (isMac)`) with `wgpu4k.toolkit` + `kotlinx.coroutines.core` dependencies.

2. **prism-native/src/iosMain/.../IosBridge.kt** — Create new file. Near-copy of `MacosBridge.kt` with:
   - `IosContext` instead of `MacosContext`
   - `iosSurfaces: AtomicRef<Map<Long, IosContext>>` atomic map
   - `iosContextRenderer(mtkView, width, height)` call
   - MTKView reconstructed via `interpretObjCPointerOrNull<MTKView>(ptr.rawValue)`
   - Same 4 `@CName` exported functions

3. **prism-flutter/...PrismFlutterPlugin.swift** — In `PrismMetalView`: replace `metalLayer: CAMetalLayer?` with `mtkView: MTKView?`. Rename `setupMetalLayer()` → `setupMtkView()`. Pass MTKView pointer via `Unmanaged.passUnretained(mtkView).toOpaque()`. Update `layoutSubviews()` to resize `mtkView.frame` (not metalLayer.frame). Add `import MetalKit` at top.

4. **prism-compose/src/nonNativeMain/.../PrismView.kt** — Add `onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)? = null` to `expect fun PrismView`. Update docstring.

5. **prism-compose/src/appleMain/.../PrismView.apple.kt** — Remove `actual fun PrismView` (keep only package declaration). The actuals move to iosMain and macosMain.

6. **prism-compose/src/iosMain/.../PrismView.ios.kt** — NEW file. Full implementation:
   - `UIKitView` holding an `MTKView`
   - `LaunchedEffect(mtkView)` creates surface via `createPrismSurface(view, w, h)`
   - After surface ready: dispatch `SurfaceResized`, call `onSurfaceReady`, start game loop
   - `MTKViewDelegateProtocol` delegate ticks `engine.gameLoop.tick()` per frame
   - Delegate dispatches FPS via `NSOperationQueue.mainQueue`
   - Strong ref to delegate stored in remembered state var (weak MTKView.delegate)
   - `DisposableEffect` cleans up: null delegate, stop game loop, detach surface

7. **prism-compose/src/macosMain/.../PrismView.macos.kt** — NEW file. Updated stub with new signature (same log warning, onDispose = {}).

8. **prism-compose/src/jvmMain/.../PrismView.jvm.kt** — Add `onSurfaceReady` parameter. Currently uses `SwingPanel(PrismPanel)` — `onSurfaceReady` is invoked from `p.onReady` callback after the panel's wgpu context is ready.

9. **prism-compose/src/androidMain/.../PrismView.android.kt** — Add `onSurfaceReady` parameter. Invoke it after `createPrismSurface` + guard checks succeed.

10. **prism-compose/src/wasmJsMain/.../PrismView.wasmJs.kt** — Add `onSurfaceReady` parameter (no-op in stub).

11. **prism-demo-core/src/iosMain/.../ComposeIosEntry.kt** — Refactor:
    - Replace manual `UIKitView(factory=MTKView)` + `ComposeRenderDelegate` with `PrismView`
    - `val engineStore = rememberEngineStore(EngineConfig("Prism iOS Compose"))`
    - `PrismView(store = engineStore, modifier = Modifier.fillMaxSize(), onSurfaceReady = { ctx, w, h -> ... })`
    - In onSurfaceReady: create scene, set `engineStore.engine.gameLoop.onRender`
    - `DisposableEffect` to clear onRender + `scene.world.shutdown()`
    - Remove `ComposeRenderDelegate` class entirely
    - Keep DemoStore / overlay UI unchanged
