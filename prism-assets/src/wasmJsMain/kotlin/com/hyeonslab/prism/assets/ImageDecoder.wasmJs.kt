@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.assets

import co.touchlab.kermit.Logger
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

private val log = Logger.withTag("ImageDecoder.WASM")

/** Allocates a JS Int8Array of [len] bytes. */
@JsFun("(len) => new Int8Array(len)") private external fun createInt8Array(len: Int): JsAny

/** Writes 4 bytes into a JS Int8Array starting at index [i]. */
@JsFun("(arr, i, b0, b1, b2, b3) => { arr[i]=b0; arr[i+1]=b1; arr[i+2]=b2; arr[i+3]=b3; }")
private external fun int8ArraySet4Bytes(arr: JsAny, i: Int, b0: Byte, b1: Byte, b2: Byte, b3: Byte)

/** Writes a single byte into a JS Int8Array at index [i]. */
@JsFun("(arr, i, b) => { arr[i] = b; }")
private external fun int8ArraySetByte(arr: JsAny, i: Int, b: Byte)

/**
 * Decodes [bytes] (PNG/JPEG/etc.) via the browser's `createImageBitmap` and `OffscreenCanvas` APIs.
 * Calls [onSuccess] with (width, height, Uint8ClampedArray of straight-alpha RGBA pixels) or
 * [onError] with an error message if decoding fails.
 */
@JsFun(
  """(bytes, onSuccess, onError) => {
  const blob = new Blob([bytes]);
  createImageBitmap(blob, { premultiplyAlpha: 'none' })
    .then(bmp => {
      const w = bmp.width, h = bmp.height;
      const canvas = new OffscreenCanvas(w, h);
      const ctx = canvas.getContext('2d');
      ctx.drawImage(bmp, 0, 0);
      bmp.close();
      const imgData = ctx.getImageData(0, 0, w, h);
      onSuccess(w, h, imgData.data);
    })
    .catch(e => onError(String(e)));
}"""
)
private external fun decodeImageBitmapJs(
  bytes: JsAny,
  onSuccess: (Int, Int, JsAny) -> Unit,
  onError: (String) -> Unit,
)

/** Returns the length of a JS typed array. */
@JsFun("(arr) => arr.length") private external fun jsTypedArrayLength(arr: JsAny): Int

/**
 * Reads 4 bytes from [arr] starting at [i] as a little-endian signed Int32. 4× fewer JS boundary
 * crossings compared to reading individual bytes.
 */
@JsFun(
  "(arr, i) => (arr[i] & 0xFF) | ((arr[i+1] & 0xFF) << 8) | ((arr[i+2] & 0xFF) << 16) | ((arr[i+3] & 0xFF) << 24)"
)
private external fun jsReadInt32LE(arr: JsAny, i: Int): Int

/** Reads a single byte from a JS typed array at index [i]. */
@JsFun("(arr, i) => arr[i]") private external fun jsReadByte(arr: JsAny, i: Int): Byte

actual object ImageDecoder {
  /**
   * Decodes [bytes] (PNG, JPEG, or any format supported by the browser) into RGBA8 pixel data using
   * the browser's `createImageBitmap` + `OffscreenCanvas` APIs.
   *
   * The [unpremultiply] parameter is ignored on WASM: `createImageBitmap` is invoked with
   * `premultiplyAlpha: 'none'`, so the returned pixels are already straight alpha.
   *
   * Returns `null` if the browser cannot decode the image.
   */
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? {
    // Copy ByteArray → JS Int8Array (4 bytes at a time to reduce boundary crossings)
    val jsBytes = createInt8Array(bytes.size)
    val chunks = bytes.size / 4
    for (c in 0 until chunks) {
      val i = c * 4
      int8ArraySet4Bytes(jsBytes, i, bytes[i], bytes[i + 1], bytes[i + 2], bytes[i + 3])
    }
    for (i in chunks * 4 until bytes.size) {
      int8ArraySetByte(jsBytes, i, bytes[i])
    }

    return suspendCancellableCoroutine { cont ->
      decodeImageBitmapJs(
        jsBytes,
        onSuccess = { width, height, pixelData ->
          val len = jsTypedArrayLength(pixelData)
          val pixels = ByteArray(len)
          // Copy Uint8ClampedArray → ByteArray (4 bytes at a time)
          val c = len / 4
          for (ci in 0 until c) {
            val i = ci * 4
            val v = jsReadInt32LE(pixelData, i)
            pixels[i] = (v and 0xFF).toByte()
            pixels[i + 1] = ((v ushr 8) and 0xFF).toByte()
            pixels[i + 2] = ((v ushr 16) and 0xFF).toByte()
            pixels[i + 3] = ((v ushr 24) and 0xFF).toByte()
          }
          for (i in c * 4 until len) {
            pixels[i] = jsReadByte(pixelData, i)
          }
          cont.resume(ImageData(width, height, pixels))
        },
        onError = { error ->
          log.w { "createImageBitmap failed: $error" }
          cont.resume(null)
        },
      )
    }
  }
}
