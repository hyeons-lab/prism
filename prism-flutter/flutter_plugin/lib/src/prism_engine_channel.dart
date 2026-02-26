import 'package:flutter/services.dart';

import 'prism_engine_interface.dart';

/// Mobile implementation of PrismEngine using platform method channels.
///
/// The render loop is driven natively (Choreographer on Android, MTKView on iOS).
/// This class provides control methods to adjust the demo scene.
class PrismEngine implements PrismEngineInterface {
  static const MethodChannel _channel =
      MethodChannel('engine.prism.flutter/engine');

  /// Always 0 on Android — the channel backend has no native handle.
  int get handle => 0;

  /// No-op on mobile — canvas binding is only needed for web multi-instance support.
  void attachCanvas(String canvasId) {}

  /// No-op on mobile — engine is initialized natively when the platform view is created.
  Future<void> initialize({String appName = 'Prism', int targetFps = 60}) async {}

  /// Toggle pause/resume of the render loop.
  Future<void> togglePause() async {
    await _channel.invokeMethod('togglePause');
  }

  /// Check if the native engine is initialized and rendering.
  Future<bool> isInitialized() async {
    final result = await _channel.invokeMethod<bool>('isInitialized');
    return result ?? false;
  }

  /// Get the current engine state (isPaused, fps).
  Future<Map<String, dynamic>> getState() async {
    final result = await _channel.invokeMethod<Map>('getState');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// Shut down the engine and release all resources.
  Future<void> shutdown() async {
    await _channel.invokeMethod('shutdown');
  }

  // Rendering methods — stubs for Android (no native prism-native binary).
  // isRendererReady returns true so the loading overlay hides after initialize().
  // GLB loading is handled natively on Android; Flutter-side loadGltfFromPath is a no-op.
  Future<void> loadGltfFromPath(String path) async {}
  bool get isRendererReady => true;

  /// Always 0.0 on Android — FPS is tracked natively but not yet bridged to Dart.
  /// The AndroidView handles gestures natively via its PrismSurface touch listener;
  /// calling orbitBy/zoom from Dart has no effect on Android.
  double get fps => 0.0;

  /// No-op on Android — orbit is driven by the native platform view's touch listener.
  void orbitBy(double dx, double dy) {}

  /// No-op on Android — zoom is driven by the native platform view's touch listener.
  void zoom(double delta) {}
}
