/// Shared value types for the Prism SDK Dart wrapper.
/// These are pure Dart — no platform dependencies.

class Vec3 {
  final double x, y, z;
  const Vec3(this.x, this.y, this.z);
  static const zero = Vec3(0, 0, 0);
  static const one = Vec3(1, 1, 1);
}

class EngineConfig {
  final String appName;
  final int targetFps;
  const EngineConfig({this.appName = 'Prism', this.targetFps = 60});
}

class TransformComponent {
  final Vec3 position;
  const TransformComponent({this.position = Vec3.zero});
}

/// Opaque entity handle. Wraps the integer ID returned by World.createEntity().
class Entity {
  final int id;
  const Entity(this.id);
}
