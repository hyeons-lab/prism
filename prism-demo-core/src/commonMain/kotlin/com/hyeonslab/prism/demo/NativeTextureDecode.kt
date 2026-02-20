package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData

/**
 * Decodes a compressed image stored at a byte range within a platform-native buffer (e.g. the JS
 * `ArrayBuffer` from a GLB fetch on WASM). Returns `null` on platforms that do not retain a native
 * buffer or if decoding fails.
 *
 * The zero-copy path is only active on WASM when the GLB was fetched via
 * [com.hyeonslab.prism.widget.fetchBytesWithNativeBuffer] and the texture's image lives in the GLB
 * BIN chunk (i.e. has a byte range from
 * [com.hyeonslab.prism.assets.GltfLoadResult.rawTextureByteRanges]). All other platforms always
 * return null and the caller falls back to the [com.hyeonslab.prism.assets.ImageDecoder.decode]
 * path.
 */
internal expect suspend fun decodeTextureFromNativeBuffer(
  nativeBuffer: Any,
  offset: Int,
  length: Int,
): ImageData?
