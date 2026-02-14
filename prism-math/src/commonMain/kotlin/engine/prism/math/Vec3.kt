package engine.prism.math

import kotlin.math.sqrt

data class Vec3(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f) {

    operator fun plus(other: Vec3): Vec3 = Vec3(x + other.x, y + other.y, z + other.z)

    operator fun minus(other: Vec3): Vec3 = Vec3(x - other.x, y - other.y, z - other.z)

    operator fun times(scalar: Float): Vec3 = Vec3(x * scalar, y * scalar, z * scalar)

    operator fun times(other: Vec3): Vec3 = Vec3(x * other.x, y * other.y, z * other.z)

    operator fun div(scalar: Float): Vec3 {
        require(scalar != 0f) { "Cannot divide by zero" }
        val inv = 1f / scalar
        return Vec3(x * inv, y * inv, z * inv)
    }

    operator fun unaryMinus(): Vec3 = Vec3(-x, -y, -z)

    fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z

    fun cross(other: Vec3): Vec3 = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x
    )

    fun lengthSquared(): Float = x * x + y * y + z * z

    fun length(): Float = sqrt(lengthSquared())

    fun normalize(): Vec3 {
        val len = length()
        if (len < EPSILON) return ZERO
        val inv = 1f / len
        return Vec3(x * inv, y * inv, z * inv)
    }

    fun distanceTo(other: Vec3): Float = (this - other).length()

    fun lerp(other: Vec3, t: Float): Vec3 = Vec3(
        x + (other.x - x) * t,
        y + (other.y - y) * t,
        z + (other.z - z) * t
    )

    companion object {
        private const val EPSILON = 1e-7f

        val ZERO = Vec3(0f, 0f, 0f)
        val ONE = Vec3(1f, 1f, 1f)
        val UP = Vec3(0f, 1f, 0f)
        val DOWN = Vec3(0f, -1f, 0f)
        val FORWARD = Vec3(0f, 0f, -1f)
        val BACK = Vec3(0f, 0f, 1f)
        val RIGHT = Vec3(1f, 0f, 0f)
        val LEFT = Vec3(-1f, 0f, 0f)
    }
}

operator fun Float.times(vec: Vec3): Vec3 = vec * this
