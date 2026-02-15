@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.NSObject

private val log = Logger.withTag("PrismIOS")

/**
 * Handle returned by [configureDemo] that manages the lifecycle of the demo scene and GPU
 * resources. Swift must call [shutdown] when the view controller is torn down (e.g. in `deinit`) to
 * release the WGPU instance, adapter, device, and surface.
 */
class IosDemoHandle(private val iosContext: IosContext, private val scene: DemoScene) {
  fun shutdown() {
    log.i { "Shutting down iOS demo..." }
    scene.shutdown()
    iosContext.close()
    log.i { "iOS demo shut down" }
  }
}

/**
 * Configures and returns an [IosDemoHandle] for the given [MTKView]. This is the main entry point
 * called from Swift via framework interop.
 *
 * After calling this, the [MTKView]'s delegate is set to a [DemoRenderDelegate] that drives the
 * render loop on each display-link callback.
 *
 * If the view's [drawableSize] is not yet computed (zero), safe defaults are used. The
 * [DemoRenderDelegate] will update the aspect ratio when [mtkView(drawableSizeWillChange:)] fires
 * with the real dimensions after layout.
 */
suspend fun configureDemo(view: MTKView): IosDemoHandle {
  var width = view.drawableSize.useContents { width.toInt() }
  var height = view.drawableSize.useContents { height.toInt() }
  if (width <= 0 || height <= 0) {
    log.w { "drawableSize not ready (${width}x${height}), using defaults" }
    width = IOS_DEFAULT_WIDTH
    height = IOS_DEFAULT_HEIGHT
  }
  log.i { "Configuring demo: ${width}x${height}" }

  val iosContext = iosContextRenderer(view, width, height)
  val scene = createDemoScene(iosContext.wgpuContext, width = width, height = height)

  view.delegate = DemoRenderDelegate(scene)
  log.i { "iOS demo configured — render delegate installed" }
  return IosDemoHandle(iosContext, scene)
}

/**
 * MTKView render delegate that drives the Prism demo render loop. Each [drawInMTKView] call updates
 * the cube rotation and ticks the ECS world.
 */
@OptIn(BetaInteropApi::class)
class DemoRenderDelegate(private val scene: DemoScene) : NSObject(), MTKViewDelegateProtocol {

  private val startTime = CACurrentMediaTime()
  private var lastFrameTime = startTime
  private var frameCount = 0L

  override fun drawInMTKView(view: MTKView) {
    val now = CACurrentMediaTime()
    val deltaTime = (now - lastFrameTime).toFloat()
    lastFrameTime = now
    val elapsed = (now - startTime).toFloat()

    frameCount++
    scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
  }

  override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {
    drawableSizeWillChange.useContents {
      val w = width.toInt()
      val h = height.toInt()
      log.i { "Drawable size changed: ${w}x${h}" }
      scene.renderer.resize(w, h)
      scene.updateAspectRatio(w, h)
    }
  }
}
