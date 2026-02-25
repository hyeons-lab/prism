## Agent
Claude Sonnet 4.6 (claude-sonnet-4-6) @ repository branch feat/flutter-macos

## Intent
Implement Flutter macOS demo using the same Dart API as iOS. Align the macOS Swift plugin with the
iOS pattern (direct C API via `@_silgen_name`, engine handle from creationParams), update
`PrismRenderView` to accept `int engineHandle` instead of `PrismEngine`, scaffold the macOS example
app, and rewrite `main.dart` to use the idiomatic `prism_sdk.dart` API.

## Progress
- [x] Devlog + plan files created
- [x] Build PrismNative.xcframework
- [x] Generate Dart FFI bindings
- [x] Rewrite macOS Swift plugin
- [x] Update Dart PrismRenderView API
- [x] Scaffold Flutter macOS example app
- [x] Rewrite main.dart
- [x] Format, check, commit
- [x] Add `prism_load_gltf_from_path` C API (SceneState, buildGltfScene)
- [x] Add `prism_orbit_by`, `prism_zoom`, `prism_get_fps`, `prism_toggle_pause`, `prism_get_pause_state`, `prism_is_renderer_ready` C API
- [x] Wire macOS gestures (mouseDragged → prism_orbit_by, scrollWheel → prism_zoom)
- [x] Wire iOS gestures (UIPanGestureRecognizer, UIPinchGestureRecognizer)
- [x] Add `resolveFlutterAssetPath` method channel to iOS + macOS plugins
- [x] Add new methods to Dart dispatch/channel layers
- [x] Update demo main.dart to load GLB and poll renderer readiness
- [x] Delete prism-flutter/example/ (wrong location)
- [x] Fix prism_sdk_ffi.dart NativeFinalizer + Finalizable issues
- [x] Fix prism_web_plugin.dart DOMStringMap.set issue

## What Changed
- `prism-flutter/flutter_plugin/macos/prism_flutter/Sources/prism_flutter/PrismFlutterPlugin.swift` — removed `configure(bridge:)` API; now registers factory + stub method channel without pre-configured bridge, matching iOS pattern
- `prism-flutter/flutter_plugin/macos/prism_flutter/Sources/prism_flutter/PrismMacOSPlatformView.swift` — rewrote with direct C API: reads `engineHandle` from creationParams, uses MTKView + MTKViewDelegate, calls `prism_attach_metal_layer` / `prism_render_frame` via `@_silgen_name`
- `prism-flutter/flutter_plugin/macos/prism_flutter/Sources/prism_flutter/PrismMetalBridgeProtocol.swift` — deleted; no longer needed
- `prism-flutter/flutter_plugin/macos/prism_flutter/Package.swift` — removed `PrismFlutter` binary target; now depends only on `PrismNative`
- `prism-flutter/flutter_plugin/macos/prism_flutter/Frameworks/.gitignore` — removed `PrismFlutter.xcframework` entry
- `prism-flutter/flutter_plugin/lib/src/prism_render_view_mobile.dart` — changed `PrismEngine engine` → `int engineHandle`; all callers updated
- `prism-flutter/flutter_plugin/lib/src/prism_render_view_web.dart` — changed `PrismEngine engine` → `int engineHandle`; removed `attachCanvas` call
- `prism-flutter/example/macos/` — scaffolded Flutter macOS app (removed in follow-up: belongs in prism-flutter-demo)
- `prism-flutter/example/lib/main.dart` — rewritten to use `prism_sdk.dart` Engine API
- `prism-flutter-demo/example/macos/Runner/AppDelegate.swift` — removed old bridge imports (PrismFlutterDemo, prism_flutter), extensions, and configure(bridge:) call; minimal clean AppDelegate
- `prism-flutter-demo/example/macos/Runner.xcodeproj/project.pbxproj` — removed all PrismFlutterDemo SPM references (XCLocalSwiftPackageReference, XCSwiftPackageProductDependency, packageProductDependencies, Frameworks entries)
- `prism-flutter-demo/example/macos/Packages/PrismFlutterDemo/` — deleted; old bridge SPM package no longer needed with direct C-API pattern
- `prism-flutter/example/macos/` — deleted (wrong location; demo belongs in prism-flutter-demo)
- `prism-flutter/example/test/`, `.gitignore`, `.metadata`, `README.md`, `analysis_options.yaml`, `pubspec.lock` — deleted; incidentally added by flutter create
- `prism-flutter-demo/example/lib/main.dart` — updated `PrismRenderView(engine: _engine)` → `PrismRenderView(engineHandle: _engine.handle)`; added GLB loading via `resolveFlutterAssetPath` + `loadGltfFromPath`; loading overlay now waits for `isRendererReady`
- `prism-native/src/nativeMain/kotlin/com/hyeonslab/prism/native/SceneState.kt` — NEW: `SceneState` class + `buildGltfScene` factory; per-handle orbit camera, timing, pause state, fps tracking; takes `Engine` param so `renderer.initialize(engine)` is called before `initializeIbl()`
- `prism-native/src/macosMain/kotlin/com/hyeonslab/prism/native/MacosBridge.kt` — full rewrite: `MacosSurfaceState` wrapper, `macosScenes` map, `prism_load_gltf_from_path` (passes Engine to `buildGltfScene`), `prism_render_frame` with ECS world update, `prism_orbit_by`, `prism_zoom`, `prism_get_fps`, `prism_toggle_pause`, `prism_get_pause_state`, `prism_is_renderer_ready`
- `prism-native/src/iosMain/kotlin/com/hyeonslab/prism/native/IosBridge.kt` — same expansion as macOS bridge
- All `prism-native` Kotlin files: package renamed `engine.prism.native` → `com.hyeonslab.prism.native` to match project group ID; files moved to matching directory structure
- `prism-native/build.gradle.kts` — added `prism-assets` dep (for GltfLoader + FileReader)
- `prism-flutter/flutter_plugin/lib/src/prism_engine_ffi.dart` — added `loadGltfFromPath`, `isRendererReady`, `orbitBy`, `zoom`, improved `getState` with live fps/isPaused
- `prism-flutter/flutter_plugin/lib/src/prism_engine_dispatch.dart` — exposed `loadGltfFromPath`, `isRendererReady`, `orbitBy`, `zoom`; added static `resolveFlutterAssetPath` via method channel
- `prism-flutter/flutter_plugin/lib/src/prism_engine_channel.dart` — added stub methods for Android
- `prism-flutter/flutter_plugin/macos/.../PrismFlutterPlugin.swift` — added `resolveFlutterAssetPath` method channel handler
- `prism-flutter/flutter_plugin/macos/.../PrismMacOSPlatformView.swift` — wired `mouseDragged` → `prism_orbit_by`, `scrollWheel` → `prism_zoom`; C declarations for both
- `prism-flutter/flutter_plugin/ios/.../PrismFlutterPlugin.swift` — added `resolveFlutterAssetPath` handler; UIPanGestureRecognizer + UIPinchGestureRecognizer; C declarations for orbit/zoom
- `prism-flutter/flutter_plugin/lib/src/prism_sdk_ffi.dart` — fixed `NativeFinalizer` (`.cast()` → module-level `_lib.lookup`), added `implements Finalizable` to Engine/World/Node/Scene
- `prism-flutter/flutter_plugin/lib/src/prism_web_plugin.dart` — fixed `dataset.set()` → `dataset['module'] =`
- `prism-flutter/example/` — deleted (wrong location; was scaffolded by flutter create incidentally)
- `prism-demo-core/src/jvmMain/kotlin/com/hyeonslab/prism/demo/ComposeMain.kt` — refactored to full-screen layout matching other demos: removed ComposePanel side panel and DemoStore, PrismPanel fills full window (1000×700), dedicated daemon render thread (PrismRenderThread) letting Metal's `nextDrawable` throttle to vsync, FPS shown in window title; drag-to-orbit via `MouseAdapter`/`MouseMotionAdapter` at `0.005f rad/px`
- `prism-demo-core/src/iosMain/kotlin/com/hyeonslab/prism/demo/ComposeIosEntry.kt` — simplified to match other demos: removed `sharedDemoStore` / `uiState` / `DemoStore` usage, removed `ComposeDemoControls` overlay and `WindowInsets.safeDrawing` padding; render callback now calls only `sc.tick()` with `time.totalTime` (no FPS dispatch or material overrides)
- `prism-demo-core/src/androidMain/kotlin/com/hyeonslab/prism/demo/ComposeAndroidEntry.kt` — same simplification as iOS: removed `DemoStore` / `uiState`, removed FPS dispatch and `setMaterialOverride`/`setEnvIntensity` calls, removed `ComposeDemoControls` overlay and `WindowInsets.safeDrawing` padding

## Decisions
- `2026-02-23T22:09-08:00` C API for geometry rendering instead of Kotlin bridge — avoids SKIE/ObjC interop layer, no new SPM packages, ~300 lines new Kotlin + ~100 lines new Dart vs. complex bridge. `buildGltfScene` in `nativeMain` replicates demo-core logic using only library deps.
- `2026-02-23T22:09-08:00` `SceneState` per-handle in `nativeMain` — orbit params, timing, pause flag, fps smoothing all stored per engine handle in an `AtomicRef<Map>`. Kotlin concurrent; accessed from main thread only (MTKView delegate / CADisplayLink).
- `2026-02-23T22:09-08:00` `resolveFlutterAssetPath` via method channel — the native side knows `Bundle.main.resourcePath`; Dart doesn't. A single method channel call returns the filesystem path so `prism_load_gltf_from_path` (which uses `FileReader.readBytes`) can locate the GLB.
- `2026-02-23T22:09-08:00` Retry `loadGltfFromPath` in poll timer — `prism_load_gltf_from_path` silently returns if surface not yet attached; polling every 500ms until `isRendererReady` is true handles the race without requiring a callback channel.
- `2026-02-23T22:48-08:00` `WgpuRenderer.initialize(engine)` does not use `engine` param — it exists only to satisfy the Subsystem interface. Passing any live `Engine` handle is sufficient; the body only accesses `wgpuContext` members.
- `2026-02-23T22:09-08:00` NativeFinalizer fix: `_bindings.prism_destroy_engine.cast()` was wrong (Dart Function has no `.cast()`); fixed by storing `DynamicLibrary` at module level and using `_lib.lookup<NativeFunction<Void Function(Pointer<Void>)>>('prism_destroy_*')`. Classes need `implements Finalizable` for `NativeFinalizer.attach`.
- `2026-02-23T22:09-08:00` Use `int engineHandle` not `PrismEngine engine` in PrismRenderView — aligns iOS and macOS creationParams pattern; the render view just needs the raw handle, not the full engine object. The caller (e.g. main.dart) holds the Engine lifecycle.
- `2026-02-23T22:09-08:00` Delete `PrismMetalBridgeProtocol.swift` — the new direct C-API model has no need for an ObjC protocol or the Kotlin bridge xcframework; Swift calls the C functions directly via `@_silgen_name`, same as iOS.
- `2026-02-23T22:09-08:00` MTKView + MTKViewDelegate for macOS (vs UIKit CADisplayLink on iOS) — MTKView has its own vsync-driven draw loop; MTKViewDelegate.draw(in:) is called on each frame. Cleaner than a manual display link on macOS.

## Issues
- `2026-02-23T22:48-08:00` `renderer.initialize(engine)` not called before `initializeIbl()`: `WgpuRenderer` requires `initialize()` to be called first to create bind group layouts, default textures, and PBR pipelines. Calling `initializeIbl()` before `initialize()` caused `NullPointerException` at `envBindGroupLayout!!`. `RenderSystem.initialize(world)` has an empty body — doesn't call `renderer.initialize()`. Fix: pass `Engine` to `buildGltfScene` and call `renderer.initialize(engine)` before `initializeIbl()`.
- `2026-02-23T22:30-08:00` `engine.prism.native` package wrong: all `prism-native` Kotlin files used `engine.prism.native` instead of `com.hyeonslab.prism.native`. Renamed package and moved files to correct directory structure.
- `2026-02-23T22:20-08:00` iOS simulator SIGABRT from unhandled Kotlin exception: `prismLoadGltfFromPath` threw when file not found at wrong path. Added try-catch with `Logger.withTag()` (needed `import co.touchlab.kermit.Logger` added to both bridge files).
- `prism-flutter/example/macos/` was incidentally scaffolded by `flutter create` in commit 9037ebb.
  The correct demo scaffold lives in `prism-flutter-demo/example/`, not in the library module's
  example directory. Resolution: `git rm` the macos/ dir and other flutter create artifacts from
  `prism-flutter/example/`, delete the now-unneeded `PrismFlutterDemo` SPM local package, and
  strip the old bridge wiring from AppDelegate.swift + project.pbxproj.
- `prism-flutter-demo/example/lib/main.dart` used the old `PrismRenderView(engine: _engine)` API.
  Updated to `PrismRenderView(engineHandle: _engine.handle)` to match the updated widget signature.
- `prism-flutter/flutter_plugin/macos/.../PrismMacOSPlatformView.swift` — reduced `scrollingDeltaY`
  zoom sensitivity from `* 0.1` to `* 0.01`; trackpad events deliver 10–50px so 0.1 caused jumps.
- `prism-native/src/nativeMain/kotlin/com/hyeonslab/prism/native/SceneState.kt` — replaced
  `Dispatchers.Default` scope + `AtomicRef<List<DecodedTexture>>` upload queue +
  `uploadNextPendingTexture()` with `textureScope` on `Dispatchers.Main` + `withContext(Default)` for
  CPU decode + inline GPU upload + `yield()` between textures (GltfDemoScene pattern).
- `prism-native/src/macosMain/kotlin/com/hyeonslab/prism/native/MacosBridge.kt` — removed stale
  `backgroundScope` creation and `uploadNextPendingTexture()` call from `prismRenderFrame`.
- `prism-native/src/iosMain/kotlin/com/hyeonslab/prism/native/IosBridge.kt` — removed stale
  `CoroutineScope`, `SupervisorJob`, `cancel` imports; no `uploadNextPendingTexture()` was added here.

- `2026-02-24T10:30-08:00` `prism-native/src/macosMain/.../MacosBridge.kt` — added `pendingGlbPaths` map; `prismLoadGltfFromPath` now queues path if surface absent; `prismAttachMetalLayer` consumes queue after storing surface; `prismDetachSurface` clears queue entry. Eliminates the need for Dart-side retry polling.
- `2026-02-24T10:30-08:00` `prism-native/src/iosMain/.../IosBridge.kt` — identical changes to MacosBridge.kt.
- `2026-02-24T10:30-08:00` `prism-flutter/flutter_plugin/lib/src/prism_engine_ffi.dart` — added synchronous `double get fps` getter (reads `prism_get_fps` directly, no async overhead).
- `2026-02-24T10:30-08:00` `prism-flutter/flutter_plugin/lib/src/prism_engine_dispatch.dart` — forwarding `fps` getter; forwards to ffi impl or returns 0.0 for Android.
- `2026-02-24T10:30-08:00` `prism-flutter/flutter_plugin/lib/src/prism_engine_channel.dart` — `double get fps => 0.0;` stub for Android.
- `2026-02-24T10:30-08:00` `prism-flutter-demo/example/lib/main.dart` — replaced `Timer.periodic` + `_isInitialized` with `SingleTickerProviderStateMixin` + `Ticker`; extracted `_setup()` for async init+load; `_onFrame` checks `isRendererReady` and fps delta ≥1 before calling `setState`.
- `2026-02-24T10:30-08:00` `devlog/plans/000021-02-remove-flutter-polling.md` — plan file created.

## Decisions
- `2026-02-24T00:00-08:00` `withContext(Dispatchers.Default)` for texture decode — per user request.
  Keeps GPU calls on `Dispatchers.Main` (render thread) while offloading PNG/JPEG decode to a worker
  thread. A `yield()` between textures lets one frame render before the next decode starts, so
  textures appear progressively without stalling the MTKView draw callback.
- `2026-02-24T10:30-08:00` Queue pending GLB path in native bridge — avoids the inherent race where
  Dart's `initState()` runs before the first `MTKView.draw(in:)`. The path is queued synchronously
  and consumed in `prismAttachMetalLayer`, which runs on the first draw callback. No retry loop
  needed on the Dart side; the scene is ready after a single `loadGltfFromPath` call.
- `2026-02-24T10:30-08:00` `Ticker` instead of `Timer.periodic` for FPS display — vsync-aligned
  (~60 fps) avoids redundant rebuilds (setState only when fps changes by ≥1 or ready state changes).
  Replaces two polling concerns (init check + FPS) with a single frame callback.
- `2026-02-24T10:30-08:00` Synchronous `fps` getter in Dart FFI layer — avoids `async/await`
  overhead in the per-frame callback. `prism_get_fps` is a trivial map lookup on the native side.

- `2026-02-24T19:00-08:00` **Black screen / spinner never disappears (iOS)**: `prism_is_renderer_ready` always returned 0. Root causes found via system log:
  1. **`as? Int64` silent nil**: Flutter's `StandardMessageCodec` encodes small Dart integers as `INT_32`. In Swift, `NSNumber(Int32: 1) as? Int64` returns `nil`. Fixed: `(params?["engineHandle"] as? NSNumber)?.int64Value ?? 0` in both iOS and macOS plugins.
  2. **Stale XCFramework**: `PrismNative.xcframework` (ios-arm64-simulator slice) was built Feb 23 22:53, *before* the `fbca70b` pendingGlbPaths commit (Feb 24 08:13). The `prismAttachMetalLayer` in the dylib lacked the auto-load queue check entirely. Fix: rebuild xcframework via `./gradlew :prism-flutter:bundleNativeiOS`.
  3. **Main-thread deadlock risk**: `prism_attach_metal_layer` was called from `layoutSubviews` (main thread). `prismAttachMetalLayer` calls `runBlocking(Dispatchers.Default) { iosContextRenderer() }`. Moved to `DispatchQueue.global(qos: .userInitiated).async` to prevent deadlock.
  4. **Zero-size layout guard missing**: `layoutSubviews` fires before Flutter sets the real frame, calling `prism_attach_metal_layer` with 0×0, causing renderer init to fail permanently. Added `guard width > 0 && height > 0 else { return }` before the `isAttached` check.
  5. **`PrismRenderView` created before engine handle valid**: The widget was unconditionally rendered with `handle=0`, so `setupMtkView()` was skipped. Fixed by guarding with `if (_engine.handle != 0)` and calling `setState(() {})` after `initialize()` in `_setup()`.
  6. **FPS chip overlapping Dynamic Island**: Fixed with `MediaQuery.of(context).padding.top + 8` as `top` offset.
  7. **Flipped pan gesture orbit (first attempt)**: `prism_orbit_by` called with `+translation.x, -translation.y`. Changed to `-translation.x, +translation.y` — but user confirmed BOTH axes were still wrong after testing with a working scene.
  8. **Flipped pan gesture orbit (corrected)**: With the scene rendering, user confirmed `-translation.x, +translation.y` still felt wrong for both axes. Reverted to `+translation.x, -translation.y`. Analysis: UIKit `translation.y` is negative when dragging up; negating it gives positive elevation (camera up), which is the natural expectation. UIKit `translation.x` positive when dragging right maps directly to increasing azimuth (camera orbits right). Final: `prism_orbit_by(handle, +translation.x * 0.01, -translation.y * 0.01)`.

- `2026-02-24T20:00-08:00` **macOS demo showing spinner (stale xcframework)**: Same root cause as iOS — `PrismNative.xcframework` (macos-arm64 slice) was built Feb 23 23:24, before the `fbca70b` pendingGlbPaths commit. Old `prismLoadGltfFromPath` returned immediately if no surface attached, without queuing the path. Since Dart calls `loadGltfFromPath` before the first `draw(in:)` fires (macOS merged UI+platform thread causes the path to arrive before the MTKView delegate runs), the path was discarded and `buildGltfScene` was never called. Fix: `./gradlew :prism-flutter:bundleNativeMacOS` (xcframework is gitignored, rebuilt locally). Verified: macOS demo renders the DamagedHelmet scene.
- `2026-02-24T20:00-08:00` **"Lost connection to device" false positives**: Multiple concurrent `flutter run -d macos` instances competed for the same device; each triggered "Lost connection to device" in the others. Not a crash. Resolved by killing all instances before launching a fresh one.

## Commits
- 475dfd7 — chore: add devlog and plan for Flutter macOS demo
- 9037ebb — feat: Flutter macOS demo, align plugin with iOS pattern
- 3e31ee0 — chore: update devlog with commit hashes
- d9ee41b — fix: move Flutter macOS demo to prism-flutter-demo, remove old bridge wiring
- f6e33fd — chore: update devlog with commit hashes
- eabd59f — fix: reduce macOS scroll zoom sensitivity (0.1 → 0.01)
- ba52107 — feat: progressive glTF texture loading for macOS and iOS
- 6c67d42 — refactor: progressive glTF texture loading via withContext(Dispatchers.Default)
- fbca70b — refactor: replace Flutter demo poll timer with Ticker + native pending-path queue
- 4f07c95 — fix: iOS black screen, engine handle decode, gesture orientation, FPS safe area
- e1d085f — fix: correct iOS pan gesture axis signs (both axes)
- 85e0494 — feat: add drag-to-orbit to Compose Desktop demo
- 5d49637 — refactor: Compose Desktop demo to full-screen, matching other demos
- 3b05d41 — refactor: simplify all Compose demos to match other platform demos
- 1502093 — fix: use dedicated render thread instead of Swing Timer in Compose demo
- HEAD — fix: revert Compose Desktop to ComposePanel+withFrameNanos, fix detekt violations

## Issues (continued)
- `2026-02-24T21:04-08:00` **Detekt CI failure on `ComposeMain.kt`**: Commit `1502093` introduced a dedicated daemon render thread with `catch (e: Exception)` (→ `TooGenericExceptionCaught`) and a `while` loop with two `break` statements (→ `LoopWithTooManyJumpStatements`). Fix: revert `ComposeMain.kt` to the `ComposePanel + withFrameNanos` approach. Lambda `return@withFrameNanos` statements are not counted as loop jump statements by detekt, and there is no try-catch, so both violations are resolved.
- `2026-02-25T00:40-08:00` **WASM build broken (`prism-compose`, `prism-flutter-demo` wasmJs targets)**: `prism-compose` and `prism-flutter-demo` `wasmJsMain` source sets were missing `wgpu4k` + `wgpu4k.toolkit` deps, causing `Unresolved reference 'io'` (WGPUContext) and `Unresolved reference 'web'` (HTMLCanvasElement) errors. Root cause: both modules depended transitively on types from wgpu4k for WASM but didn't declare it explicitly (unlike `prism-native-widgets` which did). Fix: added `wasmJsMain.dependencies { wgpu4k + wgpu4k.toolkit }` to both build.gradle.kts files.
- `2026-02-25T00:40-08:00` **Web Flutter demo: auto-rotation + FPS not updating**: Auto-rotation (`rotationSpeed = 45f`) was firing from `DemoStore` default state — not appropriate for the touch/pointer-drag Flutter demo. Fix: dispatch `DemoIntent.SetRotationSpeed(0f)` immediately after instance creation in `FlutterWasmEntry.kt`. FPS was always 0 on web because the Dart `fps` getter returned a hardcoded 0.0. Fix: exported `@JsExport fun prismGetFps(canvasId)` from Kotlin; added `@JS` binding and `getFps(canvasId)` to `PrismWebEngine`; tracked `lastCanvasId` in `PrismWebEngine.init()`; `prism_engine_web.dart` `fps` getter now calls `PrismWebEngine.getFps(lastCanvasId)`.
- `2026-02-24T22:37-08:00` **Web Flutter demo compile errors (missing methods on `prism_engine_web.dart`)**: `resolveFlutterAssetPath`, `loadGltfFromPath`, `fps`, `isRendererReady`, `orbitBy`, `zoom` were added to `prism_engine_ffi.dart` and `prism_engine_channel.dart` but never added to `prism_engine_web.dart`. Build failed with "Member not found" errors on `flutter run -d chrome`. Root cause: no shared contract enforcing API parity across backends. Fixes: (1) Added `prism_engine_interface.dart` abstract interface; all three backends now `implements PrismEngineInterface`. (2) Typed `dispatch._impl` as `PrismEngineInterface` instead of `dynamic`; removed now-unnecessary `_impl is ffi.PrismEngine` downcasts. (3) Added stubs to `prism_engine_web.dart`: `resolveFlutterAssetPath` → null, `loadGltfFromPath` → no-op, `fps` → 0.0, `isRendererReady` → true, `orbitBy`/`zoom` → no-op. (4) Fixed `main.dart`: `if (_engine.handle != 0)` → `if (kIsWeb || _engine.handle != 0)` for render view; `if (!_isSceneReady)` → `if (!kIsWeb && !_isSceneReady)` for spinner. (5) Added `PrismWebEngine.isWasmLoaded` public getter (was private). `flutter build web` confirms clean compile.
- `2026-02-24T22:15-08:00` **Compose Desktop orbit axes flipped**: `orbitBy(dx, -dy)` had both signs wrong vs. the macOS native GLFW reference (`-dx`, `+dy`). AWT and GLFW share the same screen-coordinate convention (y increases downward), so the correct call is `orbitBy(-dx * ORBIT_SENSITIVITY, dy * ORBIT_SENSITIVITY)`.

## Decisions (continued)
- `2026-02-24T22:15-08:00` Compose Desktop orbit sign convention matches macOS native GLFW demo: `-dx` for azimuth (drag right → azimuth decreases, scene rotates right), `+dy` for elevation (drag down → elevation increases, camera goes up). WASM demo uses the same convention.
