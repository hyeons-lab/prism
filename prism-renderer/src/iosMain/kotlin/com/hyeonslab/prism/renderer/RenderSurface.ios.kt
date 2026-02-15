package com.hyeonslab.prism.renderer

import co.touchlab.kermit.Logger

private val log = Logger.withTag("RenderSurface.ios")

/**
 * iOS implementation of [RenderSurface].
 *
 * On iOS the GPU surface is managed externally by wgpu4k's [iosContextRenderer] via an MTKView, so
 * this class only stores dimensions. The [configure] method logs the configuration but does not
 * create or modify any GPU resources.
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
    log.i { "RenderSurface configured: ${width}x${height} (surface managed by MTKView)" }
  }
}
