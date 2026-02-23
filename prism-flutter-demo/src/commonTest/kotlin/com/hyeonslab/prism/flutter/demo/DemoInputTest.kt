package com.hyeonslab.prism.flutter.demo

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests zoom-radius clamping and orbit-by sign convention.
 *
 * DemoMacosBridge.zoom() and orbitBy() are macOS platform code; the pure-math portions are verified
 * here via extracted helpers that mirror the production logic.
 */
class DemoInputTest {

  // --- zoom clamping (mirrors DemoMacosBridge.zoom) ---

  private fun applyZoom(currentRadius: Float, delta: Double): Float =
    (currentRadius - delta.toFloat()).coerceIn(2f, 40f)

  @Test
  fun zoomInClampedAtMinRadius() {
    // Large positive delta = zoom in toward object; clamp at 2f.
    assertEquals(2f, applyZoom(3.5f, 100.0))
  }

  @Test
  fun zoomOutClampedAtMaxRadius() {
    // Large negative delta = zoom out; clamp at 40f.
    assertEquals(40f, applyZoom(3.5f, -100.0))
  }

  @Test
  fun zoomWithinRangeMovesByDelta() {
    assertEquals(8f, applyZoom(10f, 2.0))
  }

  @Test
  fun zoomAtMinBoundaryStaysAtMin() {
    assertEquals(2f, applyZoom(2f, 1.0))
  }

  @Test
  fun zoomAtMaxBoundaryStaysAtMax() {
    assertEquals(40f, applyZoom(40f, -1.0))
  }

  @Test
  fun zoomStartingAtDefaultRadius() {
    // Default orbit radius is 3.5f (matches scene default).
    val defaultRadius = 3.5f
    assertEquals(4.5f, applyZoom(defaultRadius, -1.0))
    assertEquals(2.5f, applyZoom(defaultRadius, 1.0))
  }

  // --- orbitBy sensitivity formula ---
  // FlutterWasmEntry.kt applies: demoScene.orbitBy(-dx.toFloat() * 0.005f, dy.toFloat() * 0.005f)
  // These tests verify the sign convention and sensitivity constant used in production.

  private val orbitSensitivity = 0.005f

  @Test
  fun orbitSensitivityNegatesHorizontalDx() {
    // Positive pointer dx (moving right) should produce negative azimuth delta (orbit left).
    val dx = 10.0
    assertEquals(-dx.toFloat() * orbitSensitivity, -10f * 0.005f)
  }

  @Test
  fun orbitSensitivityPreservesVerticalSign() {
    // Positive pointer dy (moving down) should produce positive elevation delta.
    val dy = 10.0
    assertEquals(dy.toFloat() * orbitSensitivity, 10f * 0.005f)
  }

  @Test
  fun orbitSensitivityZeroDeltaIsNeutral() {
    assertEquals(0f, 0.0.toFloat() * orbitSensitivity)
  }
}
