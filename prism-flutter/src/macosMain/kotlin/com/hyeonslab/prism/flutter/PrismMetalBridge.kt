@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store
import ffi.NativeAddress
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.MacosContext
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.macosContextRendererFromLayer
import kotlin.time.TimeSource
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking

/**
 * macOS-specific [PrismBridge] base that handles all wgpu Metal surface lifecycle.
 *
 * Subclasses implement only two methods:
 *  - [createScene] — build and return the scene from the configured [WGPUContext].
 *  - [tickScene] — advance the scene by one frame using the provided timing values.
 *
 * Usage (Swift side):
 *  1. Instantiate a concrete subclass (e.g. `DemoMacosBridge`).
 *  2. Call `attachMetalLayer(layerPtr:width:height:)` once the CAMetalLayer is ready.
 *  3. Call `renderFrame()` every display-link tick.
 *  4. Call `detachSurface()` on teardown.
 */
abstract class PrismMetalBridge<T : Any, S : Store<*, *>>(store: S) : PrismBridge<T, S>(store) {

    private var ctx: MacosContext? = null
    private var surfaceConfig: SurfaceConfiguration? = null
    private var start = TimeSource.Monotonic.markNow()
    private var lastMark = TimeSource.Monotonic.markNow()
    private var frameCount = 0L

    /** Called after the wgpu surface is configured. Return the fully initialised scene. */
    protected abstract fun createScene(wgpuContext: WGPUContext, width: Int, height: Int): T

    /** Advance the scene by one frame. Called by [renderFrame] with pre-computed timing. */
    protected abstract fun tickScene(
        scene: T,
        deltaTime: Float,
        elapsed: Float,
        frameCount: Long,
    )

    /** Override to return true when rendering should be skipped (e.g. app paused). */
    protected open val isPaused: Boolean get() = false

    /**
     * Creates a wgpu [MacosContext] from [layerPtr] (raw `CAMetalLayer*`), configures the
     * surface, creates the scene via [createScene], and attaches it to this bridge.
     */
    fun attachMetalLayer(layerPtr: COpaquePointer?, width: Int, height: Int) {
        ctx?.close()
        val ptr = layerPtr ?: return
        val ctx = runBlocking { macosContextRendererFromLayer(NativeAddress(ptr), width, height) }
        val surface = ctx.wgpuContext.surface
        val alphaMode = CompositeAlphaMode.Inherit
            .takeIf { surface.supportedAlphaMode.contains(it) }
            ?: CompositeAlphaMode.Opaque
        val surfaceConfig = SurfaceConfiguration(
            device = ctx.wgpuContext.device,
            format = ctx.wgpuContext.renderingContext.textureFormat,
            alphaMode = alphaMode,
        )
        surface.configure(surfaceConfig)
        val scene = createScene(ctx.wgpuContext, width, height)
        // Re-apply surface config after scene creation: createScene() blocks the main
        // thread (runBlocking), which can allow the CAMetalLayer size to change and
        // mark the surface as Outdated. Re-configuring clears the Outdated status.
        surface.configure(surfaceConfig)
        attachScene(scene)
        this.ctx = ctx
        this.surfaceConfig = surfaceConfig
        this.start = TimeSource.Monotonic.markNow()
        this.lastMark = TimeSource.Monotonic.markNow()
        this.frameCount = 0L
    }

    /**
     * Reconfigures the wgpu surface for the new [width]/[height] and notifies the scene
     * via [onResize]. Called by the Swift layer from `drawableSizeWillChange`.
     */
    fun resize(width: Int, height: Int) {
        surfaceConfig?.let { ctx?.wgpuContext?.surface?.configure(it) }
        onResize(width, height)
    }

    /** Override to update the scene's camera / viewport when the drawable size changes. */
    protected open fun onResize(width: Int, height: Int) {}

    /** Computes delta/elapsed time and delegates to [tickScene] (skipped when [isPaused]). */
    fun renderFrame() {
        val s = scene ?: return
        val now = TimeSource.Monotonic.markNow()
        val deltaTime = (now - lastMark).inWholeNanoseconds / 1_000_000_000f
        lastMark = now
        val elapsed = (now - start).inWholeNanoseconds / 1_000_000_000f
        frameCount++
        if (isPaused) return
        try {
            tickScene(s, deltaTime, elapsed, frameCount)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("Outdated", ignoreCase = true) == true ||
                e.message?.contains("fail to get texture", ignoreCase = true) == true
            ) {
                // Surface became Outdated (window resize, display change, etc.).
                // Reconfigure and skip the frame; the next draw will succeed.
                ctx?.wgpuContext?.surface?.configure(surfaceConfig ?: return)
            } else {
                throw e
            }
        }
    }

    /** Shuts down the scene (via [PrismBridge.shutdown]) and closes the wgpu context. */
    fun detachSurface() {
        shutdown()
        ctx?.close()
        ctx = null
    }
}
