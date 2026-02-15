package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import ffi.LibraryLoader
import io.ygdrasil.webgpu.glfwContextRenderer
import kotlin.math.PI
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
  val rotationSpeed = PI.toFloat() / 4f
  var frameCount = 0L

  while (!glfwWindowShouldClose(glfwContext.windowHandler)) {
    glfwPollEvents()

    val elapsed = (System.nanoTime() - startTime) / 1_000_000_000f
    val angle = elapsed * rotationSpeed

    // Update cube rotation via ECS
    val cubeTransform = scene.world.getComponent<TransformComponent>(scene.cubeEntity)
    cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)

    frameCount++
    val time = Time(deltaTime = 1f / 60f, totalTime = elapsed, frameCount = frameCount)
    scene.world.update(time)
  }

  log.i { "Shutting down..." }
  scene.shutdown()
}
