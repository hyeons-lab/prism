import 'dart:io' show Directory, File, Platform;

import 'package:flutter/services.dart' show rootBundle;

import 'prism_engine_channel.dart' as channel;
import 'prism_engine_ffi.dart' as ffi; // used for PrismEngine() constructor
import 'prism_engine_interface.dart';

/// Runtime dispatcher: MethodChannel on Android; FFI on all other
/// native platforms (iOS, macOS, Linux, Windows).
///
/// Android uses the MethodChannel bridge because it lacks a prism-native binary.
/// iOS, macOS, Linux, and Windows talk directly to the prism-native C API via Dart FFI.
///
/// Prerequisite: run `./gradlew :prism-native:generateFfiBindings` before
/// building on any FFI platform (generates prism_native_bindings.dart).
class PrismEngine implements PrismEngineInterface {
  final PrismEngineInterface _impl;

  PrismEngine()
      : _impl = (Platform.isAndroid)
            ? channel.PrismEngine()
            : ffi.PrismEngine();

  /// Raw engine handle (non-zero on FFI platforms). Used by platform views to
  /// call prism-native C API functions (e.g. prism_attach_metal_layer).
  int get handle => _impl.handle;

  void attachCanvas(String canvasId) => _impl.attachCanvas(canvasId);
  Future<void> initialize({String appName = 'Prism', int targetFps = 60}) =>
      _impl.initialize(appName: appName, targetFps: targetFps);
  Future<void> togglePause() => _impl.togglePause();
  Future<bool> isInitialized() => _impl.isInitialized();
  Future<Map<String, dynamic>> getState() => _impl.getState();
  Future<void> shutdown() => _impl.shutdown();

  // ── Scene loading ────────────────────────────────────────────────────────────

  /// Loads the glTF/GLB model at the filesystem path [glbPath] and initialises a
  /// full rendering scene. Must be called after the platform view has created the
  /// Metal surface. Use [resolveFlutterAssetPath] to obtain the path from an asset
  /// key.
  Future<void> loadGltfFromPath(String glbPath) async =>
      _impl.loadGltfFromPath(glbPath);

  /// Returns true once [loadGltfFromPath] has completed and the scene is ready to
  /// render.
  bool get isRendererReady => _impl.isRendererReady;

  /// Smoothed frames-per-second. Safe to call every frame.
  double get fps => _impl.fps;

  // ── Camera control ───────────────────────────────────────────────────────────

  /// Rotates the orbit camera by [dx] radians horizontally and [dy] radians
  /// vertically. Wire to pan/drag gesture events.
  void orbitBy(double dx, double dy) => _impl.orbitBy(dx, dy);

  /// Adjusts the orbit radius by [delta] units (positive = zoom in). Wire to
  /// pinch or scroll-wheel events.
  void zoom(double delta) => _impl.zoom(delta);

  // ── Asset path helper ────────────────────────────────────────────────────────

  /// In-memory cache mapping asset keys to their extracted temp-file paths.
  static final _assetPathCache = <String, String>{};

  /// Resolves a Flutter asset key (e.g. `'assets/DamagedHelmet.glb'`) to an
  /// absolute filesystem path the native engine can open directly.
  ///
  /// The asset is read from Flutter's asset bundle via [rootBundle] and written
  /// to the system temp directory on first use. Subsequent calls return the
  /// cached path without re-reading the bundle. This approach works on all FFI
  /// platforms (iOS, macOS, Linux, Windows) without platform-specific Swift or
  /// native code.
  ///
  /// Returns null on Android (the native PrismSurface loads assets directly via
  /// the AssetManager) and when the asset cannot be loaded.
  static Future<String?> resolveFlutterAssetPath(String assetKey) async {
    if (Platform.isAndroid) return null;
    final cached = _assetPathCache[assetKey];
    if (cached != null) return cached;
    try {
      final bytes = await rootBundle.load(assetKey);
      final fileName = assetKey.replaceAll('/', '_');
      final tempFile = File('${Directory.systemTemp.path}/$fileName');
      await tempFile.writeAsBytes(bytes.buffer.asUint8List(), flush: true);
      _assetPathCache[assetKey] = tempFile.path;
      return tempFile.path;
    } catch (_) {
      return null;
    }
  }
}
