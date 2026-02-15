package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine

private val log = Logger.withTag("PrismSurface.Linux")

actual class PrismSurface {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine (Linux stub — X11/Wayland not yet implemented)" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine (Linux stub)" }
    engine = null
  }

  actual fun resize(width: Int, height: Int) {
    log.d { "Resize: ${width}x${height} (Linux stub)" }
    _width = width
    _height = height
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}
