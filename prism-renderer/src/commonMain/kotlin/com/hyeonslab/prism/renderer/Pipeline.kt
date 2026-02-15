package com.hyeonslab.prism.renderer

/** Primitive topology used to interpret vertex data. */
enum class PrimitiveTopology {
  /** Every three vertices form an independent triangle. */
  TRIANGLE_LIST,

  /** Vertices form a strip of connected triangles. */
  TRIANGLE_STRIP,

  /** Every two vertices form an independent line segment. */
  LINE_LIST,

  /** Vertices form a connected polyline. */
  LINE_STRIP,

  /** Each vertex is rendered as a separate point. */
  POINT_LIST,
}

/** Which faces to cull during rasterization. */
enum class CullMode {
  /** No face culling. */
  NONE,

  /** Cull front-facing triangles. */
  FRONT,

  /** Cull back-facing triangles. */
  BACK,
}

/** Winding order that defines a front-facing triangle. */
enum class FrontFace {
  /** Counter-clockwise winding is front-facing. */
  CCW,

  /** Clockwise winding is front-facing. */
  CW,
}

/** Blend mode controlling how fragment colors are combined with the framebuffer. */
enum class BlendMode {
  /** No blending; fragments overwrite the framebuffer. */
  OPAQUE,

  /** Standard alpha blending: srcAlpha * src + (1 - srcAlpha) * dst. */
  ALPHA_BLEND,

  /** Additive blending: src + dst. */
  ADDITIVE,
}

/**
 * Describes the full configuration of a render pipeline.
 *
 * @param shader Compiled shader module providing vertex and fragment stages.
 * @param vertexLayout Memory layout of vertex data fed to the vertex stage.
 * @param topology How vertices are assembled into primitives.
 * @param cullMode Which triangle faces to discard.
 * @param frontFace Winding order that defines a front face.
 * @param blendMode How fragment output is blended with existing framebuffer contents.
 * @param depthTest Whether to perform depth testing.
 * @param depthWrite Whether to write to the depth buffer.
 * @param label Optional debug label.
 */
data class PipelineDescriptor(
  val shader: ShaderModule,
  val vertexLayout: VertexLayout,
  val topology: PrimitiveTopology = PrimitiveTopology.TRIANGLE_LIST,
  val cullMode: CullMode = CullMode.BACK,
  val frontFace: FrontFace = FrontFace.CCW,
  val blendMode: BlendMode = BlendMode.OPAQUE,
  val depthTest: Boolean = true,
  val depthWrite: Boolean = true,
  val label: String = "",
)

/**
 * A fully configured GPU render pipeline ready for use in draw calls.
 *
 * The [handle] property is set by the platform-specific renderer backend after pipeline creation on
 * the GPU.
 *
 * @param descriptor The pipeline configuration used to create this pipeline.
 */
class RenderPipeline(val descriptor: PipelineDescriptor) {
  /** Platform-specific GPU handle. */
  var handle: Any? = null

  override fun toString(): String {
    val d = descriptor
    return "RenderPipeline(topology=${d.topology}, cull=${d.cullMode}, label='${d.label}')"
  }
}
