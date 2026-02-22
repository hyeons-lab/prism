@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package engine.prism.native

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.scene.CameraNode
import com.hyeonslab.prism.scene.LightNode
import com.hyeonslab.prism.scene.MeshNode
import com.hyeonslab.prism.scene.Node
import com.hyeonslab.prism.scene.Scene
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

// ---------------------------------------------------------------------------
// Engine API
// ---------------------------------------------------------------------------

/**
 * Creates an Engine with [appName] and [targetFps] and returns an opaque handle. [appName] is a
 * null-terminated C string; pass NULL to use the default app name.
 */
@CName("prism_create_engine")
fun prismCreateEngine(appName: CPointer<ByteVar>?, targetFps: Int): Long =
  Registry.put(
    Engine(EngineConfig(appName = appName?.toKString() ?: "Prism", targetFps = targetFps))
  )

/** Initializes the engine identified by [handle]. Must be called before the render loop. */
@CName("prism_engine_initialize")
fun prismEngineInitialize(handle: Long) {
  Registry.get<Engine>(handle)?.initialize()
}

/** Returns 1 if [handle] refers to a live engine, 0 otherwise. */
@CName("prism_engine_is_alive")
fun prismEngineIsAlive(handle: Long): Int = if (Registry.get<Engine>(handle) != null) 1 else 0

/** Returns the most recent frame delta time in seconds, or 0 if handle is invalid. */
@CName("prism_engine_get_delta_time")
fun prismEngineGetDeltaTime(handle: Long): Float =
  Registry.get<Engine>(handle)?.time?.deltaTime ?: 0f

/** Returns total elapsed time in seconds since the engine started, or 0 if invalid. */
@CName("prism_engine_get_total_time")
fun prismEngineGetTotalTime(handle: Long): Float =
  Registry.get<Engine>(handle)?.time?.totalTime ?: 0f

/** Returns the total frame count, or 0 if the handle is invalid. */
@CName("prism_engine_get_frame_count")
fun prismEngineGetFrameCount(handle: Long): Long =
  Registry.get<Engine>(handle)?.time?.frameCount ?: 0L

/** Shuts down the engine and releases the handle. */
@CName("prism_destroy_engine")
fun prismDestroyEngine(handle: Long) {
  Registry.get<Engine>(handle)?.shutdown()
  Registry.remove(handle)
}

// ---------------------------------------------------------------------------
// ECS API
// ---------------------------------------------------------------------------

/** Creates an ECS World and returns its handle. */
@CName("prism_create_world")
fun prismCreateWorld(): Long = Registry.put(World())

/** Creates a new entity in the world. Returns its integer ID, or -1 if the handle is invalid. */
@CName("prism_world_create_entity")
fun prismWorldCreateEntity(worldHandle: Long): Int {
  val world = Registry.get<World>(worldHandle) ?: return -1
  return world.createEntity().id.toInt()
}

/** Destroys an entity by its integer ID. */
@CName("prism_world_destroy_entity")
fun prismWorldDestroyEntity(worldHandle: Long, entityId: Int) {
  val world = Registry.get<World>(worldHandle) ?: return
  world.destroyEntity(Entity(entityId.toUInt()))
}

/** Adds or replaces a TransformComponent with the given position on [entityId]. */
@CName("prism_world_add_transform_component")
fun prismWorldAddTransformComponent(
  worldHandle: Long,
  entityId: Int,
  x: Float,
  y: Float,
  z: Float,
) {
  val world = Registry.get<World>(worldHandle) ?: return
  world.addComponent(Entity(entityId.toUInt()), TransformComponent(position = Vec3(x, y, z)))
}

/** Returns the X coordinate of the TransformComponent position, or 0 if not found. */
@CName("prism_world_get_transform_x")
fun prismWorldGetTransformX(worldHandle: Long, entityId: Int): Float {
  val world = Registry.get<World>(worldHandle) ?: return 0f
  return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.x ?: 0f
}

/** Returns the Y coordinate of the TransformComponent position, or 0 if not found. */
@CName("prism_world_get_transform_y")
fun prismWorldGetTransformY(worldHandle: Long, entityId: Int): Float {
  val world = Registry.get<World>(worldHandle) ?: return 0f
  return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.y ?: 0f
}

/** Returns the Z coordinate of the TransformComponent position, or 0 if not found. */
@CName("prism_world_get_transform_z")
fun prismWorldGetTransformZ(worldHandle: Long, entityId: Int): Float {
  val world = Registry.get<World>(worldHandle) ?: return 0f
  return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.z ?: 0f
}

/** Shuts down the world and releases its handle. */
@CName("prism_destroy_world")
fun prismDestroyWorld(worldHandle: Long) {
  Registry.get<World>(worldHandle)?.shutdown()
  Registry.remove(worldHandle)
}

// ---------------------------------------------------------------------------
// Scene API
// ---------------------------------------------------------------------------

/** Creates a Scene and returns its handle. */
@CName("prism_create_scene")
fun prismCreateScene(name: CPointer<ByteVar>?): Long =
  Registry.put(Scene(name?.toKString() ?: "Scene"))

/** Advances the scene by [deltaTime] seconds (updates all nodes). */
@CName("prism_scene_update")
fun prismSceneUpdate(handle: Long, deltaTime: Float) {
  Registry.get<Scene>(handle)?.update(deltaTime)
}

/** Destroys a scene handle. */
@CName("prism_destroy_scene")
fun prismDestroyScene(handle: Long) {
  Registry.remove(handle)
}

/** Creates a plain Node and returns its handle. */
@CName("prism_create_node")
fun prismCreateNode(name: CPointer<ByteVar>?): Long = Registry.put(Node(name?.toKString() ?: "Node"))

/** Creates a MeshNode and returns its handle. */
@CName("prism_create_mesh_node")
fun prismCreateMesh_node(name: CPointer<ByteVar>?): Long =
  Registry.put(MeshNode(name?.toKString() ?: "MeshNode"))

/** Creates a CameraNode and returns its handle. */
@CName("prism_create_camera_node")
fun prismCreateCameraNode(name: CPointer<ByteVar>?): Long =
  Registry.put(CameraNode(name?.toKString() ?: "CameraNode"))

/** Creates a LightNode and returns its handle. */
@CName("prism_create_light_node")
fun prismCreateLightNode(name: CPointer<ByteVar>?): Long =
  Registry.put(LightNode(name?.toKString() ?: "LightNode"))

/** Adds a node as a direct child of the scene root. */
@CName("prism_scene_add_node")
fun prismSceneAddNode(sceneHandle: Long, nodeHandle: Long) {
  val scene = Registry.get<Scene>(sceneHandle) ?: return
  val node = Registry.get<Node>(nodeHandle) ?: return
  scene.addNode(node)
}

/** Sets the world-space position of a node. */
@CName("prism_node_set_position")
fun prismNodeSetPosition(handle: Long, x: Float, y: Float, z: Float) {
  val node = Registry.get<Node>(handle) ?: return
  node.transform = node.transform.copy(position = Vec3(x, y, z))
}

/** Sets the rotation of a node from a quaternion (x, y, z, w). */
@CName("prism_node_set_rotation")
fun prismNodeSetRotation(handle: Long, x: Float, y: Float, z: Float, w: Float) {
  val node = Registry.get<Node>(handle) ?: return
  node.transform = node.transform.copy(rotation = Quaternion(x, y, z, w))
}

/** Sets the uniform scale of a node. */
@CName("prism_node_set_scale")
fun prismNodeSetScale(handle: Long, x: Float, y: Float, z: Float) {
  val node = Registry.get<Node>(handle) ?: return
  node.transform = node.transform.copy(scale = Vec3(x, y, z))
}

/** Sets the active camera for rendering. [cameraNodeHandle] must refer to a CameraNode. */
@CName("prism_scene_set_active_camera")
fun prismSceneSetActiveCamera(sceneHandle: Long, cameraNodeHandle: Long) {
  val scene = Registry.get<Scene>(sceneHandle) ?: return
  scene.activeCamera = Registry.get<CameraNode>(cameraNodeHandle)
}

/** Destroys a node handle. */
@CName("prism_destroy_node")
fun prismDestroyNode(handle: Long) {
  Registry.remove(handle)
}
