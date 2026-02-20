package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.LightComponent
import com.hyeonslab.prism.ecs.components.LightType
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

private val log = Logger.withTag("CornellBoxScene")

private const val HALF = 2.5f
private const val CORNELL_ORBIT_RADIUS = 7f

/**
 * Creates a Cornell box scene closely matching the classic Cornell box reference image.
 *
 * Room: 5×5×5 (±2.5 units). Red left wall, green right wall, white floor/ceiling/back wall. A
 * single bright point light on the ceiling simulates the characteristic rectangular area light. Two
 * white matte boxes (a tall one on the left and a short one on the right) sit on the floor, each
 * rotated ≈17° around Y, matching the Cornell box reference layout.
 *
 * The orbit camera starts outside the open front face at [CORNELL_ORBIT_RADIUS] units.
 */
fun createCornellBoxScene(wgpuContext: WGPUContext, width: Int, height: Int): DemoScene {
  val renderer = WgpuRenderer(wgpuContext)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  // HDR tone mapping for accurate light response — IBL disabled (no environment map in a Cornell
  // box), ambient set to near-zero so only direct lighting illuminates the scene.
  renderer.hdrEnabled = true

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera — outside the open front face, looking straight in.
  val cameraEntity = world.createEntity()
  val camera =
    Camera().apply {
      position = Vec3(0f, 0f, CORNELL_ORBIT_RADIUS)
      target = Vec3(0f, 0f, 0f)
      fovY = 50f
      aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1f
      nearPlane = 0.1f
      farPlane = 50f
    }
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Ceiling area-light simulation: single bright warm-white point light just below the ceiling
  // center, matching the small rectangular light panel in the Cornell box reference image.
  val ceilingLight = world.createEntity()
  world.addComponent(ceilingLight, TransformComponent(position = Vec3(0f, HALF - 0.1f, 0f)))
  world.addComponent(
    ceilingLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(1.0f, 0.98f, 0.9f),
      intensity = 150f,
      range = 15f,
    ),
  )

  // Walls — shared quad mesh, each rotated so its front face (normal) points into the room.
  val wallMesh = Mesh.quad()
  val wallScale = Vec3(HALF * 2f, HALF * 2f, 1f)

  // Back wall: identity rotation — normal +Z faces the open front (toward camera). ✓
  addWall(world, wallMesh, wallScale, Vec3(0f, 0f, -HALF), Quaternion.identity(), Color.WHITE)
  // Left wall: normal +X (into room) after Ry(+90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(-HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f),
    Color.RED,
  )
  // Right wall: normal -X (into room) after Ry(-90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, -PI.toFloat() / 2f),
    Color.GREEN,
  )
  // Floor: normal +Y (into room) after Rx(-90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, -HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, -PI.toFloat() / 2f),
    Color.WHITE,
  )
  // Ceiling: normal -Y (into room) after Rx(+90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, PI.toFloat() / 2f),
    Color.WHITE,
  )

  // Cornell box boxes — two white matte rectangular prisms using Mesh.cube().
  // Tall box: ~60% room height, 30% room width/depth, rotated -17° around Y.
  val tallBox = world.createEntity()
  world.addComponent(
    tallBox,
    TransformComponent(
      position = Vec3(-0.7f, -HALF + 1.5f, -0.6f),
      rotation = Quaternion.fromAxisAngle(Vec3.UP, -(17f * PI.toFloat() / 180f)),
      scale = Vec3(1.5f, 3.0f, 1.5f),
    ),
  )
  world.addComponent(tallBox, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(
    tallBox,
    MaterialComponent(
      material = Material(baseColor = Color(0.9f, 0.9f, 0.9f), metallic = 0.0f, roughness = 0.9f)
    ),
  )

  // Short box: ~33% room height, 30% room width/depth, rotated +17° around Y.
  val shortBox = world.createEntity()
  world.addComponent(
    shortBox,
    TransformComponent(
      position = Vec3(0.85f, -HALF + 0.75f, -0.3f),
      rotation = Quaternion.fromAxisAngle(Vec3.UP, (17f * PI.toFloat() / 180f)),
      scale = Vec3(1.5f, 1.5f, 1.5f),
    ),
  )
  world.addComponent(shortBox, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(
    shortBox,
    MaterialComponent(
      material = Material(baseColor = Color(0.9f, 0.9f, 0.9f), metallic = 0.0f, roughness = 0.9f)
    ),
  )

  world.initialize()
  log.i { "Cornell box scene initialized (${width}×${height})" }
  return DemoScene(engine, world, renderer, cameraEntity, orbitRadius = CORNELL_ORBIT_RADIUS)
}

private fun addWall(
  world: World,
  mesh: Mesh,
  scale: Vec3,
  position: Vec3,
  rotation: Quaternion,
  color: Color,
): Entity {
  val entity = world.createEntity()
  world.addComponent(
    entity,
    TransformComponent(position = position, rotation = rotation, scale = scale),
  )
  world.addComponent(entity, MeshComponent(mesh = mesh))
  world.addComponent(
    entity,
    MaterialComponent(material = Material(baseColor = color, metallic = 0.0f, roughness = 0.9f)),
  )
  return entity
}
