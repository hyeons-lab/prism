package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.Entity
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
import io.ygdrasil.webgpu.WGPUContext
import kotlin.math.PI

private val log = Logger.withTag("DemoScene")

private val defaultRotationSpeed = PI.toFloat() / 4f

/**
 * Holds the engine, ECS world, renderer, and key entity handles for the demo scene.
 *
 * @param ownsEngine When true, [shutdown] will also shut down the engine. When false (external
 *   engine from EngineStore), [shutdown] only disposes scene-specific resources.
 */
class DemoScene(
  val engine: Engine,
  val world: World,
  val renderer: WgpuRenderer,
  val cubeEntity: Entity,
  val cameraEntity: Entity,
  private val ownsEngine: Boolean = true,
) {
  private var disposed = false

  /**
   * Advances the scene by one frame: rotates the cube and runs the ECS update. Used by
   * non-interactive demos (GLFW, WASM) where rotation is not user-controllable.
   *
   * @param rotationSpeed Rotation speed in radians per second. Defaults to PI/4 (~45 deg/s).
   */
  fun tick(
    deltaTime: Float,
    elapsed: Float,
    frameCount: Long,
    rotationSpeed: Float = defaultRotationSpeed,
  ) {
    tickWithAngle(
      deltaTime = deltaTime,
      elapsed = elapsed,
      frameCount = frameCount,
      angle = elapsed * rotationSpeed,
    )
  }

  /**
   * Advances the scene by one frame with an explicit rotation angle. Use this when the caller
   * manages angle accumulation (e.g. variable speed or pause/resume scenarios).
   */
  fun tickWithAngle(deltaTime: Float, elapsed: Float, frameCount: Long, angle: Float) {
    val cubeTransform = world.getComponent<TransformComponent>(cubeEntity)
    cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)

    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    world.update(time)
  }

  /** Updates the camera's aspect ratio. Call this when the rendering surface is resized. */
  fun updateAspectRatio(width: Int, height: Int) {
    if (width <= 0 || height <= 0) return
    val cameraComponent = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cameraComponent.camera.aspectRatio = width.toFloat() / height.toFloat()
  }

  /**
   * Releases scene-specific resources (world, renderer subsystem) without shutting down the engine.
   * Safe to call on scenes that use a shared engine (e.g. from EngineStore).
   */
  fun dispose() {
    if (disposed) return
    disposed = true
    world.shutdown()
    engine.removeSubsystem(renderer)
    engine.gameLoop.onRender = null
  }

  /**
   * Full shutdown: disposes scene resources and, if this scene owns the engine, shuts down the
   * engine too.
   */
  fun shutdown() {
    if (ownsEngine) {
      world.shutdown()
      engine.shutdown()
    } else {
      dispose()
    }
  }
}

/**
 * Creates a standard demo scene with a camera and a lit cube. Used by the GLFW, Compose, WASM, and
 * iOS demo entry points.
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

/**
 * Creates a demo scene that attaches to an existing [Engine] (e.g. one owned by an [EngineStore]).
 * The renderer is registered as a subsystem on the engine and [GameLoop.onRender] is wired to drive
 * the ECS world each frame.
 *
 * @param engine An already-initialized engine (typically from [EngineStore.engine]).
 * @param wgpuContext The WebGPU context to render into.
 * @param width Initial surface width in pixels.
 * @param height Initial surface height in pixels.
 * @param surfacePreConfigured When true, the renderer skips its own surface configuration.
 * @param initialColor The initial cube material color.
 */
fun createDemoScene(
  engine: Engine,
  wgpuContext: WGPUContext,
  width: Int,
  height: Int,
  surfacePreConfigured: Boolean = false,
  initialColor: Color = Color(0.3f, 0.5f, 0.9f),
): DemoScene {
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = surfacePreConfigured)
  engine.addSubsystem(renderer)

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

  // Wire the ECS update into the engine's render callback so that PrismView's
  // gameLoop.tick() automatically drives the scene each frame.
  engine.gameLoop.onRender = { time -> world.update(time) }

  log.i { "Demo scene attached to existing engine (${width}x${height})" }
  return DemoScene(engine, world, renderer, cubeEntity, cameraEntity, ownsEngine = false)
}
