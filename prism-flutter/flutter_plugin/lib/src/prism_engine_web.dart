import 'prism_engine_interface.dart';
import 'prism_web_plugin.dart';

/// Web implementation of PrismEngine that delegates to WASM-exported JS functions.
///
/// The render loop is driven by requestAnimationFrame inside the Kotlin/WASM module.
/// Each instance is bound to a specific canvas via [attachCanvas], allowing multiple
/// PrismRenderView widgets to coexist without state conflicts.
class PrismEngine implements PrismEngineInterface {
  String? _canvasId;

  /// Bind this engine to a specific canvas element. Called by PrismRenderView
  /// when the platform view is created.
  void attachCanvas(String canvasId) {
    _canvasId = canvasId;
  }

  /// No-op on web: the WASM module drives itself via requestAnimationFrame.
  Future<void> initialize({String appName = 'Prism', int targetFps = 60}) async {}

  /// No native handle on web; returns 0 as the sentinel "not initialized" value.
  int get handle => 0;

  /// Toggle pause/resume of the render loop.
  Future<void> togglePause() async {
    final id = _canvasId ?? PrismWebEngine.lastCanvasId;
    if (id != null) await PrismWebEngine.togglePause(id);
  }

  /// Check if the WASM engine is initialized and rendering.
  Future<bool> isInitialized() async {
    final id = _canvasId ?? PrismWebEngine.lastCanvasId;
    if (id == null) return false;
    return PrismWebEngine.isInitialized(id);
  }

  /// Get the current engine state (isPaused, fps).
  Future<Map<String, dynamic>> getState() async {
    final id = _canvasId ?? PrismWebEngine.lastCanvasId;
    if (id == null) return {};
    return PrismWebEngine.getState(id);
  }

  /// Shut down the engine and release all resources.
  Future<void> shutdown() async {
    final id = _canvasId ?? PrismWebEngine.lastCanvasId;
    if (id != null) await PrismWebEngine.shutdown(id);
    _canvasId = null;
  }

  // ── Scene loading ────────────────────────────────────────────────────────

  /// No-op on web — the GLB is passed directly to [PrismWebEngine.init] via
  /// the canvas render view (not as a filesystem path).
  Future<void> loadGltfFromPath(String path) async {}

  /// Always true on web — the WASM canvas manages its own startup state and
  /// does not need a Flutter-level loading overlay.
  bool get isRendererReady => true;

  /// Smoothed FPS for the most recently initialised canvas. Synchronous.
  double get fps =>
      PrismWebEngine.getFps(_canvasId ?? PrismWebEngine.lastCanvasId ?? '');

  // ── Camera control ───────────────────────────────────────────────────────

  /// No-op on web — gesture handling is not wired via the engine API.
  void orbitBy(double dx, double dy) {}

  /// No-op on web.
  void zoom(double delta) {}

  // ── Asset path helper ────────────────────────────────────────────────────

  /// Returns null on web — assets are fetched by URL inside the WASM module,
  /// not via a filesystem path resolved from a method channel.
  static Future<String?> resolveFlutterAssetPath(String assetKey) async =>
      null;
}
