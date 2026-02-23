// Non-web: runtime dispatch — FFI on macOS/Linux/Windows, channel on iOS/Android.
// Web: prism_engine_web.dart via Kotlin/WASM @JS() bindings.
export 'prism_engine_dispatch.dart'
    if (dart.library.js_interop) 'prism_engine_web.dart';
