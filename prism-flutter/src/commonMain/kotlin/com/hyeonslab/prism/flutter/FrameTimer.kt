package com.hyeonslab.prism.flutter

import kotlin.time.TimeSource

/**
 * Tracks frame timing (delta, elapsed, and rendered-frame count) for a render loop.
 *
 * Usage pattern:
 *  - Call [reset] when the render loop starts (e.g. after scene attach).
 *  - Call [resetLastMark] after a pause so the first resumed frame doesn't compute a huge delta.
 *  - Each frame: call [advanceTiming] to update lastMark and compute (deltaTime, elapsed).
 *  - If the frame should be rendered: call [nextFrame] to increment and retrieve the counter.
 *    Skip [nextFrame] when paused so [frameCount] reflects rendered-frame count only.
 */
class FrameTimer {
    private var start = TimeSource.Monotonic.markNow()
    private var lastMark = TimeSource.Monotonic.markNow()
    private var _frameCount = 0L

    /** Number of frames that were actually rendered (i.e. times [nextFrame] was called). */
    val frameCount: Long get() = _frameCount

    /**
     * Samples the current time, updates lastMark, and returns (deltaTime, elapsed) in seconds.
     * Always called — even when paused — so that resume doesn't compute a huge first delta.
     */
    fun advanceTiming(): Pair<Float, Float> {
        val now = TimeSource.Monotonic.markNow()
        val delta = (now - lastMark).inWholeNanoseconds / 1_000_000_000f
        lastMark = now
        val elapsed = (now - start).inWholeNanoseconds / 1_000_000_000f
        return delta to elapsed
    }

    /**
     * Increments the rendered-frame counter and returns the new count.
     * Call only when a frame is actually rendered (i.e. not paused).
     */
    fun nextFrame(): Long = ++_frameCount

    /** Resets all timing (start, lastMark, frameCount). Call when the render loop starts. */
    fun reset() {
        start = TimeSource.Monotonic.markNow()
        lastMark = TimeSource.Monotonic.markNow()
        _frameCount = 0L
    }

    /**
     * Resets only lastMark. Call after a pause so the first resumed frame doesn't compute
     * a delta covering the entire paused interval.
     */
    fun resetLastMark() {
        lastMark = TimeSource.Monotonic.markNow()
    }
}
