package com.hyeonslab.prism.demo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.widget.AwtRenderingContext
import com.hyeonslab.prism.widget.PrismPanel
import ffi.LibraryLoader
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.Timer

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
      val s =
        createDemoScene(
          ctx,
          prismPanel.width,
          prismPanel.height,
          surfacePreConfigured = true,
          initialColor = store.state.value.cubeColor,
        )
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
    val uiState by store.state.collectAsState()
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

  // Render loop via Swing Timer (fires on EDT, ~unlimited FPS, GPU-limited)
  val startTimeNs = System.nanoTime()
  var frameCount = 0L
  var lastFrameTimeNs = startTimeNs
  var rotationAngle = 0f

  val renderTimer =
    Timer(1) {
      val s = scene ?: return@Timer
      if (!prismPanel.isReady) return@Timer

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
      val cubeTransform = s.world.getComponent<TransformComponent>(s.cubeEntity)
      cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, rotationAngle)

      // Update material color
      val cubeMaterial = s.world.getComponent<MaterialComponent>(s.cubeEntity)
      cubeMaterial?.material = Material(baseColor = currentState.cubeColor)

      // Run ECS update (triggers RenderSystem)
      val time = Time(deltaTime = deltaSec, totalTime = totalSec, frameCount = frameCount)
      s.world.update(time)
    }
  renderTimer.start()

  // Shut down gracefully when window closes
  frame.addWindowListener(
    object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        renderTimer.stop()
        scene?.let { s ->
          log.i { "Shutting down scene..." }
          s.shutdown()
        }
      }
    }
  )
}
