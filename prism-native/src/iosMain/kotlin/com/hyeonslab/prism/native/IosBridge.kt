@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package com.hyeonslab.prism.native

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceRenderingContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.getAndUpdate
import kotlinx.atomicfu.update
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.MetalKit.MTKView

// ---------------------------------------------------------------------------
// Per-engine Metal surface + scene storage
// ---------------------------------------------------------------------------

private data class IosSurfaceState(val ctx: IosContext, val width: Int, val height: Int)

private val iosSurfaces: AtomicRef<Map<Long, IosSurfaceState>> = atomic(mapOf())
private val iosScenes: AtomicRef<Map<Long, SceneState>> = atomic(mapOf())

// ---------------------------------------------------------------------------
// iOS Metal surface API
// ---------------------------------------------------------------------------

/**
 * Attaches a wgpu Metal surface to the engine identified by [engineHandle].
 *
 * [layerPtr] is the raw `MTKView *` pointer obtained via
 * `Unmanaged.passUnretained(mtkView).toOpaque()` in Swift. [width] and [height] are the initial
 * drawable dimensions in pixels.
 *
 * The engine game-loop is started in external-tick mode; call [prismRenderFrame] each frame to
 * advance time and submit a render pass.
 */
@CName("prism_attach_metal_layer")
fun prismAttachMetalLayer(engineHandle: Long, layerPtr: COpaquePointer?, width: Int, height: Int) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val ptr = layerPtr ?: return
  val mtkView = interpretObjCPointerOrNull<MTKView>(ptr.rawValue) ?: return

  val ctx = runBlocking(Dispatchers.Default) { iosContextRenderer(mtkView, width, height) }

  val surface = ctx.wgpuContext.surface
  val alphaMode =
    CompositeAlphaMode.Inherit.takeIf { surface.supportedAlphaMode.contains(it) }
      ?: CompositeAlphaMode.Opaque
  surface.configure(
    SurfaceConfiguration(
      device = ctx.wgpuContext.device,
      format = ctx.wgpuContext.renderingContext.textureFormat,
      alphaMode = alphaMode,
    )
  )

  // Insert into the map BEFORE starting the game loop. The CADisplayLink fires immediately
  // after startExternal() and prismRenderFrame checks iosSurfaces — inserting first prevents
  // a dropped first frame.
  iosSurfaces.update { it + (engineHandle to IosSurfaceState(ctx, width, height)) }
  if (!engine.gameLoop.isRunning) engine.gameLoop.startExternal()
}

/**
 * Loads a glTF/GLB model from [glbPath] and initialises a full rendering scene for [engineHandle].
 *
 * Must be called **after** [prismAttachMetalLayer] (the Metal surface must already be configured).
 * The path is a null-terminated C string pointing to the GLB file on the local filesystem — e.g.
 * `NSBundle.mainBundle.resourcePath + "/flutter_assets/assets/DamagedHelmet.glb"`.
 *
 * Subsequent [prismRenderFrame] calls will render this scene instead of the clear-colour fallback.
 */
@CName("prism_load_gltf_from_path")
fun prismLoadGltfFromPath(engineHandle: Long, glbPath: CPointer<ByteVar>?) {
  val path = glbPath?.toKString() ?: return
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val surface = iosSurfaces.value[engineHandle] ?: return

  // Shut down any existing scene before replacing it.
  iosScenes.getAndUpdate { it - engineHandle }[engineHandle]?.shutdown()

  val scene =
    try {
      buildGltfScene(
        engine = engine,
        wgpuContext = surface.ctx.wgpuContext,
        glbPath = path,
        width = surface.width,
        height = surface.height,
      )
    } catch (e: Exception) {
      Logger.withTag("IosBridge").e(e) { "Failed to load glTF from $path" }
      return
    }
  iosScenes.update { it + (engineHandle to scene) }
}

/**
 * Advances the engine by one frame and renders the scene (or a clear-colour fallback when no scene
 * has been loaded yet).
 *
 * If a scene was created by [prismLoadGltfFromPath], delegates rendering to its [World] (which runs
 * [RenderSystem] → [WgpuRenderer] → surface present). Otherwise falls back to a dark-blue clear
 * pass so the platform view is never blank while the scene is loading.
 *
 * Must be called from the CADisplayLink callback each frame.
 */
@CName("prism_render_frame")
fun prismRenderFrame(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val surfaceState = iosSurfaces.value[engineHandle] ?: return
  val scene = iosScenes.value[engineHandle]

  engine.gameLoop.tick()

  if (scene != null && !scene.isPaused) {
    val (deltaTime, elapsed) = scene.advanceTiming()
    val frameCount = scene.nextFrame()
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    scene.world.update(time)
    // WgpuRenderer.render() calls surface.present() internally via RenderSystem.
    return
  }

  // No scene yet (or paused): render a dark-blue clear pass so the view isn't blank.
  val ctx = surfaceState.ctx.wgpuContext
  val renderingContext = ctx.renderingContext
  val surface = ctx.surface

  renderingContext.getCurrentTexture().use { texture ->
    texture.createView().use { view ->
      ctx.device.createCommandEncoder().use { encoder ->
        encoder
          .beginRenderPass(
            io.ygdrasil.webgpu.RenderPassDescriptor(
              colorAttachments =
                listOf(
                  io.ygdrasil.webgpu.RenderPassColorAttachment(
                    view = view,
                    loadOp = io.ygdrasil.webgpu.GPULoadOp.Clear,
                    clearValue = io.ygdrasil.webgpu.Color(0.05, 0.05, 0.1, 1.0),
                    storeOp = io.ygdrasil.webgpu.GPUStoreOp.Store,
                  )
                )
            )
          )
          .end()
        ctx.device.queue.submit(listOf(encoder.finish()))
      }
    }
  }
  if (renderingContext is SurfaceRenderingContext) surface.present()
}

/**
 * Reconfigures the wgpu Metal surface for the new [width]/[height] and updates the scene camera
 * aspect ratio if a scene has been loaded.
 */
@CName("prism_resize")
@Suppress("UNUSED_PARAMETER")
fun prismResize(engineHandle: Long, width: Int, height: Int) {
  val surfaceState = iosSurfaces.value[engineHandle] ?: return
  val surface = surfaceState.ctx.wgpuContext.surface
  val alphaMode =
    CompositeAlphaMode.Inherit.takeIf { surface.supportedAlphaMode.contains(it) }
      ?: CompositeAlphaMode.Opaque
  surface.configure(
    SurfaceConfiguration(
      device = surfaceState.ctx.wgpuContext.device,
      format = surfaceState.ctx.wgpuContext.renderingContext.textureFormat,
      alphaMode = alphaMode,
    )
  )
  iosScenes.value[engineHandle]?.updateAspectRatio(width, height)
  iosSurfaces.update { it + (engineHandle to surfaceState.copy(width = width, height = height)) }
}

/**
 * Detaches the wgpu Metal surface and shuts down any loaded scene for [engineHandle]. After this
 * call, [prismRenderFrame] is a no-op for this handle.
 */
@CName("prism_detach_surface")
fun prismDetachSurface(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle)
  engine?.gameLoop?.stop()

  val oldScenes = iosScenes.getAndUpdate { it - engineHandle }
  oldScenes[engineHandle]?.shutdown()

  val oldSurfaces = iosSurfaces.getAndUpdate { it - engineHandle }
  oldSurfaces[engineHandle]?.ctx?.close()
}

// ---------------------------------------------------------------------------
// Camera / input control
// ---------------------------------------------------------------------------

/**
 * Rotates the orbit camera by [dx] radians horizontally (azimuth) and [dy] radians vertically
 * (elevation). Call from a UIPanGestureRecognizer handler.
 */
@CName("prism_orbit_by")
fun prismOrbitBy(engineHandle: Long, dx: Double, dy: Double) {
  iosScenes.value[engineHandle]?.orbitBy(dx.toFloat(), dy.toFloat())
}

/**
 * Adjusts the orbit radius by [delta] units (positive = zoom in, negative = zoom out). Call from a
 * UIPinchGestureRecognizer handler.
 */
@CName("prism_zoom")
fun prismZoom(engineHandle: Long, delta: Double) {
  iosScenes.value[engineHandle]?.zoom(delta.toFloat())
}

// ---------------------------------------------------------------------------
// Engine state queries
// ---------------------------------------------------------------------------

/** Returns the smoothed frames-per-second for the scene, or 0 if no scene is loaded. */
@CName("prism_get_fps")
fun prismGetFps(engineHandle: Long): Double = iosScenes.value[engineHandle]?.fps ?: 0.0

/** Toggles pause. While paused, [prismRenderFrame] skips world ticks and renders a clear pass. */
@CName("prism_toggle_pause")
fun prismTogglePause(engineHandle: Long) {
  val scene = iosScenes.value[engineHandle] ?: return
  scene.isPaused = !scene.isPaused
  if (!scene.isPaused) scene.advanceTiming() // reset last-mark so first resumed delta is small
}

/** Returns 1 if the engine is paused, 0 otherwise. */
@CName("prism_get_pause_state")
fun prismGetPauseState(engineHandle: Long): Int =
  if (iosScenes.value[engineHandle]?.isPaused == true) 1 else 0

/** Returns 1 if a scene has been loaded for [engineHandle], 0 otherwise. */
@CName("prism_is_renderer_ready")
fun prismIsRendererReady(engineHandle: Long): Int =
  if (iosScenes.value[engineHandle] != null) 1 else 0
