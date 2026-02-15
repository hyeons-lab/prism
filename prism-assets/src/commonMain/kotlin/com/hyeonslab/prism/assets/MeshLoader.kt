package com.hyeonslab.prism.assets

import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.VertexLayout

class MeshLoader : AssetLoader<Mesh> {
  override val supportedExtensions: List<String> = listOf("obj", "gltf", "glb")

  override suspend fun load(path: String, data: ByteArray): Mesh {
    // Stub: actual model parsing will be implemented later
    return Mesh(vertexLayout = VertexLayout.positionNormalUv(), label = path)
  }
}
