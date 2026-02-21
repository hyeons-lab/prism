import 'package:flutter/services.dart';

/// Mobile implementation of PrismEngine using platform method channels.
///
/// The render loop is driven natively (Choreographer on Android, MTKView on iOS).
/// This class provides control methods to adjust the demo scene.
class PrismEngine {
  static const MethodChannel _channel =
      MethodChannel('engine.prism.flutter/engine');

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
}
