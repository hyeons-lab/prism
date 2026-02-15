package com.hyeonslab.prism.renderer

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.math.sqrt
import kotlin.test.Test

class MeshTest {

  private val epsilon = 1e-5f

  /** Returns the float-array index where the "normal" attribute starts for vertex [vertexIndex]. */
  private fun normalFloatOffset(mesh: Mesh, vertexIndex: Int): Int {
    val floatsPerVertex = mesh.vertexLayout.stride / 4
    val normalAttr = mesh.vertexLayout.attributes.first { it.name == "normal" }
    return vertexIndex * floatsPerVertex + normalAttr.offset / 4
  }

  private fun assertNormalsUnitLength(mesh: Mesh) {
    for (v in 0 until mesh.vertexCount) {
      val i = normalFloatOffset(mesh, v)
      val nx = mesh.vertices[i]
      val ny = mesh.vertices[i + 1]
      val nz = mesh.vertices[i + 2]
      val length = sqrt(nx * nx + ny * ny + nz * nz)
      length shouldBe (1f plusOrMinus epsilon)
    }
  }

  // --- Triangle ---

  @Test
  fun triangleHas3Vertices() {
    Mesh.triangle().vertexCount shouldBe 3
  }

  @Test
  fun triangleHas3Indices() {
    Mesh.triangle().indexCount shouldBe 3
  }

  @Test
  fun triangleIsIndexed() {
    Mesh.triangle().isIndexed shouldBe true
  }

  @Test
  fun triangleLabel() {
    Mesh.triangle().label shouldBe "Triangle"
  }

  @Test
  fun triangleUsesPositionNormalUvLayout() {
    val mesh = Mesh.triangle()
    mesh.vertexLayout shouldBe VertexLayout.positionNormalUv()
    mesh.vertexLayout.stride shouldBe 32
  }

  @Test
  fun triangleNormalsAreUnitLength() {
    assertNormalsUnitLength(Mesh.triangle())
  }

  // --- Quad ---

  @Test
  fun quadHas4Vertices() {
    Mesh.quad().vertexCount shouldBe 4
  }

  @Test
  fun quadHas6Indices() {
    Mesh.quad().indexCount shouldBe 6
  }

  @Test
  fun quadIsIndexed() {
    Mesh.quad().isIndexed shouldBe true
  }

  @Test
  fun quadLabel() {
    Mesh.quad().label shouldBe "Quad"
  }

  @Test
  fun quadNormalsAreUnitLength() {
    assertNormalsUnitLength(Mesh.quad())
  }

  // --- Cube ---

  @Test
  fun cubeHas24Vertices() {
    Mesh.cube().vertexCount shouldBe 24
  }

  @Test
  fun cubeHas36Indices() {
    Mesh.cube().indexCount shouldBe 36
  }

  @Test
  fun cubeIsIndexed() {
    Mesh.cube().isIndexed shouldBe true
  }

  @Test
  fun cubeLabel() {
    Mesh.cube().label shouldBe "Cube"
  }

  @Test
  fun cubeNormalsAreUnitLength() {
    assertNormalsUnitLength(Mesh.cube())
  }

  @Test
  fun cubeFaceNormalsAreConsistentPerFace() {
    val mesh = Mesh.cube()
    // 6 faces, 4 vertices each — all 4 vertices of a face share the same normal
    for (face in 0 until 6) {
      val firstVertex = face * 4
      val i0 = normalFloatOffset(mesh, firstVertex)
      val expectedNx = mesh.vertices[i0]
      val expectedNy = mesh.vertices[i0 + 1]
      val expectedNz = mesh.vertices[i0 + 2]

      for (v in 1 until 4) {
        val i = normalFloatOffset(mesh, firstVertex + v)
        mesh.vertices[i] shouldBe (expectedNx plusOrMinus epsilon)
        mesh.vertices[i + 1] shouldBe (expectedNy plusOrMinus epsilon)
        mesh.vertices[i + 2] shouldBe (expectedNz plusOrMinus epsilon)
      }
    }
  }

  // --- Empty mesh ---

  @Test
  fun emptyMeshVertexCount() {
    Mesh(VertexLayout.positionOnly()).vertexCount shouldBe 0
  }

  @Test
  fun emptyMeshIndexCount() {
    Mesh(VertexLayout.positionOnly()).indexCount shouldBe 0
  }

  @Test
  fun emptyMeshIsNotIndexed() {
    Mesh(VertexLayout.positionOnly()).isIndexed shouldBe false
  }

  // --- GPU buffers start null ---

  @Test
  fun vertexBufferStartsNull() {
    Mesh.triangle().vertexBuffer.shouldBeNull()
  }

  @Test
  fun indexBufferStartsNull() {
    Mesh.triangle().indexBuffer.shouldBeNull()
  }

  // --- vertexCount calculation ---

  @Test
  fun vertexCountFromStrideAndData() {
    val mesh = Mesh(VertexLayout.positionOnly()) // stride=12, 3 floats per vertex
    mesh.vertices = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f) // 2 vertices
    mesh.vertexCount shouldBe 2
  }

  @Test
  fun vertexCountWithZeroStride() {
    val layout = VertexLayout(stride = 0, attributes = emptyList())
    val mesh = Mesh(layout)
    mesh.vertices = floatArrayOf(1f, 2f, 3f)
    mesh.vertexCount shouldBe 0
  }

  // --- toString ---

  @Test
  fun toStringFormat() {
    val mesh = Mesh.triangle()
    mesh.toString() shouldBe "Mesh(vertices=3, indices=3, indexed=true, label='Triangle')"
  }
}
