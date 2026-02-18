package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.GLFWContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.glfwContextRenderer

private val log = Logger.withTag("PrismSurface.JVM")

actual class PrismSurface(glfwContext: GLFWContext? = null) {
  private var _glfwContext: GLFWContext? = glfwContext
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  /** wgpu context created via GLFW. Available when constructed via [createPrismSurface]. */
  actual val wgpuContext: WGPUContext?
    get() = _glfwContext?.wgpuContext

  /** GLFW window handle (LWJGL Long). */
  val windowHandler: Long?
    get() = _glfwContext?.windowHandler

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    _glfwContext?.close()
    _glfwContext = null
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

/**
 * Creates a [PrismSurface] backed by a GLFW window with a ready-to-use wgpu context.
 *
 * The `suspend` modifier is for API consistency with native targets where context creation is
 * genuinely asynchronous.
 */
@Suppress("RedundantSuspendModifier")
suspend fun createPrismSurface(width: Int, height: Int, title: String = "Prism"): PrismSurface {
  log.i { "Creating GLFW surface: ${width}x${height}" }
  val ctx = glfwContextRenderer(width = width, height = height, title = title)
  log.i { "wgpu surface ready (JVM)" }
  return PrismSurface(ctx).apply { resize(width, height) }
}
