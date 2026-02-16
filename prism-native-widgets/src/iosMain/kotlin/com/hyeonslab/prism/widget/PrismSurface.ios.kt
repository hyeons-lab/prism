@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import platform.MetalKit.MTKView

private val log = Logger.withTag("PrismSurface.iOS")

actual class PrismSurface(private val view: MTKView? = null) {
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null
  private var iosContext: IosContext? = null

  /** wgpu context created from the MTKView. Available after first valid [resize]. */
  val wgpuContext: WGPUContext?
    get() = iosContext?.wgpuContext

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun resize(width: Int, height: Int) {
    _width = width
    _height = height
    val v =
      view
        ?: run {
          log.w {
            "Resize: ${width}x${height} (no MTKView — use PrismSurface(mtkView) constructor)"
          }
          return
        }
    if (iosContext == null && width > 0 && height > 0) {
      log.i { "Creating wgpu surface from MTKView: ${width}x${height}" }
      iosContext = runBlocking { iosContextRenderer(v, width, height) }
      log.i { "wgpu surface ready (iOS)" }
    } else {
      log.d { "Resize: ${width}x${height}" }
    }
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    iosContext?.close()
    iosContext = null
    engine = null
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}
