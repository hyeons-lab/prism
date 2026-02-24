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

## Decisions
- `2026-02-24T00:00-08:00` `withContext(Dispatchers.Default)` for texture decode — per user request.
  Keeps GPU calls on `Dispatchers.Main` (render thread) while offloading PNG/JPEG decode to a worker
  thread. A `yield()` between textures lets one frame render before the next decode starts, so
  textures appear progressively without stalling the MTKView draw callback.

## Commits
- 475dfd7 — chore: add devlog and plan for Flutter macOS demo
- 9037ebb — feat: Flutter macOS demo, align plugin with iOS pattern
- 3e31ee0 — chore: update devlog with commit hashes
- d9ee41b — fix: move Flutter macOS demo to prism-flutter-demo, remove old bridge wiring
- f6e33fd — chore: update devlog with commit hashes
- eabd59f — fix: reduce macOS scroll zoom sensitivity (0.1 → 0.01)
- ba52107 — feat: progressive glTF texture loading for macOS and iOS
- HEAD — refactor: progressive glTF texture loading via withContext(Dispatchers.Default)
