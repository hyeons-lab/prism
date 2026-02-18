# 000013: Flutter Integration (M11)

**Agent:** Claude Code (claude-opus-4-6) @ repository:prism branch:feat/flutter-integration
**Intent:** Implement Flutter integration for Android, iOS, and Flutter Web — enabling the Prism rotating cube demo inside a Flutter app with Material UI controls.

## What Changed
- 2026-02-18 `settings.gradle.kts` — Uncommented `prism-flutter` module
- 2026-02-18 `prism-flutter/build.gradle.kts` — Added prism-quality, android target, wasmJs, jvmToolchain(25), prism-demo-core dep
- 2026-02-18 `prism-flutter/src/commonMain/.../PrismBridge.kt` — Rewritten to hold DemoScene + DemoStore (MVI), native-driven render loop
- 2026-02-18 `prism-flutter/src/commonMain/.../FlutterMethodHandler.kt` — Rewritten with setRotationSpeed, togglePause, setCubeColor, getState
- 2026-02-18 `flutter_plugin/android/` — Created PrismFlutterPlugin.kt + PrismPlatformView.kt (SurfaceView + Choreographer)
- 2026-02-18 `flutter_plugin/ios/` — Created PrismFlutterPlugin.swift + PrismPlatformView.swift (MTKView + configureDemo)
- 2026-02-18 `flutter_plugin/lib/src/prism_engine.dart` — Rewritten with new API (setRotationSpeed, togglePause, setCubeColor, getState)
- 2026-02-18 `flutter_plugin/lib/src/prism_render_view.dart` — Fixed unnecessary import lint
- 2026-02-18 `flutter_plugin/example/` — Created example app with Material3 controls
- 2026-02-18 `prism-flutter/build.gradle.kts` — Added `binaries.executable()`, `outputModuleName.set("prism-flutter")` for wasmJs
- 2026-02-18 `prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt` — Kotlin/WASM entry point with @JsExport control functions + requestAnimationFrame render loop
- 2026-02-18 `flutter_plugin/lib/src/prism_web_plugin.dart` — JS interop bindings (@JS) + ES module loader for Kotlin/WASM bundle
- 2026-02-18 `flutter_plugin/lib/src/prism_engine.dart` — Rewritten as conditional export (channel vs web)
- 2026-02-18 `flutter_plugin/lib/src/prism_engine_channel.dart` — Mobile MethodChannel implementation (extracted)
- 2026-02-18 `flutter_plugin/lib/src/prism_engine_web.dart` — Web implementation delegating to PrismWebEngine
- 2026-02-18 `flutter_plugin/lib/src/prism_render_view.dart` — Rewritten as conditional export (mobile vs web)
- 2026-02-18 `flutter_plugin/lib/src/prism_render_view_mobile.dart` — Mobile AndroidView/UiKitView (extracted)
- 2026-02-18 `flutter_plugin/lib/src/prism_render_view_web.dart` — Web HtmlElementView with canvas + WASM init
- 2026-02-18 `flutter_plugin/pubspec.yaml` — Added web platform (pluginClass: none) + package:web dependency
- 2026-02-18 `AGENTS.md` — Updated Platform Implementations, Current Project Status, module descriptions for Flutter M11
- 2026-02-18 `BUILD_STATUS.md` — Marked M11 as complete with implementation details
- 2026-02-18 `ARCHITECTURE.md` — Updated prism-flutter module description, added Flutter demo entry points, removed Flutter from planned improvements
- 2026-02-18 `prism-demo-core/.../IosDemoController.kt` — Added `configureDemo(view, store)` overload for Flutter to pass its own DemoStore
- 2026-02-18 `flutter_plugin/ios/.../PrismPlatformView.swift` — Pass store to configureDemo, replace fatalError with error label, add shutdown(), log errors
- 2026-02-18 `flutter_plugin/ios/.../PrismFlutterPlugin.swift` — Track activePlatformView for shutdown, pass store+plugin through factory
- 2026-02-18 `flutter_plugin/android/.../PrismPlatformView.kt` — Add cube color via MaterialComponent, smoothed FPS (EMA), detachScene() instead of shutdown(), renderer.resize() on surfaceChanged
- 2026-02-18 `prism-flutter/.../PrismBridge.kt` — Added detachScene() method (clear reference without shutdown)
- 2026-02-18 `prism-flutter/.../FlutterWasmEntry.kt` — Create store before coroutine (fix race), add cube color application, read canvas dimensions, handle NaN in JSON
- 2026-02-18 `flutter_plugin/lib/src/prism_web_plugin.dart` — Fix event listener leak (remove after fire, guard double-complete)
- 2026-02-18 `flutter_plugin/lib/src/prism_render_view_web.dart` — Use LayoutBuilder for canvas dimensions instead of hardcoded 800x600
- 2026-02-18 `flutter_plugin/example/lib/main.dart` — Add dispose() to call engine.shutdown()
- 2026-02-18 `FlutterWasmEntry.kt` — Refactored from global singleton to instance-based (Map keyed by canvasId); surface tracked for proper shutdown
- 2026-02-18 `prism_web_plugin.dart` — All JS interop functions now take canvasId parameter; track loaded module URL
- 2026-02-18 `prism_engine_web.dart` — PrismEngine holds canvasId, routes all calls through it; attachCanvas() method
- 2026-02-18 `prism_engine_channel.dart` — No-op attachCanvas() for API parity with web
- 2026-02-18 `prism_render_view_web.dart` — Removed dispose() (ownership belongs to page, not view); calls engine.attachCanvas()
- 2026-02-18 `PrismFlutterPlugin.kt` — Implements ActivityAware for pause/resume; uses MethodNotImplementedException
- 2026-02-18 `PrismPlatformView.kt` — Added pauseRendering()/resumeRendering(); removed redundant renderer.resize(); standardized bridge access
- 2026-02-18 `FlutterMethodHandler.kt` — Added MethodNotImplementedException (replaces broad IllegalStateException)
- 2026-02-18 `AndroidManifest.xml` — Removed deprecated package attribute (namespace already in build.gradle)
- 2026-02-18 `PrismFlutterPlugin.swift` — Added method channel contract documentation
- 2026-02-18 `build.gradle.kts` — Removed unused jvm() target
- 2026-02-18 `flutter_plugin/example/web/` — Generated Flutter web boilerplate via `flutter create --platforms=web .` (index.html, manifest.json, favicon.png, icons/)
- 2026-02-18 `flutter_plugin/example/web/index.html` — Added import map for `@js-joda/core` bare specifier (kotlinx-datetime dependency)
- 2026-02-18 `prism-flutter/build.gradle.kts` — Added `copyWasmToFlutterWeb` Gradle task to copy WASM + Skiko artifacts to example/web/
- 2026-02-18 `flutter_plugin/example/.gitignore` — Added WASM build artifact exclusions (web/prism-flutter.*, web/skiko.*)
- 2026-02-18 `flutter_plugin/pubspec.yaml` — Removed web platform entry (pluginClass: none, fileName) that caused broken web_plugin_registrant.dart

## Decisions
- 2026-02-18 Native-driven render loop — Choreographer on Android, MTKView delegate on iOS. Flutter only sends control intents via method channel.
- 2026-02-18 Android plugin consumes KMP artifacts from Maven local — `engine.prism:prism-flutter-android:0.1.0-SNAPSHOT`
- 2026-02-18 iOS plugin consumes PrismDemo.xcframework via podspec — reuses existing configureDemo/IosDemoHandle pattern
- 2026-02-18 Shared DemoStore for MVI state — same pattern as Compose integration
- 2026-02-18 Flutter Web uses conditional imports (`dart.library.js_interop`) to split PrismEngine and PrismRenderView into mobile (MethodChannel) and web (JS interop) variants
- 2026-02-18 Kotlin/WASM `@JsExport` functions exposed globally via inline ES module loader script — Dart `@JS()` annotations reference `window.*` globals
- 2026-02-18 Web render loop driven by `requestAnimationFrame` inside Kotlin/WASM — matches mobile pattern (native-driven, not Dart-driven)
- 2026-02-18 WASM instance-based architecture — each canvas gets its own EngineInstance (store, scene, surface, timing state) in a Map keyed by canvasId, supporting multiple PrismRenderView widgets
- 2026-02-18 Shutdown ownership: PrismEngine owner (page/parent) is responsible for calling shutdown(); PrismRenderView does NOT call shutdown in its own dispose()
- 2026-02-18 MethodNotImplementedException — typed exception for unknown method dispatch, instead of broad IllegalStateException catch
- 2026-02-18 Import map for `@js-joda/core` — resolves bare npm specifier from kotlinx-datetime's WASM output via CDN (jsdelivr), avoids needing a bundler

## Issues
- 2026-02-18 Kotlin 2.3.0 deprecated `moduleName` in wasmJs target — replaced with `outputModuleName.set("prism-flutter")` (Provider API)
- 2026-02-18 Critical review revealed 13 issues (4 critical, 8 moderate, 1 minor). All fixed in follow-up commit.
- 2026-02-18 Second review revealed 14 more issues (2 critical, 7 moderate, 5 minor). All fixed — WASM refactored to instance-based, Android ActivityAware added, etc.
- 2026-02-18 Gradle `copyWasmToFlutterWeb` task initially depended on wrong task (`compileProductionExecutableKotlinWasmJs` instead of `compileProductionExecutableKotlinWasmJsOptimize`) — Gradle 9.2 caught the implicit dependency on optimized output directory.
- 2026-02-18 `pluginClass: none` in pubspec.yaml web platform — Flutter interprets "none" as a literal class name, generating broken `web_plugin_registrant.dart`. Fix: remove web platform entry entirely; web support uses Dart conditional imports, not Flutter plugin registration.

## Commits
- 5f3c415 — chore: add devlog and plan for Flutter integration (M11)
- 91b4bd9 — feat: enable prism-flutter module with DemoScene/DemoStore bridge
- 104c395 — feat: implement Android Flutter plugin with platform view
- ec73dda — feat: implement iOS Flutter plugin with MTKView platform view
- 9cbaed9 — feat: implement Flutter Web support with Kotlin/WASM and JS interop
- edc085f — feat: add Flutter Web boilerplate with import map and WASM copy task

## Progress
- [x] Phase 0: Build system — uncomment prism-flutter, add Android/wasmJs targets
- [x] Phase 1: Wire PrismBridge to DemoScene + DemoStore
- [x] Phase 2: Android Flutter plugin (PrismFlutterPlugin.kt, PrismPlatformView.kt)
- [x] Phase 3: iOS Flutter plugin (PrismFlutterPlugin.swift, PrismPlatformView.swift)
- [x] Phase 5: Dart plugin updates (PrismEngine, PrismRenderView) — done as part of Phase 2
- [x] Phase 6: Flutter example app (full demo with UI controls) — done as part of Phase 2
- [x] Phase 4: Flutter Web (HtmlElementView + WASM bundle)
- [x] Phase 7: Documentation & CI
- [x] Phase 8: Critical review fixes (13 issues addressed)
- [x] Phase 9: Second review fixes (14 issues — instance-based WASM, ActivityAware, surface leak, etc.)
- [x] Phase 10: Flutter Web boilerplate — web/ directory, import map, copyWasmToFlutterWeb Gradle task, .gitignore
