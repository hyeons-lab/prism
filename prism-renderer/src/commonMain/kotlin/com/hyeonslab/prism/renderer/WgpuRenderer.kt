package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.math.Mat4
import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.AutoClosableContext
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.Color as WGPUColor
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.DepthStencilState
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBindGroup
import io.ygdrasil.webgpu.GPUBuffer as WGPUBuffer
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCommandEncoder as WGPUCommandEncoder
import io.ygdrasil.webgpu.GPUCompareFunction
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPURenderPassEncoder as WGPURenderPassEncoder
import io.ygdrasil.webgpu.GPURenderPipeline
import io.ygdrasil.webgpu.GPUShaderModule
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTexture as WGPUTexture
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.GPUTextureView as WGPUTextureView
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.MultisampleState
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDepthStencilAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor as WGPURenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderingContext
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.TextureDescriptor as WGPUTextureDescriptor
import io.ygdrasil.webgpu.VertexAttribute as WGPUVertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.beginRenderPass
import kotlin.math.pow

/**
 * WebGPU-based renderer implementation using wgpu4k.
 *
 * Provides cross-platform GPU rendering for desktop (Vulkan/Metal/DX12), web (WebGPU), and mobile
 * (Metal/Vulkan) platforms.
 *
 * @param wgpuContext The wgpu4k context containing device, surface, and rendering context.
 * @param surfacePreConfigured When true, skips surface configuration in [initialize] — the caller
 *   is responsible for configuring the surface before rendering begins. Used for AWT Canvas
 *   integration where the surface is configured externally.
 */
class WgpuRenderer(
  private val wgpuContext: WGPUContext,
  private val surfacePreConfigured: Boolean = false,
) : Renderer {

  override val name: String = "WgpuRenderer"

  private val device: GPUDevice = wgpuContext.device
  private val renderingContext: RenderingContext = wgpuContext.renderingContext

  // Per-frame AutoClosableContext — manages ephemeral GPU handles (.bind())
  private var frameContext: AutoClosableContext? = null

  // Per-frame state
  private var commandEncoder: WGPUCommandEncoder? = null
  private var currentRenderPass: WGPURenderPassEncoder? = null

  // Cached GPU resources (long-lived, not per-frame)
  private var depthTexture: WGPUTexture? = null
  private var depthTextureView: WGPUTextureView? = null
  private var uniformBuffer: WGPUBuffer? = null
  private var materialUniformBuffer: WGPUBuffer? = null
  private var bindGroup: GPUBindGroup? = null

  // Current rendering state
  private var currentCamera: Camera? = null
  private var currentPipeline: RenderPipeline? = null
  private var width: Int = renderingContext.width.toInt()
  private var height: Int = renderingContext.height.toInt()

  private val depthFormat = GPUTextureFormat.Depth24Plus
  private val surfaceIsSrgb = renderingContext.textureFormat.name.endsWith("Srgb")

  override fun initialize(engine: Engine) {
    if (!surfacePreConfigured) {
      // Configure the surface for presentation
      val format = renderingContext.textureFormat
      val alphaMode = CompositeAlphaMode.Auto
      wgpuContext.surface.configure(
        SurfaceConfiguration(
          device = device,
          format = format,
          usage = GPUTextureUsage.RenderAttachment,
          alphaMode = alphaMode,
        )
      )
    }

    createDepthTexture()

    // Create uniform buffer: viewProjection (64 bytes) + model (64 bytes) = 128 bytes
    uniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "Uniforms",
        )
      )

    // Create material uniform buffer: baseColor vec4 = 16 bytes
    materialUniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.MATERIAL_UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "MaterialUniforms",
        )
      )

    // Write default white color
    val defaultColor = floatArrayOf(1f, 1f, 1f, 1f)
    device.queue.writeBuffer(materialUniformBuffer!!, 0u, ArrayBuffer.of(defaultColor))
  }

  override fun update(time: Time) {
    // Rendering is driven by beginFrame/endFrame, not update
  }

  override fun shutdown() {
    bindGroup?.close()
    uniformBuffer?.close()
    materialUniformBuffer?.close()
    depthTextureView?.close()
    depthTexture?.close()
    // Only close the context when we own it (i.e., not externally pre-configured).
    // When surfacePreConfigured=true, the caller (e.g., PrismPanel) owns the WGPUContext lifecycle.
    if (!surfacePreConfigured) {
      wgpuContext.close()
    }
  }

  override fun beginFrame() {
    val ctx = AutoClosableContext()
    frameContext = ctx
    commandEncoder = with(ctx) { device.createCommandEncoder().bind() }
  }

  override fun endFrame() {
    val encoder = commandEncoder ?: error("No active command encoder — call beginFrame() first")
    val ctx = frameContext ?: error("No active frame context — call beginFrame() first")
    val commandBuffer = encoder.finish()
    device.queue.submit(listOf(commandBuffer))
    wgpuContext.surface.present()
    ctx.close()
    frameContext = null
    commandEncoder = null
  }

  override fun beginRenderPass(descriptor: RenderPassDescriptor) {
    val encoder = commandEncoder ?: error("No active command encoder — call beginFrame() first")
    val ctx = frameContext ?: error("No active frame context — call beginFrame() first")
    val surfaceTexture = renderingContext.getCurrentTexture()
    val surfaceView = with(ctx) { surfaceTexture.createView().bind() }

    val cc = descriptor.clearColor
    val colorAttachment =
      RenderPassColorAttachment(
        view = surfaceView,
        loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store,
        clearValue =
          if (surfaceIsSrgb) {
            WGPUColor(srgbToLinear(cc.r), srgbToLinear(cc.g), srgbToLinear(cc.b), cc.a.toDouble())
          } else {
            WGPUColor(cc.r.toDouble(), cc.g.toDouble(), cc.b.toDouble(), cc.a.toDouble())
          },
      )

    val wgpuDescriptor =
      WGPURenderPassDescriptor(
        colorAttachments = listOf(colorAttachment),
        depthStencilAttachment =
          depthTextureView?.let { depthView ->
            RenderPassDepthStencilAttachment(
              view = depthView,
              depthClearValue = descriptor.clearDepth,
              depthLoadOp = GPULoadOp.Clear,
              depthStoreOp = GPUStoreOp.Store,
            )
          },
        label = descriptor.label.ifEmpty { "Main Render Pass" },
      )

    currentRenderPass = encoder.beginRenderPass(wgpuDescriptor)
  }

  override fun endRenderPass() {
    val renderPass = currentRenderPass ?: error("No active render pass")
    renderPass.end()
    currentRenderPass = null
  }

  override fun bindPipeline(pipeline: RenderPipeline) {
    val renderPass = currentRenderPass ?: error("No active render pass")
    currentPipeline = pipeline

    val wgpuPipeline =
      pipeline.handle as? GPURenderPipeline
        ?: error("Pipeline handle is not a GPURenderPipeline — call createPipeline() first")

    renderPass.setPipeline(wgpuPipeline)

    // Create bind group from the pipeline's auto-derived layout
    val ub = uniformBuffer ?: return
    val mb = materialUniformBuffer ?: return

    bindGroup?.close()
    bindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = wgpuPipeline.getBindGroupLayout(0u),
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = ub)),
              BindGroupEntry(binding = 1u, resource = BufferBinding(buffer = mb)),
            ),
        )
      )

    renderPass.setBindGroup(0u, bindGroup)
  }

  override fun setCamera(camera: Camera) {
    currentCamera = camera
    val ub = uniformBuffer ?: return
    val vpMatrix = camera.viewProjectionMatrix()
    device.queue.writeBuffer(ub, 0u, ArrayBuffer.of(vpMatrix.data))
  }

  override fun setMaterialColor(color: Color) {
    val mb = materialUniformBuffer ?: return
    val colorData =
      if (surfaceIsSrgb) {
        floatArrayOf(
          srgbToLinear(color.r).toFloat(),
          srgbToLinear(color.g).toFloat(),
          srgbToLinear(color.b).toFloat(),
          color.a,
        )
      } else {
        floatArrayOf(color.r, color.g, color.b, color.a)
      }
    device.queue.writeBuffer(mb, 0u, ArrayBuffer.of(colorData))
  }

  override fun drawMesh(mesh: Mesh, transform: Mat4) {
    val renderPass = currentRenderPass ?: error("No active render pass")

    // Write model matrix to uniform buffer at offset 64 (after viewProjection)
    val ub = uniformBuffer
    if (ub != null) {
      device.queue.writeBuffer(ub, 64u, ArrayBuffer.of(transform.data))
    }

    val vertexBuffer =
      mesh.vertexBuffer?.handle as? WGPUBuffer
        ?: error("Mesh '${mesh.label}' has no vertex buffer — call uploadMesh() first")

    renderPass.setVertexBuffer(0u, vertexBuffer)

    if (mesh.isIndexed) {
      val indexBuffer =
        mesh.indexBuffer?.handle as? WGPUBuffer
          ?: error("Mesh '${mesh.label}' is indexed but has no index buffer")
      renderPass.setIndexBuffer(indexBuffer, GPUIndexFormat.Uint32)
      renderPass.drawIndexed(mesh.indexCount.toUInt())
    } else {
      renderPass.draw(mesh.vertexCount.toUInt())
    }
  }

  /**
   * Callback invoked when [resize] is called, allowing external code to reconfigure the surface
   * with the new dimensions. Used by AWT Canvas integration to reconfigure the wgpu surface.
   */
  var onResize: ((width: Int, height: Int) -> Unit)? = null

  override fun resize(width: Int, height: Int) {
    this.width = width
    this.height = height
    onResize?.invoke(width, height)
    createDepthTexture()
    currentCamera?.aspectRatio = width.toFloat() / height.toFloat()
  }

  override fun createBuffer(usage: BufferUsage, sizeInBytes: Long, label: String): GpuBuffer {
    val wgpuUsage =
      when (usage) {
        BufferUsage.VERTEX -> GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst
        BufferUsage.INDEX -> GPUBufferUsage.Index or GPUBufferUsage.CopyDst
        BufferUsage.UNIFORM -> GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst
        BufferUsage.STORAGE -> GPUBufferUsage.Storage or GPUBufferUsage.CopyDst
      }

    val gpuBuffer =
      device.createBuffer(
        BufferDescriptor(size = sizeInBytes.toULong(), usage = wgpuUsage, label = label)
      )

    val buffer = GpuBuffer(usage, sizeInBytes, label)
    buffer.handle = gpuBuffer
    return buffer
  }

  override fun createTexture(descriptor: TextureDescriptor): Texture {
    val wgpuFormat = mapTextureFormat(descriptor.format)

    val gpuTexture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(descriptor.width.toUInt(), descriptor.height.toUInt()),
          format = wgpuFormat,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          label = descriptor.label,
        )
      )

    val texture = Texture(descriptor)
    texture.handle = gpuTexture
    return texture
  }

  override fun createShaderModule(
    vertexSource: ShaderSource,
    fragmentSource: ShaderSource,
    label: String,
  ): ShaderModule {
    val wgslCode = "${vertexSource.code}\n\n${fragmentSource.code}"

    val gpuModule =
      device.createShaderModule(ShaderModuleDescriptor(code = wgslCode, label = label))

    val module = ShaderModule(vertexSource, fragmentSource, label)
    module.handle = gpuModule
    return module
  }

  override fun createPipeline(descriptor: PipelineDescriptor): RenderPipeline {
    val shaderHandle =
      descriptor.shader.handle as? GPUShaderModule
        ?: error("Shader module has no GPU handle — call createShaderModule() first")

    val wgpuVertexAttributes =
      descriptor.vertexLayout.attributes.mapIndexed { index, attr ->
        WGPUVertexAttribute(
          shaderLocation = index.toUInt(),
          offset = attr.offset.toULong(),
          format = mapVertexFormat(attr.format),
        )
      }

    val wgpuPipeline =
      device.createRenderPipeline(
        RenderPipelineDescriptor(
          vertex =
            VertexState(
              entryPoint = descriptor.shader.vertexSource.entryPoint,
              module = shaderHandle,
              buffers =
                listOf(
                  VertexBufferLayout(
                    arrayStride = descriptor.vertexLayout.stride.toULong(),
                    attributes = wgpuVertexAttributes,
                  )
                ),
            ),
          fragment =
            FragmentState(
              entryPoint = descriptor.shader.fragmentSource.entryPoint,
              module = shaderHandle,
              targets = listOf(ColorTargetState(format = renderingContext.textureFormat)),
            ),
          primitive =
            PrimitiveState(
              topology = mapTopology(descriptor.topology),
              cullMode = mapCullMode(descriptor.cullMode),
            ),
          depthStencil =
            if (descriptor.depthTest) {
              DepthStencilState(
                format = depthFormat,
                depthWriteEnabled = descriptor.depthWrite,
                depthCompare = GPUCompareFunction.Less,
              )
            } else null,
          multisample = MultisampleState(count = 1u, mask = 0xFFFFFFFu),
          label = descriptor.label,
        )
      )

    val pipeline = RenderPipeline(descriptor)
    pipeline.handle = wgpuPipeline
    return pipeline
  }

  override fun uploadMesh(mesh: Mesh) {
    val vertexSizeBytes = (mesh.vertices.size * Float.SIZE_BYTES).toLong()
    val vertexBuffer = createBuffer(BufferUsage.VERTEX, vertexSizeBytes, "${mesh.label} Vertices")
    val gpuVertexBuffer = vertexBuffer.handle as WGPUBuffer
    device.queue.writeBuffer(gpuVertexBuffer, 0u, ArrayBuffer.of(mesh.vertices))
    mesh.vertexBuffer = vertexBuffer

    if (mesh.isIndexed) {
      val indexSizeBytes = (mesh.indices.size * Int.SIZE_BYTES).toLong()
      val indexBuffer = createBuffer(BufferUsage.INDEX, indexSizeBytes, "${mesh.label} Indices")
      val gpuIndexBuffer = indexBuffer.handle as WGPUBuffer
      device.queue.writeBuffer(gpuIndexBuffer, 0u, ArrayBuffer.of(mesh.indices))
      mesh.indexBuffer = indexBuffer
    }
  }

  private fun createDepthTexture() {
    depthTextureView?.close()
    depthTexture?.close()
    depthTexture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(width.toUInt(), height.toUInt()),
          format = depthFormat,
          usage = GPUTextureUsage.RenderAttachment,
          label = "Depth Texture",
        )
      )
    depthTextureView = depthTexture!!.createView()
  }

  private fun mapVertexFormat(format: VertexAttributeFormat): GPUVertexFormat =
    when (format) {
      VertexAttributeFormat.FLOAT -> GPUVertexFormat.Float32
      VertexAttributeFormat.FLOAT2 -> GPUVertexFormat.Float32x2
      VertexAttributeFormat.FLOAT3 -> GPUVertexFormat.Float32x3
      VertexAttributeFormat.FLOAT4 -> GPUVertexFormat.Float32x4
      VertexAttributeFormat.INT -> GPUVertexFormat.Sint32
      VertexAttributeFormat.UINT -> GPUVertexFormat.Uint32
    }

  private fun mapTopology(topology: PrimitiveTopology): GPUPrimitiveTopology =
    when (topology) {
      PrimitiveTopology.TRIANGLE_LIST -> GPUPrimitiveTopology.TriangleList
      PrimitiveTopology.TRIANGLE_STRIP -> GPUPrimitiveTopology.TriangleStrip
      PrimitiveTopology.LINE_LIST -> GPUPrimitiveTopology.LineList
      PrimitiveTopology.LINE_STRIP -> GPUPrimitiveTopology.LineStrip
      PrimitiveTopology.POINT_LIST -> GPUPrimitiveTopology.PointList
    }

  private fun mapCullMode(cullMode: CullMode): GPUCullMode =
    when (cullMode) {
      CullMode.NONE -> GPUCullMode.None
      CullMode.FRONT -> GPUCullMode.Front
      CullMode.BACK -> GPUCullMode.Back
    }

  /** Convert an sRGB-encoded channel value to linear light for use with sRGB surface formats. */
  private fun srgbToLinear(value: Float): Double {
    val v = value.toDouble()
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
  }

  private fun mapTextureFormat(format: TextureFormat): GPUTextureFormat =
    when (format) {
      TextureFormat.RGBA8_UNORM -> GPUTextureFormat.RGBA8Unorm
      TextureFormat.RGBA8_SRGB -> GPUTextureFormat.RGBA8UnormSrgb
      TextureFormat.BGRA8_UNORM -> GPUTextureFormat.BGRA8Unorm
      TextureFormat.DEPTH32_FLOAT -> GPUTextureFormat.Depth32Float
      TextureFormat.DEPTH24_STENCIL8 -> GPUTextureFormat.Depth24PlusStencil8
    }
}
