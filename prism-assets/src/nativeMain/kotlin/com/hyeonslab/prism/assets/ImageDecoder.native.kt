@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hyeonslab.prism.assets

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFRelease
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextDrawImage
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.CGRectMake
import platform.ImageIO.CGImageSourceCreateImageAtIndex
import platform.ImageIO.CGImageSourceCreateWithData

actual object ImageDecoder {
  /**
   * Decodes PNG/JPEG/etc. bytes into RGBA8 pixel data using CoreGraphics.
   *
   * Alpha is premultiplied (kCGImageAlphaPremultipliedLast), which is identical to straight alpha
   * for fully opaque textures.
   */
  actual fun decode(bytes: ByteArray): ImageData? {
    // Wrap ByteArray in CFData
    val cfData =
      bytes.usePinned { pinned ->
        CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
      } ?: return null

    // Decode via ImageIO
    val imageSource = CGImageSourceCreateWithData(cfData, null)
    CFRelease(cfData)
    imageSource ?: return null

    val cgImage = CGImageSourceCreateImageAtIndex(imageSource, 0u, null)
    CFRelease(imageSource)
    cgImage ?: return null

    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()

    if (width == 0 || height == 0) {
      CGImageRelease(cgImage)
      return null
    }

    // Render into a RGBA8 bitmap context
    val pixels = ByteArray(width * height * 4)
    pixels.usePinned { pinned ->
      val colorSpace = CGColorSpaceCreateDeviceRGB()
      val context =
        CGBitmapContextCreate(
          data = pinned.addressOf(0),
          width = width.toULong(),
          height = height.toULong(),
          bitsPerComponent = 8u,
          bytesPerRow = (width * 4).toULong(),
          space = colorSpace,
          bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value,
        )
      CGColorSpaceRelease(colorSpace)
      if (context != null) {
        CGContextDrawImage(
          context,
          CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()),
          cgImage,
        )
        CGContextRelease(context)
      }
    }

    CGImageRelease(cgImage)
    return ImageData(width, height, pixels)
  }
}
