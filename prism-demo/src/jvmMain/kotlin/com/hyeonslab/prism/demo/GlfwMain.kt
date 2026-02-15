package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.WgpuRenderer
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
  val wgpuContext = glfwContext.wgpuContext

  // Create engine and register WgpuRenderer as a subsystem
  val renderer = WgpuRenderer(wgpuContext)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  // Create ECS world with RenderSystem
  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera entity
  val cameraEntity = world.createEntity()
  val camera = Camera()
  camera.position = Vec3(2f, 2f, 4f)
  camera.target = Vec3(0f, 0f, 0f)
  camera.fovY = 60f
  camera.aspectRatio = 800f / 600f
  camera.nearPlane = 0.1f
  camera.farPlane = 100f
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Cube entity
  val cubeEntity = world.createEntity()
  world.addComponent(cubeEntity, TransformComponent())
  world.addComponent(cubeEntity, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(
    cubeEntity,
    MaterialComponent(material = Material(baseColor = Color(0.3f, 0.5f, 0.9f))),
  )

  // Initialize all systems (creates shader, pipeline)
  world.initialize()

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
    val cubeTransform = world.getComponent<TransformComponent>(cubeEntity)
    if (cubeTransform != null) {
      cubeTransform.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)
    }

    frameCount++
    val time = Time(deltaTime = 1f / 60f, totalTime = elapsed, frameCount = frameCount)
    world.update(time)
  }

  log.i { "Shutting down..." }
  world.shutdown()
  engine.shutdown()
}
