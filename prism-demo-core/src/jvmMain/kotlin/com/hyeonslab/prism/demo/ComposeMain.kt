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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.swing.JFrame
import javax.swing.SwingUtilities
import kotlinx.coroutines.runBlocking

private val log = Logger.withTag("ComposeMain")

/** Radians of orbit per pixel of drag — matches GLFW and WASM demos. */
private const val ORBIT_SENSITIVITY = 0.005f

/**
 * Compose Desktop demo entry point using JFrame + PrismPanel + ComposePanel.
 *
 * PrismPanel (heavyweight AWT Canvas) fills the main area for GPU rendering. A ComposePanel on the
 * right hosts the PBR control sliders and drives the render loop via [withFrameNanos] (vsync-
 * aligned, same mechanism as the iOS Compose demo). Drag-to-orbit is wired via AWT mouse listeners.
 *
 * All Swing/AWT setup runs on the EDT via [SwingUtilities.invokeLater] as required by AWT.
 */
fun main() {
  log.i { "Starting Prism Compose Demo..." }
  LibraryLoader.load()
  SwingUtilities.invokeLater { createAndShowUi() }
}

private fun createAndShowUi() {
  val store = DemoStore()
  // `scene` is only accessed from the EDT (onReady, onResized, withFrameNanos) — no sync needed.
  var scene: DemoScene? = null

  val frame = JFrame("Prism 3D Engine \u2014 Compose Demo")
  frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
  frame.layout = BorderLayout()

  val prismPanel = PrismPanel()
  prismPanel.preferredSize = Dimension(800, 700)

  // Drag-to-orbit: left-mouse drag rotates the orbit camera.
  var lastDragX = 0
  var lastDragY = 0
  prismPanel.addMouseListener(
    object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        lastDragX = e.x
        lastDragY = e.y
      }
    }
  )
  prismPanel.addMouseMotionListener(
    object : MouseMotionAdapter() {
      override fun mouseDragged(e: MouseEvent) {
        val dx = e.x - lastDragX
        val dy = e.y - lastDragY
        lastDragX = e.x
        lastDragY = e.y
        scene?.orbitBy(-dx * ORBIT_SENSITIVITY, dy * ORBIT_SENSITIVITY)
      }
    }
  )

  prismPanel.onReady = {
    log.i { "PrismPanel ready \u2014 initializing scene" }
    val ctx = prismPanel.wgpuContext
    if (ctx != null) {
      val glbData =
        File("DamagedHelmet.glb").takeIf { it.exists() }?.readBytes()
          ?: error("DamagedHelmet.glb not found \u2014 run ./gradlew downloadDemoAssets")
      val s = runBlocking {
        createGltfDemoScene(
          ctx,
          prismPanel.width,
          prismPanel.height,
          glbData,
          surfacePreConfigured = true,
        )
      }
      s.renderer.onResize = { w, h ->
        val rc = ctx.renderingContext
        if (rc is AwtRenderingContext) rc.updateSize(w, h)
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

  // ComposePanel on the right: PBR controls + vsync-aligned render loop.
  // withFrameNanos uses Compose Desktop's frame clock, which is tied to the display
  // refresh and correctly flushes the CAMetalLayer each frame — a plain Swing Timer
  // is not display-link-synchronized on macOS.
  val composePanel = ComposePanel()
  composePanel.preferredSize = Dimension(280, 700)
  composePanel.setContent {
    val uiState by store.state.collectAsStateWithLifecycle()

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

          if (deltaSec > 0f) {
            val smoothedFps = currentState.fps * 0.9f + (1f / deltaSec) * 0.1f
            store.dispatch(DemoIntent.UpdateFps(smoothedFps))
          }

          // Apply PBR slider values and run ECS update. Pass 0 deltaTime when paused.
          s.setMaterialOverride(currentState.metallic, currentState.roughness)
          s.setEnvIntensity(currentState.envIntensity)
          s.tick(
            deltaTime = if (currentState.isPaused) 0f else deltaSec,
            elapsed = totalSec,
            frameCount = frameCount,
          )
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

  frame.add(prismPanel, BorderLayout.CENTER)
  frame.add(composePanel, BorderLayout.EAST)
  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.isVisible = true

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
