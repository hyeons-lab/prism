package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.core.Subsystem
import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.math.Vec3

/**
 * Core rendering interface for the Prism engine.
 *
 * Extends [Subsystem] so it can be registered with the engine and participate in the main loop
 * lifecycle. Platform-specific implementations (e.g. wgpu4k, Metal, WebGPU) implement this
 * interface to provide actual GPU rendering.
 *
 * A typical frame looks like:
 * ```
 * renderer.beginFrame()
 * renderer.beginRenderPass(RenderPassDescriptor())
 * renderer.setCamera(camera)
 * renderer.bindPipeline(pipeline)
 * renderer.drawMesh(mesh, transform)
 * renderer.endRenderPass()
 * renderer.endFrame()
 * ```
 */
interface Renderer : Subsystem {

  /** Signals the start of a new frame. Acquires the next swap-chain texture. */
  fun beginFrame()

  /** Signals the end of the current frame. Presents the rendered image. */
  fun endFrame()

  /**
   * Begins a render pass with the given configuration. Clears attachments according to the
   * descriptor.
   *
   * @param descriptor Render pass configuration (clear color, depth, stencil).
   */
  fun beginRenderPass(descriptor: RenderPassDescriptor)

  /** Ends the current render pass and finalizes its command encoding. */
  fun endRenderPass()

  /**
   * Binds a render pipeline for subsequent draw calls.
   *
   * @param pipeline The pipeline to bind (defines shader, vertex layout, blend state, etc.).
   */
  fun bindPipeline(pipeline: RenderPipeline)

  /**
   * Issues a draw call for the given mesh with an optional model transform.
   *
   * The mesh must have been previously uploaded via [uploadMesh].
   *
   * @param mesh The mesh to draw.
   * @param transform Model-to-world transformation matrix. Defaults to identity.
   */
  fun drawMesh(mesh: Mesh, transform: Mat4 = Mat4.identity())

  /**
   * Sets the active camera whose view and projection matrices will be used for subsequent draw
   * calls.
   *
   * @param camera The camera to use for rendering.
   */
  fun setCamera(camera: Camera)

  /**
   * Notifies the renderer that the viewport/surface has been resized.
   *
   * @param width New width in pixels.
   * @param height New height in pixels.
   */
  fun resize(width: Int, height: Int)

  /**
   * Allocates a GPU buffer.
   *
   * @param usage Intended usage of the buffer.
   * @param sizeInBytes Size of the buffer in bytes.
   * @param label Optional debug label.
   * @return The created GPU buffer with a valid platform handle.
   */
  fun createBuffer(usage: BufferUsage, sizeInBytes: Long, label: String = ""): GpuBuffer

  /**
   * Creates a GPU texture from the given descriptor.
   *
   * @param descriptor Texture configuration.
   * @return The created texture with a valid platform handle.
   */
  fun createTexture(descriptor: TextureDescriptor): Texture

  /**
   * Compiles shader source code into a GPU shader module.
   *
   * @param vertexSource Vertex stage source code.
   * @param fragmentSource Fragment stage source code.
   * @param label Optional debug label.
   * @return The compiled shader module with a valid platform handle.
   */
  fun createShaderModule(
    vertexSource: ShaderSource,
    fragmentSource: ShaderSource,
    label: String = "",
  ): ShaderModule

  /**
   * Creates a render pipeline from the given descriptor.
   *
   * @param descriptor Full pipeline configuration.
   * @return The created pipeline with a valid platform handle.
   */
  fun createPipeline(descriptor: PipelineDescriptor): RenderPipeline

  /**
   * Uploads mesh vertex and index data to the GPU.
   *
   * After this call, the mesh's [Mesh.vertexBuffer] and [Mesh.indexBuffer] (if indexed) will be
   * populated with valid GPU buffers.
   *
   * @param mesh The mesh whose data should be uploaded.
   */
  fun uploadMesh(mesh: Mesh)

  /**
   * Sets the material base color for subsequent draw calls.
   *
   * @param color The RGBA color to use as the material's base color.
   */
  fun setMaterialColor(color: Color) {}

  /**
   * Sets a PBR material for subsequent draw calls. Writes material parameters and binds textures
   * (or defaults).
   */
  fun setMaterial(material: Material) {}

  /** Sets the light array for the current frame. */
  fun setLights(lights: List<LightData>) {}

  /** Sets the camera world-space position for PBR specular calculations. */
  fun setCameraPosition(position: Vec3) {}

  /**
   * Uploads raw RGBA8 pixel data to a previously created [Texture].
   *
   * Must be called after [createTexture] and before the texture is used in a draw call.
   *
   * @param texture The texture to populate. Must have a valid [Texture.handle].
   * @param pixels Raw RGBA8 pixel data in row-major order (4 bytes per pixel).
   */
  fun uploadTextureData(texture: Texture, pixels: ByteArray) {}
}
