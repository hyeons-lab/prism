/// SDK implementation for native platforms (FFI).
///
/// Mirrors the web/WASM API but calls into prism-native via Dart FFI.
/// This file is selected when [dart.library.ffi] is available.
import 'dart:ffi';
import 'dart:io';
import 'package:ffi/ffi.dart';
import 'generated/prism_native_bindings.dart';
import 'prism_sdk_types.dart';

// Module-level singleton: the dynamic library is loaded exactly once here.
// Returns null when the native library is unavailable (e.g., running Flutter
// tests on a Linux/Windows host without a built libprism, or if this file is
// accidentally imported on Android). All classes guard against null bindings so
// construction never throws.
final DynamicLibrary? _lib = _openLibraryOrNull();
final PrismNativeBindings? _bindings = _lib != null ? PrismNativeBindings(_lib!) : null;

DynamicLibrary? _openLibraryOrNull() {
  try {
    return Platform.isLinux
        ? DynamicLibrary.open('libprism.so')
        : Platform.isWindows
            ? DynamicLibrary.open('prism.dll')
            : DynamicLibrary.process();
  } on ArgumentError {
    // Native library not available (e.g., test environment or unsupported platform).
    return null;
  }
}

NativeFinalizer? _lookupFinalizer(String symbol) {
  final lib = _lib;
  if (lib == null) return null;
  try {
    return NativeFinalizer(
        lib.lookup<NativeFunction<Void Function(Pointer<Void>)>>(symbol));
  } on ArgumentError {
    return null;
  }
}

// NativeFinalizer function pointers — the _ptr variants accept Pointer<Void>
// directly. The token passed to attach() carries the handle as its address
// via Pointer.fromAddress(handle); rawValue in the Kotlin wrapper recovers the
// original Long handle so the correct destroy function is called.
final NativeFinalizer? _destroyEngineFinalizer =
    _lookupFinalizer('prism_destroy_engine_ptr');
final NativeFinalizer? _destroyWorldFinalizer =
    _lookupFinalizer('prism_destroy_world_ptr');
final NativeFinalizer? _destroyNodeFinalizer =
    _lookupFinalizer('prism_destroy_node_ptr');
final NativeFinalizer? _destroySceneFinalizer =
    _lookupFinalizer('prism_destroy_scene_ptr');

// ── Engine ────────────────────────────────────────────────────────────────────

class EngineTime {
  final int _h;
  EngineTime._(this._h);
  double get deltaTime => _bindings?.prism_engine_get_delta_time(_h) ?? 0.0;
  double get totalTime => _bindings?.prism_engine_get_total_time(_h) ?? 0.0;
}

class Engine implements Finalizable {
  final int _h;
  late final EngineTime time;

  Engine([EngineConfig config = const EngineConfig()])
      : _h = _create(config.appName, config.targetFps) {
    if (_h != 0) {
      _destroyEngineFinalizer?.attach(this, Pointer<Void>.fromAddress(_h),
          detach: this);
      _bindings?.prism_engine_initialize(_h);
    }
    time = EngineTime._(_h);
  }

  static int _create(String name, int fps) {
    final b = _bindings;
    if (b == null) return 0;
    final nativeName = name.toNativeUtf8();
    try {
      return b.prism_create_engine(nativeName.cast<Void>(), fps);
    } finally {
      malloc.free(nativeName);
    }
  }

  bool get isAlive => _bindings?.prism_engine_is_alive(_h) != 0;
  void destroy() {
    _destroyEngineFinalizer?.detach(this);
    _bindings?.prism_destroy_engine(_h);
  }

  /// Raw engine handle — exposed so platform views can pass it to the C API.
  int get handle => _h;
}

// ── ECS World ─────────────────────────────────────────────────────────────────

class World implements Finalizable {
  final int _h;

  World() : _h = _bindings?.prism_create_world() ?? 0 {
    if (_h != 0) {
      _destroyWorldFinalizer?.attach(this, Pointer<Void>.fromAddress(_h),
          detach: this);
    }
  }

  Entity createEntity() =>
      Entity(_bindings?.prism_world_create_entity(_h) ?? 0);
  void destroyEntity(Entity entity) =>
      _bindings?.prism_world_destroy_entity(_h, entity.id);

  void addComponent<T>(Entity entity, T component) {
    if (component is TransformComponent) {
      _bindings?.prism_world_add_transform_component(
        _h,
        entity.id,
        component.position.x.toDouble(),
        component.position.y.toDouble(),
        component.position.z.toDouble(),
      );
    } else {
      throw ArgumentError.value(
          component, 'component', 'Unsupported component type: ${component.runtimeType}');
    }
  }

  void destroy() {
    _destroyWorldFinalizer?.detach(this);
    _bindings?.prism_destroy_world(_h);
  }
}

// ── Scene graph ───────────────────────────────────────────────────────────────

class Node implements Finalizable {
  final int _h;

  Node._(this._h) {
    if (_h != 0) {
      _destroyNodeFinalizer?.attach(this, Pointer<Void>.fromAddress(_h),
          detach: this);
    }
  }

  void setPosition(double x, double y, double z) =>
      _bindings?.prism_node_set_position(_h, x, y, z);
  void setRotation(double x, double y, double z, double w) =>
      _bindings?.prism_node_set_rotation(_h, x, y, z, w);
  void setScale(double x, double y, double z) =>
      _bindings?.prism_node_set_scale(_h, x, y, z);
  void destroy() {
    _destroyNodeFinalizer?.detach(this);
    _bindings?.prism_destroy_node(_h);
  }

  int get _handle => _h;
}

class MeshNode extends Node {
  MeshNode([String name = 'MeshNode']) : super._(_create(name));
  static int _create(String name) {
    final b = _bindings;
    if (b == null) return 0;
    final nativeName = name.toNativeUtf8();
    try {
      return b.prism_create_mesh_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class CameraNode extends Node {
  CameraNode([String name = 'CameraNode']) : super._(_create(name));
  static int _create(String name) {
    final b = _bindings;
    if (b == null) return 0;
    final nativeName = name.toNativeUtf8();
    try {
      return b.prism_create_camera_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class LightNode extends Node {
  LightNode([String name = 'LightNode']) : super._(_create(name));
  static int _create(String name) {
    final b = _bindings;
    if (b == null) return 0;
    final nativeName = name.toNativeUtf8();
    try {
      return b.prism_create_light_node(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }
}

class Scene implements Finalizable {
  final int _h;

  Scene([String name = 'Scene']) : _h = _create(name) {
    if (_h != 0) {
      _destroySceneFinalizer?.attach(this, Pointer<Void>.fromAddress(_h),
          detach: this);
    }
  }

  static int _create(String name) {
    final b = _bindings;
    if (b == null) return 0;
    final nativeName = name.toNativeUtf8();
    try {
      return b.prism_create_scene(nativeName.cast<Void>());
    } finally {
      malloc.free(nativeName);
    }
  }

  void addNode(Node node) => _bindings?.prism_scene_add_node(_h, node._handle);
  set activeCamera(CameraNode cam) =>
      _bindings?.prism_scene_set_active_camera(_h, cam._handle);
  void update(double deltaTime) => _bindings?.prism_scene_update(_h, deltaTime);
  void destroy() {
    _destroySceneFinalizer?.detach(this);
    _bindings?.prism_destroy_scene(_h);
  }
}
