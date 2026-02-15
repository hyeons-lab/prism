package com.hyeonslab.prism.renderer

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class VertexLayoutTest {

  /** Asserts that all attributes are packed contiguously and the last one ends at stride. */
  private fun assertContiguousLayout(layout: VertexLayout) {
    for (i in 1 until layout.attributes.size) {
      val prev = layout.attributes[i - 1]
      layout.attributes[i].offset shouldBe (prev.offset + prev.format.sizeInBytes)
    }
    val last = layout.attributes.last()
    (last.offset + last.format.sizeInBytes) shouldBe layout.stride
  }

  // --- positionOnly ---

  @Test
  fun positionOnlyStride() {
    VertexLayout.positionOnly().stride shouldBe 12
  }

  @Test
  fun positionOnlyAttributeCount() {
    VertexLayout.positionOnly().attributes.size shouldBe 1
  }

  @Test
  fun positionOnlyAttribute() {
    val attr = VertexLayout.positionOnly().attributes[0]
    attr.name shouldBe "position"
    attr.format shouldBe VertexAttributeFormat.FLOAT3
    attr.offset shouldBe 0
  }

  @Test
  fun positionOnlyOffsetsAreContiguous() {
    assertContiguousLayout(VertexLayout.positionOnly())
  }

  // --- positionColor ---

  @Test
  fun positionColorStride() {
    VertexLayout.positionColor().stride shouldBe 28
  }

  @Test
  fun positionColorAttributeCount() {
    VertexLayout.positionColor().attributes.size shouldBe 2
  }

  @Test
  fun positionColorAttributes() {
    val attrs = VertexLayout.positionColor().attributes
    attrs[0].name shouldBe "position"
    attrs[0].format shouldBe VertexAttributeFormat.FLOAT3
    attrs[0].offset shouldBe 0

    attrs[1].name shouldBe "color"
    attrs[1].format shouldBe VertexAttributeFormat.FLOAT4
    attrs[1].offset shouldBe 12
  }

  @Test
  fun positionColorOffsetsAreContiguous() {
    assertContiguousLayout(VertexLayout.positionColor())
  }

  // --- positionNormalUv ---

  @Test
  fun positionNormalUvStride() {
    VertexLayout.positionNormalUv().stride shouldBe 32
  }

  @Test
  fun positionNormalUvAttributeCount() {
    VertexLayout.positionNormalUv().attributes.size shouldBe 3
  }

  @Test
  fun positionNormalUvAttributes() {
    val attrs = VertexLayout.positionNormalUv().attributes
    attrs[0].name shouldBe "position"
    attrs[0].format shouldBe VertexAttributeFormat.FLOAT3
    attrs[0].offset shouldBe 0

    attrs[1].name shouldBe "normal"
    attrs[1].format shouldBe VertexAttributeFormat.FLOAT3
    attrs[1].offset shouldBe 12

    attrs[2].name shouldBe "uv"
    attrs[2].format shouldBe VertexAttributeFormat.FLOAT2
    attrs[2].offset shouldBe 24
  }

  @Test
  fun positionNormalUvOffsetsAreContiguous() {
    assertContiguousLayout(VertexLayout.positionNormalUv())
  }

  // --- positionNormalUvTangent ---

  @Test
  fun positionNormalUvTangentStride() {
    VertexLayout.positionNormalUvTangent().stride shouldBe 48
  }

  @Test
  fun positionNormalUvTangentAttributeCount() {
    VertexLayout.positionNormalUvTangent().attributes.size shouldBe 4
  }

  @Test
  fun positionNormalUvTangentAttributes() {
    val attrs = VertexLayout.positionNormalUvTangent().attributes
    attrs[0].name shouldBe "position"
    attrs[0].format shouldBe VertexAttributeFormat.FLOAT3
    attrs[0].offset shouldBe 0

    attrs[1].name shouldBe "normal"
    attrs[1].format shouldBe VertexAttributeFormat.FLOAT3
    attrs[1].offset shouldBe 12

    attrs[2].name shouldBe "uv"
    attrs[2].format shouldBe VertexAttributeFormat.FLOAT2
    attrs[2].offset shouldBe 24

    attrs[3].name shouldBe "tangent"
    attrs[3].format shouldBe VertexAttributeFormat.FLOAT4
    attrs[3].offset shouldBe 32
  }

  @Test
  fun positionNormalUvTangentOffsetsAreContiguous() {
    assertContiguousLayout(VertexLayout.positionNormalUvTangent())
  }

  // --- VertexAttributeFormat enum ---

  @Test
  fun floatFormatProperties() {
    VertexAttributeFormat.FLOAT.componentCount shouldBe 1
    VertexAttributeFormat.FLOAT.sizeInBytes shouldBe 4
  }

  @Test
  fun float2FormatProperties() {
    VertexAttributeFormat.FLOAT2.componentCount shouldBe 2
    VertexAttributeFormat.FLOAT2.sizeInBytes shouldBe 8
  }

  @Test
  fun float3FormatProperties() {
    VertexAttributeFormat.FLOAT3.componentCount shouldBe 3
    VertexAttributeFormat.FLOAT3.sizeInBytes shouldBe 12
  }

  @Test
  fun float4FormatProperties() {
    VertexAttributeFormat.FLOAT4.componentCount shouldBe 4
    VertexAttributeFormat.FLOAT4.sizeInBytes shouldBe 16
  }

  @Test
  fun intFormatProperties() {
    VertexAttributeFormat.INT.componentCount shouldBe 1
    VertexAttributeFormat.INT.sizeInBytes shouldBe 4
  }

  @Test
  fun uintFormatProperties() {
    VertexAttributeFormat.UINT.componentCount shouldBe 1
    VertexAttributeFormat.UINT.sizeInBytes shouldBe 4
  }
}
