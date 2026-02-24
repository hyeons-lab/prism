## Thinking

The macOS Flutter plugin uses a bridge-protocol architecture requiring a pre-configured
`DemoMacosBridge` Kotlin object and `PrismFlutter.xcframework`. This doesn't align with the iOS
pattern, which receives an `engineHandle` from Dart and calls the C API directly.

The iOS plugin (PrismFlutterPlugin.swift on iOS) is self-contained: it reads the engineHandle from
creationParams, and uses `@_silgen_name` to call `prism_attach_metal_layer` / `prism_render_frame`
from `PrismNative.xcframework`. No separate bridge xcframework needed.

The macOS plugin should do the same. The key differences from iOS are:
- macOS uses `NSView` / `AppKitView` (not UIView / UiKitView)
- macOS can use `MTKView + MTKViewDelegate` natively (no CADisplayLink needed)
- macOS factory returns `NSView` directly (not wrapped in FlutterPlatformView)

The `PrismRenderView` Dart widget currently takes `PrismEngine engine` and accesses `engine.handle`.
Changing to `int engineHandle` makes the dependency explicit and allows callers to use either
`PrismEngine` (legacy dispatcher) or `Engine` (SDK FFI class).

## Plan

1. Create worktree devlog + plan files, commit, push, draft PR.

2. Build PrismNative.xcframework:
   ```bash
   ./gradlew :prism-flutter:bundleNativeMacOS
   ```

3. Generate Dart FFI bindings:
   ```bash
   ./gradlew :prism-native:generateFfiBindings
   ```

4. Rewrite macOS Swift plugin:
   - `PrismFlutterPlugin.swift`: remove `configure(bridge:)` static API, register factory + stub channel directly
   - `PrismMacOSPlatformView.swift`: rewrite factory to read engineHandle from args; rewrite view as MTKView + MTKViewDelegate with direct C API calls
   - Delete `PrismMetalBridgeProtocol.swift`
   - `Package.swift`: remove PrismFlutter binary target
   - `Frameworks/.gitignore`: remove PrismFlutter.xcframework entry

5. Update Dart PrismRenderView:
   - `prism_render_view_mobile.dart`: `PrismEngine engine` → `int engineHandle`
   - `prism_render_view_web.dart`: `PrismEngine engine` → `int engineHandle`; remove `attachCanvas` call

6. Scaffold Flutter macOS example:
   ```bash
   cd prism-flutter/example && flutter create --platforms=macos .
   ```

7. Rewrite `main.dart` to use `prism_sdk.dart` Engine API with `PrismRenderView(engineHandle: _engine.handle)`.

8. Format + CI check:
   ```bash
   ./gradlew ktfmtFormat
   ./gradlew ktfmtCheck detektJvmMain jvmTest
   ```

9. Commit and push.
