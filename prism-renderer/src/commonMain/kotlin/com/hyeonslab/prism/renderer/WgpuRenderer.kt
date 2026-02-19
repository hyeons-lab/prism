@file:Suppress("DEPRECATION")

package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.math.Vec3
import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.AutoClosableContext
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BindGroupLayoutDescriptor
import io.ygdrasil.webgpu.BindGroupLayoutEntry
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferBindingLayout
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.Color as WGPUColor
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.DepthStencilState
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUAddressMode
import io.ygdrasil.webgpu.GPUBindGroup
import io.ygdrasil.webgpu.GPUBindGroupLayout
import io.ygdrasil.webgpu.GPUBuffer as WGPUBuffer
import io.ygdrasil.webgpu.GPUBufferBindingType
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCommandEncoder as WGPUCommandEncoder
import io.ygdrasil.webgpu.GPUCompareFunction
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUFilterMode
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUMipmapFilterMode
import io.ygdrasil.webgpu.GPUPipelineLayout
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPURenderPassEncoder as WGPURenderPassEncoder
import io.ygdrasil.webgpu.GPURenderPipeline
import io.ygdrasil.webgpu.GPUSampler
import io.ygdrasil.webgpu.GPUShaderModule
import io.ygdrasil.webgpu.GPUShaderStage
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTexture as WGPUTexture
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.GPUTextureView as WGPUTextureView
import io.ygdrasil.webgpu.GPUTextureViewDimension
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.MultisampleState
import io.ygdrasil.webgpu.Origin3D
import io.ygdrasil.webgpu.PipelineLayoutDescriptor
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDepthStencilAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor as WGPURenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.RenderingContext
import io.ygdrasil.webgpu.SamplerBindingLayout
import io.ygdrasil.webgpu.SamplerDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.TexelCopyBufferLayout
import io.ygdrasil.webgpu.TexelCopyTextureInfo
import io.ygdrasil.webgpu.TextureBindingLayout
import io.ygdrasil.webgpu.TextureDescriptor as WGPUTextureDescriptor
import io.ygdrasil.webgpu.TextureViewDescriptor
import io.ygdrasil.webgpu.VertexAttribute as WGPUVertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.writeBuffer

/**
 * WebGPU-based PBR renderer using wgpu4k.
 *
 * Uses explicit 4-group bind group layout:
 * - Group 0 (Scene): camera VP, camera position, lights
 * - Group 1 (Object): model matrix, normal matrix
 * - Group 2 (Material): PBR parameters + 5 textures + sampler
 * - Group 3 (Environment): IBL cubemaps, BRDF LUT, environment params
 */
@Suppress("LargeClass", "TooManyFunctions")
class WgpuRenderer(
  private val wgpuContext: WGPUContext,
  private val surfacePreConfigured: Boolean = false,
) : Renderer {

  override val name: String = "WgpuRenderer"

  private val device: GPUDevice = wgpuContext.device
  private val renderingContext: RenderingContext = wgpuContext.renderingContext

  // Per-frame state
  private var frameContext: AutoClosableContext? = null
  private var commandEncoder: WGPUCommandEncoder? = null
  private var currentRenderPass: WGPURenderPassEncoder? = null
  private var currentCamera: Camera? = null

  // Depth buffer
  private var depthTexture: WGPUTexture? = null
  private var depthTextureView: WGPUTextureView? = null
  private val depthFormat = GPUTextureFormat.Depth24Plus

  // Surface info
  private var width: Int = renderingContext.width.toInt()
  private var height: Int = renderingContext.height.toInt()
  private val surfaceIsSrgb =
    renderingContext.textureFormat == GPUTextureFormat.RGBA8UnormSrgb ||
      renderingContext.textureFormat == GPUTextureFormat.BGRA8UnormSrgb

  // --- PBR Bind Group Layouts ---
  private var sceneBindGroupLayout: GPUBindGroupLayout? = null
  private var objectBindGroupLayout: GPUBindGroupLayout? = null
  private var materialBindGroupLayout: GPUBindGroupLayout? = null
  private var envBindGroupLayout: GPUBindGroupLayout? = null
  private var pbrPipelineLayout: GPUPipelineLayout? = null

  // --- PBR Uniform Buffers ---
  private var sceneUniformBuffer: WGPUBuffer? = null
  private var objectUniformBuffer: WGPUBuffer? = null
  private var pbrMaterialUniformBuffer: WGPUBuffer? = null
  private var lightStorageBuffer: WGPUBuffer? = null
  private var envUniformBuffer: WGPUBuffer? = null

  // --- Default Textures & Sampler ---
  private var defaultWhiteTexture: WGPUTexture? = null
  private var defaultWhiteTextureView: WGPUTextureView? = null
  private var defaultNormalTexture: WGPUTexture? = null
  private var defaultNormalTextureView: WGPUTextureView? = null
  private var defaultBlackTexture: WGPUTexture? = null
  private var defaultBlackTextureView: WGPUTextureView? = null
  private var defaultCubemap: WGPUTexture? = null
  private var defaultCubemapView: WGPUTextureView? = null
  private var defaultSampler: GPUSampler? = null
  private var defaultClampSampler: GPUSampler? = null

  // --- PBR Bind Groups ---
  private var sceneBindGroup: GPUBindGroup? = null
  private var objectBindGroup: GPUBindGroup? = null
  private var defaultMaterialBindGroup: GPUBindGroup? = null
  private var envBindGroup: GPUBindGroup? = null

  // --- PBR Pipeline ---
  private var pbrPipeline: GPURenderPipeline? = null

  // --- HDR render target ---
  /** When true, PBR renders to RGBA16Float then tone-maps to the swapchain each frame. */
  var hdrEnabled: Boolean = false

  /**
   * Snapshot of [hdrEnabled] taken at [beginRenderPass] — prevents a mid-frame toggle from causing
   * a mismatch between the render target selected in beginRenderPass and the tone-map pass decision
   * in endFrame (which would produce a black or garbled frame).
   */
  private var hdrEnabledForFrame: Boolean = false

  private var hdrTexture: WGPUTexture? = null
  private var hdrTextureView: WGPUTextureView? = null
  private var pbrPipelineHdr: GPURenderPipeline? = null

  // --- IBL textures (set by initializeIbl()) ---
  private var iblBrdfLutTexture: WGPUTexture? = null
  private var iblBrdfLutView: WGPUTextureView? = null
  private var iblIrradianceTexture: WGPUTexture? = null
  private var iblIrradianceView: WGPUTextureView? = null
  private var iblPrefilteredTexture: WGPUTexture? = null
  private var iblPrefilteredView: WGPUTextureView? = null

  // --- Tone mapping pipeline ---
  private var toneMapBindGroupLayout: GPUBindGroupLayout? = null
  private var toneMapPipelineLayout: GPUPipelineLayout? = null
  private var toneMapPipeline: GPURenderPipeline? = null
  private var toneMapBindGroup: GPUBindGroup? = null
  private var toneMapParamsBuffer: WGPUBuffer? = null

  // --- Per-frame state ---
  /** Surface view saved during beginRenderPass for use in HDR tone map pass in endFrame. */
  private var currentSurfaceView: WGPUTextureView? = null

  // Scene uniform tracking
  private var currentNumLights: Int = 0

  @Suppress("LongMethod")
  override fun initialize(engine: Engine) {
    if (!surfacePreConfigured) {
      val format = renderingContext.textureFormat
      val alphaMode = CompositeAlphaMode.Opaque
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
    createDefaultTextures()
    createBindGroupLayouts()
    createPbrBuffers()
    createDefaultBindGroups()
    createHdrTexture()
    createToneMapPipeline()
    updateToneMapBindGroup()
    pbrPipeline = buildPbrPipeline(renderingContext.textureFormat)
    pbrPipelineHdr = buildPbrPipeline(GPUTextureFormat.RGBA16Float)
  }

  override fun update(time: Time) {
    // Rendering is driven by beginFrame/endFrame, not update
  }

  override fun shutdown() {
    pbrPipeline = null
    pbrPipelineHdr = null
    toneMapPipeline = null
    toneMapBindGroup?.close()
    toneMapParamsBuffer?.close()
    hdrTextureView?.close()
    hdrTexture?.close()
    iblBrdfLutView?.close()
    iblBrdfLutTexture?.close()
    iblIrradianceView?.close()
    iblIrradianceTexture?.close()
    iblPrefilteredView?.close()
    iblPrefilteredTexture?.close()
    sceneBindGroup?.close()
    objectBindGroup?.close()
    defaultMaterialBindGroup?.close()
    envBindGroup?.close()
    sceneUniformBuffer?.close()
    objectUniformBuffer?.close()
    pbrMaterialUniformBuffer?.close()
    lightStorageBuffer?.close()
    envUniformBuffer?.close()
    defaultWhiteTextureView?.close()
    defaultWhiteTexture?.close()
    defaultNormalTextureView?.close()
    defaultNormalTexture?.close()
    defaultBlackTextureView?.close()
    defaultBlackTexture?.close()
    defaultCubemapView?.close()
    defaultCubemap?.close()
    defaultSampler?.close()
    defaultClampSampler?.close()
    depthTextureView?.close()
    depthTexture?.close()
    if (!surfacePreConfigured) {
      wgpuContext.close()
    }
  }

  // ===== Frame lifecycle =====

  override fun beginFrame() {
    val ctx = AutoClosableContext()
    frameContext = ctx
    commandEncoder = with(ctx) { device.createCommandEncoder().bind() }
  }

  override fun endFrame() {
    val encoder = commandEncoder ?: error("No active command encoder")
    val ctx = frameContext ?: error("No active frame context")

    if (hdrEnabledForFrame) {
      runToneMapPass(encoder)
    }

    val commandBuffer = encoder.finish()
    device.queue.submit(listOf(commandBuffer))
    wgpuContext.surface.present()
    ctx.close()
    frameContext = null
    commandEncoder = null
    currentSurfaceView = null
  }

  private fun runToneMapPass(encoder: WGPUCommandEncoder) {
    val surfaceView = currentSurfaceView ?: return
    val tmPipeline = toneMapPipeline ?: return
    val tmBindGroup = toneMapBindGroup ?: return

    val colorAttachment =
      RenderPassColorAttachment(
        view = surfaceView,
        loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store,
        clearValue = WGPUColor(0.0, 0.0, 0.0, 1.0),
      )

    val toneMapPass =
      encoder.beginRenderPass(
        WGPURenderPassDescriptor(
          colorAttachments = listOf(colorAttachment),
          label = "Tone Map Pass",
        )
      )

    toneMapPass.setPipeline(tmPipeline)
    toneMapPass.setBindGroup(0u, tmBindGroup)
    toneMapPass.draw(3u) // fullscreen triangle
    toneMapPass.end()
  }

  override fun beginRenderPass(descriptor: RenderPassDescriptor) {
    val encoder = commandEncoder ?: error("No active command encoder")
    val ctx = frameContext ?: error("No active frame context")
    val surfaceTexture = renderingContext.getCurrentTexture()
    val surfaceView = with(ctx) { surfaceTexture.createView().bind() }
    currentSurfaceView = surfaceView // save for HDR tone map pass in endFrame

    // Snapshot hdrEnabled once so beginRenderPass and endFrame agree on routing.
    hdrEnabledForFrame = hdrEnabled

    val cc = descriptor.clearColor
    // HDR and sRGB targets both expect linear color values
    val needsLinear = hdrEnabledForFrame || surfaceIsSrgb
    val clearColor =
      if (needsLinear) {
        val lc = cc.toLinear()
        WGPUColor(lc.r.toDouble(), lc.g.toDouble(), lc.b.toDouble(), cc.a.toDouble())
      } else {
        WGPUColor(cc.r.toDouble(), cc.g.toDouble(), cc.b.toDouble(), cc.a.toDouble())
      }

    // Route to HDR texture when enabled, otherwise directly to swapchain
    val renderTarget =
      if (hdrEnabledForFrame && hdrTextureView != null) hdrTextureView!! else surfaceView

    val colorAttachment =
      RenderPassColorAttachment(
        view = renderTarget,
        loadOp = GPULoadOp.Clear,
        storeOp = GPUStoreOp.Store,
        clearValue = clearColor,
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

    // Auto-bind appropriate PBR pipeline and scene/env bind groups
    val pipeline = if (hdrEnabledForFrame) pbrPipelineHdr else pbrPipeline
    val sceneBg = sceneBindGroup
    val envBg = envBindGroup
    if (pipeline != null && sceneBg != null && envBg != null) {
      currentRenderPass?.setPipeline(pipeline)
      currentRenderPass?.setBindGroup(0u, sceneBg)
      currentRenderPass?.setBindGroup(3u, envBg)
    }
  }

  override fun endRenderPass() {
    val renderPass = currentRenderPass ?: error("No active render pass")
    renderPass.end()
    currentRenderPass = null
  }

  // ===== Camera =====

  override fun setCamera(camera: Camera) {
    currentCamera = camera
    writeSceneUniforms(camera)
  }

  override fun setCameraPosition(position: Vec3) {
    // Camera position is written as part of setCamera's scene uniforms.
    // This method allows updating just the position if needed.
    val buf = sceneUniformBuffer ?: return
    val posData = floatArrayOf(position.x, position.y, position.z)
    device.queue.writeBuffer(buf, 64u, posData)
  }

  // ===== Lights =====

  override fun setLights(lights: List<LightData>) {
    val buf = lightStorageBuffer ?: return
    val count = lights.size.coerceAtMost(Shaders.MAX_LIGHTS)
    currentNumLights = count

    if (count > 0) {
      val data = FloatArray(count * LightData.FLOAT_COUNT)
      for (i in 0 until count) {
        val lightFloats = lights[i].toFloatArray()
        lightFloats.copyInto(data, i * LightData.FLOAT_COUNT)
      }
      device.queue.writeBuffer(buf, 0u, data)
    }

    // Update numLights in scene uniforms (offset 76 = byte 76, u32)
    val sceneBuf = sceneUniformBuffer ?: return
    val numLightsData = floatArrayOf(Float.fromBits(count))
    device.queue.writeBuffer(sceneBuf, 76u, numLightsData)
  }

  // ===== Materials =====

  override fun setMaterial(material: Material) {
    val renderPass = currentRenderPass ?: return
    writePbrMaterialUniforms(material)

    // Bind material bind group with default textures
    // (Real texture binding will be added when texture loading is implemented)
    val matBg = defaultMaterialBindGroup
    if (matBg != null) {
      renderPass.setBindGroup(2u, matBg)
    }
  }

  override fun setMaterialColor(color: Color) {
    // Legacy compat: create a simple material from the color
    setMaterial(Material(baseColor = color))
  }

  // ===== Drawing =====

  override fun drawMesh(mesh: Mesh, transform: Mat4) {
    val renderPass = currentRenderPass ?: error("No active render pass")

    // Write object uniforms (model + padded normalMatrix)
    writeObjectUniforms(transform)

    // Bind object bind group
    val objBg = objectBindGroup
    if (objBg != null) {
      renderPass.setBindGroup(1u, objBg)
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

  // ===== Pipeline (legacy) =====

  override fun bindPipeline(pipeline: RenderPipeline) {
    val renderPass = currentRenderPass ?: error("No active render pass")
    val wgpuPipeline =
      pipeline.handle as? GPURenderPipeline ?: error("Pipeline handle is not a GPURenderPipeline")
    renderPass.setPipeline(wgpuPipeline)
  }

  // ===== Resize =====

  var onResize: ((width: Int, height: Int) -> Unit)? = null

  override fun resize(width: Int, height: Int) {
    this.width = width
    this.height = height
    onResize?.invoke(width, height)
    createDepthTexture()
    createHdrTexture()
    updateToneMapBindGroup()
    currentCamera?.aspectRatio = width.toFloat() / height.toFloat()
  }

  // ===== IBL initialization =====

  /**
   * Generates IBL textures (BRDF LUT, irradiance cubemap, prefiltered env) and activates
   * environment-based ambient lighting.
   *
   * Must be called AFTER [initialize]. Safe to call multiple times (previous IBL textures are
   * released). Generates textures using [IblGenerator] on the CPU and uploads to the GPU.
   *
   * @param skySize Internal sky gradient resolution (CPU only, not a GPU texture).
   * @param irradianceSize Diffuse irradiance cubemap face size.
   * @param prefilteredSize Specular prefiltered env cubemap face size.
   * @param prefilteredMipLevels Number of roughness mip levels in the prefiltered cubemap.
   * @param brdfLutSize BRDF LUT texture resolution.
   */
  @Suppress("LongParameterList")
  fun initializeIbl(
    skySize: Int = 32,
    irradianceSize: Int = 16,
    prefilteredSize: Int = 32,
    prefilteredMipLevels: Int = 5,
    brdfLutSize: Int = 256,
  ) {
    // Release any previously generated IBL textures
    iblBrdfLutView?.close()
    iblBrdfLutTexture?.close()
    iblIrradianceView?.close()
    iblIrradianceTexture?.close()
    iblPrefilteredView?.close()
    iblPrefilteredTexture?.close()

    val ibl =
      IblGenerator.generate(
        device = device,
        skySize = skySize,
        irradianceSize = irradianceSize,
        prefilteredSize = prefilteredSize,
        prefilteredMipLevels = prefilteredMipLevels,
        brdfLutSize = brdfLutSize,
      )

    iblBrdfLutTexture = ibl.brdfLutTexture
    iblBrdfLutView = ibl.brdfLutTexture.createView()

    iblIrradianceTexture = ibl.irradianceTexture
    iblIrradianceView =
      ibl.irradianceTexture.createView(
        TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube, arrayLayerCount = 6u)
      )

    iblPrefilteredTexture = ibl.prefilteredTexture
    iblPrefilteredView =
      ibl.prefilteredTexture.createView(
        TextureViewDescriptor(
          dimension = GPUTextureViewDimension.Cube,
          arrayLayerCount = 6u,
          mipLevelCount = ibl.prefilteredMipLevels.toUInt(),
        )
      )

    // Rebuild env bind group with real IBL textures
    envBindGroup?.close()
    envBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = envBindGroupLayout!!,
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = envUniformBuffer!!)),
              BindGroupEntry(binding = 1u, resource = defaultClampSampler!!),
              BindGroupEntry(binding = 2u, resource = iblIrradianceView!!),
              BindGroupEntry(binding = 3u, resource = iblPrefilteredView!!),
              BindGroupEntry(binding = 4u, resource = iblBrdfLutView!!),
            ),
          label = "IBL Environment Bind Group",
        )
      )

    // Enable IBL in env uniforms: envIntensity=1.0, maxMipLevel=mipLevels-1
    val maxMip = (ibl.prefilteredMipLevels - 1).toFloat()
    val envData = floatArrayOf(1f, maxMip, 0f, 0f)
    device.queue.writeBuffer(envUniformBuffer!!, 0u, envData)
  }

  // ===== Resource creation =====

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
      descriptor.shader.handle as? GPUShaderModule ?: error("Shader module has no GPU handle")

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
          layout = pbrPipelineLayout,
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
    device.queue.writeBuffer(gpuVertexBuffer, 0u, mesh.vertices)
    mesh.vertexBuffer = vertexBuffer

    if (mesh.isIndexed) {
      val indexSizeBytes = (mesh.indices.size * Int.SIZE_BYTES).toLong()
      val indexBuffer = createBuffer(BufferUsage.INDEX, indexSizeBytes, "${mesh.label} Indices")
      val gpuIndexBuffer = indexBuffer.handle as WGPUBuffer
      // Reinterpret IntArray as FloatArray (same bit pattern) to use the byte-order-safe
      // deprecated writeBuffer overload — avoids big-endian ByteBuffer on Android ARM64.
      val indexAsFloats = FloatArray(mesh.indices.size) { Float.fromBits(mesh.indices[it]) }
      device.queue.writeBuffer(gpuIndexBuffer, 0u, indexAsFloats)
      mesh.indexBuffer = indexBuffer
    }
  }

  // ===== Private: Initialization =====

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

  private fun createDefaultTextures() {
    // White 1x1 (base color, metallic-roughness, occlusion defaults)
    defaultWhiteTexture = create1x1Texture("Default White", 255u, 255u, 255u, 255u)
    defaultWhiteTextureView = defaultWhiteTexture!!.createView()

    // Flat normal 1x1 (tangent-space: 0,0,1 = rgb 128,128,255)
    defaultNormalTexture = create1x1Texture("Default Normal", 128u, 128u, 255u, 255u)
    defaultNormalTextureView = defaultNormalTexture!!.createView()

    // Black 1x1 (emissive default)
    defaultBlackTexture = create1x1Texture("Default Black", 0u, 0u, 0u, 255u)
    defaultBlackTextureView = defaultBlackTexture!!.createView()

    // Default cubemap: 1x1 white faces (6 layers)
    defaultCubemap =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(1u, 1u, 6u),
          format = GPUTextureFormat.RGBA8Unorm,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          label = "Default Cubemap",
        )
      )
    val whitePixel = byteArrayOf(-1, -1, -1, -1) // 255,255,255,255 as signed bytes
    for (face in 0u until 6u) {
      device.queue.writeTexture(
        TexelCopyTextureInfo(texture = defaultCubemap!!, origin = Origin3D(z = face)),
        ArrayBuffer.of(whitePixel),
        TexelCopyBufferLayout(bytesPerRow = 4u),
        Extent3D(1u, 1u),
      )
    }
    defaultCubemapView =
      defaultCubemap!!.createView(
        TextureViewDescriptor(dimension = GPUTextureViewDimension.Cube, arrayLayerCount = 6u)
      )

    // Linear sampler (repeat)
    defaultSampler =
      device.createSampler(
        SamplerDescriptor(
          magFilter = GPUFilterMode.Linear,
          minFilter = GPUFilterMode.Linear,
          mipmapFilter = GPUMipmapFilterMode.Linear,
          addressModeU = GPUAddressMode.Repeat,
          addressModeV = GPUAddressMode.Repeat,
          addressModeW = GPUAddressMode.Repeat,
          label = "Default Sampler",
        )
      )

    // Clamp sampler (for BRDF LUT and environment)
    defaultClampSampler =
      device.createSampler(
        SamplerDescriptor(
          magFilter = GPUFilterMode.Linear,
          minFilter = GPUFilterMode.Linear,
          mipmapFilter = GPUMipmapFilterMode.Linear,
          addressModeU = GPUAddressMode.ClampToEdge,
          addressModeV = GPUAddressMode.ClampToEdge,
          addressModeW = GPUAddressMode.ClampToEdge,
          label = "Clamp Sampler",
        )
      )
  }

  private fun create1x1Texture(label: String, r: UByte, g: UByte, b: UByte, a: UByte): WGPUTexture {
    val texture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(1u, 1u),
          format = GPUTextureFormat.RGBA8Unorm,
          usage = GPUTextureUsage.TextureBinding or GPUTextureUsage.CopyDst,
          label = label,
        )
      )
    val pixels = byteArrayOf(r.toByte(), g.toByte(), b.toByte(), a.toByte())
    device.queue.writeTexture(
      TexelCopyTextureInfo(texture = texture),
      ArrayBuffer.of(pixels),
      TexelCopyBufferLayout(bytesPerRow = 4u),
      Extent3D(1u, 1u),
    )
    return texture
  }

  @Suppress("LongMethod")
  private fun createBindGroupLayouts() {
    val vertFrag = GPUShaderStage.Vertex or GPUShaderStage.Fragment
    val fragOnly = GPUShaderStage.Fragment

    // Group 0: Scene
    sceneBindGroupLayout =
      device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
          entries =
            listOf(
              BindGroupLayoutEntry(
                binding = 0u,
                visibility = vertFrag,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform),
              ),
              BindGroupLayoutEntry(
                binding = 1u,
                visibility = fragOnly,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.ReadOnlyStorage),
              ),
            ),
          label = "Scene Bind Group Layout",
        )
      )

    // Group 1: Object
    objectBindGroupLayout =
      device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
          entries =
            listOf(
              BindGroupLayoutEntry(
                binding = 0u,
                visibility = vertFrag,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform),
              )
            ),
          label = "Object Bind Group Layout",
        )
      )

    // Group 2: Material (uniform + sampler + 5 textures)
    materialBindGroupLayout =
      device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
          entries =
            listOf(
              BindGroupLayoutEntry(
                binding = 0u,
                visibility = fragOnly,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform),
              ),
              BindGroupLayoutEntry(
                binding = 1u,
                visibility = fragOnly,
                sampler = SamplerBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 2u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 3u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 4u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 5u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 6u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
            ),
          label = "Material Bind Group Layout",
        )
      )

    // Group 3: Environment (uniform + sampler + 2 cubemaps + 1 2D texture)
    envBindGroupLayout =
      device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
          entries =
            listOf(
              BindGroupLayoutEntry(
                binding = 0u,
                visibility = fragOnly,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform),
              ),
              BindGroupLayoutEntry(
                binding = 1u,
                visibility = fragOnly,
                sampler = SamplerBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 2u,
                visibility = fragOnly,
                texture = TextureBindingLayout(viewDimension = GPUTextureViewDimension.Cube),
              ),
              BindGroupLayoutEntry(
                binding = 3u,
                visibility = fragOnly,
                texture = TextureBindingLayout(viewDimension = GPUTextureViewDimension.Cube),
              ),
              BindGroupLayoutEntry(
                binding = 4u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
            ),
          label = "Environment Bind Group Layout",
        )
      )

    pbrPipelineLayout =
      device.createPipelineLayout(
        PipelineLayoutDescriptor(
          bindGroupLayouts =
            listOf(
              sceneBindGroupLayout!!,
              objectBindGroupLayout!!,
              materialBindGroupLayout!!,
              envBindGroupLayout!!,
            ),
          label = "PBR Pipeline Layout",
        )
      )
  }

  private fun createPbrBuffers() {
    sceneUniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.SCENE_UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "Scene Uniforms",
        )
      )

    objectUniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.OBJECT_UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "Object Uniforms",
        )
      )

    pbrMaterialUniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.PBR_MATERIAL_UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "PBR Material Uniforms",
        )
      )

    lightStorageBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.LIGHT_BUFFER_SIZE.toULong(),
          usage = GPUBufferUsage.Storage or GPUBufferUsage.CopyDst,
          label = "Light Storage",
        )
      )

    envUniformBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = Shaders.ENV_UNIFORMS_SIZE.toULong(),
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "Environment Uniforms",
        )
      )

    // Initialize env uniforms: envIntensity=0 (IBL disabled), maxMipLevel=0
    val envData = floatArrayOf(0f, 0f, 0f, 0f)
    device.queue.writeBuffer(envUniformBuffer!!, 0u, envData)

    // Initialize scene ambient color
    val defaultScene = FloatArray(24) // 96 bytes / 4
    // ambientColor at offset 80 = float index 20
    defaultScene[20] = 0.15f
    defaultScene[21] = 0.15f
    defaultScene[22] = 0.15f
    device.queue.writeBuffer(sceneUniformBuffer!!, 0u, defaultScene)

    // Initialize material with white, metallic=0, roughness=0.5
    val defaultMat =
      floatArrayOf(
        1f,
        1f,
        1f,
        1f, // baseColor
        0f, // metallic
        0.5f, // roughness
        1f, // occlusionStrength
        Float.fromBits(0), // flags
        0f,
        0f,
        0f,
        1f, // emissive (black)
      )
    device.queue.writeBuffer(pbrMaterialUniformBuffer!!, 0u, defaultMat)
  }

  private fun createDefaultBindGroups() {
    // Scene bind group
    sceneBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = sceneBindGroupLayout!!,
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = sceneUniformBuffer!!)),
              BindGroupEntry(binding = 1u, resource = BufferBinding(buffer = lightStorageBuffer!!)),
            ),
          label = "Scene Bind Group",
        )
      )

    // Object bind group
    objectBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = objectBindGroupLayout!!,
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = objectUniformBuffer!!))
            ),
          label = "Object Bind Group",
        )
      )

    // Default material bind group (all default textures)
    defaultMaterialBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = materialBindGroupLayout!!,
          entries =
            listOf(
              BindGroupEntry(
                binding = 0u,
                resource = BufferBinding(buffer = pbrMaterialUniformBuffer!!),
              ),
              BindGroupEntry(binding = 1u, resource = defaultSampler!!),
              BindGroupEntry(binding = 2u, resource = defaultWhiteTextureView!!),
              BindGroupEntry(binding = 3u, resource = defaultWhiteTextureView!!),
              BindGroupEntry(binding = 4u, resource = defaultNormalTextureView!!),
              BindGroupEntry(binding = 5u, resource = defaultWhiteTextureView!!),
              BindGroupEntry(binding = 6u, resource = defaultBlackTextureView!!),
            ),
          label = "Default Material Bind Group",
        )
      )

    // Environment bind group (default cubemaps, IBL disabled)
    envBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = envBindGroupLayout!!,
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = BufferBinding(buffer = envUniformBuffer!!)),
              BindGroupEntry(binding = 1u, resource = defaultClampSampler!!),
              BindGroupEntry(binding = 2u, resource = defaultCubemapView!!),
              BindGroupEntry(binding = 3u, resource = defaultCubemapView!!),
              BindGroupEntry(binding = 4u, resource = defaultWhiteTextureView!!),
            ),
          label = "Environment Bind Group",
        )
      )
  }

  /**
   * Builds a PBR render pipeline targeting [targetFormat].
   *
   * Called twice during init: once for the swapchain format (LDR direct path) and once for
   * RGBA16Float (HDR path, tone-mapped before presentation).
   */
  private fun buildPbrPipeline(targetFormat: GPUTextureFormat): GPURenderPipeline {
    val wgslCode = "${Shaders.PBR_VERTEX_SHADER.code}\n\n${Shaders.PBR_FRAGMENT_SHADER.code}"
    val shaderModule =
      device.createShaderModule(ShaderModuleDescriptor(code = wgslCode, label = "PBR Shader"))

    val layout = VertexLayout.positionNormalUvTangent()
    val wgpuVertexAttributes =
      layout.attributes.mapIndexed { index, attr ->
        WGPUVertexAttribute(
          shaderLocation = index.toUInt(),
          offset = attr.offset.toULong(),
          format = mapVertexFormat(attr.format),
        )
      }

    return device.createRenderPipeline(
      RenderPipelineDescriptor(
        vertex =
          VertexState(
            entryPoint = Shaders.PBR_VERTEX_SHADER.entryPoint,
            module = shaderModule,
            buffers =
              listOf(
                VertexBufferLayout(
                  arrayStride = layout.stride.toULong(),
                  attributes = wgpuVertexAttributes,
                )
              ),
          ),
        fragment =
          FragmentState(
            entryPoint = Shaders.PBR_FRAGMENT_SHADER.entryPoint,
            module = shaderModule,
            targets = listOf(ColorTargetState(format = targetFormat)),
          ),
        primitive = PrimitiveState(cullMode = GPUCullMode.Back),
        depthStencil =
          DepthStencilState(
            format = depthFormat,
            depthWriteEnabled = true,
            depthCompare = GPUCompareFunction.Less,
          ),
        multisample = MultisampleState(count = 1u, mask = 0xFFFFFFFu),
        layout = pbrPipelineLayout,
        label = "PBR Pipeline (${targetFormat.name})",
      )
    )
  }

  private fun createHdrTexture() {
    hdrTextureView?.close()
    hdrTexture?.close()
    hdrTexture =
      device.createTexture(
        WGPUTextureDescriptor(
          size = Extent3D(width.toUInt(), height.toUInt()),
          format = GPUTextureFormat.RGBA16Float,
          usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.TextureBinding,
          label = "HDR Color Texture",
        )
      )
    hdrTextureView = hdrTexture!!.createView()
  }

  private fun createToneMapPipeline() {
    // Create the params buffer: applySrgb u32 (+ 12 bytes pad to 16-byte alignment).
    // applySrgb=1 → shader applies γ≈2.2 encoding (needed when swapchain is NOT sRGB).
    val applySrgb = if (surfaceIsSrgb) 0 else 1
    toneMapParamsBuffer =
      device.createBuffer(
        BufferDescriptor(
          size = 16u, // u32 + 12 bytes alignment padding
          usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
          label = "Tone Map Params",
        )
      )
    device.queue.writeBuffer(
      toneMapParamsBuffer!!,
      0u,
      floatArrayOf(Float.fromBits(applySrgb), 0f, 0f, 0f),
    )

    val fragOnly = GPUShaderStage.Fragment
    toneMapBindGroupLayout =
      device.createBindGroupLayout(
        BindGroupLayoutDescriptor(
          entries =
            listOf(
              BindGroupLayoutEntry(
                binding = 0u,
                visibility = fragOnly,
                texture = TextureBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 1u,
                visibility = fragOnly,
                sampler = SamplerBindingLayout(),
              ),
              BindGroupLayoutEntry(
                binding = 2u,
                visibility = fragOnly,
                buffer = BufferBindingLayout(type = GPUBufferBindingType.Uniform),
              ),
            ),
          label = "Tone Map Bind Group Layout",
        )
      )

    toneMapPipelineLayout =
      device.createPipelineLayout(
        PipelineLayoutDescriptor(
          bindGroupLayouts = listOf(toneMapBindGroupLayout!!),
          label = "Tone Map Pipeline Layout",
        )
      )

    val code = "${Shaders.TONE_MAP_VERTEX_SHADER.code}\n\n${Shaders.TONE_MAP_FRAGMENT_SHADER.code}"
    val shader =
      device.createShaderModule(ShaderModuleDescriptor(code = code, label = "Tone Map Shader"))

    toneMapPipeline =
      device.createRenderPipeline(
        RenderPipelineDescriptor(
          vertex =
            VertexState(
              entryPoint = Shaders.TONE_MAP_VERTEX_SHADER.entryPoint,
              module = shader,
              buffers = emptyList(),
            ),
          fragment =
            FragmentState(
              entryPoint = Shaders.TONE_MAP_FRAGMENT_SHADER.entryPoint,
              module = shader,
              targets = listOf(ColorTargetState(format = renderingContext.textureFormat)),
            ),
          primitive = PrimitiveState(topology = GPUPrimitiveTopology.TriangleList),
          multisample = MultisampleState(count = 1u, mask = 0xFFFFFFFu),
          layout = toneMapPipelineLayout,
          label = "Tone Map Pipeline",
        )
      )
  }

  private fun updateToneMapBindGroup() {
    toneMapBindGroup?.close()
    val layout = toneMapBindGroupLayout ?: return
    val hdrView = hdrTextureView ?: return
    val sampler = defaultClampSampler ?: return
    val paramsBuffer = toneMapParamsBuffer ?: return
    toneMapBindGroup =
      device.createBindGroup(
        BindGroupDescriptor(
          layout = layout,
          entries =
            listOf(
              BindGroupEntry(binding = 0u, resource = hdrView),
              BindGroupEntry(binding = 1u, resource = sampler),
              BindGroupEntry(binding = 2u, resource = BufferBinding(buffer = paramsBuffer)),
            ),
          label = "Tone Map Bind Group",
        )
      )
  }

  // ===== Private: Uniform writing =====

  private fun writeSceneUniforms(camera: Camera) {
    val buf = sceneUniformBuffer ?: return
    val vp = camera.viewProjectionMatrix()
    val pos = camera.position

    // Pack: mat4x4f(16) + vec3f(3) + u32(1) + vec3f(3) + pad(1) = 24 floats
    val data = FloatArray(24)
    vp.data.copyInto(data, 0) // viewProjection at offset 0
    data[16] = pos.x // cameraPosition at offset 64
    data[17] = pos.y
    data[18] = pos.z
    data[19] = Float.fromBits(currentNumLights) // numLights at offset 76
    data[20] = 0.15f // ambientColor at offset 80
    data[21] = 0.15f
    data[22] = 0.15f
    data[23] = 0f // pad

    device.queue.writeBuffer(buf, 0u, data)
  }

  private fun writeObjectUniforms(model: Mat4) {
    val buf = objectUniformBuffer ?: return

    // Pack: mat4x4f(16 floats) + mat3x3f padded(12 floats) = 28 floats
    val data = FloatArray(28) // 112 bytes
    model.data.copyInto(data, 0) // model at offset 0

    // Normal matrix: transpose(inverse(upperLeft3x3))
    // Padded to 3 columns of vec4 (each column gets a 0-padding float)
    val nm = model.normalMatrix()
    // Column 0
    data[16] = nm[0, 0]
    data[17] = nm[1, 0]
    data[18] = nm[2, 0]
    data[19] = 0f // padding
    // Column 1
    data[20] = nm[0, 1]
    data[21] = nm[1, 1]
    data[22] = nm[2, 1]
    data[23] = 0f // padding
    // Column 2
    data[24] = nm[0, 2]
    data[25] = nm[1, 2]
    data[26] = nm[2, 2]
    data[27] = 0f // padding

    device.queue.writeBuffer(buf, 0u, data)
  }

  private fun writePbrMaterialUniforms(material: Material) {
    val buf = pbrMaterialUniformBuffer ?: return

    var flags = 0
    if (material.albedoTexture != null) flags = flags or 1
    if (material.metallicRoughnessTexture != null) flags = flags or 2
    if (material.normalTexture != null) flags = flags or 4
    if (material.occlusionTexture != null) flags = flags or 8
    if (material.emissiveTexture != null) flags = flags or 16

    // HDR and sRGB surfaces both require linear-space color values for correct PBR math
    val needsLinear = hdrEnabledForFrame || surfaceIsSrgb
    val bc = if (needsLinear) material.baseColor.toLinear() else material.baseColor
    val em = material.emissive // emissive is always linear HDR — never sRGB-encoded

    val data =
      floatArrayOf(
        bc.r,
        bc.g,
        bc.b,
        bc.a, // baseColor
        material.metallic, // metallic
        material.roughness, // roughness
        material.occlusionStrength, // occlusionStrength
        Float.fromBits(flags), // flags
        em.r,
        em.g,
        em.b,
        em.a, // emissive
      )

    device.queue.writeBuffer(buf, 0u, data)
  }

  // ===== Private: Format mapping =====

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

  private fun mapTextureFormat(format: TextureFormat): GPUTextureFormat =
    when (format) {
      TextureFormat.RGBA8_UNORM -> GPUTextureFormat.RGBA8Unorm
      TextureFormat.RGBA8_SRGB -> GPUTextureFormat.RGBA8UnormSrgb
      TextureFormat.BGRA8_UNORM -> GPUTextureFormat.BGRA8Unorm
      TextureFormat.DEPTH32_FLOAT -> GPUTextureFormat.Depth32Float
      TextureFormat.DEPTH24_STENCIL8 -> GPUTextureFormat.Depth24PlusStencil8
      TextureFormat.RGBA16_FLOAT -> GPUTextureFormat.RGBA16Float
    }
}
