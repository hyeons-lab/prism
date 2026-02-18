@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlinx.cinterop.ExperimentalForeignApi
import platform.MetalKit.MTKView

private val log = Logger.withTag("PrismSurface.iOS")

actual class PrismSurface(iosContext: IosContext? = null) {
  private var _iosContext: IosContext? = iosContext
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  /** wgpu context created from the MTKView. Available when constructed via [createPrismSurface]. */
  actual val wgpuContext: WGPUContext?
    get() = _iosContext?.wgpuContext

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
    _iosContext?.close()
    _iosContext = null
    engine = null
  }

  actual val width: Int
    get() = _width

  actual val height: Int
    get() = _height
}

/** Creates a [PrismSurface] backed by an MTKView with a ready-to-use wgpu context. */
suspend fun createPrismSurface(view: MTKView, width: Int, height: Int): PrismSurface {
  log.i { "Creating wgpu surface from MTKView: ${width}x${height}" }
  val ctx = iosContextRenderer(view, width, height)
  log.i { "wgpu surface ready (iOS)" }
  return PrismSurface(ctx).apply { resize(width, height) }
}
