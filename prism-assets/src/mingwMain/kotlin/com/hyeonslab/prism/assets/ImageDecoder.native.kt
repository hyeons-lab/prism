package com.hyeonslab.prism.assets

actual object ImageDecoder {
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? {
    TODO("Image decoding for Windows is not yet implemented")
  }

  actual suspend fun decodeFromNativeBuffer(
    nativeBuffer: Any,
    offset: Int,
    length: Int,
  ): ImageData? {
    return null
  }
}
