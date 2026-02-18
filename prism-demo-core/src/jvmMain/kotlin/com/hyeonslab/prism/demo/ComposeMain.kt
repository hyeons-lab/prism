package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.AwtRenderingContext
import com.hyeonslab.prism.widget.PrismPanel
import ffi.LibraryLoader
import java.awt.BorderLayout
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
 * All Swing/AWT setup runs on the EDT via [SwingUtilities.invokeLater] as required by
 * [ComposePanel].
 */
fun main() {
  log.i { "Starting Prism Compose Demo..." }
  LibraryLoader.load()

  SwingUtilities.invokeLater { createAndShowUi() }
}

private fun createAndShowUi() {
  val store = DemoStore()
  // `scene` is only accessed from the Swing EDT: createAndShowUi() is invoked via
  // SwingUtilities.invokeLater, and all callbacks (onReady, onResized, withFrameNanos)
  // run on the EDT. No additional synchronization is required.
  var scene: DemoScene? = null

  val frame = JFrame("Prism 3D Engine \u2014 Compose Demo")
  frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
  frame.layout = BorderLayout()

  // Left: 3D rendering canvas (heavyweight AWT Canvas for GPU rendering)
  val prismPanel = PrismPanel()
  prismPanel.preferredSize = Dimension(800, 700)
  prismPanel.onReady = {
    log.i { "PrismPanel ready \u2014 initializing scene" }
    val ctx = prismPanel.wgpuContext
    if (ctx != null) {
      val s = createDemoScene(ctx, prismPanel.width, prismPanel.height, surfacePreConfigured = true)
      s.renderer.onResize = { w, h ->
        val rc = ctx.renderingContext
        if (rc is AwtRenderingContext) {
          rc.updateSize(w, h)
        }
      }
      scene = s
    }
  }
  prismPanel.onResized = { w, h ->
    scene?.let { s ->
      s.renderer.resize(w, h)
      s.updateAspectRatio(w, h)
    }
  }
  frame.add(prismPanel, BorderLayout.CENTER)

  // Right: Compose UI controls via ComposePanel (embeds Compose in Swing)
  val composePanel = ComposePanel()
  composePanel.preferredSize = Dimension(280, 700)
  composePanel.setContent {
    val uiState by store.state.collectAsStateWithLifecycle()

    // Render loop driven by Compose's frame scheduling, synchronized with the display
    // refresh rate. withFrameNanos suspends until the next vsync, then runs on the EDT.
    LaunchedEffect(Unit) {
      val startTimeNs = System.nanoTime()
      var frameCount = 0L
      var lastFrameTimeNs = startTimeNs

      while (true) {
        withFrameNanos {
          val s = scene ?: return@withFrameNanos
          if (!prismPanel.isReady) return@withFrameNanos

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

          // Run ECS update (triggers RenderSystem)
          s.tick(deltaTime = deltaSec, elapsed = totalSec, frameCount = frameCount)
        }
      }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
      ComposeDemoControls(
        state = uiState,
        onIntent = store::dispatch,
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
  frame.add(composePanel, BorderLayout.EAST)

  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.isVisible = true

  // Shut down gracefully when window closes
  frame.addWindowListener(
    object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        scene?.let { s ->
          log.i { "Shutting down scene..." }
          s.shutdown()
        }
      }
    }
  )
}
