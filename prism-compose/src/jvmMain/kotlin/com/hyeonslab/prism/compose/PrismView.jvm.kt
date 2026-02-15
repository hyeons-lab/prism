package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.widget.PrismPanel
import kotlinx.coroutines.delay

private val log = Logger.withTag("PrismView.JVM")

/**
 * JVM Desktop implementation of [PrismView]. Embeds a [PrismPanel] (heavyweight AWT Canvas with
 * wgpu surface) inside Compose via [SwingPanel].
 *
 * The PrismPanel initializes the wgpu surface from the Canvas's native handle when it becomes
 * visible. Once ready, a coroutine-based render loop calls [onFrame] at ~60fps.
 */
@Composable
actual fun PrismView(
  engine: Engine,
  modifier: Modifier,
  onFrame: ((Time) -> Unit)?,
  onResize: ((width: Int, height: Int) -> Unit)?,
) {
  var panel by remember { mutableStateOf<PrismPanel?>(null) }
  var isReady by remember { mutableStateOf(false) }

  SwingPanel(
    modifier = modifier,
    factory = {
      PrismPanel().also { p ->
        p.onReady = {
          log.i { "PrismPanel surface ready" }
          isReady = true
        }
        p.onResized = { w, h -> onResize?.invoke(w, h) }
        panel = p
      }
    },
  )

  // Render loop — runs when the panel's wgpu surface is ready
  if (isReady && onFrame != null) {
    LaunchedEffect(panel, isReady) {
      val p = panel ?: return@LaunchedEffect
      log.i { "Starting render loop" }

      val startTimeNs = System.nanoTime()
      var frameCount = 0L
      var lastFrameTimeNs = startTimeNs

      while (true) {
        val ctx = p.wgpuContext
        if (ctx == null || !p.isReady) {
          delay(16)
          continue
        }

        val nowNs = System.nanoTime()
        val deltaSec = (nowNs - lastFrameTimeNs) / 1_000_000_000f
        val totalSec = (nowNs - startTimeNs) / 1_000_000_000f
        lastFrameTimeNs = nowNs
        frameCount++

        val time = Time(deltaTime = deltaSec, totalTime = totalSec, frameCount = frameCount)
        onFrame(time)

        // Yield to Compose's frame scheduling (~16ms for 60fps)
        delay(1)
      }
    }
  }

  DisposableEffect(engine) {
    onDispose {
      log.i { "PrismView disposing" }
      isReady = false
    }
  }
}
