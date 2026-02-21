/// Prism SDK — Dart wrapper that mirrors the Kotlin API surface.
///
/// Exposes the same class names as the Kotlin library (Engine, World, Scene,
/// MeshNode, CameraNode, etc.) so code patterns are nearly identical across
/// Kotlin, Swift, JavaScript (prism-sdk.mjs), and Dart.
///
/// Web: routes to prism.mjs via generated @JS() bindings.
/// Native/mobile: throws UnsupportedError — use prism_flutter.dart instead.
///
/// Prerequisites (web only):
///   Run `./gradlew :prism-flutter:generateDartJsBindings` before building.
library prism_sdk;

export 'src/prism_sdk_types.dart';
export 'src/prism_sdk_stub.dart'
    if (dart.library.js_interop) 'src/prism_sdk_web.dart';
