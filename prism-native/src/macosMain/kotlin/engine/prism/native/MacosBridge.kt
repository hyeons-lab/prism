@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package engine.prism.native

import com.hyeonslab.prism.core.Engine
import ffi.NativeAddress
import io.ygdrasil.webgpu.Color
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.MacosContext
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.SurfaceRenderingContext
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.macosContextRendererFromLayer
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.atomicfu.AtomicRef
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

// ---------------------------------------------------------------------------
// Per-engine Metal surface storage
// ---------------------------------------------------------------------------

private val macosSurfaces: AtomicRef<Map<Long, MacosContext>> = atomic(mapOf())

// ---------------------------------------------------------------------------
// macOS Metal surface API
// ---------------------------------------------------------------------------

/**
 * Attaches a wgpu Metal surface to the engine identified by [engineHandle].
 *
 * [layerPtr] is the raw `CAMetalLayer *` obtained from the MTKView's `.layer` property via
 * `Unmanaged.passUnretained(layer).toOpaque()` in Swift. [width] and [height] are the initial
 * drawable dimensions in pixels.
 *
 * The engine game-loop is started in external-tick mode; call [prismRenderFrame] each frame to
 * advance time and submit a render pass.
 */
@CName("prism_attach_metal_layer")
fun prismAttachMetalLayer(engineHandle: Long, layerPtr: COpaquePointer?, width: Int, height: Int) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val ptr = layerPtr ?: return

  val ctx = runBlocking(Dispatchers.Default) {
    macosContextRendererFromLayer(NativeAddress(ptr), width, height)
  }

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

  engine.gameLoop.startExternal()
  macosSurfaces.update { it + (engineHandle to ctx) }
}

/**
 * Advances the engine by one frame and submits a wgpu render pass.
 *
 * Ticks the game-loop (updates time / fixed-update callbacks), then executes a clear-color render
 * pass via wgpu and presents the frame. Must be called from the MTKViewDelegate's `draw(in:)`
 * callback.
 */
@CName("prism_render_frame")
fun prismRenderFrame(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle) ?: return
  val ctx = macosSurfaces.value[engineHandle] ?: return

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
}

/**
 * Detaches the wgpu Metal surface from the engine and releases GPU resources. After this call,
 * [prismRenderFrame] is a no-op for this engine handle.
 */
@CName("prism_detach_surface")
fun prismDetachSurface(engineHandle: Long) {
  val engine = Registry.get<Engine>(engineHandle)
  engine?.gameLoop?.stop()

  var closedCtx: MacosContext? = null
  macosSurfaces.update {
    val ctx = it[engineHandle]
    closedCtx = ctx
    it - engineHandle
  }
  closedCtx?.close()
}
