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
 * Calls [onSuccess] with (width, height, underlying `ArrayBuffer` of the pixel data as a JS object)
 * or [onError] with an error message if decoding fails.
 *
 * The pixel data is returned as a sliced JS `ArrayBuffer` so callers can wrap it with zero copy via
 * `ArrayBuffer.wrap()`. Using `.slice()` ensures byteOffset=0 in the returned buffer.
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
      // Return the underlying ArrayBuffer directly (zero-copy to wgpu4k ArrayBuffer.wrap).
      // Use .slice() to ensure byteOffset=0 in case the UA returns a non-zero-offset view.
      const clamped = imgData.data;
      const buf = clamped.buffer.slice(clamped.byteOffset, clamped.byteOffset + clamped.byteLength);
      onSuccess(w, h, buf);
    })
    .catch(e => onError(String(e)));
}"""
)
private external fun decodeImageBitmapJs(
  bytes: JsAny,
  onSuccess: (Int, Int, JsAny) -> Unit,
  onError: (String) -> Unit,
)

actual object ImageDecoder {
  /**
   * Decodes [bytes] (PNG, JPEG, or any format supported by the browser) into RGBA8 pixel data using
   * the browser's `createImageBitmap` + `OffscreenCanvas` APIs.
   *
   * On WASM, pixel data stays on the JS side: the returned [ImageData] has an empty [pixels] array
   * and [ImageData.nativePixelBuffer] set to the raw JS `ArrayBuffer` object (`JsAny`). Callers in
   * `prism-demo-core` wrap it via `ArrayBuffer.wrap()` with zero Kotlin↔JS element-by-element copy,
   * avoiding ~4 million interop calls per 2K texture.
   *
   * The [unpremultiply] parameter is ignored on WASM: `createImageBitmap` is invoked with
   * `premultiplyAlpha: 'none'`, so the returned pixels are already straight alpha.
   *
   * Returns `null` if the browser cannot decode the image.
   */
  actual suspend fun decode(bytes: ByteArray, unpremultiply: Boolean): ImageData? {
    // Copy compressed image bytes ByteArray → JS Int8Array (4 bytes at a time).
    // This is ~500KB for a typical texture — unavoidable, but much smaller than 16MB pixels.
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
        onSuccess = { width, height, jsArrayBuffer ->
          // Store the raw JS ArrayBuffer as nativePixelBuffer (Any?). Callers in prism-demo-core
          // (which has wgpu4k on its classpath) wrap it via ArrayBuffer.wrap() — zero copy.
          val imageData = ImageData(width, height, ByteArray(0))
          imageData.nativePixelBuffer = jsArrayBuffer
          cont.resume(imageData)
        },
        onError = { error ->
          log.w { "createImageBitmap failed: $error" }
          cont.resume(null)
        },
      )
    }
  }
}
