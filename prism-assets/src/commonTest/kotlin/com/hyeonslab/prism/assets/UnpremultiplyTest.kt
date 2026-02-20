package com.hyeonslab.prism.assets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class UnpremultiplyTest {

  @Test
  fun unpremultiplyAlpha_fullyOpaque_unchanged() {
    // A=255: straight == premultiplied, no change expected
    val pixels = byteArrayOf(100.b, 120.b, 80.b, 255.b)
    pixels.unpremultiplyAlpha()
    pixels[0].u shouldBe 100
    pixels[1].u shouldBe 120
    pixels[2].u shouldBe 80
    pixels[3].u shouldBe 255
  }

  @Test
  fun unpremultiplyAlpha_fullyTransparent_unchanged() {
    // A=0: undefined color, leave as-is
    val pixels = byteArrayOf(0, 0, 0, 0)
    pixels.unpremultiplyAlpha()
    pixels[0] shouldBe 0
    pixels[1] shouldBe 0
    pixels[2] shouldBe 0
    pixels[3] shouldBe 0
  }

  @Test
  fun unpremultiplyAlpha_halfTransparent_recoversOriginalColor() {
    // Premultiplied 50% transparent red: (128, 0, 0, 128) → straight (255, 0, 0, 128)
    val pixels = byteArrayOf(128.b, 0, 0, 128.b)
    pixels.unpremultiplyAlpha()
    pixels[0].u shouldBe 255
    pixels[1].u shouldBe 0
    pixels[2].u shouldBe 0
    pixels[3].u shouldBe 128
  }

  @Test
  fun unpremultiplyAlpha_quarterAlpha_recoversApproximateColor() {
    // Premultiplied 25%: 64 * 255 / 128 with rounding = 128
    val pixels = byteArrayOf(64.b, 0, 0, 128.b)
    pixels.unpremultiplyAlpha()
    pixels[0].u shouldBe ((64 * 255 + 64) / 128)
    pixels[1].u shouldBe 0
    pixels[2].u shouldBe 0
  }

  @Test
  fun unpremultiplyAlpha_noOverflow_clampsAt255() {
    // Max premultiplied: A=254, RGB=254 → straight should not exceed 255
    val pixels = byteArrayOf(254.b, 254.b, 254.b, 254.b)
    pixels.unpremultiplyAlpha()
    pixels[0].u shouldBe 255
    pixels[1].u shouldBe 255
    pixels[2].u shouldBe 255
  }

  @Test
  fun unpremultiplyAlpha_nonMultipleOf4Size_throwsIllegalArgumentException() {
    val pixels = byteArrayOf(100.b, 120.b, 80.b) // 3 bytes — not a multiple of 4
    shouldThrow<IllegalArgumentException> { pixels.unpremultiplyAlpha() }
  }

  @Test
  fun unpremultiplyAlpha_emptyArray_noOp() {
    val pixels = byteArrayOf()
    pixels.unpremultiplyAlpha() // should not throw
    pixels.size shouldBe 0
  }

  // Helpers to reduce verbosity for out-of-range byte literals
  private val Int.b: Byte
    get() = toByte()

  private val Byte.u: Int
    get() = toInt() and 0xFF
}
