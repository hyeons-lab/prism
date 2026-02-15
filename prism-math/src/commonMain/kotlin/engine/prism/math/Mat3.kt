package engine.prism.math

/**
 * A 3x3 matrix stored in column-major order.
 *
 * Column-major layout: data[column * 3 + row]
 */
class Mat3(val data: FloatArray) {

  init {
    require(data.size == 9) { "Mat3 requires exactly 9 elements, got ${data.size}" }
  }

  /** Access element at [row, col] (both 0-based). */
  operator fun get(row: Int, col: Int): Float = data[col * 3 + row]

  /** Matrix multiplication: this * other */
  operator fun times(other: Mat3): Mat3 {
    val result = FloatArray(9)
    for (col in 0..2) {
      for (row in 0..2) {
        var sum = 0f
        for (k in 0..2) {
          sum += this[row, k] * other[k, col]
        }
        result[col * 3 + row] = sum
      }
    }
    return Mat3(result)
  }

  /** Matrix-vector multiplication: this * vec */
  operator fun times(vec: Vec3): Vec3 {
    return Vec3(
      this[0, 0] * vec.x + this[0, 1] * vec.y + this[0, 2] * vec.z,
      this[1, 0] * vec.x + this[1, 1] * vec.y + this[1, 2] * vec.z,
      this[2, 0] * vec.x + this[2, 1] * vec.y + this[2, 2] * vec.z,
    )
  }

  fun transpose(): Mat3 {
    val result = FloatArray(9)
    for (col in 0..2) {
      for (row in 0..2) {
        result[col * 3 + row] = this[col, row]
      }
    }
    return Mat3(result)
  }

  fun determinant(): Float {
    val a = this[0, 0]
    val b = this[0, 1]
    val c = this[0, 2]
    val d = this[1, 0]
    val e = this[1, 1]
    val f = this[1, 2]
    val g = this[2, 0]
    val h = this[2, 1]
    val i = this[2, 2]

    return a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
  }

  fun inverse(): Mat3 {
    val a = this[0, 0]
    val b = this[0, 1]
    val c = this[0, 2]
    val d = this[1, 0]
    val e = this[1, 1]
    val f = this[1, 2]
    val g = this[2, 0]
    val h = this[2, 1]
    val i = this[2, 2]

    val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    require(det != 0f) { "Matrix is singular and cannot be inverted" }

    val invDet = 1f / det

    // Cofactor matrix transposed (adjugate) divided by determinant
    return Mat3(
      floatArrayOf(
        (e * i - f * h) * invDet, // [0,0]
        (f * g - d * i) * invDet, // [1,0]
        (d * h - e * g) * invDet, // [2,0]
        (c * h - b * i) * invDet, // [0,1]
        (a * i - c * g) * invDet, // [1,1]
        (b * g - a * h) * invDet, // [2,1]
        (b * f - c * e) * invDet, // [0,2]
        (c * d - a * f) * invDet, // [1,2]
        (a * e - b * d) * invDet, // [2,2]
      )
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Mat3) return false
    return data.contentEquals(other.data)
  }

  override fun hashCode(): Int = data.contentHashCode()

  override fun toString(): String {
    val sb = StringBuilder("Mat3(\n")
    for (row in 0..2) {
      sb.append("  [")
      for (col in 0..2) {
        if (col > 0) sb.append(", ")
        sb.append(this[row, col])
      }
      sb.append("]\n")
    }
    sb.append(")")
    return sb.toString()
  }

  companion object {
    fun identity(): Mat3 = Mat3(floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f))
  }
}
