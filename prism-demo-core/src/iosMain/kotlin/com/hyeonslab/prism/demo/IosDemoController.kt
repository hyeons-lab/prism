@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.NSObject

private val log = Logger.withTag("PrismIOS")

/**
 * Handle returned by [configureDemo] / [configureDemoWithGltf] that manages the lifecycle of the
 * demo scene and GPU resources. Swift must call [shutdown] when the view controller is torn down
 * (e.g. in `deinit`) to release the WGPU instance, adapter, device, and surface.
 *
 * [renderDelegate] is stored here to prevent garbage collection — MTKView.delegate is a WEAK
 * reference in UIKit, so without a strong reference the K/N GC will collect the delegate.
 */
class IosDemoHandle(
  private val surface: PrismSurface,
  private val scene: DemoScene,
  @Suppress("unused") private val renderDelegate: MTKViewDelegateProtocol,
) {
  fun orbitBy(dx: Float, dy: Float) {
    scene.orbitBy(dx, dy)
  }

  fun shutdown() {
    log.i { "Shutting down iOS demo..." }
    scene.shutdown()
    surface.detach()
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
suspend fun configureDemo(view: MTKView): IosDemoHandle = configureDemo(view, sharedDemoStore)

/**
 * Configures the demo with an explicit [DemoStore]. Use this overload when the caller owns the
 * store instance (e.g. Flutter plugin) rather than sharing [sharedDemoStore].
 */
suspend fun configureDemo(view: MTKView, store: DemoStore): IosDemoHandle {
  var width = view.drawableSize.useContents { width.toInt() }
  var height = view.drawableSize.useContents { height.toInt() }
  if (width <= 0 || height <= 0) {
    log.w { "drawableSize not ready (${width}x${height}), using defaults" }
    width = IOS_DEFAULT_WIDTH
    height = IOS_DEFAULT_HEIGHT
  }
  log.i { "Configuring demo: ${width}x${height}" }

  val surface = createPrismSurface(view, width, height)
  val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }
  val scene = createDemoScene(wgpuContext, width = width, height = height)

  val delegate = DemoRenderDelegate(scene, store)
  view.delegate = delegate
  log.i { "iOS demo configured — render delegate installed" }
  return IosDemoHandle(surface, scene, delegate)
}

/**
 * Configures a glTF demo scene for the Flutter plugin. Tries to load DamagedHelmet.glb from the app
 * bundle (Flutter assets are stored under `flutter_assets/`); falls back to the PBR sphere-grid
 * demo if loading fails.
 */
suspend fun configureDemoWithGltf(view: MTKView, store: DemoStore): IosDemoHandle {
  var width = view.drawableSize.useContents { width.toInt() }
  var height = view.drawableSize.useContents { height.toInt() }
  if (width <= 0 || height <= 0) {
    log.w { "drawableSize not ready (${width}x${height}), using defaults" }
    width = IOS_DEFAULT_WIDTH
    height = IOS_DEFAULT_HEIGHT
  }
  log.i { "Configuring glTF demo: ${width}x${height}" }

  val surface = createPrismSurface(view, width, height)
  val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }

  // Load the GLB from the bundle — Flutter assets land at flutter_assets/<path> in the bundle.
  val glbData = loadBundleAssetBytes("flutter_assets/assets/DamagedHelmet.glb")
  val scene =
    if (glbData != null) {
      log.i { "Loaded DamagedHelmet.glb (${glbData.size} bytes)" }
      createGltfDemoScene(wgpuContext, width = width, height = height, glbData = glbData)
    } else {
      log.w { "DamagedHelmet.glb not found in bundle — falling back to sphere-grid demo" }
      createDemoScene(wgpuContext, width = width, height = height)
    }

  val delegate = DemoRenderDelegate(scene, store)
  view.delegate = delegate
  log.i { "iOS glTF demo configured — render delegate installed" }
  return IosDemoHandle(surface, scene, delegate)
}

/**
 * Loads a file from the app bundle by its relative path (e.g. `"flutter_assets/assets/Foo.glb"`).
 * Returns null if the file cannot be found or read.
 */
private fun loadBundleAssetBytes(relativePath: String): ByteArray? {
  val resourcePath = NSBundle.mainBundle.resourcePath ?: return null
  val fullPath = "$resourcePath/$relativePath"
  val nsData = NSFileManager.defaultManager.contentsAtPath(fullPath) ?: return null
  val length = nsData.length.toInt()
  if (length == 0) return null
  return nsData.bytes?.reinterpret<ByteVar>()?.readBytes(length)
}

/**
 * MTKView render delegate that drives the Prism demo render loop. Each [drawInMTKView] call updates
 * the cube rotation and ticks the ECS world.
 */
@OptIn(BetaInteropApi::class)
class DemoRenderDelegate(
  private val scene: DemoScene,
  private val store: DemoStore = sharedDemoStore,
) : NSObject(), MTKViewDelegateProtocol {

  private var lastFrameTime = CACurrentMediaTime()
  private var frameCount = 0L

  override fun drawInMTKView(view: MTKView) {
    val now = CACurrentMediaTime()
    val deltaTime = (now - lastFrameTime).toFloat()
    lastFrameTime = now
    frameCount++
    tickDemoFrame(scene, store, deltaTime, frameCount)
  }

  override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {
    drawableSizeWillChange.useContents {
      val w = width.toInt()
      val h = height.toInt()
      if (w <= 0 || h <= 0) return
      log.i { "Drawable size changed: ${w}x${h}" }
      scene.renderer.resize(w, h)
      scene.updateAspectRatio(w, h)
    }
  }
}
