import 'prism_web_plugin.dart';

/// Web implementation of PrismEngine that delegates to WASM-exported JS functions.
///
/// The render loop is driven by requestAnimationFrame inside the Kotlin/WASM module.
/// Each instance is bound to a specific canvas via [attachCanvas], allowing multiple
/// PrismRenderView widgets to coexist without state conflicts.
class PrismEngine {
  String? _canvasId;

  /// Bind this engine to a specific canvas element. Called by PrismRenderView
  /// when the platform view is created.
  void attachCanvas(String canvasId) {
    _canvasId = canvasId;
  }

  /// Toggle pause/resume of the render loop.
  Future<void> togglePause() async {
    final id = _canvasId;
    if (id != null) await PrismWebEngine.togglePause(id);
  }

  /// Check if the WASM engine is initialized and rendering.
  Future<bool> isInitialized() async {
    final id = _canvasId;
    if (id == null) return false;
    return PrismWebEngine.isInitialized(id);
  }

  /// Get the current engine state (isPaused, fps).
  Future<Map<String, dynamic>> getState() async {
    final id = _canvasId;
    if (id == null) return {};
    return PrismWebEngine.getState(id);
  }

  /// Shut down the engine and release all resources.
  Future<void> shutdown() async {
    final id = _canvasId;
    if (id != null) await PrismWebEngine.shutdown(id);
    _canvasId = null;
  }
}
