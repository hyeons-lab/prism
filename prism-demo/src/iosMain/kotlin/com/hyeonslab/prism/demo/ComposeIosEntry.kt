@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Material
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.Foundation.NSOperationQueue
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
  val store = remember { DemoStore() }
  val uiState by store.state.collectAsStateWithLifecycle()

  // Hold scene + context so they survive recomposition but can be cleaned up
  var scene by remember { mutableStateOf<DemoScene?>(null) }
  var iosContext by remember { mutableStateOf<IosContext?>(null) }
  var mtkView by remember { mutableStateOf<MTKView?>(null) }
  var initError by remember { mutableStateOf<String?>(null) }

  // Clean up wgpu resources when the composable leaves the composition.
  // Null the delegate first to stop render callbacks before shutting down.
  DisposableEffect(Unit) {
    onDispose {
      log.i { "Disposing Compose iOS demo" }
      mtkView?.delegate = null
      scene?.shutdown()
      iosContext?.close()
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
          val ctx = iosContextRenderer(view, width, height)
          iosContext = ctx
          val s =
            createDemoScene(
              ctx.wgpuContext,
              width = width,
              height = height,
              initialColor = store.state.value.cubeColor,
            )
          scene = s

          view.delegate =
            ComposeRenderDelegate(s, store) { w, h ->
              s.renderer.resize(w, h)
              s.updateAspectRatio(w, h)
            }
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

      // Overlay Compose UI controls
      ComposeDemoControls(
        state = uiState,
        onIntent = store::dispatch,
        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
      )
    }
  }
}

/**
 * MTKView render delegate for the Compose iOS demo. Reads [DemoStore] state each frame to support
 * user-controllable rotation speed, pause, and material color — mirroring ComposeMain.kt's render
 * loop.
 *
 * FPS updates are dispatched on the main queue to avoid cross-thread Compose state mutations from
 * the Metal display-link thread.
 */
@OptIn(BetaInteropApi::class)
private class ComposeRenderDelegate(
  private val scene: DemoScene,
  private val store: DemoStore,
  private val onResize: (Int, Int) -> Unit,
) : NSObject(), MTKViewDelegateProtocol {

  private val startTime = CACurrentMediaTime()
  private var lastFrameTime = startTime
  private var frameCount = 0L
  private var rotationAngle = 0f

  override fun drawInMTKView(view: MTKView) {
    val now = CACurrentMediaTime()
    val deltaTime = (now - lastFrameTime).toFloat()
    lastFrameTime = now
    val elapsed = (now - startTime).toFloat()
    frameCount++

    val currentState = store.state.value

    // Update FPS (smoothed) — dispatch on main queue for thread-safe Compose state updates
    if (deltaTime > 0f) {
      val smoothedFps = currentState.fps * 0.9f + (1f / deltaTime) * 0.1f
      NSOperationQueue.mainQueue.addOperationWithBlock {
        store.dispatch(DemoIntent.UpdateFps(smoothedFps))
      }
    }

    // Update rotation (user-controllable speed + pause)
    if (!currentState.isPaused) {
      rotationAngle += deltaTime * MathUtils.toRadians(currentState.rotationSpeed)
    }
    val cubeTransform = scene.world.getComponent<TransformComponent>(scene.cubeEntity)
    cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, rotationAngle)

    // Update material color
    val cubeMaterial = scene.world.getComponent<MaterialComponent>(scene.cubeEntity)
    cubeMaterial?.material = Material(baseColor = currentState.cubeColor)

    // Run ECS update (triggers RenderSystem)
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    scene.world.update(time)
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
