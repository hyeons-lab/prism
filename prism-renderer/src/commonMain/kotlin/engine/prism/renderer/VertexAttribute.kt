package engine.prism.renderer

/** Describes the format of a single vertex attribute element. */
enum class VertexAttributeFormat(val componentCount: Int, val sizeInBytes: Int) {
  FLOAT(1, 4),
  FLOAT2(2, 8),
  FLOAT3(3, 12),
  FLOAT4(4, 16),
  INT(1, 4),
  UINT(1, 4),
}

/**
 * Describes a single attribute within a vertex layout.
 *
 * @param name Semantic name of the attribute (e.g. "position", "normal", "uv").
 * @param format Data format of the attribute.
 * @param offset Byte offset of this attribute within a single vertex.
 */
data class VertexAttribute(val name: String, val format: VertexAttributeFormat, val offset: Int)

/**
 * Describes the memory layout of vertices in a vertex buffer.
 *
 * @param stride Total byte size of a single vertex.
 * @param attributes Ordered list of attributes that compose the vertex.
 */
data class VertexLayout(val stride: Int, val attributes: List<VertexAttribute>) {
  companion object {

    /**
     * Layout with only a position attribute (Vec3).
     *
     * Stride: 12 bytes
     * - position: FLOAT3 at offset 0
     */
    fun positionOnly(): VertexLayout {
      val attributes = listOf(VertexAttribute("position", VertexAttributeFormat.FLOAT3, offset = 0))
      return VertexLayout(stride = 12, attributes = attributes)
    }

    /**
     * Layout with position (Vec3) and color (Vec4) attributes.
     *
     * Stride: 28 bytes
     * - position: FLOAT3 at offset 0
     * - color: FLOAT4 at offset 12
     */
    fun positionColor(): VertexLayout {
      val attributes =
        listOf(
          VertexAttribute("position", VertexAttributeFormat.FLOAT3, offset = 0),
          VertexAttribute("color", VertexAttributeFormat.FLOAT4, offset = 12),
        )
      return VertexLayout(stride = 28, attributes = attributes)
    }

    /**
     * Layout with position (Vec3), normal (Vec3), and UV (Vec2) attributes.
     *
     * Stride: 32 bytes
     * - position: FLOAT3 at offset 0
     * - normal: FLOAT3 at offset 12
     * - uv: FLOAT2 at offset 24
     */
    fun positionNormalUv(): VertexLayout {
      val attributes =
        listOf(
          VertexAttribute("position", VertexAttributeFormat.FLOAT3, offset = 0),
          VertexAttribute("normal", VertexAttributeFormat.FLOAT3, offset = 12),
          VertexAttribute("uv", VertexAttributeFormat.FLOAT2, offset = 24),
        )
      return VertexLayout(stride = 32, attributes = attributes)
    }

    /**
     * Layout with position (Vec3), normal (Vec3), UV (Vec2), and tangent (Vec4) attributes.
     *
     * Stride: 48 bytes
     * - position: FLOAT3 at offset 0
     * - normal: FLOAT3 at offset 12
     * - uv: FLOAT2 at offset 24
     * - tangent: FLOAT4 at offset 32
     */
    fun positionNormalUvTangent(): VertexLayout {
      val attributes =
        listOf(
          VertexAttribute("position", VertexAttributeFormat.FLOAT3, offset = 0),
          VertexAttribute("normal", VertexAttributeFormat.FLOAT3, offset = 12),
          VertexAttribute("uv", VertexAttributeFormat.FLOAT2, offset = 24),
          VertexAttribute("tangent", VertexAttributeFormat.FLOAT4, offset = 32),
        )
      return VertexLayout(stride = 48, attributes = attributes)
    }
  }
}
