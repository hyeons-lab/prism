package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.math.Vec3

/** Light type constants matching WGSL shader expectations. */
enum class LightType(val value: Int) {
  DIRECTIONAL(0),
  POINT(1),
  SPOT(2),
}

/**
 * GPU-ready light descriptor. Each instance packs to 64 bytes (16 floats) for direct upload to a
 * storage buffer.
 *
 * Layout (std430):
 * ```
 * vec3f position   + f32 type       (16 bytes)
 * vec3f direction  + f32 intensity  (16 bytes)
 * vec3f color      + f32 range      (16 bytes)
 * f32 spotAngle    + vec3f _pad     (16 bytes)
 * ```
 */
data class LightData(
  val type: LightType = LightType.DIRECTIONAL,
  val position: Vec3 = Vec3.ZERO,
  val direction: Vec3 = Vec3(0f, -1f, 0f),
  val color: Color = Color.WHITE,
  val intensity: Float = 1f,
  val range: Float = 10f,
  val spotAngle: Float = 45f,
) {

  /** Pack into a 16-float array matching the GPU struct layout (64 bytes). */
  fun toFloatArray(): FloatArray =
    floatArrayOf(
      // vec4: position.xyz, type
      position.x,
      position.y,
      position.z,
      type.value.toFloat(),
      // vec4: direction.xyz, intensity
      direction.x,
      direction.y,
      direction.z,
      intensity,
      // vec4: color.rgb, range
      color.r,
      color.g,
      color.b,
      range,
      // vec4: spotAngle, pad, pad, pad
      spotAngle,
      0f,
      0f,
      0f,
    )

  companion object {
    /** Size of one light in floats. */
    const val FLOAT_COUNT = 16

    /** Size of one light in bytes. */
    const val SIZE_BYTES = 64L
  }
}
