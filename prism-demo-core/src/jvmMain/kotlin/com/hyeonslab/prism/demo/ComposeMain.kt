package com.hyeonslab.prism.demo

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
 * Compose Desktop demo entry point using JFrame + PrismPanel.
 *
 * Uses JFrame (Swing) as the top-level container. On macOS, PrismPanel attaches a CAMetalLayer
 * sublayer to the Canvas's NSView, which means Metal rendering sits above any Java2D overlay — so
 * controls are shown in the window title bar rather than as an in-canvas overlay.
 *
 * All Swing/AWT setup runs on the EDT via [SwingUtilities.invokeLater] as required by AWT.
 * Rendering runs on a dedicated daemon thread; Metal's CAMetalLayer.nextDrawable naturally
 * throttles to vsync.
 */
fun main() {
  log.i { "Starting Prism Compose Demo..." }
  LibraryLoader.load()
  SwingUtilities.invokeLater { createAndShowUi() }
}

private fun createAndShowUi() {
  var scene: DemoScene? = null
  var renderThread: Thread? = null

  val frame = JFrame("Prism 3D Engine \u2014 Compose Demo")
  frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
  frame.layout = BorderLayout()

  val prismPanel = PrismPanel()
  prismPanel.preferredSize = Dimension(1000, 700)

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
        scene?.orbitBy(dx * ORBIT_SENSITIVITY, -dy * ORBIT_SENSITIVITY)
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

      // Render loop on a dedicated daemon thread. Metal's CAMetalLayer.nextDrawable
      // naturally rate-limits to vsync (~60 fps) when displaySyncEnabled=true (default),
      // so no explicit sleep is needed. The tight loop lets Metal control pacing.
      val startTimeNs = System.nanoTime()
      var lastFrameTimeNs = startTimeNs
      var frameCount = 0L
      var fpsSmoothed = 0f
      var lastTitleUpdateMs = System.currentTimeMillis()

      val t = Thread {
        while (!Thread.currentThread().isInterrupted) {
          try {
            val nowNs = System.nanoTime()
            val deltaSec = (nowNs - lastFrameTimeNs) / 1_000_000_000f
            val totalSec = (nowNs - startTimeNs) / 1_000_000_000f
            lastFrameTimeNs = nowNs
            frameCount++

            s.tick(deltaTime = deltaSec, elapsed = totalSec, frameCount = frameCount)

            // Update window title at most once per second to avoid EDT flood.
            val nowMs = System.currentTimeMillis()
            if (deltaSec > 0f && nowMs - lastTitleUpdateMs >= 1000L) {
              fpsSmoothed = fpsSmoothed * 0.8f + (1f / deltaSec) * 0.2f
              val fps = fpsSmoothed.toInt()
              lastTitleUpdateMs = nowMs
              SwingUtilities.invokeLater { frame.title = "Prism 3D Engine \u2014 $fps FPS" }
            }
          } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            break
          } catch (e: Exception) {
            log.e(e) { "Render tick failed: ${e.message}" }
            break
          }
        }
      }
      t.isDaemon = true
      t.start()
      renderThread = t
    }
  }

  prismPanel.onResized = { w, h ->
    scene?.let { s ->
      s.renderer.resize(w, h)
      s.updateAspectRatio(w, h)
    }
  }

  frame.add(prismPanel, BorderLayout.CENTER)
  frame.pack()
  frame.setLocationRelativeTo(null)
  frame.isVisible = true

  frame.addWindowListener(
    object : WindowAdapter() {
      override fun windowClosing(e: WindowEvent) {
        renderThread?.interrupt()
        renderThread?.join(2000)
        scene?.let { s ->
          log.i { "Shutting down scene..." }
          s.shutdown()
        }
      }
    }
  )
}
