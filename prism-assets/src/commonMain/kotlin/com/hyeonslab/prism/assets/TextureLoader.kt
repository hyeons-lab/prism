package com.hyeonslab.prism.assets

import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.TextureDescriptor
import com.hyeonslab.prism.renderer.TextureFormat

class TextureLoader : AssetLoader<Texture> {
  override val supportedExtensions: List<String> = listOf("png", "jpg", "jpeg", "bmp", "tga")

  override suspend fun load(path: String, data: ByteArray): Texture {
    // Stub: actual image decoding and GPU upload will be implemented with wgpu4k
    val descriptor =
      TextureDescriptor(width = 1, height = 1, format = TextureFormat.RGBA8_SRGB, label = path)
    return Texture(descriptor)
  }
}
