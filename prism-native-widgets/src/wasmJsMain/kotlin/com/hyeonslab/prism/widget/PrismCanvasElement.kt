package com.hyeonslab.prism.widget

/**
 * Web canvas element for embedding Prism engine rendering. Wraps an HTMLCanvasElement with WebGPU
 * surface attachment.
 */
class PrismCanvasElement {
  val surface: PrismSurface = PrismSurface()
  // TODO: Attach to HTMLCanvasElement and create WebGPU surface
}
