package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import ffi.LibraryLoader
import io.ygdrasil.webgpu.glfwContextRenderer
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose

private val log = Logger.withTag("Prism")

fun main() = runBlocking {
  log.i { "Starting Prism GLFW Demo..." }
  LibraryLoader.load()

  val glfwContext = glfwContextRenderer(width = 800, height = 600, title = "Prism Demo")
  val scene = createDemoScene(glfwContext.wgpuContext, width = 800, height = 600)

  glfwShowWindow(glfwContext.windowHandler)
  log.i { "Window opened — entering render loop" }

  val startTime = System.nanoTime()
  var frameCount = 0L

  while (!glfwWindowShouldClose(glfwContext.windowHandler)) {
    glfwPollEvents()

    val elapsed = (System.nanoTime() - startTime) / 1_000_000_000f
    frameCount++
    scene.tick(deltaTime = 1f / 60f, elapsed = elapsed, frameCount = frameCount)
  }

  log.i { "Shutting down..." }
  scene.shutdown()
}
