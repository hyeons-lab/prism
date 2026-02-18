package com.hyeonslab.prism.widget

import android.view.SurfaceHolder
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import io.ygdrasil.webgpu.AndroidContext
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.androidContextRenderer

private val log = Logger.withTag("PrismSurface.Android")

actual class PrismSurface(androidContext: AndroidContext? = null) {
  private var _androidContext: AndroidContext? = androidContext
  private var _width: Int = 0
  private var _height: Int = 0
  private var engine: Engine? = null

  /** wgpu context created via Android SurfaceHolder. Available when constructed via
   * [createPrismSurface]. */
  actual val wgpuContext: WGPUContext?
    get() = _androidContext?.wgpuContext

  actual fun attach(engine: Engine) {
    log.i { "Attaching engine '${engine.config.appName}'" }
    this.engine = engine
  }

  actual fun detach() {
    log.i { "Detaching engine" }
    _androidContext?.close()
    _androidContext = null
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
 * Creates a [PrismSurface] backed by an Android [SurfaceHolder] with a ready-to-use wgpu context.
 *
 * Internally calls wgpu4k's [androidContextRenderer] which creates a Vulkan-backed wgpu surface
 * from the Android native window.
 *
 * @param surfaceHolder The Android SurfaceHolder from a SurfaceView.
 * @param width Surface width in pixels.
 * @param height Surface height in pixels.
 */
suspend fun createPrismSurface(
  surfaceHolder: SurfaceHolder,
  width: Int,
  height: Int,
): PrismSurface {
  log.i { "Creating Android surface: ${width}x${height}" }
  val ctx = androidContextRenderer(surfaceHolder, width, height)
  log.i { "wgpu surface ready (Android/Vulkan)" }
  return PrismSurface(ctx).apply { resize(width, height) }
}
