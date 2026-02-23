package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store
import io.ygdrasil.webgpu.WGPUContext

/**
 * Android-specific [PrismBridge] base that manages the wgpu SurfaceView lifecycle.
 *
 * Subclasses implement only:
 *  - [createScene] — build and return the scene from the configured [WGPUContext].
 *  - [tickScene] — advance the scene by one frame.
 *  - [isPaused] — return true to skip ticking (defaults to false).
 *
 * Platform view usage:
 *  1. Pass a concrete subclass (e.g. [DemoAndroidBridge]) to [PrismPlatformView].
 *  2. [PrismPlatformView] forwards [onSurfaceReady], [doFrame], and [onSurfaceDestroyed].
 */
abstract class PrismAndroidBridge<T : Any, S : Store<*, *>>(store: S) : PrismBridge<T, S>(store) {

    private val frameTimer = FrameTimer()

    protected open val isPaused: Boolean get() = false

    /** Called when the Android surface is ready. Subclasses create and attach the scene. */
    protected abstract suspend fun createScene(wgpuContext: WGPUContext, width: Int, height: Int): T

    /** Advance the scene by one frame. */
    protected abstract fun tickScene(scene: T, deltaTime: Float, elapsed: Float, frameCount: Long)

    /**
     * Called by [PrismPlatformView] when the surface dimensions change after initialization.
     * Override to update the scene's aspect ratio or viewport.
     */
    open fun onDimensionsChanged(width: Int, height: Int) {}

    /** Called by [PrismPlatformView] once the wgpu context is ready on the surface. */
    suspend fun onSurfaceReady(wgpuContext: WGPUContext, width: Int, height: Int) {
        val scene = createScene(wgpuContext, width, height)
        attachScene(scene)
        frameTimer.reset()
    }

    /** Resets the frame-timing baseline. Call after a pause so the first resumed frame
     *  does not compute a multi-second delta. */
    fun resetFrameTiming() {
        frameTimer.resetLastMark()
    }

    /** Called by [PrismPlatformView] on every Choreographer frame. */
    fun doFrame() {
        val s = scene ?: return
        val (deltaTime, elapsed) = frameTimer.advanceTiming()
        val frameCount = frameTimer.nextFrame()
        if (!isPaused) tickScene(s, deltaTime, elapsed, frameCount)
    }
}
