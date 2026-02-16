package com.hyeonslab.prism.renderer

import co.touchlab.kermit.Logger

private val log = Logger.withTag("RenderSurface.wasmJs")

/**
 * WebAssembly (JS) implementation of [RenderSurface].
 *
 * On WASM the GPU surface is managed externally via HTML Canvas and the WebGPU API, so this class
 * only stores dimensions.
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
    log.i { "RenderSurface configured: ${width}x${height} (surface managed externally)" }
  }
}
