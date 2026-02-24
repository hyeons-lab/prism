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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.compose.EngineStateEvent
import com.hyeonslab.prism.compose.PrismView
import com.hyeonslab.prism.compose.rememberEngineStore
import com.hyeonslab.prism.core.EngineConfig
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.cancel
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIViewController

private val log = Logger.withTag("ComposeIOS")

/** Entry point called from Swift to create the Compose-based demo view controller. */
fun composeDemoViewController(): UIViewController = ComposeUIViewController {
  IosComposeDemoContent()
}

@Composable
private fun IosComposeDemoContent() {
  val engineStore = rememberEngineStore(EngineConfig(appName = "Prism iOS Compose"))
  // Scope for progressive glTF texture uploads. Tied to this composable's composition lifetime
  // and automatically cancelled when it leaves; also explicitly cancelled in gameLoop.onStop
  // (before scene.shutdown()) so uploads stop before the renderer frees GPU resources.
  val backgroundScope = rememberCoroutineScope()
  val store = sharedDemoStore
  val uiState by store.state.collectAsStateWithLifecycle()
  val engineState by engineStore.state.collectAsStateWithLifecycle()

  var scene by remember { mutableStateOf<DemoScene?>(null) }
  var surfaceCtx by remember { mutableStateOf<WGPUContext?>(null) }
  var surfaceWidth by remember { mutableStateOf(0) }
  var surfaceHeight by remember { mutableStateOf(0) }
  var initError by remember { mutableStateOf<String?>(null) }

  // Update the scene's camera aspect ratio whenever the engine-reported surface dimensions change.
  // This corrects the initial aspect ratio when PrismView fell back to the 800×600 default before
  // the MTKView completed its first layout pass.
  LaunchedEffect(engineState.surfaceWidth, engineState.surfaceHeight, scene) {
    val sc = scene ?: return@LaunchedEffect
    sc.updateAspectRatio(engineState.surfaceWidth, engineState.surfaceHeight)
  }

  // Once the surface is ready (surfaceCtx becomes non-null), create the glTF demo scene and
  // wire per-frame logic (material overrides, scene tick, FPS dispatch) into the game loop.
  LaunchedEffect(surfaceCtx) {
    val ctx = surfaceCtx ?: return@LaunchedEffect
    val w = surfaceWidth
    val h = surfaceHeight

    log.i { "Initializing glTF demo scene (${w}x${h})" }
    try {
      val glbBytes =
        checkNotNull(loadBundleAssetBytes("DamagedHelmet.glb")) {
          "DamagedHelmet.glb not found in app bundle"
        }
      // surfacePreConfigured defaults to false: WgpuRenderer will configure the wgpu surface on
      // first use. createPrismSurface (called by PrismView) intentionally does not pre-configure
      // the surface, so this is the correct default for the Compose path.
      val sc =
        createGltfDemoScene(
          ctx,
          width = w,
          height = h,
          glbData = glbBytes,
          progressiveScope = backgroundScope,
        )

      // Register cleanup to run inside PrismView's gameLoop.stop(), which fires BEFORE the wgpu
      // surface is detached. This ensures GPU resources are freed while the context is still
      // valid, avoiding use-after-free if wgpu4k ever closes resources eagerly on context close.
      engineStore.engine.gameLoop.onStop = {
        engineStore.engine.gameLoop.onRender = null
        backgroundScope.cancel() // stop progressive texture uploads before renderer is freed
        sc.shutdown()
      }

      scene = sc
      engineStore.dispatch(EngineStateEvent.SurfaceResized(w, h))

      // Wire per-frame logic into the game loop. This callback is invoked by
      // PrismView.ios.kt's MTKViewDelegate on the display-link thread.
      engineStore.engine.gameLoop.onRender = { time ->
        val currentState = store.state.value
        if (!currentState.isPaused) {
          sc.setMaterialOverride(currentState.metallic, currentState.roughness)
          sc.setEnvIntensity(currentState.envIntensity)
        }
        var elapsed = 0f
        SharedDemoTime.tick(isPaused = currentState.isPaused) { e -> elapsed = e }
        sc.tick(
          deltaTime = if (currentState.isPaused) 0f else time.deltaTime,
          elapsed = elapsed,
          frameCount = time.frameCount,
        )
        if (time.deltaTime > 0f) {
          val smoothedFps = currentState.fps * 0.9f + (1f / time.deltaTime) * 0.1f
          NSOperationQueue.mainQueue.addOperationWithBlock {
            store.dispatch(DemoIntent.UpdateFps(smoothedFps))
          }
        }
      }
      log.i { "Compose iOS demo initialized" }
    } catch (e: Exception) {
      log.e(e) { "Failed to initialize demo scene: ${e.message}" }
      initError = e.message ?: "Failed to initialize GPU"
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = Modifier.fillMaxSize()) {
      PrismView(
        store = engineStore,
        modifier = Modifier.fillMaxSize(),
        onSurfaceReady = { ctx, w, h ->
          surfaceCtx = ctx
          surfaceWidth = w
          surfaceHeight = h
        },
      )

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
