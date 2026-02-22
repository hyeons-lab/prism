@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import glfw.GLFW_MOUSE_BUTTON_LEFT
import glfw.GLFW_PRESS
import glfw.glfwPollEvents
import glfw.glfwSetCursorPosCallback
import glfw.glfwSetMouseButtonCallback
import glfw.glfwShowWindow
import glfw.glfwWindowShouldClose
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.SurfaceConfiguration
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.runBlocking
import platform.QuartzCore.CACurrentMediaTime
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

private val log = Logger.withTag("PrismMacOS")

// Radians of orbit per pixel.
private const val ORBIT_SENSITIVITY = 0.005f

// Top-level state shared with staticCFunction callbacks (which cannot capture locals).
private lateinit var orbitScene: DemoScene
private var lastMouseX = 0.0
private var lastMouseY = 0.0
private var mouseButtonDown = false

fun main() = runBlocking {
  log.i { "Starting Prism macOS Native Demo..." }

  val surface = createPrismSurface(width = 800, height = 600, title = "Prism macOS Demo")
  val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }
  val windowHandler = checkNotNull(surface.windowHandler) { "GLFW window not available" }

  val glbData =
    loadGlbBytes("DamagedHelmet.glb")
      ?: error("DamagedHelmet.glb not found — place the file in the working directory")
  val scene = createGltfDemoScene(wgpuContext, width = 800, height = 600, glbData = glbData)
  orbitScene = scene

  glfwSetMouseButtonCallback(
    windowHandler,
    staticCFunction { _, button, action, _ ->
      if (button == GLFW_MOUSE_BUTTON_LEFT) mouseButtonDown = (action == GLFW_PRESS)
    },
  )

  glfwSetCursorPosCallback(
    windowHandler,
    staticCFunction { _, x, y ->
      if (mouseButtonDown) {
        orbitScene.orbitBy(
          deltaAzimuth = -((x - lastMouseX) * ORBIT_SENSITIVITY).toFloat(),
          deltaElevation = ((y - lastMouseY) * ORBIT_SENSITIVITY).toFloat(),
        )
      }
      lastMouseX = x
      lastMouseY = y
    },
  )

  glfwShowWindow(windowHandler)
  // Re-configure surface to clear any Outdated status triggered by glfwShowWindow.
  // Showing the window causes the CAMetalLayer to present for the first time, which
  // can mark the wgpu surface as Outdated before the first render call.
  wgpuContext.surface.configure(
    SurfaceConfiguration(
      device = wgpuContext.device,
      format = wgpuContext.renderingContext.textureFormat,
      alphaMode = CompositeAlphaMode.Opaque,
    )
  )
  log.i { "Window opened — entering render loop" }

  var lastFrameTime = CACurrentMediaTime()
  var frameCount = 0L
  val startTime = lastFrameTime

  try {
    while (glfwWindowShouldClose(windowHandler) == 0) {
      glfwPollEvents()

      val now = CACurrentMediaTime()
      val deltaTime = (now - lastFrameTime).toFloat()
      val elapsed = (now - startTime).toFloat()
      lastFrameTime = now
      frameCount++

      try {
        scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
      } catch (e: IllegalStateException) {
        // Surface became Outdated (window resize, display change, etc.). Reconfigure and skip frame.
        log.w { "Surface Outdated: ${e.message} — reconfiguring" }
        wgpuContext.surface.configure(
          SurfaceConfiguration(
            device = wgpuContext.device,
            format = wgpuContext.renderingContext.textureFormat,
            alphaMode = CompositeAlphaMode.Opaque,
          )
        )
      }
    }
  } finally {
    log.i { "Shutting down..." }
    scene.shutdown()
    surface.detach()
  }
}

private fun loadGlbBytes(path: String): ByteArray? {
  val file = fopen(path, "rb") ?: return null
  fseek(file, 0, SEEK_END)
  val size = ftell(file)
  fseek(file, 0, SEEK_SET)
  if (size <= 0L) {
    fclose(file)
    return null
  }
  val bytes = ByteArray(size.toInt())
  bytes.usePinned { pinned -> fread(pinned.addressOf(0), 1uL, size.toULong(), file) }
  fclose(file)
  log.i { "Loaded $path ($size bytes)" }
  return bytes
}
