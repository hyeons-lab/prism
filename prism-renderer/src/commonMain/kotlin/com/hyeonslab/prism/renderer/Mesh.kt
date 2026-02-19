package com.hyeonslab.prism.renderer

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A mesh containing vertex and optional index data, ready for GPU upload and rendering.
 *
 * Vertex data is stored as a flat [FloatArray] whose layout is described by [vertexLayout]. Index
 * data, when present, allows vertices to be shared across multiple primitives.
 *
 * @param vertexLayout Describes the per-vertex attribute layout.
 * @param label Optional debug label for graphics debuggers.
 */
class Mesh(val vertexLayout: VertexLayout, val label: String = "") {
  /** Raw vertex data packed according to [vertexLayout]. */
  var vertices: FloatArray = floatArrayOf()

  /** Index data referencing vertices. Empty if the mesh is non-indexed. */
  var indices: IntArray = intArrayOf()

  /** GPU vertex buffer, assigned after [Renderer.uploadMesh] is called. */
  var vertexBuffer: GpuBuffer? = null

  /** GPU index buffer, assigned after [Renderer.uploadMesh] is called (if indexed). */
  var indexBuffer: GpuBuffer? = null

  /** Number of vertices in this mesh, derived from vertex data size and layout stride. */
  val vertexCount: Int
    get() {
      val floatsPerVertex = vertexLayout.stride / 4 // stride is in bytes, floats are 4 bytes
      return if (floatsPerVertex > 0) vertices.size / floatsPerVertex else 0
    }

  /** Number of indices in this mesh. */
  val indexCount: Int
    get() = indices.size

  /** Whether this mesh uses indexed drawing. */
  val isIndexed: Boolean
    get() = indices.isNotEmpty()

  override fun toString(): String =
    "Mesh(vertices=$vertexCount, indices=$indexCount, indexed=$isIndexed, label='$label')"

  companion object {

    /**
     * Creates a unit triangle in the XY plane with position, normal, and UV data.
     *
     * Vertices are wound counter-clockwise. The triangle spans roughly [-0.5, 0.5] in X and
     * [−0.5, 0.5] in Y, centered at the origin.
     *
     * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) = 32 bytes/vertex
     */
    fun triangle(): Mesh {
      val layout = VertexLayout.positionNormalUv()
      val mesh = Mesh(layout, label = "Triangle")

      // Normal pointing towards the viewer (positive Z in right-handed coords)
      val nx = 0f
      val ny = 0f
      val nz = 1f

      mesh.vertices =
        floatArrayOf(
          // position          // normal      // uv
          0.0f,
          0.5f,
          0.0f,
          nx,
          ny,
          nz,
          0.5f,
          1.0f, // top center
          -0.5f,
          -0.5f,
          0.0f,
          nx,
          ny,
          nz,
          0.0f,
          0.0f, // bottom left
          0.5f,
          -0.5f,
          0.0f,
          nx,
          ny,
          nz,
          1.0f,
          0.0f, // bottom right
        )

      mesh.indices = intArrayOf(0, 1, 2)
      return mesh
    }

    /**
     * Creates a unit quad in the XY plane with position, normal, and UV data.
     *
     * The quad spans [-0.5, 0.5] in both X and Y, centered at the origin. Composed of two triangles
     * with counter-clockwise winding.
     *
     * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) = 32 bytes/vertex
     */
    fun quad(): Mesh {
      val layout = VertexLayout.positionNormalUv()
      val mesh = Mesh(layout, label = "Quad")

      val nx = 0f
      val ny = 0f
      val nz = 1f

      mesh.vertices =
        floatArrayOf(
          // position          // normal      // uv
          -0.5f,
          0.5f,
          0.0f,
          nx,
          ny,
          nz,
          0.0f,
          1.0f, // top-left
          -0.5f,
          -0.5f,
          0.0f,
          nx,
          ny,
          nz,
          0.0f,
          0.0f, // bottom-left
          0.5f,
          -0.5f,
          0.0f,
          nx,
          ny,
          nz,
          1.0f,
          0.0f, // bottom-right
          0.5f,
          0.5f,
          0.0f,
          nx,
          ny,
          nz,
          1.0f,
          1.0f, // top-right
        )

      mesh.indices =
        intArrayOf(
          0,
          1,
          2, // first triangle
          0,
          2,
          3, // second triangle
        )
      return mesh
    }

    /**
     * Creates a unit cube centered at the origin with position, normal, UV, and tangent data.
     *
     * Each face has its own set of vertices so normals are correct per-face. The cube spans
     * [-0.5, 0.5] in all axes. All faces use counter-clockwise winding when viewed from outside the
     * cube. Per-face tangent vectors point in the direction of increasing U coordinate.
     *
     * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) + tangent (Vec4) = 48 bytes/vertex
     */
    @Suppress("LongMethod") // Vertex data array, not logic complexity
    fun cube(): Mesh {
      val layout = VertexLayout.positionNormalUvTangent()
      val mesh = Mesh(layout, label = "Cube")

      // Helper to write one face (4 vertices) with a shared normal and tangent directly into
      // [dst] at [dstOffset], avoiding intermediate array allocations.
      // Tangent = direction of increasing U, w=1.0 (right-handed TBN).
      fun face(
        dst: FloatArray,
        dstOffset: Int,
        positions: FloatArray,
        n: FloatArray,
        t: FloatArray,
        uvs: FloatArray,
      ) {
        for (v in 0 until 4) {
          val o = dstOffset + v * 12
          val p = v * 3
          val u = v * 2
          dst[o + 0] = positions[p]
          dst[o + 1] = positions[p + 1]
          dst[o + 2] = positions[p + 2]
          dst[o + 3] = n[0]
          dst[o + 4] = n[1]
          dst[o + 5] = n[2]
          dst[o + 6] = uvs[u]
          dst[o + 7] = uvs[u + 1]
          dst[o + 8] = t[0]
          dst[o + 9] = t[1]
          dst[o + 10] = t[2]
          dst[o + 11] = t[3]
        }
      }

      val s = 0.5f
      val verts = FloatArray(6 * 4 * 12) // 6 faces × 4 verts × 12 floats, no intermediate copies
      face( // Front (+Z): tangent = +X
        verts,
        0 * 48,
        floatArrayOf(-s, s, s, -s, -s, s, s, -s, s, s, s, s),
        floatArrayOf(0f, 0f, 1f),
        floatArrayOf(1f, 0f, 0f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )
      face( // Back (-Z): tangent = -X
        verts,
        1 * 48,
        floatArrayOf(s, s, -s, s, -s, -s, -s, -s, -s, -s, s, -s),
        floatArrayOf(0f, 0f, -1f),
        floatArrayOf(-1f, 0f, 0f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )
      face( // Right (+X): tangent = -Z
        verts,
        2 * 48,
        floatArrayOf(s, s, s, s, -s, s, s, -s, -s, s, s, -s),
        floatArrayOf(1f, 0f, 0f),
        floatArrayOf(0f, 0f, -1f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )
      face( // Left (-X): tangent = +Z
        verts,
        3 * 48,
        floatArrayOf(-s, s, -s, -s, -s, -s, -s, -s, s, -s, s, s),
        floatArrayOf(-1f, 0f, 0f),
        floatArrayOf(0f, 0f, 1f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )
      face( // Top (+Y): tangent = +X
        verts,
        4 * 48,
        floatArrayOf(-s, s, -s, -s, s, s, s, s, s, s, s, -s),
        floatArrayOf(0f, 1f, 0f),
        floatArrayOf(1f, 0f, 0f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )
      face( // Bottom (-Y): tangent = +X
        verts,
        5 * 48,
        floatArrayOf(-s, -s, s, -s, -s, -s, s, -s, -s, s, -s, s),
        floatArrayOf(0f, -1f, 0f),
        floatArrayOf(1f, 0f, 0f, 1f),
        floatArrayOf(0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f),
      )

      mesh.vertices = verts
      mesh.indices =
        intArrayOf(
          0,
          1,
          2,
          0,
          2,
          3, // Front
          4,
          5,
          6,
          4,
          6,
          7, // Back
          8,
          9,
          10,
          8,
          10,
          11, // Right
          12,
          13,
          14,
          12,
          14,
          15, // Left
          16,
          17,
          18,
          16,
          18,
          19, // Top
          20,
          21,
          22,
          20,
          22,
          23, // Bottom
        )
      return mesh
    }

    /**
     * Creates a UV sphere centered at the origin with position, normal, UV, and tangent data.
     *
     * Standard parametric sphere using latitude rings (−PI/2 to PI/2) and longitude (0 to 2PI). UV
     * seam vertices are duplicated so the texture wraps correctly. Tangent vectors are the partial
     * derivative ∂position/∂u, with w = +1.0 (right-handed TBN).
     *
     * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) + tangent (Vec4) = 48 bytes/vertex
     *
     * @param stacks Number of horizontal rings (latitude divisions).
     * @param slices Number of vertical segments (longitude divisions).
     * @param radius Sphere radius.
     */
    @Suppress("LongMethod") // Vertex generation loop, not logic complexity
    fun sphere(stacks: Int = 32, slices: Int = 32, radius: Float = 0.5f): Mesh {
      val layout = VertexLayout.positionNormalUvTangent()
      val mesh = Mesh(layout, label = "Sphere")
      val floatsPerVertex = layout.stride / 4 // 48 / 4 = 12

      val vertCount = (stacks + 1) * (slices + 1)
      val verts = FloatArray(vertCount * floatsPerVertex)

      var vi = 0
      for (stack in 0..stacks) {
        val phi = PI.toFloat() * stack / stacks - PI.toFloat() / 2f // -PI/2 to +PI/2
        val sinPhi = sin(phi)
        val cosPhi = cos(phi)

        for (slice in 0..slices) {
          val theta = 2f * PI.toFloat() * slice / slices // 0 to 2PI
          val sinTheta = sin(theta)
          val cosTheta = cos(theta)

          // Position
          val px = radius * cosPhi * cosTheta
          val py = radius * sinPhi
          val pz = radius * cosPhi * sinTheta

          // Normal = normalized position (unit sphere)
          val nx = cosPhi * cosTheta
          val ny = sinPhi
          val nz = cosPhi * sinTheta

          // UV
          val u = slice.toFloat() / slices
          val v = stack.toFloat() / stacks

          // Tangent = ∂position/∂theta normalized = (-sinTheta, 0, cosTheta)
          val tx = -sinTheta
          val ty = 0f
          val tz = cosTheta
          val tw = 1f // right-handed TBN

          verts[vi++] = px
          verts[vi++] = py
          verts[vi++] = pz
          verts[vi++] = nx
          verts[vi++] = ny
          verts[vi++] = nz
          verts[vi++] = u
          verts[vi++] = v
          verts[vi++] = tx
          verts[vi++] = ty
          verts[vi++] = tz
          verts[vi++] = tw
        }
      }

      val idxCount = stacks * slices * 6
      val idx = IntArray(idxCount)
      var ii = 0
      for (stack in 0 until stacks) {
        for (slice in 0 until slices) {
          val topLeft = stack * (slices + 1) + slice
          val topRight = topLeft + 1
          val bottomLeft = topLeft + (slices + 1)
          val bottomRight = bottomLeft + 1

          // Two triangles per quad (CCW winding)
          idx[ii++] = topLeft
          idx[ii++] = bottomLeft
          idx[ii++] = bottomRight

          idx[ii++] = topLeft
          idx[ii++] = bottomRight
          idx[ii++] = topRight
        }
      }

      mesh.vertices = verts
      mesh.indices = idx
      return mesh
    }
  }
}
