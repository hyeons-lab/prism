package com.hyeonslab.prism.compose

import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.PrismSurface
import com.hyeonslab.prism.widget.createPrismSurface

private val log = Logger.withTag("PrismView.Android")

/**
 * Android implementation of [PrismView]. Embeds a [SurfaceView] via [AndroidView] and initializes a
 * wgpu/Vulkan surface when the Android [SurfaceHolder] becomes available.
 *
 * Once the GPU surface is ready, a render loop driven by Compose's frame scheduling calls
 * [com.hyeonslab.prism.core.GameLoop.tick] each frame. All state changes are dispatched as
 * [EngineStateEvent]s through [EngineStore.dispatch].
 */
@Composable
actual fun PrismView(store: EngineStore, modifier: Modifier) {
  // Surface lifecycle state: null until surfaceChanged fires with valid dimensions.
  var surfaceInfo by remember { mutableStateOf<SurfaceInfo?>(null) }
  var prismSurface by remember { mutableStateOf<PrismSurface?>(null) }
  // Synchronous flag to stop rendering immediately when the surface is destroyed.
  // Compose state updates are async (take effect on next recomposition), so we need this
  // to prevent rendering into a destroyed surface in the current frame.
  var renderingActive by remember { mutableStateOf(false) }

  AndroidView(
    modifier = modifier,
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
              surfaceInfo = SurfaceInfo(holder, width, height)
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
              log.i { "surfaceDestroyed" }
              renderingActive = false
              surfaceInfo = null
            }
          }
        )
      }
    },
  )

  // Initialize or re-initialize wgpu surface when the SurfaceHolder changes.
  LaunchedEffect(surfaceInfo) {
    val info = surfaceInfo ?: return@LaunchedEffect

    // Detach the old surface before creating a new one to avoid GPU resource leaks
    // (e.g., on fold/unfold which triggers surfaceChanged with a new holder).
    prismSurface?.let { old ->
      log.i { "Detaching old PrismSurface before re-creation" }
      renderingActive = false
      store.engine.gameLoop.stop()
      old.detach()
    }

    log.i { "Creating PrismSurface: ${info.width}x${info.height}" }
    val surface = createPrismSurface(info.holder, info.width, info.height)
    prismSurface = surface
    store.dispatch(EngineStateEvent.SurfaceResized(info.width, info.height))
    renderingActive = true
    log.i { "PrismSurface ready" }
  }

  // Render loop — synchronized with display refresh rate via Compose's frame scheduling.
  // Re-launched whenever prismSurface changes (new surface after fold/unfold).
  LaunchedEffect(prismSurface) {
    val surface = prismSurface ?: return@LaunchedEffect
    if (surface.wgpuContext == null) return@LaunchedEffect
    if (!renderingActive) return@LaunchedEffect
    log.i { "Starting render loop" }

    val engine = store.engine
    engine.gameLoop.startExternal()

    while (true) {
      withFrameNanos {
        if (!renderingActive) return@withFrameNanos

        engine.gameLoop.tick()

        val time = engine.time
        val currentFps = store.state.value.fps
        val smoothedFps =
          if (time.deltaTime > 0f) currentFps * 0.9f + (1f / time.deltaTime) * 0.1f
          else currentFps
        store.dispatch(EngineStateEvent.FrameTick(time, smoothedFps))
      }
    }
  }

  DisposableEffect(store) {
    onDispose {
      log.i { "PrismView disposing" }
      renderingActive = false
      store.engine.gameLoop.stop()
      prismSurface?.detach()
      prismSurface = null
    }
  }
}

/** Holder for Android surface parameters needed to create a PrismSurface. */
private data class SurfaceInfo(val holder: SurfaceHolder, val width: Int, val height: Int)
