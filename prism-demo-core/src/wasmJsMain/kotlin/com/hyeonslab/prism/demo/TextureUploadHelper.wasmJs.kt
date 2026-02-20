@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData
import com.hyeonslab.prism.renderer.Renderer
import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.ArrayBuffer
import kotlin.js.ExperimentalWasmJsInterop

internal actual fun uploadDecodedImage(renderer: Renderer, texture: Texture, imageData: ImageData) {
  // nativePixelBuffer holds the raw JS ArrayBuffer stored as Any? by ImageDecoder.wasmJs.kt.
  // The safe cast to JsAny is an unchecked cast on JS external interfaces — suppressed above
  // because we know this value is always a JS ArrayBuffer object when non-null on WASM.
  val jsAny = imageData.nativePixelBuffer as? JsAny
  if (jsAny != null && renderer is WgpuRenderer) {
    // Zero-copy path: wrap JS ArrayBuffer directly — no element-by-element copy.
    val wgpuBuffer = ArrayBuffer.wrap(jsAny.unsafeCast<js.buffer.ArrayBuffer>())
    renderer.writeTextureFromArrayBuffer(texture, wgpuBuffer)
  } else {
    renderer.uploadTextureData(texture, imageData.pixels)
  }
}
