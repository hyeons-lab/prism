package com.hyeonslab.prism.assets

actual object ImageDecoder {
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? = null

  actual suspend fun decodeFromNativeBuffer(
    nativeBuffer: Any,
    offset: Int,
    length: Int,
  ): ImageData? {
    return null
  }
}
