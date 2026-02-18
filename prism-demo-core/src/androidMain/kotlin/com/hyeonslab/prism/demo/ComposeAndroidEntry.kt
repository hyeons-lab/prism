package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.compose.PrismOverlay
import com.hyeonslab.prism.compose.rememberEngineStore
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Material

private val log = Logger.withTag("ComposeAndroid")

/**
 * Top-level composable for the Android Compose demo. Uses [PrismOverlay] to embed a GPU rendering
 * surface and overlays Material3 controls via [ComposeDemoControls].
 *
 * Scene setup happens in [onSurfaceReady] — the [DemoScene] is created with the [EngineStore]'s
 * engine, and per-frame logic (rotation, material updates) is wired into the engine's
 * [GameLoop.onRender] callback. PrismView's render loop drives everything via [GameLoop.tick].
 */
@Composable
fun AndroidComposeDemoContent() {
  val engineStore =
    rememberEngineStore(
      config = EngineConfig(appName = "Prism Android Demo", targetFps = 60, enableDebug = true)
    )
  val demoStore = remember { DemoStore() }
  val uiState by demoStore.state.collectAsStateWithLifecycle()
  var scene by remember { mutableStateOf<DemoScene?>(null) }
  // rotationAngle is mutated inside gameLoop.onRender which runs on the main thread
  // (PrismView drives the render loop via Compose's withFrameNanos).
  var rotationAngle by remember { mutableFloatStateOf(0f) }

  // Dispose scene resources when the composable leaves composition.
  // EngineStore handles engine shutdown separately via its own DisposableEffect.
  DisposableEffect(Unit) {
    onDispose {
      log.i { "Disposing demo scene" }
      scene?.dispose()
      scene = null
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = Modifier.fillMaxSize()) {
      PrismOverlay(
        store = engineStore,
        modifier = Modifier.fillMaxSize(),
        onSurfaceReady = { ctx, w, h ->
          log.i { "Surface ready \u2014 initializing demo scene (${w}x${h})" }

          // Clean up any existing scene (e.g., surface re-created after fold/unfold).
          scene?.dispose()

          @Suppress("TooGenericExceptionCaught")
          try {
            val sc =
              createDemoScene(
                engine = engineStore.engine,
                wgpuContext = ctx,
                width = w,
                height = h,
                initialColor = demoStore.state.value.cubeColor,
              )
            scene = sc

            // Wrap onRender to add demo-specific per-frame logic before the ECS update.
            val baseOnRender = engineStore.engine.gameLoop.onRender
            engineStore.engine.gameLoop.onRender = { time ->
              val currentState = demoStore.state.value

              // Update rotation
              if (!currentState.isPaused) {
                rotationAngle += time.deltaTime * MathUtils.toRadians(currentState.rotationSpeed)
              }
              val cubeTransform = sc.world.getComponent<TransformComponent>(sc.cubeEntity)
              cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, rotationAngle)

              // Update material color when it changes
              val cubeMaterial = sc.world.getComponent<MaterialComponent>(sc.cubeEntity)
              if (
                cubeMaterial != null && cubeMaterial.material?.baseColor != currentState.cubeColor
              ) {
                cubeMaterial.material = Material(baseColor = currentState.cubeColor)
              }

              // Run ECS update (triggers RenderSystem)
              baseOnRender?.invoke(time)

              // Update DemoStore FPS for the controls UI.
              // (EngineStore also tracks FPS via FrameTick, but ComposeDemoControls
              // reads from DemoStore which is the demo-specific UI state.)
              if (time.deltaTime > 0f) {
                val smoothedFps = currentState.fps * 0.9f + (1f / time.deltaTime) * 0.1f
                demoStore.dispatch(DemoIntent.UpdateFps(smoothedFps))
              }
            }
          } catch (e: Exception) {
            log.e(e) { "Failed to initialize demo scene: ${e.message}" }
          }
        },
        onSurfaceResized = { w, h ->
          log.i { "Surface resized: ${w}x${h}" }
          scene?.renderer?.resize(w, h)
        },
      ) {
        ComposeDemoControls(
          state = uiState,
          onIntent = demoStore::dispatch,
          modifier =
            Modifier.align(Alignment.TopEnd)
              .windowInsetsPadding(WindowInsets.safeDrawing)
              .padding(8.dp),
        )
      }
    }
  }
}
