package engine.prism.renderer

/**
 * A mesh containing vertex and optional index data, ready for GPU upload and rendering.
 *
 * Vertex data is stored as a flat [FloatArray] whose layout is described by [vertexLayout].
 * Index data, when present, allows vertices to be shared across multiple primitives.
 *
 * @param vertexLayout Describes the per-vertex attribute layout.
 * @param label Optional debug label for graphics debuggers.
 */
class Mesh(
    val vertexLayout: VertexLayout,
    val label: String = "",
) {
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
         * Vertices are wound counter-clockwise. The triangle spans roughly [-0.5, 0.5]
         * in X and [−0.5, 0.5] in Y, centered at the origin.
         *
         * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) = 32 bytes/vertex
         */
        fun triangle(): Mesh {
            val layout = VertexLayout.positionNormalUv()
            val mesh = Mesh(layout, label = "Triangle")

            // Normal pointing towards the viewer (positive Z in right-handed coords)
            val nx = 0f; val ny = 0f; val nz = 1f

            mesh.vertices = floatArrayOf(
                // position          // normal      // uv
                 0.0f,  0.5f, 0.0f,  nx, ny, nz,   0.5f, 1.0f,  // top center
                -0.5f, -0.5f, 0.0f,  nx, ny, nz,   0.0f, 0.0f,  // bottom left
                 0.5f, -0.5f, 0.0f,  nx, ny, nz,   1.0f, 0.0f,  // bottom right
            )

            mesh.indices = intArrayOf(0, 1, 2)
            return mesh
        }

        /**
         * Creates a unit quad in the XY plane with position, normal, and UV data.
         *
         * The quad spans [-0.5, 0.5] in both X and Y, centered at the origin.
         * Composed of two triangles with counter-clockwise winding.
         *
         * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) = 32 bytes/vertex
         */
        fun quad(): Mesh {
            val layout = VertexLayout.positionNormalUv()
            val mesh = Mesh(layout, label = "Quad")

            val nx = 0f; val ny = 0f; val nz = 1f

            mesh.vertices = floatArrayOf(
                // position          // normal      // uv
                -0.5f,  0.5f, 0.0f,  nx, ny, nz,   0.0f, 1.0f,  // top-left
                -0.5f, -0.5f, 0.0f,  nx, ny, nz,   0.0f, 0.0f,  // bottom-left
                 0.5f, -0.5f, 0.0f,  nx, ny, nz,   1.0f, 0.0f,  // bottom-right
                 0.5f,  0.5f, 0.0f,  nx, ny, nz,   1.0f, 1.0f,  // top-right
            )

            mesh.indices = intArrayOf(
                0, 1, 2,  // first triangle
                0, 2, 3,  // second triangle
            )
            return mesh
        }

        /**
         * Creates a unit cube centered at the origin with position, normal, and UV data.
         *
         * Each face has its own set of vertices so normals are correct per-face.
         * The cube spans [-0.5, 0.5] in all axes. All faces use counter-clockwise
         * winding when viewed from outside the cube.
         *
         * Layout: position (Vec3) + normal (Vec3) + uv (Vec2) = 32 bytes/vertex
         */
        fun cube(): Mesh {
            val layout = VertexLayout.positionNormalUv()
            val mesh = Mesh(layout, label = "Cube")

            mesh.vertices = floatArrayOf(
                // --- Front face (Z+) ---
                // position          // normal       // uv
                -0.5f,  0.5f,  0.5f,  0f, 0f, 1f,  0.0f, 1.0f,  // 0
                -0.5f, -0.5f,  0.5f,  0f, 0f, 1f,  0.0f, 0.0f,  // 1
                 0.5f, -0.5f,  0.5f,  0f, 0f, 1f,  1.0f, 0.0f,  // 2
                 0.5f,  0.5f,  0.5f,  0f, 0f, 1f,  1.0f, 1.0f,  // 3

                // --- Back face (Z-) ---
                 0.5f,  0.5f, -0.5f,  0f, 0f, -1f,  0.0f, 1.0f,  // 4
                 0.5f, -0.5f, -0.5f,  0f, 0f, -1f,  0.0f, 0.0f,  // 5
                -0.5f, -0.5f, -0.5f,  0f, 0f, -1f,  1.0f, 0.0f,  // 6
                -0.5f,  0.5f, -0.5f,  0f, 0f, -1f,  1.0f, 1.0f,  // 7

                // --- Right face (X+) ---
                 0.5f,  0.5f,  0.5f,  1f, 0f, 0f,  0.0f, 1.0f,  // 8
                 0.5f, -0.5f,  0.5f,  1f, 0f, 0f,  0.0f, 0.0f,  // 9
                 0.5f, -0.5f, -0.5f,  1f, 0f, 0f,  1.0f, 0.0f,  // 10
                 0.5f,  0.5f, -0.5f,  1f, 0f, 0f,  1.0f, 1.0f,  // 11

                // --- Left face (X-) ---
                -0.5f,  0.5f, -0.5f, -1f, 0f, 0f,  0.0f, 1.0f,  // 12
                -0.5f, -0.5f, -0.5f, -1f, 0f, 0f,  0.0f, 0.0f,  // 13
                -0.5f, -0.5f,  0.5f, -1f, 0f, 0f,  1.0f, 0.0f,  // 14
                -0.5f,  0.5f,  0.5f, -1f, 0f, 0f,  1.0f, 1.0f,  // 15

                // --- Top face (Y+) ---
                -0.5f,  0.5f, -0.5f,  0f, 1f, 0f,  0.0f, 1.0f,  // 16
                -0.5f,  0.5f,  0.5f,  0f, 1f, 0f,  0.0f, 0.0f,  // 17
                 0.5f,  0.5f,  0.5f,  0f, 1f, 0f,  1.0f, 0.0f,  // 18
                 0.5f,  0.5f, -0.5f,  0f, 1f, 0f,  1.0f, 1.0f,  // 19

                // --- Bottom face (Y-) ---
                -0.5f, -0.5f,  0.5f,  0f, -1f, 0f,  0.0f, 1.0f,  // 20
                -0.5f, -0.5f, -0.5f,  0f, -1f, 0f,  0.0f, 0.0f,  // 21
                 0.5f, -0.5f, -0.5f,  0f, -1f, 0f,  1.0f, 0.0f,  // 22
                 0.5f, -0.5f,  0.5f,  0f, -1f, 0f,  1.0f, 1.0f,  // 23
            )

            mesh.indices = intArrayOf(
                // Front
                0, 1, 2, 0, 2, 3,
                // Back
                4, 5, 6, 4, 6, 7,
                // Right
                8, 9, 10, 8, 10, 11,
                // Left
                12, 13, 14, 12, 14, 15,
                // Top
                16, 17, 18, 16, 18, 19,
                // Bottom
                20, 21, 22, 20, 22, 23,
            )
            return mesh
        }
    }
}
