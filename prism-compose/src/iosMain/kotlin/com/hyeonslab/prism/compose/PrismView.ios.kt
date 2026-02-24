@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.Foundation.NSOperationQueue
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.darwin.NSObject

private val log = Logger.withTag("PrismView.iOS")

// Default fallback dimensions when MTKView's drawableSize is not yet computed.
private const val IOS_DEFAULT_WIDTH = 800
private const val IOS_DEFAULT_HEIGHT = 600

/**
 * iOS implementation of [PrismView]. Embeds an [MTKView] inside the Compose layout via [UIKitView]
 * and drives the engine render loop through [MTKViewDelegateProtocol].
 *
 * The MTKView display link fires at the target frame rate, calling
 * [com.hyeonslab.prism.core.GameLoop.tick] each frame. FPS is smoothed and dispatched to [store] on
 * the main queue. Surface resize notifications are forwarded as [EngineStateEvent.SurfaceResized]
 * events.
 *
 * [onSurfaceReady] is invoked once after the wgpu surface is successfully created. Use it to
 * initialize scene resources (e.g. upload meshes, configure a renderer) that require a live
 * [WGPUContext].
 */
@Suppress("DEPRECATION") // UIKitView deprecated in CMP ≥ 1.7 in favour of newer interop APIs
@Composable
actual fun PrismView(
  store: EngineStore,
  modifier: Modifier,
  onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)?,
) {
  var mtkView by remember { mutableStateOf<MTKView?>(null) }
  var prismSurface by remember { mutableStateOf<PrismSurface?>(null) }
  // MTKView.delegate is a WEAK reference — store a strong ref here to prevent GC.
  var renderDelegate by remember { mutableStateOf<MTKViewDelegateProtocol?>(null) }

  UIKitView(factory = { MTKView().also { mtkView = it } }, modifier = modifier, interactive = false)

  LaunchedEffect(mtkView) {
    val view = mtkView ?: return@LaunchedEffect

    var width = view.drawableSize.useContents { width.toInt() }
    var height = view.drawableSize.useContents { height.toInt() }
    if (width <= 0 || height <= 0) {
      log.w { "drawableSize not ready (${width}x${height}), using defaults" }
      width = IOS_DEFAULT_WIDTH
      height = IOS_DEFAULT_HEIGHT
    }

    log.i { "Creating wgpu surface from MTKView: ${width}x${height}" }
    try {
      val s = createPrismSurface(view, width, height)
      prismSurface = s
      store.dispatch(EngineStateEvent.SurfaceResized(width, height))

      val wgpuCtx = checkNotNull(s.wgpuContext) { "wgpu context not available after surface init" }
      onSurfaceReady?.invoke(wgpuCtx, width, height)

      val engine = store.engine
      engine.gameLoop.startExternal()

      val delegate =
        object : NSObject(), MTKViewDelegateProtocol {
          override fun drawInMTKView(view: MTKView) {
            engine.gameLoop.tick()

            val time = engine.time
            val currentFps = store.state.value.fps
            val smoothedFps =
              if (time.deltaTime > 0f) currentFps * 0.9f + (1f / time.deltaTime) * 0.1f
              else currentFps
            NSOperationQueue.mainQueue.addOperationWithBlock {
              store.dispatch(EngineStateEvent.FrameTick(time, smoothedFps))
            }
          }

          override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {
            drawableSizeWillChange.useContents {
              val w = this.width.toInt()
              val h = this.height.toInt()
              if (w <= 0 || h <= 0) return
              log.i { "Drawable size changed: ${w}x${h}" }
              prismSurface?.resize(w, h)
              NSOperationQueue.mainQueue.addOperationWithBlock {
                store.dispatch(EngineStateEvent.SurfaceResized(w, h))
              }
            }
          }
        }

      renderDelegate = delegate
      view.delegate = delegate
      log.i { "PrismView iOS render delegate installed (${width}x${height})" }
    } catch (e: Exception) {
      log.e(e) { "Failed to initialize wgpu surface: ${e.message}" }
    }
  }

  DisposableEffect(store) {
    onDispose {
      log.i { "PrismView iOS disposing" }
      mtkView?.delegate = null
      renderDelegate = null
      store.engine.gameLoop.stop()
      prismSurface?.detach()
      prismSurface = null
    }
  }
}
