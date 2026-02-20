package com.hyeonslab.prism.demo

import com.hyeonslab.prism.assets.ImageData

internal actual suspend fun decodeTextureFromNativeBuffer(
  nativeBuffer: Any,
  offset: Int,
  length: Int,
): ImageData? = null
