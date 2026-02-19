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
 * Converts premultiplied-alpha RGBA8 pixels to straight (un-premultiplied) alpha in-place.
 *
 * Premultiplied: each RGB channel has already been scaled by `A/255`. This reverses that by
 * dividing RGB by `A/255`, restoring the original color values.
 *
 * Pixels with `A=0` or `A=255` are left unchanged (zero-alpha is undefined; full-opaque is
 * identical in both representations).
 */
internal fun ByteArray.unpremultiplyAlpha() {
  require(size % 4 == 0) { "unpremultiplyAlpha: array size must be a multiple of 4, got $size" }
  var i = 0
  while (i < size) {
    val a = this[i + 3].toInt() and 0xFF
    if (a in 1..254) {
      this[i + 0] = (((this[i + 0].toInt() and 0xFF) * 255 + a / 2) / a).coerceAtMost(255).toByte()
      this[i + 1] = (((this[i + 1].toInt() and 0xFF) * 255 + a / 2) / a).coerceAtMost(255).toByte()
      this[i + 2] = (((this[i + 2].toInt() and 0xFF) * 255 + a / 2) / a).coerceAtMost(255).toByte()
    }
    i += 4
  }
}

/**
 * Platform-specific image decoder. Decodes PNG/JPEG/etc. bytes into RGBA8 pixel data.
 *
 * @param bytes Raw image file bytes.
 * @param unpremultiply When `true`, convert premultiplied-alpha pixels to straight alpha before
 *   returning. Defaults to `false` (premultiplied pixels returned as-is). Has no effect on
 *   platforms where the decoder already returns straight alpha (JVM). Pass `true` when loading
 *   textures for GPU upload, which expects straight alpha.
 *
 * Returns `null` if decoding is unsupported on this platform or the bytes are invalid.
 */
expect object ImageDecoder {
  suspend fun decode(bytes: ByteArray, unpremultiply: Boolean = false): ImageData?
}
