@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import kotlin.coroutines.resume
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.suspendCancellableCoroutine

private val log = Logger.withTag("WasmUtils")

@JsFun(
  """(url, onSuccess, onError) => {
  fetch(url)
    .then(r => r.arrayBuffer())
    .then(buf => onSuccess(new Int8Array(buf)))
    .catch(e => onError(String(e)));
}"""
)
private external fun jsFetch(url: String, onSuccess: (JsAny) -> Unit, onError: (String) -> Unit)

@JsFun("(arr) => arr.length") private external fun jsArrayLength(arr: JsAny): Int

@JsFun("(arr, i) => arr[i]") private external fun jsArrayByte(arr: JsAny, i: Int): Byte

@JsFun(
  "(arr, i) => (arr[i] & 0xFF) | ((arr[i+1] & 0xFF) << 8) | ((arr[i+2] & 0xFF) << 16) | ((arr[i+3] & 0xFF) << 24)"
)
private external fun jsReadInt32LE(arr: JsAny, i: Int): Int

/**
 * Fetches a binary resource from [url] and returns its bytes, or `null` if the request fails.
 *
 * Reads the JS `ArrayBuffer` in 4-byte (Int32LE) chunks, reducing JS↔Kotlin boundary crossings by
 * 4× compared to byte-by-byte reads.
 */
suspend fun fetchBytes(url: String): ByteArray? = suspendCancellableCoroutine { cont ->
  jsFetch(
    url,
    onSuccess = { jsArr ->
      val len = jsArrayLength(jsArr)
      val bytes = ByteArray(len)
      val chunks = len / 4
      for (c in 0 until chunks) {
        val i = c * 4
        val v = jsReadInt32LE(jsArr, i)
        bytes[i] = (v and 0xFF).toByte()
        bytes[i + 1] = ((v ushr 8) and 0xFF).toByte()
        bytes[i + 2] = ((v ushr 16) and 0xFF).toByte()
        bytes[i + 3] = ((v ushr 24) and 0xFF).toByte()
      }
      for (i in chunks * 4 until len) {
        bytes[i] = jsArrayByte(jsArr, i)
      }
      cont.resume(bytes)
    },
    onError = { error ->
      log.w { "fetchBytes failed for '$url': $error" }
      cont.resume(null)
    },
  )
}
