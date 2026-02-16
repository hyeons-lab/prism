package com.hyeonslab.prism.renderer

import co.touchlab.kermit.Logger

private val log = Logger.withTag("RenderSurface.Android")

/**
 * Android implementation of [RenderSurface].
 *
 * GPU surface management on Android will be backed by a SurfaceView with wgpu4k (planned). For now,
 * this stores dimensions only.
 */
actual class RenderSurface {

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height

  private var _width: Int = 0
  private var _height: Int = 0

  actual fun configure(width: Int, height: Int) {
    _width = width
    _height = height
    log.i { "RenderSurface configured: ${width}x${height} (Android stub)" }
  }
}
