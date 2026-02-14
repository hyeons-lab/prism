package engine.prism.ecs.components

import engine.prism.ecs.Component
import engine.prism.math.Quaternion
import engine.prism.math.Transform
import engine.prism.math.Vec3

data class TransformComponent(
    var position: Vec3 = Vec3.ZERO,
    var rotation: Quaternion = Quaternion.identity(),
    var scale: Vec3 = Vec3.ONE,
) : Component {
    fun toTransform(): Transform = Transform(position, rotation, scale)
}
