/// Web implementation of the Prism SDK Dart wrapper.
/// Routes all calls to the prism.mjs WASM module via generated @JS() bindings.
///
/// Requires `generated/prism_js_bindings.dart` to be present.
/// Run: ./gradlew :prism-flutter:generateDartJsBindings
// ignore: unused_import
import 'dart:js_interop';

import 'generated/prism_js_bindings.dart';
import 'prism_sdk_types.dart';

export 'prism_sdk_types.dart';

// ── Engine ────────────────────────────────────────────────────────────────────

class EngineTime {
  final String _h;
  EngineTime._(this._h);
  double get deltaTime => prismEngineGetDeltaTime(_h);
  double get totalTime => prismEngineGetTotalTime(_h);
}

class Engine {
  final String _h;
  late final EngineTime time;

  Engine([EngineConfig config = const EngineConfig()])
      : _h = prismCreateEngine(config.appName, config.targetFps.toDouble()) {
    prismEngineInitialize(_h);
    time = EngineTime._(_h);
  }

  bool get isAlive => prismEngineIsAlive(_h);
  void destroy() => prismDestroyEngine(_h);
}

// ── ECS World ─────────────────────────────────────────────────────────────────

class World {
  final String _h;
  World() : _h = prismCreateWorld();

  Entity createEntity() => Entity(prismWorldCreateEntity(_h).toInt());
  void destroyEntity(Entity entity) =>
      prismWorldDestroyEntity(_h, entity.id.toDouble());

  /// Supported component types: [TransformComponent].
  void addComponent<T>(Entity entity, T component) {
    if (component is TransformComponent) {
      prismWorldAddTransformComponent(
        _h,
        entity.id.toDouble(),
        component.position.x,
        component.position.y,
        component.position.z,
      );
    }
  }

  void destroy() => prismDestroyWorld(_h);
}

// ── Scene graph ───────────────────────────────────────────────────────────────

class Node {
  final String _h;
  Node._(this._h);

  void setPosition(double x, double y, double z) =>
      prismNodeSetPosition(_h, x, y, z);
  void setRotation(double x, double y, double z, double w) =>
      prismNodeSetRotation(_h, x, y, z, w);
  void setScale(double x, double y, double z) =>
      prismNodeSetScale(_h, x, y, z);
  void destroy() => prismDestroyNode(_h);

  // @internal — used by Scene
  String get _handle => _h;
}

class MeshNode extends Node {
  MeshNode([String name = 'MeshNode']) : super._(prismCreateMeshNode(name));
}

class CameraNode extends Node {
  CameraNode([String name = 'CameraNode'])
      : super._(prismCreateCameraNode(name));
}

class LightNode extends Node {
  LightNode([String name = 'LightNode']) : super._(prismCreateLightNode(name));
}

class Scene {
  final String _h;
  Scene([String name = 'Scene']) : _h = prismCreateScene(name);

  void addNode(Node node) => prismSceneAddNode(_h, node._handle);
  set activeCamera(CameraNode cam) =>
      prismSceneSetActiveCamera(_h, cam._handle);
  void update(double deltaTime) => prismSceneUpdate(_h, deltaTime);
  void destroy() => prismDestroyScene(_h);
}
