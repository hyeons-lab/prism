package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine

private val log = Logger.withTag("PrismSurface.JVM")

actual class PrismSurface {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine" }
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
