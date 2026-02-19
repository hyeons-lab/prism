import 'package:flutter/services.dart';

/// Mobile implementation of PrismEngine using platform method channels.
///
/// The render loop is driven natively (Choreographer on Android, MTKView on iOS).
/// This class provides control methods to adjust the PBR demo scene.
class PrismEngine {
  static const MethodChannel _channel =
      MethodChannel('engine.prism.flutter/engine');

  /// No-op on mobile — canvas binding is only needed for web multi-instance support.
  void attachCanvas(String canvasId) {}

  /// Toggle pause/resume of the render loop.
  Future<void> togglePause() async {
    await _channel.invokeMethod('togglePause');
  }

  /// Set the metallic factor for the PBR spheres (0.0 to 1.0).
  Future<void> setMetallic(double metallic) async {
    await _channel.invokeMethod('setMetallic', {'metallic': metallic});
  }

  /// Set the roughness factor for the PBR spheres (0.0 to 1.0).
  Future<void> setRoughness(double roughness) async {
    await _channel.invokeMethod('setRoughness', {'roughness': roughness});
  }

  /// Set the environment (IBL) intensity (0.0 to 2.0).
  Future<void> setEnvIntensity(double intensity) async {
    await _channel.invokeMethod('setEnvIntensity', {'intensity': intensity});
  }

  /// Check if the native engine is initialized and rendering.
  Future<bool> isInitialized() async {
    final result = await _channel.invokeMethod<bool>('isInitialized');
    return result ?? false;
  }

  /// Get the current engine state (metallic, roughness, envIntensity, isPaused, fps).
  Future<Map<String, dynamic>> getState() async {
    final result = await _channel.invokeMethod<Map>('getState');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// Shut down the engine and release all resources.
  Future<void> shutdown() async {
    await _channel.invokeMethod('shutdown');
  }
}
