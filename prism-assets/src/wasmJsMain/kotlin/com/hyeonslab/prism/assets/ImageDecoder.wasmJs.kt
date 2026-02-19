package com.hyeonslab.prism.assets

actual object ImageDecoder {
  // TODO: implement using browser createImageBitmap / OffscreenCanvas
  actual fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? = null
}
