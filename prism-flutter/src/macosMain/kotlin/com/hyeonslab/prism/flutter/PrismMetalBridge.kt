@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Store
import ffi.NativeAddress
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.MacosContext
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.macosContextRendererFromLayer
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking

/**
 * macOS-specific [PrismBridge] base that handles all wgpu Metal surface lifecycle.
 *
 * Subclasses implement only two methods:
 * - [createScene] — build and return the scene from the configured [WGPUContext].
 * - [tickScene] — advance the scene by one frame using the provided timing values.
 *
 * Usage (Swift side):
 * 1. Instantiate a concrete subclass (e.g. `DemoMacosBridge`).
 * 2. Call `attachMetalLayer(layerPtr:width:height:)` once the CAMetalLayer is ready.
 * 3. Call `renderFrame()` every display-link tick.
 * 4. Call `detachSurface()` on teardown.
 */
abstract class PrismMetalBridge<T : Any, S : Store<*, *>>(store: S) : PrismBridge<T, S>(store) {

  private var ctx: MacosContext? = null
  private var surfaceConfig: SurfaceConfiguration? = null
  private val frameTimer = FrameTimer()

  /** Called after the wgpu surface is configured. Return the fully initialised scene. */
  protected abstract fun createScene(wgpuContext: WGPUContext, width: Int, height: Int): T

  /** Advance the scene by one frame. Called by [renderFrame] with pre-computed timing. */
  protected abstract fun tickScene(scene: T, deltaTime: Float, elapsed: Float, frameCount: Long)

  /** Override to return true when rendering should be skipped (e.g. app paused). */
  protected open val isPaused: Boolean
    get() = false

  /**
   * Returns the current engine state as a plain map for the Flutter method channel. The base
   * implementation includes only `"initialized"`. Override to add domain fields (e.g. fps,
   * rotationSpeed) so that macOS and Android `getState` responses match.
   */
  open fun getState(): Map<String, Any> = mapOf("initialized" to isInitialized)

  /**
   * Creates a wgpu [MacosContext] from [layerPtr] (raw `CAMetalLayer*`), configures the surface,
   * creates the scene via [createScene], and attaches it to this bridge.
   *
   * If [createScene] throws, the newly allocated [MacosContext] is closed before rethrowing so that
   * GPU resources are not leaked.
   */
  fun attachMetalLayer(layerPtr: COpaquePointer?, width: Int, height: Int) {
    ctx?.close()
    val ptr = layerPtr ?: return
    val newCtx = runBlocking { macosContextRendererFromLayer(NativeAddress(ptr), width, height) }
    val (surfaceConfig, scene) =
      try {
        val surface = newCtx.wgpuContext.surface
        val alphaMode =
          CompositeAlphaMode.Inherit.takeIf { surface.supportedAlphaMode.contains(it) }
            ?: CompositeAlphaMode.Opaque
        val config =
          SurfaceConfiguration(
            device = newCtx.wgpuContext.device,
            format = newCtx.wgpuContext.renderingContext.textureFormat,
            alphaMode = alphaMode,
          )
        surface.configure(config)
        val s = createScene(newCtx.wgpuContext, width, height)
        // Re-apply surface config after scene creation: createScene() blocks the main
        // thread (runBlocking), which can allow the CAMetalLayer size to change and
        // mark the surface as Outdated. Re-configuring clears the Outdated status.
        surface.configure(config)
        config to s
      } catch (t: Throwable) {
        newCtx.close()
        throw t
      }
    attachScene(scene)
    this.ctx = newCtx
    this.surfaceConfig = surfaceConfig
    frameTimer.reset()
  }

  /**
   * Reconfigures the wgpu surface for the new [width]/[height] and notifies the scene via
   * [onResize]. Called by the Swift layer from `drawableSizeWillChange`.
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
    // advanceTiming always updates lastMark so resume doesn't see a huge delta.
    val (deltaTime, elapsed) = frameTimer.advanceTiming()
    if (isPaused) return
    // nextFrame increments only when we actually render, keeping frameCount accurate.
    val frameCount = frameTimer.nextFrame()
    try {
      tickScene(s, deltaTime, elapsed, frameCount)
    } catch (e: IllegalStateException) {
      if (
        e.message?.contains("Outdated", ignoreCase = true) == true ||
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
    surfaceConfig = null
  }
}
