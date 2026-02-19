package com.hyeonslab.prism.assets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer

actual object ImageDecoder {
  actual fun decode(bytes: ByteArray): ImageData? {
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
    val width = bitmap.width
    val height = bitmap.height

    // Ensure ARGB_8888 config for consistent pixel format
    val rgba =
      if (bitmap.config == Bitmap.Config.ARGB_8888) bitmap
      else bitmap.copy(Bitmap.Config.ARGB_8888, false)

    // copyPixelsToBuffer writes pixels in RGBA order on Android
    val buffer = ByteBuffer.allocate(width * height * 4)
    rgba.copyPixelsToBuffer(buffer)
    return ImageData(width, height, buffer.array())
  }
}
