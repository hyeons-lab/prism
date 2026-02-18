import 'package:flutter/services.dart';

/// Dart-side interface to the Prism engine via platform channels.
///
/// The render loop is driven natively (Choreographer on Android, MTKView on iOS).
/// This class provides control methods to adjust the demo scene.
class PrismEngine {
  static const MethodChannel _channel =
      MethodChannel('engine.prism.flutter/engine');

  /// Set the cube rotation speed in degrees per second.
  Future<void> setRotationSpeed(double degreesPerSecond) async {
    await _channel.invokeMethod('setRotationSpeed', {
      'speed': degreesPerSecond,
    });
  }

  /// Toggle pause/resume of the rotation animation.
  Future<void> togglePause() async {
    await _channel.invokeMethod('togglePause');
  }

  /// Set the cube color (RGB, 0.0 to 1.0).
  Future<void> setCubeColor(double r, double g, double b) async {
    await _channel.invokeMethod('setCubeColor', {
      'r': r,
      'g': g,
      'b': b,
    });
  }

  /// Check if the native engine is initialized and rendering.
  Future<bool> isInitialized() async {
    final result = await _channel.invokeMethod<bool>('isInitialized');
    return result ?? false;
  }

  /// Get the current engine state (rotationSpeed, isPaused, fps).
  Future<Map<String, dynamic>> getState() async {
    final result = await _channel.invokeMethod<Map>('getState');
    return Map<String, dynamic>.from(result ?? {});
  }

  /// Shut down the engine and release all resources.
  Future<void> shutdown() async {
    await _channel.invokeMethod('shutdown');
  }
}
