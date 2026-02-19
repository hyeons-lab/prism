package com.hyeonslab.prism.assets

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual object ImageDecoder {
  // javax.imageio (TYPE_INT_ARGB) returns straight alpha — unpremultiply is a no-op on JVM.
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? {
    val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
    val width = src.width
    val height = src.height

    // Ensure the image is in ARGB format so getRGB() gives consistent results
    val img =
      if (src.type == BufferedImage.TYPE_INT_ARGB) {
        src
      } else {
        val converted = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = converted.createGraphics()
        g.drawImage(src, 0, 0, null)
        g.dispose()
        converted
      }

    val pixels = ByteArray(width * height * 4)
    for (y in 0 until height) {
      for (x in 0 until width) {
        val argb = img.getRGB(x, y)
        val i = (y * width + x) * 4
        pixels[i + 0] = ((argb shr 16) and 0xFF).toByte() // R
        pixels[i + 1] = ((argb shr 8) and 0xFF).toByte() // G
        pixels[i + 2] = (argb and 0xFF).toByte() // B
        pixels[i + 3] = ((argb shr 24) and 0xFF).toByte() // A
      }
    }
    return ImageData(width, height, pixels)
  }
}
