package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.widget.createPrismSurface
import ffi.LibraryLoader
import java.io.File
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

private val log = Logger.withTag("Prism")

fun main() = runBlocking {
  log.i { "Starting Prism GLFW Demo..." }
  LibraryLoader.load()

  val surface = createPrismSurface(width = 800, height = 600, title = "Prism Demo")
  val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }
  val windowHandler = checkNotNull(surface.windowHandler) { "GLFW window not available" }

  val glbData =
    File("DamagedHelmet.glb").takeIf { it.exists() }?.readBytes()
      ?: error("DamagedHelmet.glb not found — place the file in the working directory")
  val scene = createGltfDemoScene(wgpuContext, width = 800, height = 600, glbData = glbData)

  glfwShowWindow(windowHandler)
  log.i { "Window opened — entering render loop" }

  val startTime = System.nanoTime()
  var lastFrameTime = startTime
  var frameCount = 0L

  while (!glfwWindowShouldClose(windowHandler)) {
    glfwPollEvents()

    val now = System.nanoTime()
    val deltaTime = (now - lastFrameTime) / 1_000_000_000f
    lastFrameTime = now
    val elapsed = (now - startTime) / 1_000_000_000f
    frameCount++
    scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
  }

  log.i { "Shutting down..." }
  scene.shutdown()
  surface.detach()
}
