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
 * Creates a Cornell box scene: a 5×5×5 room with a red left wall, green right wall, white
 * back/floor/ceiling, and two PBR spheres inside. A point light near the ceiling illuminates the
 * scene from above.
 *
 * The walls are quads (facing the room interior) using [CullMode.BACK] with outward normals. The
 * orbit camera starts in front of the open face at [CORNELL_ORBIT_RADIUS] units.
 */
fun createCornellBoxScene(wgpuContext: WGPUContext, width: Int, height: Int): DemoScene {
  val renderer = WgpuRenderer(wgpuContext)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  renderer.hdrEnabled = true
  renderer.initializeIbl()

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera — positioned outside the open front face, looking in
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

  // Area light simulation — bright point light near the ceiling center
  val ceilingLight = world.createEntity()
  world.addComponent(ceilingLight, TransformComponent(position = Vec3(0f, HALF - 0.3f, -0.5f)))
  world.addComponent(
    ceilingLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(1.0f, 0.98f, 0.9f),
      intensity = 60f,
      range = 15f,
    ),
  )

  // Weak directional fill so the back wall doesn't go completely dark
  val fillLight = world.createEntity()
  world.addComponent(fillLight, TransformComponent())
  world.addComponent(
    fillLight,
    LightComponent(
      lightType = LightType.DIRECTIONAL,
      color = Color(0.4f, 0.4f, 0.45f),
      intensity = 0.3f,
      direction = Vec3(0f, -1f, -1f),
    ),
  )

  // Build walls from quads. A Mesh.quad() lies in the XY plane with normal +Z.
  // We rotate each quad so its front face (normal side) points into the room interior.
  val wallMesh = Mesh.quad()
  val wallScale = Vec3(HALF * 2f, HALF * 2f, 1f)

  addWall(world, wallMesh, wallScale, Vec3(0f, 0f, -HALF), Quaternion.identity(), Color.WHITE)
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(-HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f),
    Color.RED,
  )
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, -PI.toFloat() / 2f),
    Color.GREEN,
  )
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, -HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, -PI.toFloat() / 2f),
    Color.WHITE,
  )
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, PI.toFloat() / 2f),
    Color.WHITE,
  )

  // Two spheres: a dielectric (white) on the left and a metallic (copper) on the right
  val sphereMesh = Mesh.sphere()
  val sphereScale = Vec3(1.5f, 1.5f, 1.5f) // 0.5 base radius → 0.75 world radius

  val leftSphere = world.createEntity()
  world.addComponent(
    leftSphere,
    TransformComponent(position = Vec3(-1f, -HALF + 0.75f, -0.8f), scale = sphereScale),
  )
  world.addComponent(leftSphere, MeshComponent(mesh = sphereMesh))
  world.addComponent(
    leftSphere,
    MaterialComponent(
      material = Material(baseColor = Color(0.92f, 0.9f, 0.88f), metallic = 0.0f, roughness = 0.4f)
    ),
  )

  val rightSphere = world.createEntity()
  world.addComponent(
    rightSphere,
    TransformComponent(position = Vec3(1f, -HALF + 0.75f, -0.8f), scale = sphereScale),
  )
  world.addComponent(rightSphere, MeshComponent(mesh = sphereMesh))
  world.addComponent(
    rightSphere,
    MaterialComponent(
      material = Material(baseColor = Color(0.95f, 0.64f, 0.54f), metallic = 1.0f, roughness = 0.2f)
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
