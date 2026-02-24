import 'dart:io' show Platform;

import 'package:flutter/services.dart';

import 'prism_engine_channel.dart' as channel;
import 'prism_engine_ffi.dart' as ffi;

/// Runtime dispatcher: MethodChannel on Android; FFI on all other
/// native platforms (iOS, macOS, Linux, Windows).
///
/// Android uses the MethodChannel bridge because it lacks a prism-native binary.
/// iOS, macOS, Linux, and Windows talk directly to the prism-native C API via Dart FFI.
///
/// Prerequisite: run `./gradlew :prism-native:generateFfiBindings` before
/// building on any FFI platform (generates prism_native_bindings.dart).
class PrismEngine {
  final dynamic _impl;

  PrismEngine()
      : _impl = (Platform.isAndroid)
            ? channel.PrismEngine()
            : ffi.PrismEngine();

  /// Raw engine handle (non-zero on FFI platforms). Used by platform views to
  /// call prism-native C API functions (e.g. prism_attach_metal_layer).
  int get handle => (_impl is ffi.PrismEngine) ? _impl.handle : 0;

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
  bool get isRendererReady => _impl.isRendererReady as bool;

  /// Smoothed frames-per-second. Safe to call every frame.
  double get fps => (_impl is ffi.PrismEngine) ? _impl.fps : 0.0;

  // ── Camera control ───────────────────────────────────────────────────────────

  /// Rotates the orbit camera by [dx] radians horizontally and [dy] radians
  /// vertically. Wire to pan/drag gesture events.
  void orbitBy(double dx, double dy) => _impl.orbitBy(dx, dy);

  /// Adjusts the orbit radius by [delta] units (positive = zoom in). Wire to
  /// pinch or scroll-wheel events.
  void zoom(double delta) => _impl.zoom(delta);

  // ── Asset path helper ────────────────────────────────────────────────────────

  /// Resolves a Flutter asset key (e.g. `'assets/DamagedHelmet.glb'`) to its
  /// absolute filesystem path inside the app bundle. Returns null if the platform
  /// cannot resolve the path (e.g. Android).
  static const _channel = MethodChannel('engine.prism.flutter/engine');
  static Future<String?> resolveFlutterAssetPath(String assetKey) async {
    return _channel.invokeMethod<String>('resolveFlutterAssetPath', assetKey);
  }
}
