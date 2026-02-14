package engine.prism.assets

import engine.prism.renderer.Mesh
import engine.prism.renderer.VertexLayout

class MeshLoader : AssetLoader<Mesh> {
    override val supportedExtensions: List<String> = listOf("obj", "gltf", "glb")

    override suspend fun load(path: String, data: ByteArray): Mesh {
        // Stub: actual model parsing will be implemented later
        return Mesh(vertexLayout = VertexLayout.positionNormalUv(), label = path)
    }
}
