import 'dart:io' show Platform;
import 'prism_engine_channel.dart' as channel;
import 'prism_engine_ffi.dart' as ffi;

/// Runtime dispatcher: FFI on all native platforms except Android (MethodChannel).
///
/// iOS, macOS, Linux, and Windows all talk directly to the prism-native C API
/// via Dart FFI. Android uses the MethodChannel bridge (no prism-native binary).
///
/// Prerequisite: run `./gradlew :prism-native:generateFfiBindings` before
/// building on any native platform (generates prism_native_bindings.dart).
class PrismEngine {
  final dynamic _impl;

  PrismEngine()
      : _impl = Platform.isAndroid ? channel.PrismEngine() : ffi.PrismEngine();

  /// Raw engine handle (non-zero on FFI platforms). Used by platform views to
  /// call prism-native C API functions (e.g. prism_attach_metal_layer).
  int get handle => Platform.isAndroid ? 0 : (_impl as ffi.PrismEngine).handle;

  void attachCanvas(String canvasId) => _impl.attachCanvas(canvasId);
  Future<void> initialize({String appName = 'Prism', int targetFps = 60}) =>
      _impl.initialize(appName: appName, targetFps: targetFps);
  Future<void> togglePause() => _impl.togglePause();
  Future<bool> isInitialized() => _impl.isInitialized();
  Future<Map<String, dynamic>> getState() => _impl.getState();
  Future<void> shutdown() => _impl.shutdown();
}
