package com.hyeonslab.prism.renderer

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class IblGeneratorTest {

  private val epsilon = 0.05f

  // --- integrateBrdf ---

  @Test
  fun brdfLutValuesInUnitRange() {
    // All outputs must be in [0, 1]
    val (scale, bias) = IblGenerator.integrateBrdf(0.5f, 0.5f, 128)
    (scale >= 0f && scale <= 1f) shouldBe true
    (bias >= 0f && bias <= 1f) shouldBe true
  }

  @Test
  fun brdfLutAtPerfectlyNormalIncidence() {
    // NdotV=1.0 (looking straight at surface), roughness near 0 → scale→1, bias→0
    val (scale, bias) = IblGenerator.integrateBrdf(1.0f, 0.04f, 512)
    scale shouldBe (1f plusOrMinus 0.08f)
    bias shouldBe (0f plusOrMinus 0.08f)
  }

  @Test
  fun brdfLutScaleDecreasesWithRoughness() {
    // At NdotV=0.5: smooth surface should have higher scale than rough
    val (smoothScale, _) = IblGenerator.integrateBrdf(0.5f, 0.1f, 256)
    val (roughScale, _) = IblGenerator.integrateBrdf(0.5f, 0.9f, 256)
    (smoothScale > roughScale) shouldBe true
  }

  @Test
  fun brdfLutScaleAndBiasNonNegative() {
    // Both channels should always be non-negative
    for (nv in listOf(0.1f, 0.3f, 0.5f, 0.7f, 1.0f)) {
      for (r in listOf(0.1f, 0.4f, 0.7f, 1.0f)) {
        val (scale, bias) = IblGenerator.integrateBrdf(nv, r, 64)
        (scale >= 0f) shouldBe true
        (bias >= 0f) shouldBe true
      }
    }
  }

  // --- skyColor ---

  @Test
  fun skyZenithIsBlue() {
    // Looking straight up (y=1) should be blue-dominant
    val color = IblGenerator.skyColor(0f, 1f, 0f)
    (color[2] > color[0]) shouldBe true // more blue than red
    color[2] shouldBe (0.95f plusOrMinus 0.1f)
  }

  @Test
  fun skyNadirIsBrown() {
    // Looking straight down (y=-1) should be reddish/brown
    val color = IblGenerator.skyColor(0f, -1f, 0f)
    (color[0] > color[2]) shouldBe true // more red than blue
  }

  @Test
  fun skyHorizonIsBright() {
    // Horizon (y≈0) should be bright (close to white)
    val color = IblGenerator.skyColor(1f, 0f, 0f)
    color[0] shouldBe (0.85f plusOrMinus 0.1f)
    color[1] shouldBe (0.90f plusOrMinus 0.1f)
    color[2] shouldBe (1.00f plusOrMinus 0.1f)
  }

  @Test
  fun skyColorInLinearRange() {
    // All sky colors must be in [0, 1] for safe LDR storage
    for (y in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) {
      val color = IblGenerator.skyColor(0f, y, 1f)
      (color[0] in 0f..1f) shouldBe true
      (color[1] in 0f..1f) shouldBe true
      (color[2] in 0f..1f) shouldBe true
    }
  }
}
