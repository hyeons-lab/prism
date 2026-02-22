package com.hyeonslab.prism.flutter.demo

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit-tests the EMA smoothing formula applied in DemoMacosBridge / DemoAndroidBridge tickScene:
 *   smoothedFps = prevFps * 0.9f + (1f / deltaTime) * 0.1f
 *
 * DemoMacosBridge and DemoAndroidBridge both use this formula; rather than requiring platform
 * binaries in commonTest, the formula is exercised directly through the DemoStore in the same
 * pattern as the production code.
 */
class FpsSmoothingTest {

    private val store = DemoStore()

    /** Applies one EMA tick using the same formula as tickScene. */
    private fun tickFps(deltaTime: Float) {
        if (deltaTime > 0f) {
            val smoothedFps = store.state.value.fps * 0.9f + (1f / deltaTime) * 0.1f
            store.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }
    }

    @Test
    fun firstTickFromZeroGivesInstantaneousRate() {
        val dt = 1f / 60f
        tickFps(dt)
        val expected = 0f * 0.9f + (1f / dt) * 0.1f
        val actual = store.state.value.fps
        assertTrue(actual > expected - 0.1f && actual < expected + 0.1f,
            "expected ~$expected, got $actual")
    }

    @Test
    fun emaConvergesAfterManyFramesAtSteadyRate() {
        repeat(200) { tickFps(1f / 60f) }
        val fps = store.state.value.fps
        // After 200 frames at 60 fps the EMA should converge near 60.
        assertTrue(fps > 58f && fps < 62f, "expected ~60 fps, got $fps")
    }

    @Test
    fun zeroDeltaTimeIsIgnored() {
        store.dispatch(DemoIntent.UpdateFps(42f))
        tickFps(0f)
        assertEquals(42f, store.state.value.fps)
    }

    @Test
    fun negativeDeltaTimeIsIgnored() {
        store.dispatch(DemoIntent.UpdateFps(30f))
        tickFps(-0.016f)
        assertEquals(30f, store.state.value.fps)
    }

    @Test
    fun emaWeightsOldValueMoreThanInstantaneous() {
        // Seed at 60 fps then get one frame at 30 fps.
        repeat(200) { tickFps(1f / 60f) }
        val before = store.state.value.fps
        tickFps(1f / 30f)
        val after = store.state.value.fps
        // Old value (≈60) has 90% weight; new instant (30) has 10% → result > 57
        assertTrue(after > 57f, "EMA should retain most of previous value; got $after")
        assertTrue(after < before, "Single slow frame should pull average down; got $after vs $before")
    }

    @Test
    fun verySmallDeltaTimeProducesHighInstantRate() {
        // dt = 0.001 s → instant fps = 1000; EMA from 0 = 0 * 0.9 + 1000 * 0.1 = 100
        tickFps(0.001f)
        val fps = store.state.value.fps
        assertTrue(fps > 99f && fps < 101f, "expected ~100 fps, got $fps")
    }
}
