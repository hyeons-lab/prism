package com.hyeonslab.prism.assets

import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.VertexLayout

class MeshLoader : AssetLoader<Mesh> {
  // gltf/glb are handled by GltfLoader; this stub covers future OBJ support
  override val supportedExtensions: List<String> = listOf("obj")

  override suspend fun load(path: String, data: ByteArray): Mesh {
    // Stub: actual model parsing will be implemented later
    return Mesh(vertexLayout = VertexLayout.positionNormalUv(), label = path)
  }
}
