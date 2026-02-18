package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.PrismPanel
import io.ygdrasil.webgpu.WGPUContext

private val log = Logger.withTag("PrismView.JVM")

/**
 * JVM Desktop implementation of [PrismView]. Embeds a [PrismPanel] (heavyweight AWT Canvas with
 * wgpu surface) inside Compose via [SwingPanel].
 *
 * The PrismPanel initializes the wgpu surface from the Canvas's native handle when it becomes
 * visible. Once ready, a render loop driven by Compose's frame scheduling calls
 * [com.hyeonslab.prism.core.GameLoop.tick] each frame, synchronized with the display refresh rate.
 * All state changes are dispatched as [EngineStateEvent]s through [EngineStore.dispatch].
 */
@Composable
actual fun PrismView(
  store: EngineStore,
  modifier: Modifier,
  onSurfaceReady: ((WGPUContext, Int, Int) -> Unit)?,
  onSurfaceResized: ((Int, Int) -> Unit)?,
) {
  // Hold the latest callback references so the SwingPanel factory (which captures once)
  // always invokes the current lambda, not a stale closure from initial composition.
  val currentOnSurfaceReady by rememberUpdatedState(onSurfaceReady)
  val currentOnSurfaceResized by rememberUpdatedState(onSurfaceResized)

  var panel by remember { mutableStateOf<PrismPanel?>(null) }
  var isReady by remember { mutableStateOf(false) }

  SwingPanel(
    modifier = modifier,
    factory = {
      PrismPanel().also { p ->
        p.onReady = {
          log.i { "PrismPanel surface ready" }
          val ctx = p.wgpuContext
          if (ctx != null) {
            currentOnSurfaceReady?.invoke(ctx, p.width, p.height)
          }
          isReady = true
        }
        p.onResized = { w, h ->
          store.dispatch(EngineStateEvent.SurfaceResized(w, h))
          currentOnSurfaceResized?.invoke(w, h)
        }
        panel = p
      }
    },
  )

  // Render loop — synchronized with display refresh rate via Compose's frame scheduling.
  // withFrameNanos suspends until the next vsync, then runs on the EDT.
  if (isReady) {
    LaunchedEffect(panel, isReady) {
      val p = panel ?: return@LaunchedEffect
      log.i { "Starting render loop" }

      val engine = store.engine
      engine.gameLoop.startExternal()

      while (true) {
        withFrameNanos {
          val ctx = p.wgpuContext
          if (ctx == null || !p.isReady) return@withFrameNanos

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
      store.engine.gameLoop.onRender = null
      // isReady is not reset here — setting Compose state during disposal cannot trigger
      // a recomposition since the composable is already being removed from the tree.
      // The render loop is cancelled by Compose when its parent LaunchedEffect leaves
      // composition.
    }
  }
}
