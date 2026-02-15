package com.hyeonslab.prism.math

import kotlin.math.sqrt

data class Vec4(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 0f) {

  operator fun plus(other: Vec4): Vec4 = Vec4(x + other.x, y + other.y, z + other.z, w + other.w)

  operator fun minus(other: Vec4): Vec4 = Vec4(x - other.x, y - other.y, z - other.z, w - other.w)

  operator fun times(scalar: Float): Vec4 = Vec4(x * scalar, y * scalar, z * scalar, w * scalar)

  operator fun div(scalar: Float): Vec4 {
    require(scalar != 0f) { "Cannot divide by zero" }
    val inv = 1f / scalar
    return Vec4(x * inv, y * inv, z * inv, w * inv)
  }

  operator fun unaryMinus(): Vec4 = Vec4(-x, -y, -z, -w)

  fun dot(other: Vec4): Float = x * other.x + y * other.y + z * other.z + w * other.w

  fun lengthSquared(): Float = x * x + y * y + z * z + w * w

  fun length(): Float = sqrt(lengthSquared())

  fun normalize(): Vec4 {
    val len = length()
    if (len < EPSILON) return ZERO
    val inv = 1f / len
    return Vec4(x * inv, y * inv, z * inv, w * inv)
  }

  fun toVec3(): Vec3 = Vec3(x, y, z)

  companion object {
    private const val EPSILON = 1e-7f

    val ZERO = Vec4(0f, 0f, 0f, 0f)
    val ONE = Vec4(1f, 1f, 1f, 1f)
  }
}

operator fun Float.times(vec: Vec4): Vec4 = vec * this
