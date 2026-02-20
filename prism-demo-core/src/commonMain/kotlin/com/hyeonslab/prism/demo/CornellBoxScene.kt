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

// Room half-extent (5×5×5 units total, proportional to original 555×555×559mm Cornell box).
private const val HALF = 2.5f

// Camera outside the open front face, looking straight in.
private const val CORNELL_ORBIT_RADIUS = 7f

// Scale factor: 555mm (original Cornell box side) → 5 units → 1mm = 5/555 ≈ 0.009009 units.
private const val MM_TO_UNITS = 5f / 555f

// Cornell box reference colors (from PBR base-color factors in the standard glTF dataset).
// These are the historically accurate muted pigment colors, not pure primaries.
private val CORNELL_WHITE = Color(0.8f, 0.8f, 0.8f)
private val CORNELL_RED = Color(0.63f, 0.065f, 0.05f) // left wall
private val CORNELL_GREEN = Color(0.14f, 0.45f, 0.091f) // right wall

/**
 * Creates a Cornell box scene matching the standard reference geometry.
 *
 * Geometry derived from the Cornell box glTF dataset (555mm room):
 * - Left wall: red, right wall: green, back/floor/ceiling: white (all Lambertian)
 * - Tall block: 165×330×165mm, right side (toward green wall), rotated +22.5° around Y
 * - Short block: 165×165×165mm, left side (toward red wall), rotated −18° around Y
 * - Ceiling light panel: emissive white rectangle at ceiling center (~130mm proportional opening)
 *
 * A point light at the panel position provides actual illumination (closest approximation to the
 * rectangular area light of the original). Color bleeding (GI) is not simulated.
 */
fun createCornellBoxScene(wgpuContext: WGPUContext, width: Int, height: Int): DemoScene {
  val renderer = WgpuRenderer(wgpuContext)
  val engine = Engine()
  engine.addSubsystem(renderer)
  engine.initialize()

  // HDR + tone mapping; IBL disabled — Cornell box is a closed environment, no sky.
  renderer.hdrEnabled = true

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // Camera — outside the open front face, looking straight in at room center.
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

  // Point light at ceiling panel (glTF LightSource node: (0, 554, 0) mm → (0, ~2.49, 0) units).
  // Simulates the rectangular area light; high intensity compensates for point vs area light.
  val ceilingLight = world.createEntity()
  world.addComponent(ceilingLight, TransformComponent(position = Vec3(0f, HALF - 0.05f, 0f)))
  world.addComponent(
    ceilingLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(1.0f, 0.97f, 0.88f),
      intensity = 200f,
      range = 20f,
    ),
  )

  // ---- Walls ----------------------------------------------------------------
  // Mesh.quad() lies in the XY plane with normal +Z. Each wall is rotated so its front face
  // (normal side) faces into the room interior.

  val wallMesh = Mesh.quad()
  val wallScale = Vec3(HALF * 2f, HALF * 2f, 1f)

  // Back wall — normal +Z (toward open front/camera). Identity rotation.
  addWall(world, wallMesh, wallScale, Vec3(0f, 0f, -HALF), Quaternion.identity(), CORNELL_WHITE)
  // Left wall — red. Normal +X (into room) after Ry(+90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(-HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f),
    CORNELL_RED,
  )
  // Right wall — green. Normal -X (into room) after Ry(-90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(HALF, 0f, 0f),
    Quaternion.fromAxisAngle(Vec3.UP, -PI.toFloat() / 2f),
    CORNELL_GREEN,
  )
  // Floor — normal +Y (into room) after Rx(-90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, -HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, -PI.toFloat() / 2f),
    CORNELL_WHITE,
  )
  // Ceiling — normal -Y (into room) after Rx(+90°).
  addWall(
    world,
    wallMesh,
    wallScale,
    Vec3(0f, HALF, 0f),
    Quaternion.fromAxisAngle(Vec3.RIGHT, PI.toFloat() / 2f),
    CORNELL_WHITE,
  )

  // ---- Ceiling light panel --------------------------------------------------
  // Emissive quad at the ceiling, representing the ~130mm area-light opening.
  // Rx(+90°) rotates the +Z normal to -Y so the panel faces downward (visible from below).
  // Panel scale (1.2 × 0.9 units) is proportional to the original ~130×105mm opening.
  val lightPanel = world.createEntity()
  world.addComponent(
    lightPanel,
    TransformComponent(
      position = Vec3(0f, HALF - 0.02f, 0f),
      rotation = Quaternion.fromAxisAngle(Vec3.RIGHT, PI.toFloat() / 2f),
      scale = Vec3(1.2f, 0.9f, 1f),
    ),
  )
  world.addComponent(lightPanel, MeshComponent(mesh = Mesh.quad()))
  world.addComponent(
    lightPanel,
    MaterialComponent(
      material =
        Material(
          baseColor = Color.WHITE,
          metallic = 0f,
          roughness = 1f,
          emissive = Color(10f, 9.5f, 8f), // warm-white HDR; tone-maps to near overexposed white
        )
    ),
  )

  // ---- Blocks ---------------------------------------------------------------
  // Positions derived from the Cornell box glTF node translations (mm), converted to our units
  // (multiply by MM_TO_UNITS = 5/555; Y values offset by -277.5mm to re-center vertically).
  //
  // Positions from glTF dataset, X-mirrored to place the tall block on the LEFT (red wall side)
  // and the short block on the RIGHT (green wall side), matching the reference render.
  //
  //   Tall block:  glTF X=+85 → mirrored to -0.77 (left), Y=-1.01, Z=-0.68
  //   Short block: glTF X=-80 → mirrored to +0.72 (right), Y=-1.75, Z=+0.68
  //
  // Rotations are also mirrored (sign flipped) so block faces align with the reference.

  val tallBoxPos =
    Vec3(
      x = -85f * MM_TO_UNITS, // -0.766 (left, toward red wall)
      y = (165f - 277.5f) * MM_TO_UNITS, // -1.014
      z = -75f * MM_TO_UNITS, // -0.676
    )
  val tallBox = world.createEntity()
  world.addComponent(
    tallBox,
    TransformComponent(
      position = tallBoxPos,
      rotation = Quaternion.fromAxisAngle(Vec3.UP, -0.3925f),
      scale = Vec3(1.5f, 3.0f, 1.5f), // 165×330×165mm proportional
    ),
  )
  world.addComponent(tallBox, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(
    tallBox,
    MaterialComponent(material = Material(baseColor = CORNELL_WHITE, metallic = 0f, roughness = 1f)),
  )

  val shortBoxPos =
    Vec3(
      x = 80f * MM_TO_UNITS, // +0.721 (right, toward green wall)
      y = (82.5f - 277.5f) * MM_TO_UNITS, // -1.757
      z = 75f * MM_TO_UNITS, // 0.676
    )
  val shortBox = world.createEntity()
  world.addComponent(
    shortBox,
    TransformComponent(
      position = shortBoxPos,
      rotation = Quaternion.fromAxisAngle(Vec3.UP, 0.314f),
      scale = Vec3(1.5f, 1.5f, 1.5f), // 165×165×165mm proportional
    ),
  )
  world.addComponent(shortBox, MeshComponent(mesh = Mesh.cube()))
  world.addComponent(
    shortBox,
    MaterialComponent(material = Material(baseColor = CORNELL_WHITE, metallic = 0f, roughness = 1f)),
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
    MaterialComponent(material = Material(baseColor = color, metallic = 0f, roughness = 1f)),
  )
  return entity
}
