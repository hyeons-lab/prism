@file:OptIn(ExperimentalJsExport::class)

package engine.prism.js

import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Vec3
import kotlin.js.JsExport

/** Creates an ECS World and returns its handle. */
@JsExport
fun prismCreateWorld(): String = Registry.put(World())

/** Creates a new entity in the world. Returns its integer ID, or -1 if the handle is invalid. */
@JsExport
fun prismWorldCreateEntity(worldHandle: String): Int {
    val world = Registry.get<World>(worldHandle) ?: return -1
    return world.createEntity().id.toInt()
}

/** Destroys an entity by its integer ID. */
@JsExport
fun prismWorldDestroyEntity(worldHandle: String, entityId: Int) {
    val world = Registry.get<World>(worldHandle) ?: return
    world.destroyEntity(Entity(entityId.toUInt()))
}

/** Adds or replaces a TransformComponent with the given position on [entityId]. */
@JsExport
fun prismWorldAddTransformComponent(
    worldHandle: String,
    entityId: Int,
    x: Float,
    y: Float,
    z: Float,
) {
    val world = Registry.get<World>(worldHandle) ?: return
    world.addComponent(Entity(entityId.toUInt()), TransformComponent(position = Vec3(x, y, z)))
}

/** Returns the X coordinate of the TransformComponent position, or 0 if not found. */
@JsExport
fun prismWorldGetTransformX(worldHandle: String, entityId: Int): Float {
    val world = Registry.get<World>(worldHandle) ?: return 0f
    return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.x ?: 0f
}

/** Returns the Y coordinate of the TransformComponent position, or 0 if not found. */
@JsExport
fun prismWorldGetTransformY(worldHandle: String, entityId: Int): Float {
    val world = Registry.get<World>(worldHandle) ?: return 0f
    return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.y ?: 0f
}

/** Returns the Z coordinate of the TransformComponent position, or 0 if not found. */
@JsExport
fun prismWorldGetTransformZ(worldHandle: String, entityId: Int): Float {
    val world = Registry.get<World>(worldHandle) ?: return 0f
    return world.getComponent<TransformComponent>(Entity(entityId.toUInt()))?.position?.z ?: 0f
}

/** Shuts down the world and releases its handle. */
@JsExport
fun prismDestroyWorld(worldHandle: String) {
    Registry.get<World>(worldHandle)?.shutdown()
    Registry.remove(worldHandle)
}
