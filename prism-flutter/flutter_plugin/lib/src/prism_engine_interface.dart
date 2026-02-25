/// Shared contract that every platform-specific [PrismEngine] backend must
/// satisfy. The Dart compiler enforces this at build time for each platform,
/// so adding a method to one backend but forgetting the others is a
/// compile-error rather than a runtime surprise.
///
/// Implemented by:
/// - [prism_engine_ffi.dart]   — FFI (iOS, macOS, Linux, Windows)
/// - [prism_engine_channel.dart] — MethodChannel (Android)
/// - [prism_engine_web.dart]   — JS/WASM (web)
abstract interface class PrismEngineInterface {
  // ── Lifecycle ────────────────────────────────────────────────────────────

  /// Raw engine handle (non-zero on FFI platforms; 0 on web/channel).
  int get handle;

  /// Bind this instance to a specific canvas element (web only; no-op elsewhere).
  void attachCanvas(String canvasId);

  /// Create and initialise the engine. No-op if already initialised.
  Future<void> initialize({String appName, int targetFps});

  /// Returns true once [initialize] has completed successfully.
  Future<bool> isInitialized();

  /// Returns basic engine state (fps, isPaused, timings).
  Future<Map<String, dynamic>> getState();

  /// Toggle the render-loop pause state.
  Future<void> togglePause();

  /// Shut down and destroy the engine; subsequent calls are no-ops.
  Future<void> shutdown();

  // ── Scene loading ────────────────────────────────────────────────────────

  /// Load the glTF/GLB model at [path] (absolute filesystem path) and
  /// initialise a full rendering scene. No-op on platforms that load
  /// assets by another mechanism (web, Android).
  Future<void> loadGltfFromPath(String path);

  /// True once the scene has loaded and the renderer is producing frames.
  bool get isRendererReady;

  // ── Per-frame state ──────────────────────────────────────────────────────

  /// Smoothed frames-per-second. Safe to call every frame (synchronous).
  double get fps;

  // ── Camera control ───────────────────────────────────────────────────────

  /// Rotate the orbit camera: [dx] radians horizontally, [dy] radians vertically.
  void orbitBy(double dx, double dy);

  /// Adjust the orbit radius by [delta] units (positive = zoom in).
  void zoom(double delta);
}
