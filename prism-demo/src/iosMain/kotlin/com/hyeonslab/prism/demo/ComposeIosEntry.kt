@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.UIViewController
import platform.darwin.NSObject

private val log = Logger.withTag("ComposeIOS")

private const val DEFAULT_WIDTH = 800
private const val DEFAULT_HEIGHT = 600

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

  // Clean up wgpu resources when the composable leaves the composition
  DisposableEffect(Unit) {
    onDispose {
      log.i { "Disposing Compose iOS demo" }
      scene?.shutdown()
      iosContext?.close()
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = Modifier.fillMaxSize()) {
      // MTKView embedded as a native UIKit view
      @Suppress("DEPRECATION")
      UIKitView(
        factory = {
          val device = platform.Metal.MTLCreateSystemDefaultDevice()
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
        log.i { "Initializing wgpu for Compose iOS demo" }

        var width = view.drawableSize.useContents { width.toInt() }
        var height = view.drawableSize.useContents { height.toInt() }
        if (width <= 0 || height <= 0) {
          log.w { "drawableSize not ready (${width}x${height}), using defaults" }
          width = DEFAULT_WIDTH
          height = DEFAULT_HEIGHT
        }

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

        view.delegate = ComposeRenderDelegate(s, store) { w, h -> s.updateAspectRatio(w, h) }
        log.i { "Compose iOS demo initialized (${width}x${height})" }
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

    // Update FPS (smoothed)
    if (deltaTime > 0f) {
      val smoothedFps = currentState.fps * 0.9f + (1f / deltaTime) * 0.1f
      store.dispatch(DemoIntent.UpdateFps(smoothedFps))
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
      log.i { "Compose drawable size changed: ${w}x${h}" }
      onResize(w, h)
    }
  }
}
