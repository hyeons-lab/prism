package engine.prism.widget

import engine.prism.core.Engine

// Will wrap an HWND for DirectX/Vulkan-backed wgpu rendering on Windows (MinGW)
actual class PrismSurface {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  actual fun attach(engine: Engine) {
    this.engine = engine
  }

  actual fun detach() {
    engine = null
  }

  actual fun resize(width: Int, height: Int) {
    _width = width
    _height = height
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}
