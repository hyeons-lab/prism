package com.hyeonslab.prism.math

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * A 4x4 matrix stored in column-major order.
 *
 * Column-major layout means the underlying array is organized as:
 * [m00, m10, m20, m30, m01, m11, m21, m31, m02, m12, m22, m32, m03, m13, m23, m33]
 *
 * Where mRC means row R, column C. Element access: data[column * 4 + row]
 */
class Mat4(val data: FloatArray) {

  init {
    require(data.size == 16) { "Mat4 requires exactly 16 elements, got ${data.size}" }
  }

  /** Access element at [row, col] (both 0-based). */
  operator fun get(row: Int, col: Int): Float = data[col * 4 + row]

  /** Matrix multiplication: this * other */
  operator fun times(other: Mat4): Mat4 {
    val result = FloatArray(16)
    for (col in 0..3) {
      for (row in 0..3) {
        var sum = 0f
        for (k in 0..3) {
          sum += this[row, k] * other[k, col]
        }
        result[col * 4 + row] = sum
      }
    }
    return Mat4(result)
  }

  /** Matrix-vector multiplication: this * vec */
  operator fun times(vec: Vec4): Vec4 {
    return Vec4(
      this[0, 0] * vec.x + this[0, 1] * vec.y + this[0, 2] * vec.z + this[0, 3] * vec.w,
      this[1, 0] * vec.x + this[1, 1] * vec.y + this[1, 2] * vec.z + this[1, 3] * vec.w,
      this[2, 0] * vec.x + this[2, 1] * vec.y + this[2, 2] * vec.z + this[2, 3] * vec.w,
      this[3, 0] * vec.x + this[3, 1] * vec.y + this[3, 2] * vec.z + this[3, 3] * vec.w,
    )
  }

  /** Extract the upper-left 3x3 sub-matrix. */
  fun toMat3(): Mat3 =
    Mat3(
      floatArrayOf(data[0], data[1], data[2], data[4], data[5], data[6], data[8], data[9], data[10])
    )

  /**
   * Compute the normal matrix: transpose(inverse(upperLeft3x3)). Returns identity if the upper-left
   * 3x3 is singular (e.g. zero-scale transform).
   */
  fun normalMatrix(): Mat3 {
    val m = toMat3()
    return if (abs(m.determinant()) < 1e-6f) Mat3.identity() else m.inverse().transpose()
  }

  fun transpose(): Mat4 {
    val result = FloatArray(16)
    for (col in 0..3) {
      for (row in 0..3) {
        result[col * 4 + row] = this[col, row]
      }
    }
    return Mat4(result)
  }

  fun determinant(): Float {
    val m = data
    // Using cofactor expansion along the first row
    val a00 = m[0]
    val a01 = m[4]
    val a02 = m[8]
    val a03 = m[12]
    val a10 = m[1]
    val a11 = m[5]
    val a12 = m[9]
    val a13 = m[13]
    val a20 = m[2]
    val a21 = m[6]
    val a22 = m[10]
    val a23 = m[14]
    val a30 = m[3]
    val a31 = m[7]
    val a32 = m[11]
    val a33 = m[15]

    val b00 = a00 * a11 - a01 * a10
    val b01 = a00 * a12 - a02 * a10
    val b02 = a00 * a13 - a03 * a10
    val b03 = a01 * a12 - a02 * a11
    val b04 = a01 * a13 - a03 * a11
    val b05 = a02 * a13 - a03 * a12
    val b06 = a20 * a31 - a21 * a30
    val b07 = a20 * a32 - a22 * a30
    val b08 = a20 * a33 - a23 * a30
    val b09 = a21 * a32 - a22 * a31
    val b10 = a21 * a33 - a23 * a31
    val b11 = a22 * a33 - a23 * a32

    return b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
  }

  fun inverse(): Mat4 {
    val m = data
    val a00 = m[0]
    val a01 = m[4]
    val a02 = m[8]
    val a03 = m[12]
    val a10 = m[1]
    val a11 = m[5]
    val a12 = m[9]
    val a13 = m[13]
    val a20 = m[2]
    val a21 = m[6]
    val a22 = m[10]
    val a23 = m[14]
    val a30 = m[3]
    val a31 = m[7]
    val a32 = m[11]
    val a33 = m[15]

    val b00 = a00 * a11 - a01 * a10
    val b01 = a00 * a12 - a02 * a10
    val b02 = a00 * a13 - a03 * a10
    val b03 = a01 * a12 - a02 * a11
    val b04 = a01 * a13 - a03 * a11
    val b05 = a02 * a13 - a03 * a12
    val b06 = a20 * a31 - a21 * a30
    val b07 = a20 * a32 - a22 * a30
    val b08 = a20 * a33 - a23 * a30
    val b09 = a21 * a32 - a22 * a31
    val b10 = a21 * a33 - a23 * a31
    val b11 = a22 * a33 - a23 * a32

    val det = b00 * b11 - b01 * b10 + b02 * b09 + b03 * b08 - b04 * b07 + b05 * b06
    require(det != 0f) { "Matrix is singular and cannot be inverted" }

    val invDet = 1f / det

    val result = FloatArray(16)
    // Row 0
    result[0] = (a11 * b11 - a12 * b10 + a13 * b09) * invDet
    result[4] = (-a01 * b11 + a02 * b10 - a03 * b09) * invDet
    result[8] = (a31 * b05 - a32 * b04 + a33 * b03) * invDet
    result[12] = (-a21 * b05 + a22 * b04 - a23 * b03) * invDet
    // Row 1
    result[1] = (-a10 * b11 + a12 * b08 - a13 * b07) * invDet
    result[5] = (a00 * b11 - a02 * b08 + a03 * b07) * invDet
    result[9] = (-a30 * b05 + a32 * b02 - a33 * b01) * invDet
    result[13] = (a20 * b05 - a22 * b02 + a23 * b01) * invDet
    // Row 2
    result[2] = (a10 * b10 - a11 * b08 + a13 * b06) * invDet
    result[6] = (-a00 * b10 + a01 * b08 - a03 * b06) * invDet
    result[10] = (a30 * b04 - a31 * b02 + a33 * b00) * invDet
    result[14] = (-a20 * b04 + a21 * b02 - a23 * b00) * invDet
    // Row 3
    result[3] = (-a10 * b09 + a11 * b07 - a12 * b06) * invDet
    result[7] = (a00 * b09 - a01 * b07 + a02 * b06) * invDet
    result[11] = (-a30 * b03 + a31 * b01 - a32 * b00) * invDet
    result[15] = (a20 * b03 - a21 * b01 + a22 * b00) * invDet

    return Mat4(result)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Mat4) return false
    return data.contentEquals(other.data)
  }

  override fun hashCode(): Int = data.contentHashCode()

  override fun toString(): String {
    val sb = StringBuilder("Mat4(\n")
    for (row in 0..3) {
      sb.append("  [")
      for (col in 0..3) {
        if (col > 0) sb.append(", ")
        sb.append(this[row, col])
      }
      sb.append("]\n")
    }
    sb.append(")")
    return sb.toString()
  }

  companion object {
    fun identity(): Mat4 =
      Mat4(floatArrayOf(1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))

    fun translation(offset: Vec3): Mat4 =
      Mat4(
        floatArrayOf(
          1f,
          0f,
          0f,
          0f,
          0f,
          1f,
          0f,
          0f,
          0f,
          0f,
          1f,
          0f,
          offset.x,
          offset.y,
          offset.z,
          1f,
        )
      )

    fun scale(s: Vec3): Mat4 =
      Mat4(floatArrayOf(s.x, 0f, 0f, 0f, 0f, s.y, 0f, 0f, 0f, 0f, s.z, 0f, 0f, 0f, 0f, 1f))

    fun rotationX(radians: Float): Mat4 {
      val c = cos(radians)
      val s = sin(radians)
      return Mat4(floatArrayOf(1f, 0f, 0f, 0f, 0f, c, s, 0f, 0f, -s, c, 0f, 0f, 0f, 0f, 1f))
    }

    fun rotationY(radians: Float): Mat4 {
      val c = cos(radians)
      val s = sin(radians)
      return Mat4(floatArrayOf(c, 0f, -s, 0f, 0f, 1f, 0f, 0f, s, 0f, c, 0f, 0f, 0f, 0f, 1f))
    }

    fun rotationZ(radians: Float): Mat4 {
      val c = cos(radians)
      val s = sin(radians)
      return Mat4(floatArrayOf(c, s, 0f, 0f, -s, c, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f))
    }

    /** Creates a right-handed look-at view matrix. */
    fun lookAt(eye: Vec3, target: Vec3, up: Vec3): Mat4 {
      val f = (target - eye).normalize() // forward (into the screen)
      val s = f.cross(up).normalize() // right
      val u = s.cross(f) // recalculated up

      return Mat4(
        floatArrayOf(
          s.x,
          u.x,
          -f.x,
          0f,
          s.y,
          u.y,
          -f.y,
          0f,
          s.z,
          u.z,
          -f.z,
          0f,
          -s.dot(eye),
          -u.dot(eye),
          f.dot(eye),
          1f,
        )
      )
    }

    /**
     * Creates a right-handed perspective projection matrix for WebGPU clip space [0,1].
     *
     * @param fovY vertical field of view in radians
     * @param aspect width / height aspect ratio
     * @param near near clipping plane distance (must be > 0)
     * @param far far clipping plane distance (must be > near)
     */
    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): Mat4 {
      require(near > 0f) { "Near plane must be positive" }
      require(far > near) { "Far plane must be greater than near plane" }

      val tanHalfFov = tan(fovY / 2f)
      val range = far - near

      return Mat4(
        floatArrayOf(
          1f / (aspect * tanHalfFov),
          0f,
          0f,
          0f,
          0f,
          1f / tanHalfFov,
          0f,
          0f,
          0f,
          0f,
          -far / range,
          -1f,
          0f,
          0f,
          -(far * near) / range,
          0f,
        )
      )
    }

    /** Creates a right-handed orthographic projection matrix for WebGPU clip space [0,1]. */
    fun orthographic(
      left: Float,
      right: Float,
      bottom: Float,
      top: Float,
      near: Float,
      far: Float,
    ): Mat4 {
      val rl = right - left
      val tb = top - bottom
      val fn = far - near

      return Mat4(
        floatArrayOf(
          2f / rl,
          0f,
          0f,
          0f,
          0f,
          2f / tb,
          0f,
          0f,
          0f,
          0f,
          -1f / fn,
          0f,
          -(right + left) / rl,
          -(top + bottom) / tb,
          -near / fn,
          1f,
        )
      )
    }
  }
}
