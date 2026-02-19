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
  fun cubeUsesPositionNormalUvTangentLayout() {
    val mesh = Mesh.cube()
    mesh.vertexLayout shouldBe VertexLayout.positionNormalUvTangent()
    mesh.vertexLayout.stride shouldBe 48
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

  @Test
  fun cubeTangentsAreUnitLength() {
    val mesh = Mesh.cube()
    val floatsPerVertex = mesh.vertexLayout.stride / 4
    val tangentAttr = mesh.vertexLayout.attributes.first { it.name == "tangent" }
    val tOff = tangentAttr.offset / 4

    for (v in 0 until mesh.vertexCount) {
      val base = v * floatsPerVertex + tOff
      val tx = mesh.vertices[base]
      val ty = mesh.vertices[base + 1]
      val tz = mesh.vertices[base + 2]
      val length = sqrt(tx * tx + ty * ty + tz * tz)
      length shouldBe (1f plusOrMinus epsilon)
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

  // --- Sphere ---

  @Test
  fun sphereVertexCount() {
    val stacks = 16
    val slices = 16
    val mesh = Mesh.sphere(stacks = stacks, slices = slices)
    mesh.vertexCount shouldBe (stacks + 1) * (slices + 1)
  }

  @Test
  fun sphereIndexCount() {
    val stacks = 16
    val slices = 16
    val mesh = Mesh.sphere(stacks = stacks, slices = slices)
    mesh.indexCount shouldBe stacks * slices * 6
  }

  @Test
  fun sphereLabel() {
    Mesh.sphere().label shouldBe "Sphere"
  }

  @Test
  fun sphereUsesPositionNormalUvTangentLayout() {
    val mesh = Mesh.sphere()
    mesh.vertexLayout shouldBe VertexLayout.positionNormalUvTangent()
    mesh.vertexLayout.stride shouldBe 48
  }

  @Test
  fun sphereNormalsAreUnitLength() {
    assertNormalsUnitLength(Mesh.sphere(stacks = 8, slices = 8))
  }

  @Test
  fun sphereTangentsAreUnitLength() {
    val mesh = Mesh.sphere(stacks = 8, slices = 8)
    val floatsPerVertex = mesh.vertexLayout.stride / 4
    val tangentAttr = mesh.vertexLayout.attributes.first { it.name == "tangent" }
    val tangentFloatOffset = tangentAttr.offset / 4

    // Pole vertices have tangent (0,0,1) which is still unit-length, so all vertices are checked.
    for (v in 0 until mesh.vertexCount) {
      val i = v * floatsPerVertex + tangentFloatOffset
      val tx = mesh.vertices[i]
      val ty = mesh.vertices[i + 1]
      val tz = mesh.vertices[i + 2]
      val length = sqrt(tx * tx + ty * ty + tz * tz)
      length shouldBe (1f plusOrMinus epsilon)
    }
  }

  @Test
  fun sphereTangentsPerpendicularToNormals() {
    val mesh = Mesh.sphere(stacks = 8, slices = 8)
    val floatsPerVertex = mesh.vertexLayout.stride / 4
    val normalAttr = mesh.vertexLayout.attributes.first { it.name == "normal" }
    val tangentAttr = mesh.vertexLayout.attributes.first { it.name == "tangent" }
    val nOff = normalAttr.offset / 4
    val tOff = tangentAttr.offset / 4

    // Check dot product is ~0 (except at poles where tangent is arbitrary)
    for (v in 0 until mesh.vertexCount) {
      val base = v * floatsPerVertex
      val nx = mesh.vertices[base + nOff]
      val ny = mesh.vertices[base + nOff + 1]
      val nz = mesh.vertices[base + nOff + 2]
      val tx = mesh.vertices[base + tOff]
      val ty = mesh.vertices[base + tOff + 1]
      val tz = mesh.vertices[base + tOff + 2]
      val dot = nx * tx + ny * ty + nz * tz
      // At the poles, normal is (0, ±1, 0) and tangent is (-sinTheta, 0, cosTheta)
      // which is always perpendicular, so this check should hold everywhere
      dot shouldBe (0f plusOrMinus 0.001f)
    }
  }

  @Test
  fun sphereDefaultParameters() {
    val mesh = Mesh.sphere()
    mesh.vertexCount shouldBe (32 + 1) * (32 + 1)
    mesh.indexCount shouldBe 32 * 32 * 6
  }

  // --- toString ---

  @Test
  fun toStringFormat() {
    val mesh = Mesh.triangle()
    mesh.toString() shouldBe "Mesh(vertices=3, indices=3, indexed=true, label='Triangle')"
  }
}
