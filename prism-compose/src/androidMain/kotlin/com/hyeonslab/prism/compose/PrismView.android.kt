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
  var isReady by remember { mutableStateOf(false) }

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
              isReady = false
              surfaceInfo = null
            }
          }
        )
      }
    },
  )

  // Initialize wgpu surface when the SurfaceHolder becomes available.
  LaunchedEffect(surfaceInfo) {
    val info = surfaceInfo ?: return@LaunchedEffect
    log.i { "Creating PrismSurface: ${info.width}x${info.height}" }
    val surface = createPrismSurface(info.holder, info.width, info.height)
    prismSurface = surface
    store.dispatch(EngineStateEvent.SurfaceResized(info.width, info.height))
    isReady = true
    log.i { "PrismSurface ready" }
  }

  // Render loop — synchronized with display refresh rate via Compose's frame scheduling.
  if (isReady) {
    LaunchedEffect(prismSurface, isReady) {
      val surface = prismSurface ?: return@LaunchedEffect
      if (surface.wgpuContext == null) return@LaunchedEffect
      log.i { "Starting render loop" }

      val engine = store.engine
      engine.gameLoop.startExternal()

      while (true) {
        withFrameNanos {
          if (!isReady) return@withFrameNanos

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
  }

  DisposableEffect(store) {
    onDispose {
      log.i { "PrismView disposing" }
      store.engine.gameLoop.stop()
      isReady = false
      prismSurface?.detach()
      prismSurface = null
    }
  }
}

/** Holder for Android surface parameters needed to create a PrismSurface. */
private data class SurfaceInfo(val holder: SurfaceHolder, val width: Int, val height: Int)
