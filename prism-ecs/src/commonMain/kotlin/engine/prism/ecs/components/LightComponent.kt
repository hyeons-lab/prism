package engine.prism.ecs.components

import engine.prism.ecs.Component
import engine.prism.math.Vec3
import engine.prism.renderer.Color

enum class LightType {
  DIRECTIONAL,
  POINT,
  SPOT,
}

data class LightComponent(
  var lightType: LightType = LightType.DIRECTIONAL,
  var color: Color = Color.WHITE,
  var intensity: Float = 1f,
  var range: Float = 10f,
  var spotAngle: Float = 45f,
  var direction: Vec3 = Vec3(0f, -1f, 0f),
) : Component
