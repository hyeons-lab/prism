/// SDK implementation for native platforms (FFI).
///
/// Mirrors the web/WASM API but calls into prism-native via Dart FFI.
/// This file is selected when [dart.library.ffi] is available.
import 'dart:ffi';
import 'dart:io';
import 'package:ffi/ffi.dart';
import 'generated/prism_native_bindings.dart';
import 'prism_sdk_types.dart';

PrismNativeBindings _loadBindings() {
  final lib = Platform.isLinux
      ? DynamicLibrary.open('libprism.so')
      : Platform.isWindows
          ? DynamicLibrary.open('prism.dll')
          : DynamicLibrary.process();
  return PrismNativeBindings(lib);
}

final _bindings = _loadBindings();

// ── Engine ────────────────────────────────────────────────────────────────────

class EngineTime {
  final int _h;
  EngineTime._(this._h);
  double get deltaTime => _bindings.prism_engine_get_delta_time(_h);
  double get totalTime => _bindings.prism_engine_get_total_time(_h);
}

class Engine {
  final int _h;
  late final EngineTime time;

  static final _finalizer = NativeFinalizer(_bindings.prism_destroy_engine.cast());

  Engine([EngineConfig config = const EngineConfig()])
      : _h = _create(config.appName, config.targetFps) {
    _finalizer.attach(this, Pointer.fromAddress(_h), detach: this);
    _bindings.prism_engine_initialize(_h);
    time = EngineTime._(_h);
  }

  static int _create(String name, int fps) {
    final nativeName = name.toNativeUtf8();
    try {
      return _bindings.prism_create_engine(nativeName.cast<Void>(), fps);
    } finally {
      malloc.free(nativeName);
    }
  }

  bool get isAlive => _bindings.prism_engine_is_alive(_h) != 0;
  void destroy() {
    _finalizer.detach(this);
    _bindings.prism_destroy_engine(_h);
  }

  /// Raw engine handle — exposed so platform views can pass it to the C API.
  int get handle => _h;
}

// ── ECS World ─────────────────────────────────────────────────────────────────

class World {
  final int _h;
  static final _finalizer = NativeFinalizer(_bindings.prism_destroy_world.cast());

  World() : _h = _bindings.prism_create_world() {
    _finalizer.attach(this, Pointer.fromAddress(_h), detach: this);
  }

  Entity createEntity() => Entity(_bindings.prism_world_create_entity(_h));
  void destroyEntity(Entity entity) =>
      _bindings.prism_world_destroy_entity(_h, entity.id);

  void addComponent<T>(Entity entity, T component) {
    if (component is TransformComponent) {
      _bindings.prism_world_add_transform_component(
        _h,
        entity.id,
        component.position.x.toDouble(),
        component.position.y.toDouble(),
        component.position.z.toDouble(),
      );
    } else {
      throw ArgumentError.value(
          component, 'component', 'Unsupported component type: $T');
    }
  }

  void destroy() {
    _finalizer.detach(this);
    _bindings.prism_destroy_world(_h);
  }
}

// ── Scene graph ───────────────────────────────────────────────────────────────

class Node {
  final int _h;
  static final _finalizer = NativeFinalizer(_bindings.prism_destroy_node.cast());

  Node._(this._h) {
    _finalizer.attach(this, Pointer.fromAddress(_h), detach: this);
  }

  void setPosition(double x, double y, double z) =>
      _bindings.prism_node_set_position(_h, x, y, z);
  void setRotation(double x, double y, double z, double w) =>
      _bindings.prism_node_set_rotation(_h, x, y, z, w);
  void setScale(double x, double y, double z) =>
      _bindings.prism_node_set_scale(_h, x, y, z);
  void destroy() {
    _finalizer.detach(this);
    _bindings.prism_destroy_node(_h);
  }

  int get _handle => _h;
}

class MeshNode extends Node {
  MeshNode([String name = 'MeshNode']) : super._(_create(name));
  static int _create(String name) {
    final nativeName = name.toNativeUtf8();
    try {
      return _bindings.prism_create_mesh_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class CameraNode extends Node {
  CameraNode([String name = 'CameraNode']) : super._(_create(name));
  static int _create(String name) {
    final nativeName = name.toNativeUtf8();
    try {
      return _bindings.prism_create_camera_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class LightNode extends Node {
  LightNode([String name = 'LightNode']) : super._(_create(name));
  static int _create(String name) {
    final nativeName = name.toNativeUtf8();
    try {
      return _bindings.prism_create_light_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class Scene {
  final int _h;
  static final _finalizer = NativeFinalizer(_bindings.prism_destroy_scene.cast());

  Scene([String name = 'Scene']) : _h = _create(name) {
    _finalizer.attach(this, Pointer.fromAddress(_h), detach: this);
  }

  static int _create(String name) {
    final nativeName = name.toNativeUtf8();
    try {
      return _bindings.prism_create_scene(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }

  void addNode(Node node) => _bindings.prism_scene_add_node(_h, node._handle);
  set activeCamera(CameraNode cam) =>
      _bindings.prism_scene_set_active_camera(_h, cam._handle);
  void update(double deltaTime) => _bindings.prism_scene_update(_h, deltaTime);
  void destroy() {
    _finalizer.detach(this);
    _bindings.prism_destroy_scene(_h);
  }
}
