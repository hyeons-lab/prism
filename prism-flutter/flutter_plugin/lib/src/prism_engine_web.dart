import 'prism_web_plugin.dart';

/// Web implementation of PrismEngine that delegates to WASM-exported JS functions.
///
/// The render loop is driven by requestAnimationFrame inside the Kotlin/WASM module.
/// This class provides control methods matching the mobile PrismEngine API.
class PrismEngine {
  /// Set the cube rotation speed in degrees per second.
  Future<void> setRotationSpeed(double degreesPerSecond) =>
      PrismWebEngine.setRotationSpeed(degreesPerSecond);

  /// Toggle pause/resume of the rotation animation.
  Future<void> togglePause() => PrismWebEngine.togglePause();

  /// Set the cube color (RGB, 0.0 to 1.0).
  Future<void> setCubeColor(double r, double g, double b) =>
      PrismWebEngine.setCubeColor(r, g, b);

  /// Check if the WASM engine is initialized and rendering.
  Future<bool> isInitialized() => PrismWebEngine.isInitialized();

  /// Get the current engine state (rotationSpeed, isPaused, fps).
  Future<Map<String, dynamic>> getState() => PrismWebEngine.getState();

  /// Shut down the engine and release all resources.
  Future<void> shutdown() => PrismWebEngine.shutdown();
}
