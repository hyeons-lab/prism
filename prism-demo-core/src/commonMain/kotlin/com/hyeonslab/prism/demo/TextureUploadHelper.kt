package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData
import com.hyeonslab.prism.renderer.Renderer
import com.hyeonslab.prism.renderer.Texture

/**
 * Uploads decoded image data to the GPU. On WASM, uses the zero-copy path via
 * [ImageData.nativePixelBuffer] (a wgpu4k `ArrayBuffer` wrapping the JS `ArrayBuffer` from
 * `getImageData()`), bypassing ~4 million JS interop calls per 2K texture. On all other platforms,
 * delegates to [Renderer.uploadTextureData] with [ImageData.pixels].
 */
internal expect fun uploadDecodedImage(renderer: Renderer, texture: Texture, imageData: ImageData)
