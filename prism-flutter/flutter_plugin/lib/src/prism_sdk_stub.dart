/// Stub implementation of the Prism SDK Dart wrapper for non-web platforms.
///
/// The full ECS/scene-graph SDK is only available on web where the prism.mjs
/// WASM module runs. On Android, iOS, and desktop use the Flutter plugin API
/// (PrismEngine, PrismRenderView) from prism_flutter.dart instead.

export 'prism_sdk_types.dart';

// ── Engine ────────────────────────────────────────────────────────────────────

class EngineTime {
  EngineTime._();
  double get deltaTime =>
      throw UnsupportedError('Prism SDK is web-only; use PrismEngine on native');
  double get totalTime =>
      throw UnsupportedError('Prism SDK is web-only; use PrismEngine on native');
}

class Engine {
  Engine([EngineConfig config = const EngineConfig()]) {
    throw UnsupportedError('Prism SDK is web-only; use PrismEngine on native');
  }
  bool get isAlive => false;
  EngineTime get time => EngineTime._();
  void destroy() {}
}

// ── ECS World ─────────────────────────────────────────────────────────────────

class World {
  World() {
    throw UnsupportedError('Prism SDK is web-only; use PrismEngine on native');
  }
  Entity createEntity() => throw UnsupportedError();
  void destroyEntity(Entity entity) => throw UnsupportedError();
  void addComponent<T>(Entity entity, T component) => throw UnsupportedError();
  void destroy() {}
}

// ── Scene graph ───────────────────────────────────────────────────────────────

class Node {
  Node._();
  void setPosition(double x, double y, double z) => throw UnsupportedError();
  void setRotation(double x, double y, double z, double w) =>
      throw UnsupportedError();
  void setScale(double x, double y, double z) => throw UnsupportedError();
  void destroy() {}
  String get _handle => '';
}

class MeshNode extends Node {
  MeshNode([String name = 'MeshNode']) : super._() {
    throw UnsupportedError('Prism SDK is web-only');
  }
}

class CameraNode extends Node {
  CameraNode([String name = 'CameraNode']) : super._() {
    throw UnsupportedError('Prism SDK is web-only');
  }
}

class LightNode extends Node {
  LightNode([String name = 'LightNode']) : super._() {
    throw UnsupportedError('Prism SDK is web-only');
  }
}

class Scene {
  Scene([String name = 'Scene']) {
    throw UnsupportedError('Prism SDK is web-only');
  }
  void addNode(Node node) => throw UnsupportedError();
  set activeCamera(CameraNode cam) => throw UnsupportedError();
  void update(double deltaTime) => throw UnsupportedError();
  void destroy() {}
}
