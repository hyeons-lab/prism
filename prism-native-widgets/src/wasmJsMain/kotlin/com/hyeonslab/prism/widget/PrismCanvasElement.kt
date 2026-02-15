package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger

private val log = Logger.withTag("PrismCanvasElement")

/**
 * Web canvas element for embedding Prism engine rendering. Wraps an HTMLCanvasElement with WebGPU
 * surface attachment.
 */
class PrismCanvasElement {
  val surface: PrismSurface = PrismSurface()

  init {
    log.i {
      "PrismCanvasElement created (WASM stub — HTMLCanvasElement attachment not yet implemented)"
    }
  }
}
