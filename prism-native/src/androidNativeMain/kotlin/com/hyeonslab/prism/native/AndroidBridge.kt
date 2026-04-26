@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package com.hyeonslab.prism.native

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import io.ygdrasil.webgpu.AndroidContext
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceRenderingContext
import io.ygdrasil.webgpu.androidContextRenderer
import io.ygdrasil.webgpu.beginRenderPass
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
import kotlinx.cinterop.toKString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.android.JNIEnvVar
import platform.android.jobject

// ---------------------------------------------------------------------------
// Per-engine Android surface + scene storage
// ---------------------------------------------------------------------------

private data class AndroidSurfaceState(val ctx: AndroidContext, val width: Int, val height: Int)

private val androidSurfaces: AtomicRef<Map<Long, AndroidSurfaceState>> = atomic(mapOf())
private val androidScenes: AtomicRef<Map<Long, SceneState>> = atomic(mapOf())
private val pendingGlbPaths: AtomicRef<Map<Long, String>> = atomic(mapOf())

// ---------------------------------------------------------------------------
// Android Native Surface API (Dart FFI)
// ---------------------------------------------------------------------------

/**
 * Attaches a wgpu Android surface to the engine identified by [engineHandle].
 *
 * [window] is a raw `ANativeWindow *` pointer. [width] and [height] are the initial surface
 * dimensions in pixels.
 */
@CName("prism_attach_android_surface")
fun prismAttachAndroidSurface(engineHandle: Long, window: COpaquePointer?, width: Int, height: Int) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val ptr = window ?: return

  val ctx = runBlocking(Dispatchers.Default) { androidContextRenderer(ptr, width, height) }

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

  // Insert into the map BEFORE starting the game loop — the Choreographer fires immediately
  // after startExternal() and prismRenderFrame checks androidSurfaces.
  androidSurfaces.update { it + (engineHandle to AndroidSurfaceState(ctx, width, height)) }
  if (!engine.gameLoop.isRunning) engine.gameLoop.startExternal()

  // Auto-load any GLB path that arrived before the surface was ready.
  pendingGlbPaths
    .getAndUpdate { it - engineHandle }[engineHandle]
    ?.let { pendingPath ->
      val scene =
        try {
          buildGltfScene(
            engine = engine,
            wgpuContext = ctx.wgpuContext,
            glbPath = pendingPath,
            width = width,
            height = height,
          )
        } catch (e: Exception) {
          Logger.withTag("AndroidBridge").e(e) { "Failed to auto-load pending glTF from $pendingPath" }
          return@let
        }
      androidScenes.update { it + (engineHandle to scene) }
    }
}

/**
 * Loads a glTF/GLB model from [glbPath] and initialises a full rendering scene for [engineHandle].
 *
 * May be called before or after [prismAttachAndroidSurface]. If the Android surface is not yet
 * attached, the path is queued and loaded automatically when [prismAttachAndroidSurface] succeeds.
 */
@CName("prism_load_gltf_from_path")
fun prismLoadGltfFromPath(engineHandle: Long, glbPath: CPointer<ByteVar>?) {
  val path = glbPath?.toKString() ?: return
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val surface = androidSurfaces.value[engineHandle]
  if (surface == null) {
    // Surface not yet attached — queue path for auto-load in prismAttachAndroidSurface.
    pendingGlbPaths.update { it + (engineHandle to path) }
    return
  }
  pendingGlbPaths.update { it - engineHandle }

  // Shut down any existing scene before replacing it.
  androidScenes.getAndUpdate { it - engineHandle }[engineHandle]?.shutdown()

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
      Logger.withTag("AndroidBridge").e(e) { "Failed to load glTF from $path" }
      return
    }
  androidScenes.update { it + (engineHandle to scene) }
}

/**
 * Advances the engine by one frame and renders the scene (or a clear-colour fallback when no scene
 * has been loaded yet).
 */
@CName("prism_render_frame")
fun prismRenderFrame(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val surfaceState = androidSurfaces.value[engineHandle] ?: return
  val scene = androidScenes.value[engineHandle]

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
            RenderPassDescriptor(
              colorAttachments =
                listOf(
                  RenderPassColorAttachment(
                    view = view,
                    loadOp = GPULoadOp.Clear,
                    clearValue = Color(0.05, 0.05, 0.1, 1.0),
                    storeOp = GPUStoreOp.Store,
                  )
                )
            )
          )
          .end()
        encoder.finish().use { cmdBuf -> ctx.device.queue.submit(listOf(cmdBuf)) }
      }
    }
  }
  if (renderingContext is SurfaceRenderingContext) surface.present()
}

/**
 * Reconfigures the wgpu Android surface for the new [width]/[height] and updates the scene camera
 * aspect ratio if a scene has been loaded.
 */
@CName("prism_resize")
fun prismResize(engineHandle: Long, width: Int, height: Int) {
  val surfaceState = androidSurfaces.value[engineHandle] ?: return
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
  androidScenes.value[engineHandle]?.updateAspectRatio(width, height)
  androidSurfaces.update { it + (engineHandle to surfaceState.copy(width = width, height = height)) }
}

/**
 * Detaches the wgpu Android surface and shuts down any loaded scene for [engineHandle].
 */
@CName("prism_detach_surface")
fun prismDetachSurface(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle)
  engine?.gameLoop?.stop()

  val oldScenes = androidScenes.getAndUpdate { it - engineHandle }
  oldScenes[engineHandle]?.shutdown()
  pendingGlbPaths.update { it - engineHandle }

  val oldSurfaces = androidSurfaces.getAndUpdate { it - engineHandle }
  oldSurfaces[engineHandle]?.ctx?.close()
}

// ---------------------------------------------------------------------------
// Camera / input control
// ---------------------------------------------------------------------------

/** Rotates the orbit camera by [dx] radians horizontally and [dy] radians vertically. */
@CName("prism_orbit_by")
fun prismOrbitBy(engineHandle: Long, dx: Double, dy: Double) {
  androidScenes.value[engineHandle]?.orbitBy(dx.toFloat(), dy.toFloat())
}

/** Adjusts the orbit radius by [delta] units (positive = zoom in, negative = zoom out). */
@CName("prism_zoom")
fun prismZoom(engineHandle: Long, delta: Double) {
  androidScenes.value[engineHandle]?.zoom(delta.toFloat())
}

// ---------------------------------------------------------------------------
// Engine state queries
// ---------------------------------------------------------------------------

/** Returns the smoothed frames-per-second for the scene, or 0 if no scene is loaded. */
@CName("prism_get_fps")
fun prismGetFps(engineHandle: Long): Double = androidScenes.value[engineHandle]?.fps ?: 0.0

/** Toggles pause. While paused, [prismRenderFrame] skips world ticks and renders a clear pass. */
@CName("prism_toggle_pause")
fun prismTogglePause(engineHandle: Long) {
  val scene = androidScenes.value[engineHandle] ?: return
  scene.isPaused = !scene.isPaused
  if (!scene.isPaused) scene.advanceTiming() // reset last-mark so first resumed delta is small
}

/** Returns 1 if the engine is paused, 0 otherwise. */
@CName("prism_get_pause_state")
fun prismGetPauseState(engineHandle: Long): Int =
  if (androidScenes.value[engineHandle]?.isPaused == true) 1 else 0

/** Returns 1 if a scene has been loaded for [engineHandle], 0 otherwise. */
@CName("prism_is_renderer_ready")
fun prismIsRendererReady(engineHandle: Long): Int =
  if (androidScenes.value[engineHandle] != null) 1 else 0

// ---------------------------------------------------------------------------
// JNI Bridge (JVM side calls from PrismAndroidNative.kt)
// ---------------------------------------------------------------------------

// jniAttachSurface lives in androidNativeArm64Main/X64Main — its body uses the
// androidNativeWindow cinterop which is registered on the leaf compilations only.

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hyeonslab_prism_flutter_PrismAndroidNative_nRenderFrame")
fun jniRenderFrame(env: CPointer<JNIEnvVar>, cls: jobject, handle: Long) =
  prismRenderFrame(handle)

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hyeonslab_prism_flutter_PrismAndroidNative_nResize")
fun jniResize(env: CPointer<JNIEnvVar>, cls: jobject, handle: Long, w: Int, h: Int) =
  prismResize(handle, w, h)

@Suppress("UNUSED_PARAMETER")
@CName("Java_com_hyeonslab_prism_flutter_PrismAndroidNative_nDetachSurface")
fun jniDetachSurface(env: CPointer<JNIEnvVar>, cls: jobject, handle: Long) =
  prismDetachSurface(handle)
