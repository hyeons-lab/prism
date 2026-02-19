package com.hyeonslab.prism.assets

/** Raw RGBA8 pixel data decoded from an image file. Pixels are row-major, 4 bytes per pixel. */
data class ImageData(val width: Int, val height: Int, val pixels: ByteArray) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is ImageData) return false
    return width == other.width && height == other.height && pixels.contentEquals(other.pixels)
  }

  override fun hashCode(): Int {
    var result = width
    result = 31 * result + height
    result = 31 * result + pixels.contentHashCode()
    return result
  }
}

/**
 * Platform-specific image decoder. Decodes PNG/JPEG/etc. bytes into RGBA8 pixel data.
 *
 * Returns null if decoding is unsupported on this platform or the bytes are invalid.
 */
expect object ImageDecoder {
  fun decode(bytes: ByteArray): ImageData?
}
