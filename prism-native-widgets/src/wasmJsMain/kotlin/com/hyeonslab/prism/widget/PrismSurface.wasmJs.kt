package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.CanvasContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.canvasContextRenderer
import web.html.HTMLCanvasElement

private val log = Logger.withTag("PrismSurface.WASM")

actual class PrismSurface(canvasContext: CanvasContext? = null) {
  private var _canvasContext: CanvasContext? = canvasContext
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  /** wgpu context created from the Canvas. Available when constructed via [createPrismSurface]. */
  val wgpuContext: WGPUContext?
    get() = _canvasContext?.wgpuContext

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    _canvasContext?.close()
    _canvasContext = null
    engine = null
  }

  actual fun resize(width: Int, height: Int) {
    log.d { "Resize: ${width}x${height}" }
    _width = width
    _height = height
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}

/** Creates a [PrismSurface] backed by an HTML Canvas with a ready-to-use WebGPU context. */
suspend fun createPrismSurface(canvas: HTMLCanvasElement, width: Int, height: Int): PrismSurface {
  log.i { "Creating WebGPU surface from Canvas: ${width}x${height}" }
  val ctx = canvasContextRenderer(htmlCanvas = canvas, width = width, height = height)
  log.i { "wgpu surface ready (WASM)" }
  return PrismSurface(ctx).apply { resize(width, height) }
}
