package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData
import com.hyeonslab.prism.renderer.Renderer
import com.hyeonslab.prism.renderer.Texture

internal actual fun uploadDecodedImage(renderer: Renderer, texture: Texture, imageData: ImageData) {
  renderer.uploadTextureData(texture, imageData.pixels)
}
