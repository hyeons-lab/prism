package engine.prism.math

import kotlin.math.sqrt

data class Vec2(val x: Float = 0f, val y: Float = 0f) {

  operator fun plus(other: Vec2): Vec2 = Vec2(x + other.x, y + other.y)

  operator fun minus(other: Vec2): Vec2 = Vec2(x - other.x, y - other.y)

  operator fun times(scalar: Float): Vec2 = Vec2(x * scalar, y * scalar)

  operator fun div(scalar: Float): Vec2 {
    require(scalar != 0f) { "Cannot divide by zero" }
    val inv = 1f / scalar
    return Vec2(x * inv, y * inv)
  }

  operator fun unaryMinus(): Vec2 = Vec2(-x, -y)

  fun dot(other: Vec2): Float = x * other.x + y * other.y

  fun lengthSquared(): Float = x * x + y * y

  fun length(): Float = sqrt(lengthSquared())

  fun normalize(): Vec2 {
    val len = length()
    if (len < EPSILON) return ZERO
    val inv = 1f / len
    return Vec2(x * inv, y * inv)
  }

  fun distanceTo(other: Vec2): Float = (this - other).length()

  companion object {
    private const val EPSILON = 1e-7f

    val ZERO = Vec2(0f, 0f)
    val ONE = Vec2(1f, 1f)
    val UP = Vec2(0f, 1f)
    val RIGHT = Vec2(1f, 0f)
  }
}

operator fun Float.times(vec: Vec2): Vec2 = vec * this
