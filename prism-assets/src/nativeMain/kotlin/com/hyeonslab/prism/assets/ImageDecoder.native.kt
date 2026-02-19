package com.hyeonslab.prism.assets

actual object ImageDecoder {
  // TODO: implement using CGImage (iOS/macOS) or stb_image (Linux/Windows)
  actual fun decode(bytes: ByteArray): ImageData? = null
}
