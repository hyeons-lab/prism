package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.GLFWContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.glfwContextRenderer
import kotlinx.coroutines.runBlocking

private val log = Logger.withTag("PrismSurface.MinGW")

actual class PrismSurface {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null
  private var glfwContext: GLFWContext? = null

  /** wgpu context created via GLFW/HWND. Available after first valid [resize]. */
  val wgpuContext: WGPUContext?
    get() = glfwContext?.wgpuContext

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun resize(width: Int, height: Int) {
    _width = width
    _height = height
    if (glfwContext == null && width > 0 && height > 0) {
      log.i { "Creating GLFW/HWND surface: ${width}x${height}" }
      glfwContext = runBlocking {
        glfwContextRenderer(
          width = width,
          height = height,
          title = engine?.config?.appName ?: "Prism",
        )
      }
      log.i { "wgpu surface ready (Windows native)" }
    } else {
      log.d { "Resize: ${width}x${height}" }
    }
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    glfwContext?.close()
    glfwContext = null
    engine = null
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}
