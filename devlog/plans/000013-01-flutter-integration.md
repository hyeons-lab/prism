## Context

Prism has working demos on JVM, Compose, WASM, iOS, macOS native, and Android. The next milestone (M11) is Flutter integration — Android, iOS, and Flutter Web — enabling the same rotating lit cube demo inside a Flutter app with Material UI controls.

The `prism-flutter/` KMP module and `prism-flutter/flutter_plugin/` Dart scaffold already exist as stubs. The native plugin classes (`PrismFlutterPlugin.kt`, `PrismFlutterPlugin.swift`) do not exist yet. The example app directory is empty.

## Plan

### Phase 0: Build System

1. Uncomment `prism-flutter` in `settings.gradle.kts`
2. Update `prism-flutter/build.gradle.kts`: add `prism-quality` plugin, `android.kotlin.multiplatform.library`, `wasmJs` target, `jvmToolchain(25)`, `api(project(":prism-demo-core"))`, wgpu4k deps
3. Verify `./gradlew :prism-flutter:build` compiles

### Phase 1: Wire PrismBridge to DemoScene

- `PrismBridge` holds `DemoScene?` + `DemoStore`, delegates method calls
- `FlutterMethodHandler` adds `setRotationSpeed`, `togglePause`; removes `initialize`, `frame`
- Frame loop is native-driven (Choreographer on Android, MTKView delegate on iOS)

### Phase 2: Android Flutter Plugin

- `flutter_plugin/android/build.gradle` — depends on KMP artifacts from Maven local
- `PrismFlutterPlugin.kt` — FlutterPlugin + MethodCallHandler + PlatformViewFactory registration
- `PrismPlatformView.kt` — SurfaceView + SurfaceHolder.Callback + Choreographer.FrameCallback (mirrors PrismDemoActivity pattern)

### Phase 3: iOS Flutter Plugin

- `flutter_plugin/ios/prism_flutter.podspec` — links PrismDemo.xcframework
- `PrismFlutterPlugin.swift` — FlutterPlugin + MethodChannel + PlatformViewFactory
- `PrismPlatformView.swift` — MTKView + configureDemo() (mirrors ViewController.swift pattern)

### Phase 4: Flutter Web

- Add web platform to pubspec.yaml
- `prism_web_plugin.dart` — HtmlElementView factory creating `<canvas id="prismCanvas">`
- Load WASM bundle (`prism-demo-core.js`) which self-initializes on canvas
- Update `PrismRenderView` with `kIsWeb` branch

### Phase 5: Dart Plugin Updates

- `PrismEngine`: add `setRotationSpeed()`, `togglePause()`; remove `initialize()`
- `PrismRenderView`: add web platform support

### Phase 6: Flutter Example App

- `flutter create` scaffolding for example/
- Full demo: PrismRenderView + speed slider + color buttons + pause/resume

### Phase 7: Documentation & CI

- Update AGENTS.md, PLAN.md, BUILD_STATUS.md, ARCHITECTURE.md, README.md
- Devlog updates
