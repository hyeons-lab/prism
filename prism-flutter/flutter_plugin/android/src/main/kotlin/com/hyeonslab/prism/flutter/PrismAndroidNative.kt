package com.hyeonslab.prism.flutter

import android.view.Surface

object PrismAndroidNative {
  init {
    System.loadLibrary("prism")
  }

  external fun nAttachSurface(handle: Long, surface: Surface, w: Int, h: Int)
  external fun nRenderFrame(handle: Long)
  external fun nResize(handle: Long, w: Int, h: Int)
  external fun nDetachSurface(handle: Long)
}
