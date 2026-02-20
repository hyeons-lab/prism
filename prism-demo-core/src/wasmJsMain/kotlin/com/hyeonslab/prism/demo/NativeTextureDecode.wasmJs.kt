@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")

package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData
import com.hyeonslab.prism.assets.ImageDecoder
import kotlin.js.ExperimentalWasmJsInterop

/**
 * WASM actual: delegates to [ImageDecoder.decodeFromJsBuffer] with zero Kotlin↔JS byte copying.
 * [nativeBuffer] must be the JS `ArrayBuffer` retained from the GLB fetch.
 */
internal actual suspend fun decodeTextureFromNativeBuffer(
  nativeBuffer: Any,
  offset: Int,
  length: Int,
): ImageData? = ImageDecoder.decodeFromJsBuffer(nativeBuffer as JsAny, offset, length)
