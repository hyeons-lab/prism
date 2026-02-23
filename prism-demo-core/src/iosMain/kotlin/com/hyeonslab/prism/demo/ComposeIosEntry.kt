@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private val log = Logger.withTag("ComposeIOS")

/** Entry point called from Swift to create the Compose-based demo view controller. */
fun composeDemoViewController(): UIViewController = ComposeUIViewController {
  IosComposeDemoContent()
}

@Composable
private fun IosComposeDemoContent() {
  val store = sharedDemoStore
  val uiState by store.state.collectAsStateWithLifecycle()

  // Hold scene + surface + delegate so they survive recomposition but can be cleaned up.
  // MTKView.delegate is a WEAK reference — without a strong ref here, the delegate gets GC'd.
  var scene by remember { mutableStateOf<DemoScene?>(null) }
  var surface by remember { mutableStateOf<PrismSurface?>(null) }
  var mtkView by remember { mutableStateOf<MTKView?>(null) }
  var renderDelegate by remember { mutableStateOf<MTKViewDelegateProtocol?>(null) }
  var initError by remember { mutableStateOf<String?>(null) }

  // Clean up wgpu resources when the composable leaves the composition.
  // Null the delegate first to stop render callbacks before shutting down.
  DisposableEffect(Unit) {
    onDispose {
      log.i { "Disposing Compose iOS demo" }
      mtkView?.delegate = null
      renderDelegate = null
      scene?.shutdown()
      surface?.detach()
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = Modifier.fillMaxSize()) {
      // MTKView embedded as a native UIKit view.
      // interactive = false: this is a display-only Metal surface; all user interaction
      // (sliders, buttons) is handled by the Compose overlay, not the MTKView.
      @Suppress("DEPRECATION")
      UIKitView(
        factory = {
          val device = platform.Metal.MTLCreateSystemDefaultDevice()
          if (device == null) {
            log.e { "Metal is not supported on this device" }
            initError = "Metal is not supported on this device"
          }
          val view =
            MTKView().apply {
              this.device = device
              this.colorPixelFormat = platform.Metal.MTLPixelFormatBGRA8Unorm
              this.depthStencilPixelFormat = platform.Metal.MTLPixelFormatDepth32Float
              this.preferredFramesPerSecond = 60
            }
          mtkView = view
          view
        },
        modifier = Modifier.fillMaxSize(),
        interactive = false,
      )

      // Initialize wgpu once the MTKView is available
      LaunchedEffect(mtkView) {
        val view = mtkView ?: return@LaunchedEffect
        if (initError != null) return@LaunchedEffect

        log.i { "Initializing wgpu for Compose iOS demo" }

        var width = view.drawableSize.useContents { width.toInt() }
        var height = view.drawableSize.useContents { height.toInt() }
        if (width <= 0 || height <= 0) {
          log.w { "drawableSize not ready (${width}x${height}), using defaults" }
          width = IOS_DEFAULT_WIDTH
          height = IOS_DEFAULT_HEIGHT
        }

        try {
          val s = createPrismSurface(view, width, height)
          surface = s
          val wgpuCtx = checkNotNull(s.wgpuContext) { "wgpu context not available" }
          val glbBytes =
            checkNotNull(loadBundleAssetBytes("DamagedHelmet.glb")) {
              "DamagedHelmet.glb not found in app bundle"
            }
          val sc = createGltfDemoScene(wgpuCtx, width = width, height = height, glbData = glbBytes)
          scene = sc

          val delegate =
            ComposeRenderDelegate(sc, store) { w, h ->
              sc.renderer.resize(w, h)
              sc.updateAspectRatio(w, h)
            }
          renderDelegate = delegate
          view.delegate = delegate
          log.i { "Compose iOS demo initialized (${width}x${height})" }
        } catch (e: Exception) {
          log.e(e) { "Failed to initialize wgpu: ${e.message}" }
          initError = e.message ?: "Failed to initialize GPU"
        }
      }

      // Show error overlay if initialization failed
      val error = initError
      if (error != null) {
        Text(
          text = error,
          color = ComposeColor.White,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.align(Alignment.Center).padding(32.dp),
        )
      }

      // Overlay Compose UI controls — safeDrawing insets avoid the Dynamic Island / notch
      ComposeDemoControls(
        state = uiState,
        onIntent = store::dispatch,
        modifier =
          Modifier.align(Alignment.TopEnd)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(8.dp),
      )
    }
  }
}

/**
 * MTKView render delegate for the Compose iOS demo. Delegates per-frame update logic to
 * [tickDemoFrame] which is shared with the Native tab's [DemoRenderDelegate].
 */
@OptIn(BetaInteropApi::class)
private class ComposeRenderDelegate(
  private val scene: DemoScene,
  private val store: DemoStore,
  private val onResize: (Int, Int) -> Unit,
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
      log.i { "Compose drawable size changed: ${w}x${h}" }
      onResize(w, h)
    }
  }
}
