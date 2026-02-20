package com.hyeonslab.prism.ecs.components

import com.hyeonslab.prism.ecs.Component
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Transform
import com.hyeonslab.prism.math.Vec3
data class TransformComponent(
  var position: Vec3 = Vec3.ZERO,
  var rotation: Quaternion = Quaternion.identity(),
  var scale: Vec3 = Vec3.ONE,
) : Component {
  fun toTransform(): Transform = Transform(position, rotation, scale)
}
