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

## Decisions
- 2026-02-18 Native-driven render loop — Choreographer on Android, MTKView delegate on iOS. Flutter only sends control intents via method channel.
- 2026-02-18 Android plugin consumes KMP artifacts from Maven local — `engine.prism:prism-flutter-android:0.1.0-SNAPSHOT`
- 2026-02-18 iOS plugin consumes PrismDemo.xcframework via podspec — reuses existing configureDemo/IosDemoHandle pattern
- 2026-02-18 Shared DemoStore for MVI state — same pattern as Compose integration
- 2026-02-18 Flutter Web uses conditional imports (`dart.library.js_interop`) to split PrismEngine and PrismRenderView into mobile (MethodChannel) and web (JS interop) variants
- 2026-02-18 Kotlin/WASM `@JsExport` functions exposed globally via inline ES module loader script — Dart `@JS()` annotations reference `window.*` globals
- 2026-02-18 Web render loop driven by `requestAnimationFrame` inside Kotlin/WASM — matches mobile pattern (native-driven, not Dart-driven)

## Issues
- 2026-02-18 Kotlin 2.3.0 deprecated `moduleName` in wasmJs target — replaced with `outputModuleName.set("prism-flutter")` (Provider API)
- 2026-02-18 Critical review revealed 13 issues (4 critical, 8 moderate, 1 minor). All fixed in follow-up commit.

## Commits
- 5f3c415 — chore: add devlog and plan for Flutter integration (M11)
- 91b4bd9 — feat: enable prism-flutter module with DemoScene/DemoStore bridge
- 104c395 — feat: implement Android Flutter plugin with platform view
- ec73dda — feat: implement iOS Flutter plugin with MTKView platform view
- 9cbaed9 — feat: implement Flutter Web support with Kotlin/WASM and JS interop

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
