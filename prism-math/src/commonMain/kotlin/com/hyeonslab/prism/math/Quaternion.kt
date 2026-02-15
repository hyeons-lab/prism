package com.hyeonslab.prism.math

import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A quaternion representing a rotation in 3D space.
 *
 * Convention: (x, y, z) is the vector part, w is the scalar part. Identity quaternion is (0, 0, 0,
 * 1).
 */
data class Quaternion(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f, val w: Float = 1f) {

  /**
   * Hamilton product: this * other.
   *
   * Combines two rotations. Apply `other` first, then `this`.
   */
  operator fun times(other: Quaternion): Quaternion =
    Quaternion(
      w * other.x + x * other.w + y * other.z - z * other.y,
      w * other.y - x * other.z + y * other.w + z * other.x,
      w * other.z + x * other.y - y * other.x + z * other.w,
      w * other.w - x * other.x - y * other.y - z * other.z,
    )

  fun length(): Float = sqrt(x * x + y * y + z * z + w * w)

  fun normalize(): Quaternion {
    val len = length()
    if (len < EPSILON) return IDENTITY
    val inv = 1f / len
    return Quaternion(x * inv, y * inv, z * inv, w * inv)
  }

  fun conjugate(): Quaternion = Quaternion(-x, -y, -z, w)

  /**
   * Returns the inverse (reciprocal) of this quaternion. For unit quaternions, this is equivalent
   * to the conjugate.
   */
  fun inverse(): Quaternion {
    val lenSq = x * x + y * y + z * z + w * w
    if (lenSq < EPSILON) return IDENTITY
    val inv = 1f / lenSq
    return Quaternion(-x * inv, -y * inv, -z * inv, w * inv)
  }

  /** Converts this quaternion to a 4x4 rotation matrix. */
  fun toMat4(): Mat4 {
    val xx = x * x
    val yy = y * y
    val zz = z * z
    val xy = x * y
    val xz = x * z
    val yz = y * z
    val wx = w * x
    val wy = w * y
    val wz = w * z

    return Mat4(
      floatArrayOf(
        1f - 2f * (yy + zz),
        2f * (xy + wz),
        2f * (xz - wy),
        0f,
        2f * (xy - wz),
        1f - 2f * (xx + zz),
        2f * (yz + wx),
        0f,
        2f * (xz + wy),
        2f * (yz - wx),
        1f - 2f * (xx + yy),
        0f,
        0f,
        0f,
        0f,
        1f,
      )
    )
  }

  /** Rotates a Vec3 by this quaternion using the formula: q * v * q^-1 */
  fun rotateVec3(v: Vec3): Vec3 {
    // Optimized rotation avoiding full quaternion multiply:
    // t = 2 * cross(q.xyz, v)
    // result = v + w * t + cross(q.xyz, t)
    val qx = x
    val qy = y
    val qz = z
    val qw = w

    val tx = 2f * (qy * v.z - qz * v.y)
    val ty = 2f * (qz * v.x - qx * v.z)
    val tz = 2f * (qx * v.y - qy * v.x)

    return Vec3(
      v.x + qw * tx + (qy * tz - qz * ty),
      v.y + qw * ty + (qz * tx - qx * tz),
      v.z + qw * tz + (qx * ty - qy * tx),
    )
  }

  companion object {
    private const val EPSILON = 1e-7f
    private val IDENTITY = Quaternion(0f, 0f, 0f, 1f)

    fun identity(): Quaternion = IDENTITY

    /**
     * Creates a quaternion from an axis-angle representation.
     *
     * @param axis the rotation axis (will be normalized)
     * @param radians the rotation angle in radians
     */
    fun fromAxisAngle(axis: Vec3, radians: Float): Quaternion {
      val normAxis = axis.normalize()
      val halfAngle = radians * 0.5f
      val s = sin(halfAngle)
      return Quaternion(normAxis.x * s, normAxis.y * s, normAxis.z * s, cos(halfAngle))
    }

    /**
     * Creates a quaternion from Euler angles (intrinsic Tait-Bryan ZYX convention).
     *
     * @param pitch rotation around X axis in radians
     * @param yaw rotation around Y axis in radians
     * @param roll rotation around Z axis in radians
     */
    fun fromEuler(pitch: Float, yaw: Float, roll: Float): Quaternion {
      val cx = cos(pitch * 0.5f)
      val sx = sin(pitch * 0.5f)
      val cy = cos(yaw * 0.5f)
      val sy = sin(yaw * 0.5f)
      val cz = cos(roll * 0.5f)
      val sz = sin(roll * 0.5f)

      return Quaternion(
        sx * cy * cz - cx * sy * sz,
        cx * sy * cz + sx * cy * sz,
        cx * cy * sz - sx * sy * cz,
        cx * cy * cz + sx * sy * sz,
      )
    }

    /**
     * Spherical linear interpolation between two quaternions.
     *
     * @param a start quaternion
     * @param b end quaternion
     * @param t interpolation factor in [0, 1]
     */
    fun slerp(a: Quaternion, b: Quaternion, t: Float): Quaternion {
      // Compute the dot product
      var dot = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w

      // If the dot product is negative, negate one quaternion to take the short path
      var bx = b.x
      var by = b.y
      var bz = b.z
      var bw = b.w
      if (dot < 0f) {
        dot = -dot
        bx = -bx
        by = -by
        bz = -bz
        bw = -bw
      }

      // If quaternions are very close, use linear interpolation to avoid division by zero
      if (dot > 0.9995f) {
        return Quaternion(
            a.x + t * (bx - a.x),
            a.y + t * (by - a.y),
            a.z + t * (bz - a.z),
            a.w + t * (bw - a.w),
          )
          .normalize()
      }

      val theta = acos(dot.coerceIn(-1f, 1f))
      val sinTheta = sin(theta)
      val wa = sin((1f - t) * theta) / sinTheta
      val wb = sin(t * theta) / sinTheta

      return Quaternion(
        wa * a.x + wb * bx,
        wa * a.y + wb * by,
        wa * a.z + wb * bz,
        wa * a.w + wb * bw,
      )
    }
  }
}
