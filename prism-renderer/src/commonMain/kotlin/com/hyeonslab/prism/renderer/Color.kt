package com.hyeonslab.prism.renderer

data class Color(val r: Float = 0f, val g: Float = 0f, val b: Float = 0f, val a: Float = 1f) {
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
  }
}
