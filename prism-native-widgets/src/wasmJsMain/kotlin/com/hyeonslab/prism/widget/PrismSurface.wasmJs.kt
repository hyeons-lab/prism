package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine

private val log = Logger.withTag("PrismSurface.WASM")

actual class PrismSurface {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine (WASM stub)" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine (WASM stub)" }
    engine = null
  }

  actual fun resize(width: Int, height: Int) {
    log.d { "Resize: ${width}x${height} (WASM stub)" }
    _width = width
    _height = height
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}
