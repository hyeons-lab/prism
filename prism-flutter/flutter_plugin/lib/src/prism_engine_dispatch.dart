import 'dart:io' show Platform;
import 'prism_engine_channel.dart' as channel;
import 'prism_engine_ffi.dart' as ffi;

/// Runtime dispatcher: FFI on macOS/Linux/Windows, MethodChannel on iOS/Android.
///
/// Prerequisite: run `./gradlew :prism-native:generateFfiBindings` before
/// building on any native platform (generates prism_native_bindings.dart).
class PrismEngine {
  final dynamic _impl;

  PrismEngine()
      : _impl = (Platform.isMacOS || Platform.isLinux || Platform.isWindows)
            ? ffi.PrismEngine()
            : channel.PrismEngine();

  void attachCanvas(String canvasId) => _impl.attachCanvas(canvasId);
  Future<void> initialize({String appName = 'Prism', int targetFps = 60}) =>
      _impl.initialize(appName: appName, targetFps: targetFps);
  Future<void> togglePause() => _impl.togglePause();
  Future<bool> isInitialized() => _impl.isInitialized();
  Future<Map<String, dynamic>> getState() => _impl.getState();
  Future<void> shutdown() => _impl.shutdown();
}
