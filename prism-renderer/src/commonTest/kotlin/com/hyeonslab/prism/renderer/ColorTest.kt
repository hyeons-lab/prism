package com.hyeonslab.prism.renderer

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ColorTest {

  // --- Color constants ---

  @Test
  fun blackConstant() {
    Color.BLACK shouldBe Color(0f, 0f, 0f, 1f)
  }

  @Test
  fun whiteConstant() {
    Color.WHITE shouldBe Color(1f, 1f, 1f, 1f)
  }

  @Test
  fun redConstant() {
    Color.RED shouldBe Color(1f, 0f, 0f, 1f)
  }

  @Test
  fun greenConstant() {
    Color.GREEN shouldBe Color(0f, 1f, 0f, 1f)
  }

  @Test
  fun blueConstant() {
    Color.BLUE shouldBe Color(0f, 0f, 1f, 1f)
  }

  @Test
  fun yellowConstant() {
    Color.YELLOW shouldBe Color(1f, 1f, 0f, 1f)
  }

  @Test
  fun cyanConstant() {
    Color.CYAN shouldBe Color(0f, 1f, 1f, 1f)
  }

  @Test
  fun magentaConstant() {
    Color.MAGENTA shouldBe Color(1f, 0f, 1f, 1f)
  }

  @Test
  fun transparentConstant() {
    Color.TRANSPARENT shouldBe Color(0f, 0f, 0f, 0f)
  }

  @Test
  fun cornflowerBlueConstant() {
    Color.CORNFLOWER_BLUE shouldBe Color(0.392f, 0.584f, 0.929f, 1f)
  }

  // --- fromRgba8 ---

  @Test
  fun fromRgba8AllZeros() {
    Color.fromRgba8(0, 0, 0, 0) shouldBe Color(0f, 0f, 0f, 0f)
  }

  @Test
  fun fromRgba8AllMax() {
    Color.fromRgba8(255, 255, 255, 255) shouldBe Color(1f, 1f, 1f, 1f)
  }

  @Test
  fun fromRgba8MidRange() {
    val c = Color.fromRgba8(128, 64, 192)
    c.r shouldBe 128f / 255f
    c.g shouldBe 64f / 255f
    c.b shouldBe 192f / 255f
  }

  @Test
  fun fromRgba8DefaultAlpha() {
    val c = Color.fromRgba8(100, 150, 200)
    c.a shouldBe 1f
  }

  @Test
  fun fromRgba8OutOfRangeDoesNotClamp() {
    val c = Color.fromRgba8(256, -1, 512)
    c.r shouldBe 256f / 255f
    c.g shouldBe -1f / 255f
    c.b shouldBe 512f / 255f
  }
}
