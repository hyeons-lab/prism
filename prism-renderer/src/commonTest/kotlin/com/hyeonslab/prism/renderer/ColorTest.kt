package com.hyeonslab.prism.renderer

import io.kotest.matchers.floats.plusOrMinus
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

  // --- toLinear / toSrgb ---

  private val eps = 1e-4f

  @Test
  fun toLinearBlack() {
    val linear = Color.BLACK.toLinear()
    linear.r shouldBe (0f plusOrMinus eps)
    linear.g shouldBe (0f plusOrMinus eps)
    linear.b shouldBe (0f plusOrMinus eps)
  }

  @Test
  fun toLinearWhite() {
    val linear = Color.WHITE.toLinear()
    linear.r shouldBe (1f plusOrMinus eps)
    linear.g shouldBe (1f plusOrMinus eps)
    linear.b shouldBe (1f plusOrMinus eps)
  }

  @Test
  fun toLinearMidRange() {
    // sRGB 0.5 -> linear ~0.2140
    val linear = Color(0.5f, 0.5f, 0.5f).toLinear()
    linear.r shouldBe (0.2140f plusOrMinus 0.001f)
  }

  @Test
  fun toLinearPreservesAlpha() {
    val c = Color(0.5f, 0.5f, 0.5f, 0.75f).toLinear()
    c.a shouldBe 0.75f
  }

  @Test
  fun toSrgbBlack() {
    val srgb = Color.BLACK.toSrgb()
    srgb.r shouldBe (0f plusOrMinus eps)
  }

  @Test
  fun toSrgbWhite() {
    val srgb = Color.WHITE.toSrgb()
    srgb.r shouldBe (1f plusOrMinus eps)
  }

  @Test
  fun roundtripLinearSrgb() {
    val original = Color(0.3f, 0.6f, 0.9f, 0.5f)
    val roundtrip = original.toLinear().toSrgb()
    roundtrip.r shouldBe (original.r plusOrMinus 0.001f)
    roundtrip.g shouldBe (original.g plusOrMinus 0.001f)
    roundtrip.b shouldBe (original.b plusOrMinus 0.001f)
    roundtrip.a shouldBe original.a
  }

  @Test
  fun roundtripSrgbLinear() {
    val original = Color(0.214f, 0.214f, 0.214f)
    val roundtrip = original.toSrgb().toLinear()
    roundtrip.r shouldBe (original.r plusOrMinus 0.001f)
  }

  // --- toFloatArray ---

  @Test
  fun toFloatArray() {
    val c = Color(0.1f, 0.2f, 0.3f, 0.4f)
    val arr = c.toFloatArray()
    arr.size shouldBe 4
    arr[0] shouldBe 0.1f
    arr[1] shouldBe 0.2f
    arr[2] shouldBe 0.3f
    arr[3] shouldBe 0.4f
  }
}
