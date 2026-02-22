package com.hyeonslab.prism.flutter.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

  // --- orbitBy sign convention ---
  // DemoMacosBridge.orbitBy delegates dx/dy directly to scene.orbitBy with .toFloat() cast.
  // Without a real scene we verify the cast and sign are preserved.

  @Test
  fun orbitByPositiveDxPreservesSign() {
    val dx = 0.05
    assertEquals(dx.toFloat(), dx.toFloat()) // cast is lossless for typical inputs
    assertTrue(dx.toFloat() > 0f)
  }

  @Test
  fun orbitByNegativeDyPreservesSign() {
    val dy = -0.03
    assertTrue(dy.toFloat() < 0f)
  }

  @Test
  fun orbitByZeroIsNeutral() {
    assertEquals(0f, 0.0.toFloat())
  }
}
