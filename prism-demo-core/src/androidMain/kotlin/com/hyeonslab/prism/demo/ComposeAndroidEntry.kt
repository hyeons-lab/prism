package com.hyeonslab.prism.demo

import android.view.SurfaceHolder
import android.view.SurfaceView
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface

private val log = Logger.withTag("ComposeAndroid")

/**
 * Top-level composable for the Android Compose demo. Embeds a [SurfaceView] for wgpu/Vulkan
 * rendering and overlays Material3 controls via [ComposeDemoControls].
 *
 * Follows the same "bypass pattern" as JVM [ComposeMain] and iOS [ComposeIosEntry]: uses [DemoStore]
 * (not EngineStore) for MVI state, creates the GPU surface and [DemoScene] directly, and drives the
 * render loop via [withFrameNanos].
 */
@Composable
fun AndroidComposeDemoContent() {
  val store = remember { DemoStore() }
  val uiState by store.state.collectAsStateWithLifecycle()

  var scene by remember { mutableStateOf<DemoScene?>(null) }
  var surface by remember { mutableStateOf<PrismSurface?>(null) }
  var surfaceInfo by remember { mutableStateOf<SurfaceInfo?>(null) }
  var initError by remember { mutableStateOf<String?>(null) }
  // Synchronous flag to stop rendering immediately when the surface is destroyed.
  // Compose state updates are async (take effect on next recomposition), so the render loop's
  // captured `sc` reference would keep rendering into a destroyed surface without this.
  var renderingActive by remember { mutableStateOf(false) }

  // Clean up wgpu resources when the composable leaves the composition.
  DisposableEffect(Unit) {
    onDispose {
      log.i { "Disposing Compose Android demo" }
      renderingActive = false
      scene?.shutdown()
      scene = null
      surface?.detach()
      surface = null
    }
  }

  MaterialTheme(colorScheme = darkColorScheme()) {
    Box(modifier = Modifier.fillMaxSize()) {
      // SurfaceView embedded as a native Android view for GPU rendering.
      AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
          SurfaceView(context).also { sv ->
            sv.holder.addCallback(
              object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                  log.i { "surfaceCreated" }
                }

                override fun surfaceChanged(
                  holder: SurfaceHolder,
                  format: Int,
                  width: Int,
                  height: Int,
                ) {
                  log.i { "surfaceChanged: ${width}x${height}" }
                  // If scene already exists, just resize
                  val existingScene = scene
                  if (existingScene != null) {
                    existingScene.renderer.resize(width, height)
                    existingScene.updateAspectRatio(width, height)
                  } else {
                    surfaceInfo = SurfaceInfo(holder, width, height)
                  }
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                  log.i { "surfaceDestroyed" }
                  renderingActive = false
                  scene?.shutdown()
                  scene = null
                  surface?.detach()
                  surface = null
                  surfaceInfo = null
                }
              }
            )
          }
        },
      )

      // Initialize wgpu surface and demo scene when the SurfaceHolder becomes available.
      LaunchedEffect(surfaceInfo) {
        val info = surfaceInfo ?: return@LaunchedEffect
        if (scene != null) return@LaunchedEffect

        log.i { "Initializing wgpu for Compose Android demo: ${info.width}x${info.height}" }
        try {
          val s = createPrismSurface(info.holder, info.width, info.height)
          surface = s
          val wgpuCtx = checkNotNull(s.wgpuContext) { "wgpu context not available" }
          val sc =
            createDemoScene(
              wgpuCtx,
              width = info.width,
              height = info.height,
              initialColor = store.state.value.cubeColor,
            )
          scene = sc
          renderingActive = true
          log.i { "Compose Android demo initialized (${info.width}x${info.height})" }
        } catch (e: Exception) {
          log.e(e) { "Failed to initialize wgpu: ${e.message}" }
          initError = e.message ?: "Failed to initialize GPU"
        }
      }

      // Render loop driven by Compose's frame scheduling (withFrameNanos).
      LaunchedEffect(scene) {
        val sc = scene ?: return@LaunchedEffect

        val startTimeNs = System.nanoTime()
        var frameCount = 0L
        var lastFrameTimeNs = startTimeNs
        var rotationAngle = 0f

        while (true) {
          withFrameNanos {
            if (!renderingActive) return@withFrameNanos
            val nowNs = System.nanoTime()
            val deltaSec = (nowNs - lastFrameTimeNs) / 1_000_000_000f
            val totalSec = (nowNs - startTimeNs) / 1_000_000_000f
            lastFrameTimeNs = nowNs
            frameCount++

            val currentState = store.state.value

            // Update FPS (smoothed)
            if (deltaSec > 0f) {
              val smoothedFps = currentState.fps * 0.9f + (1f / deltaSec) * 0.1f
              store.dispatch(DemoIntent.UpdateFps(smoothedFps))
            }

            // Update rotation
            if (!currentState.isPaused) {
              rotationAngle += deltaSec * MathUtils.toRadians(currentState.rotationSpeed)
            }

            // Update material color when it changes
            val cubeMaterial = sc.world.getComponent<MaterialComponent>(sc.cubeEntity)
            if (cubeMaterial != null && cubeMaterial.material?.baseColor != currentState.cubeColor) {
              cubeMaterial.material = Material(baseColor = currentState.cubeColor)
            }

            // Rotation + ECS update via shared DemoScene method
            sc.tickWithAngle(
              deltaTime = deltaSec,
              elapsed = totalSec,
              frameCount = frameCount,
              angle = rotationAngle,
            )
          }
        }
      }

      // Show error overlay if initialization failed
      val error = initError
      if (error != null) {
        Text(
          text = error,
          color = androidx.compose.ui.graphics.Color.White,
          style = MaterialTheme.typography.bodyLarge,
          modifier = Modifier.align(Alignment.Center).padding(32.dp),
        )
      }

      // Overlay Compose UI controls
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

/** Holder for Android surface parameters needed to create a PrismSurface. */
private data class SurfaceInfo(val holder: SurfaceHolder, val width: Int, val height: Int)
