package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
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
import ffi.LibraryLoader
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities

private val log = Logger.withTag("ComposeMain")

/**
 * Compose Desktop demo entry point using JFrame + ComposePanel.
 *
 * Uses JFrame (Swing) as the top-level container instead of Compose Desktop's `application {}`
 * function. This avoids the `-XstartOnFirstThread` deadlock on macOS: Compose's `application {}`
 * conflicts with the flag, but `ComposePanel` (a standard Swing component) works fine with it.
 * `-XstartOnFirstThread` is required for `Native.getComponentPointer()` to return a valid NSView
 * pointer on macOS.
 *
 * The 3D scene renders fullscreen with Compose controls overlaid via [PrismOverlay].
 */
fun main() {
  log.i { "Starting Prism Compose Demo..." }
  LibraryLoader.load()

  SwingUtilities.invokeLater { createAndShowUi() }
}

private fun createAndShowUi() {
  val frame = JFrame("Prism 3D Engine \u2014 Compose Demo")
  frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
  frame.size = Dimension(1080, 700)

  // Track the scene at frame scope so windowClosing can dispose it.
  var activeScene: DemoScene? = null

  frame.addWindowListener(
    object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent?) {
        log.i { "Window closing \u2014 disposing scene" }
        activeScene?.dispose()
        activeScene = null
        // frame.dispose() synchronously removes the ComposePanel, which triggers
        // Compose's DisposableEffect.onDispose chain (engine shutdown, gameLoop.stop).
        // The scene is already disposed above, so the engine cleanup is safe.
        frame.dispose()
      }
    }
  )

  val composePanel = ComposePanel()
  frame.add(composePanel)

  composePanel.setContent {
    val engineStore =
      rememberEngineStore(
        config = EngineConfig(appName = "Prism Compose Demo", targetFps = 60, enableDebug = true)
      )
    val demoStore = remember { DemoStore() }
    val uiState by demoStore.state.collectAsStateWithLifecycle()
    var scene by remember { mutableStateOf<DemoScene?>(null) }
    // Plain array ref — not Compose state. Only accessed inside onRender (EDT)
    // so no synchronization needed, and avoids unnecessary snapshot writes at 60fps.
    val rotationAngle = remember { floatArrayOf(0f) }

    // Dispose scene resources when the composable leaves composition.
    // EngineStore handles engine shutdown separately via its own DisposableEffect.
    DisposableEffect(Unit) {
      onDispose {
        log.i { "Disposing demo scene" }
        scene?.dispose()
        scene = null
        activeScene = null
      }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
      Box(modifier = Modifier.fillMaxSize()) {
        PrismOverlay(
          store = engineStore,
          modifier = Modifier.fillMaxSize(),
          onSurfaceReady = { ctx, w, h ->
            log.i { "Surface ready \u2014 initializing demo scene (${w}x${h})" }

            // Clean up any existing scene (e.g., panel resized triggering re-creation).
            scene?.dispose()
            // Always clear onRender before re-wiring to prevent lambda chain accumulation
            // if dispose() didn't run (e.g., scene was null but onRender was set).
            engineStore.engine.gameLoop.onRender = null

            @Suppress("TooGenericExceptionCaught")
            try {
              val sc =
                createDemoScene(
                  engine = engineStore.engine,
                  wgpuContext = ctx,
                  width = w,
                  height = h,
                  surfacePreConfigured = true,
                  initialColor = demoStore.state.value.cubeColor,
                )
              scene = sc
              activeScene = sc

              // Wrap onRender to add demo-specific per-frame logic before the ECS update.
              val baseOnRender = engineStore.engine.gameLoop.onRender
              engineStore.engine.gameLoop.onRender = { time ->
                val currentState = demoStore.state.value

                // Update rotation
                if (!currentState.isPaused) {
                  rotationAngle[0] +=
                    time.deltaTime * MathUtils.toRadians(currentState.rotationSpeed)
                }
                val cubeTransform = sc.world.getComponent<TransformComponent>(sc.cubeEntity)
                cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, rotationAngle[0])

                // Update material color when it changes
                val cubeMaterial = sc.world.getComponent<MaterialComponent>(sc.cubeEntity)
                if (
                  cubeMaterial != null && cubeMaterial.material?.baseColor != currentState.cubeColor
                ) {
                  cubeMaterial.material = Material(baseColor = currentState.cubeColor)
                }

                // Run ECS update (triggers RenderSystem)
                baseOnRender?.invoke(time)

                // Forward EngineStore's smoothed FPS to DemoStore for the controls UI.
                val engineFps = engineStore.state.value.fps
                if (engineFps > 0f) {
                  demoStore.dispatch(DemoIntent.UpdateFps(engineFps))
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
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
          )
        }
      }
    }
  }

  frame.setLocationRelativeTo(null)
  frame.isVisible = true
}
