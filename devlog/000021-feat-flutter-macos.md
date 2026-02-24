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
- `prism-flutter/example/macos/` — scaffolded Flutter macOS app
- `prism-flutter/example/lib/main.dart` — rewritten to use `prism_sdk.dart` Engine API

## Decisions
- `2026-02-23T12:00-08:00` Use `int engineHandle` not `PrismEngine engine` in PrismRenderView — aligns iOS and macOS creationParams pattern; the render view just needs the raw handle, not the full engine object. The caller (e.g. main.dart) holds the Engine lifecycle.
- `2026-02-23T12:00-08:00` Delete `PrismMetalBridgeProtocol.swift` — the new direct C-API model has no need for an ObjC protocol or the Kotlin bridge xcframework; Swift calls the C functions directly via `@_silgen_name`, same as iOS.
- `2026-02-23T12:00-08:00` MTKView + MTKViewDelegate for macOS (vs UIKit CADisplayLink on iOS) — MTKView has its own vsync-driven draw loop; MTKViewDelegate.draw(in:) is called on each frame. Cleaner than a manual display link on macOS.

## Issues
(none yet)

## Commits
HEAD — feat: Flutter macOS demo, align plugin with iOS pattern
