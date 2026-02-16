@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.widget

import cnames.structs.GLFWwindow
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.GLFWContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.glfwContextRenderer
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.ExperimentalForeignApi

private val log = Logger.withTag("PrismSurface.Linux")

actual class PrismSurface(glfwContext: GLFWContext? = null) {
  private var _glfwContext: GLFWContext? = glfwContext
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  /** wgpu context created via GLFW/X11. Available when constructed via [createPrismSurface]. */
  val wgpuContext: WGPUContext?
    get() = _glfwContext?.wgpuContext

  /** Native GLFW window handle for the render loop. */
  val windowHandler: CValuesRef<GLFWwindow>?
    get() = _glfwContext?.windowHandler

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun resize(width: Int, height: Int) {
    _width = width
    _height = height
    log.d { "Resize: ${width}x${height}" }
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    _glfwContext?.close()
    _glfwContext = null
    engine = null
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}

/** Creates a [PrismSurface] backed by a GLFW/X11 window with a ready-to-use wgpu context. */
suspend fun createPrismSurface(width: Int, height: Int, title: String = "Prism"): PrismSurface {
  log.i { "Creating GLFW/X11 surface: ${width}x${height}" }
  val ctx = glfwContextRenderer(width = width, height = height, title = title)
  log.i { "wgpu surface ready (Linux native)" }
  return PrismSurface(ctx).apply { resize(width, height) }
}
