package com.hyeonslab.prism.assets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer

actual object ImageDecoder {
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val width = bitmap.width
    val height = bitmap.height

    // Ensure ARGB_8888 config for consistent pixel format
    val rgba =
      if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
      else bitmap.copy(Bitmap.Config.ARGB_8888, false)

    // BitmapFactory produces premultiplied alpha by default (Bitmap.isPremultiplied() == true).
    // copyPixelsToBuffer copies raw premultiplied RGBA bytes.
    val buffer = ByteBuffer.allocate(width * height * 4)
    rgba.copyPixelsToBuffer(buffer)
    val pixels = buffer.array()

    if (unpremultiply) pixels.unpremultiplyAlpha()
    return ImageData(width, height, pixels)
  }
}
