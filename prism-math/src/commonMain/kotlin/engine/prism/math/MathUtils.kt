package engine.prism.math

import kotlin.math.abs

/**
 * Common math constants and utility functions for the Prism engine.
 */
object MathUtils {

    const val PI: Float = 3.1415927f
    const val TWO_PI: Float = 6.2831855f
    const val HALF_PI: Float = 1.5707964f
    const val DEG_TO_RAD: Float = PI / 180f
    const val RAD_TO_DEG: Float = 180f / PI

    /** Converts degrees to radians. */
    fun toRadians(degrees: Float): Float = degrees * DEG_TO_RAD

    /** Converts radians to degrees. */
    fun toDegrees(radians: Float): Float = radians * RAD_TO_DEG

    /** Clamps [value] to the range [min, max]. */
    fun clamp(value: Float, min: Float, max: Float): Float {
        return when {
            value < min -> min
            value > max -> max
            else -> value
        }
    }

    /** Linearly interpolates between [a] and [b] by factor [t]. */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Inverse linear interpolation: returns the interpolation factor t such that
     * lerp(a, b, t) == value. Returns 0 if a == b.
     */
    fun inverseLerp(a: Float, b: Float, value: Float): Float {
        val range = b - a
        return if (abs(range) < 1e-7f) 0f else (value - a) / range
    }

    /**
     * Smooth Hermite interpolation between 0 and 1 when [x] is in [edge0, edge1].
     * Returns 0 if x <= edge0, 1 if x >= edge1.
     */
    fun smoothStep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * Returns true if [a] and [b] are approximately equal within the given [epsilon].
     */
    fun approximately(a: Float, b: Float, epsilon: Float = 1e-6f): Boolean {
        return abs(a - b) <= epsilon
    }
}
