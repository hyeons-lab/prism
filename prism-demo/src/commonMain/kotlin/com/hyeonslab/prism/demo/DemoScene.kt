package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext

private val log = Logger.withTag("DemoScene")

/** Holds the engine, ECS world, renderer, and key entity handles for the demo scene. */
class DemoScene(
  val engine: Engine,
  val world: World,
  val renderer: WgpuRenderer,
  val cubeEntity: Entity,
  val cameraEntity: Entity,
) {
  /** Updates the camera's aspect ratio. Call this when the rendering surface is resized. */
  fun updateAspectRatio(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val cameraComponent = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cameraComponent.camera.aspectRatio = width.toFloat() / height.toFloat()
  }

  fun shutdown() {
    world.shutdown()
    engine.shutdown()
  }
}

/**
 * Creates a standard demo scene with a camera and a lit cube. Used by both the GLFW and Compose
 * demo entry points.
 *
 * @param wgpuContext The WebGPU context to render into.
 * @param width Initial surface width in pixels.
 * @param height Initial surface height in pixels.
 * @param surfacePreConfigured When true, the renderer skips its own surface configuration (used for
 *   AWT Canvas integration where the surface is pre-configured externally).
 * @param initialColor The initial cube material color.
 */
fun createDemoScene(
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  surfacePreConfigured: Boolean = false,
  initialColor: Color = Color(0.3f, 0.5f, 0.9f),
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = surfacePreConfigured)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera
  val cameraEntity = world.createEntity()
  val camera = Camera()
  camera.position = Vec3(2f, 2f, 4f)
  camera.target = Vec3(0f, 0f, 0f)
  camera.fovY = 60f
  camera.aspectRatio = width.toFloat() / height.toFloat()
  camera.nearPlane = 0.1f
  camera.farPlane = 100f
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Cube
  val cubeEntity = world.createEntity()
  world.addComponent(cubeEntity, TransformComponent())
  world.addComponent(cubeEntity, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(cubeEntity, MaterialComponent(material = Material(baseColor = initialColor)))

  world.initialize()
  log.i { "Demo scene initialized (${width}x${height})" }
  return DemoScene(engine, world, renderer, cubeEntity, cameraEntity)
}
