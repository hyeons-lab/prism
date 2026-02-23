@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package engine.prism.native

import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceRenderingContext
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.iosContextRenderer
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.interpretObjCPointerOrNull
import kotlinx.cinterop.rawValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.MetalKit.MTKView

// ---------------------------------------------------------------------------
// Per-engine Metal surface storage
// ---------------------------------------------------------------------------

private val iosSurfaces: AtomicRef<Map<Long, IosContext>> = atomic(mapOf())

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

  // Guard against calling startExternal() a second time if the caller re-attaches to the same
  // engine handle (e.g. view recreated after returning to the screen).
  if (!engine.gameLoop.isRunning) engine.gameLoop.startExternal()
  iosSurfaces.update { it + (engineHandle to ctx) }
}

/**
 * Advances the engine by one frame and submits a wgpu render pass.
 *
 * Ticks the game-loop (updates time / fixed-update callbacks), then executes a clear-color render
 * pass via wgpu and presents the frame. Must be called from the CADisplayLink or MTKViewDelegate
 * callback each frame.
 */
@CName("prism_render_frame")
fun prismRenderFrame(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val ctx = iosSurfaces.value[engineHandle] ?: return

  engine.gameLoop.tick()

  val device = ctx.wgpuContext.device
  val renderingContext = ctx.wgpuContext.renderingContext
  val surface = ctx.wgpuContext.surface

  val encoder = device.createCommandEncoder()
  val texture = renderingContext.getCurrentTexture()
  val view = texture.createView()

  encoder.beginRenderPass(
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
  ) {
    end()
  }

  val commandBuffer = encoder.finish()
  device.queue.submit(listOf(commandBuffer))
  view.close()
  commandBuffer.close()
  encoder.close()

  if (renderingContext is SurfaceRenderingContext) {
    surface.present()
  }
  texture.close()
}

/**
 * Reconfigures the wgpu Metal surface for the new [width]/[height].
 *
 * Call this from `layoutSubviews` or `mtkView(_:drawableSizeWillChange:)` whenever the drawable
 * dimensions change. This clears the `Outdated` surface status before the next [prismRenderFrame]
 * call.
 */
@CName("prism_resize")
@Suppress("UNUSED_PARAMETER")
fun prismResize(engineHandle: Long, width: Int, height: Int) {
  val ctx = iosSurfaces.value[engineHandle] ?: return
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
}

/**
 * Detaches the wgpu Metal surface from the engine and releases GPU resources. After this call,
 * [prismRenderFrame] is a no-op for this engine handle.
 */
@CName("prism_detach_surface")
fun prismDetachSurface(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle)
  engine?.gameLoop?.stop()

  var closedCtx: IosContext? = null
  iosSurfaces.update {
    val ctx = it[engineHandle]
    closedCtx = ctx
    it - engineHandle
  }
  closedCtx?.close()
}
