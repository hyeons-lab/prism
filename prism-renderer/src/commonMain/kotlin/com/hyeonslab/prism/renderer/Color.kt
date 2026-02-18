package com.hyeonslab.prism.renderer

import kotlin.math.pow

data class Color(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f, val a: Float = 1f) {

  /** Convert from sRGB to linear light space. Alpha is unchanged. */
  fun toLinear(): Color =
    Color(srgbChannelToLinear(r), srgbChannelToLinear(g), srgbChannelToLinear(b), a)

  /** Convert from linear light space to sRGB. Alpha is unchanged. */
  fun toSrgb(): Color =
    Color(linearChannelToSrgb(r), linearChannelToSrgb(g), linearChannelToSrgb(b), a)

  /** Pack RGBA components into a [FloatArray] of length 4. */
  fun toFloatArray(): FloatArray = floatArrayOf(r, g, b, a)

  companion object {
    val BLACK = Color(0f, 0f, 0f)
    val WHITE = Color(1f, 1f, 1f)
    val RED = Color(1f, 0f, 0f)
    val GREEN = Color(0f, 1f, 0f)
    val BLUE = Color(0f, 0f, 1f)
    val YELLOW = Color(1f, 1f, 0f)
    val CYAN = Color(0f, 1f, 1f)
    val MAGENTA = Color(1f, 0f, 1f)
    val TRANSPARENT = Color(0f, 0f, 0f, 0f)
    val CORNFLOWER_BLUE = Color(0.392f, 0.584f, 0.929f)

    fun fromRgba8(r: Int, g: Int, b: Int, a: Int = 255): Color =
      Color(r / 255f, g / 255f, b / 255f, a / 255f)

    private fun srgbChannelToLinear(v: Float): Float {
      val d = v.toDouble()
      return (if (d <= 0.04045) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)).toFloat()
    }

    private fun linearChannelToSrgb(v: Float): Float {
      val d = v.toDouble()
      return (if (d <= 0.0031308) d * 12.92 else 1.055 * d.pow(1.0 / 2.4) - 0.055).toFloat()
    }
  }
}
