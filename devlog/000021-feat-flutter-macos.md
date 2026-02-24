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
- `prism-flutter-demo/example/lib/main.dart` — updated `PrismRenderView(engine: _engine)` → `PrismRenderView(engineHandle: _engine.handle)`

## Decisions
- `2026-02-23T12:00-08:00` Use `int engineHandle` not `PrismEngine engine` in PrismRenderView — aligns iOS and macOS creationParams pattern; the render view just needs the raw handle, not the full engine object. The caller (e.g. main.dart) holds the Engine lifecycle.
- `2026-02-23T12:00-08:00` Delete `PrismMetalBridgeProtocol.swift` — the new direct C-API model has no need for an ObjC protocol or the Kotlin bridge xcframework; Swift calls the C functions directly via `@_silgen_name`, same as iOS.
- `2026-02-23T12:00-08:00` MTKView + MTKViewDelegate for macOS (vs UIKit CADisplayLink on iOS) — MTKView has its own vsync-driven draw loop; MTKViewDelegate.draw(in:) is called on each frame. Cleaner than a manual display link on macOS.

## Issues
- `prism-flutter/example/macos/` was incidentally scaffolded by `flutter create` in commit 9037ebb.
  The correct demo scaffold lives in `prism-flutter-demo/example/`, not in the library module's
  example directory. Resolution: `git rm` the macos/ dir and other flutter create artifacts from
  `prism-flutter/example/`, delete the now-unneeded `PrismFlutterDemo` SPM local package, and
  strip the old bridge wiring from AppDelegate.swift + project.pbxproj.
- `prism-flutter-demo/example/lib/main.dart` used the old `PrismRenderView(engine: _engine)` API.
  Updated to `PrismRenderView(engineHandle: _engine.handle)` to match the updated widget signature.

## Commits
- 475dfd7 — chore: add devlog and plan for Flutter macOS demo
- 9037ebb — feat: Flutter macOS demo, align plugin with iOS pattern
- 3e31ee0 — chore: update devlog with commit hashes
- d9ee41b — fix: move Flutter macOS demo to prism-flutter-demo, remove old bridge wiring
- HEAD — chore: update devlog with commit hashes
