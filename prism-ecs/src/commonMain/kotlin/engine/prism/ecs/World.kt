package engine.prism.ecs

import engine.prism.core.Time

class World {
    private var nextId: UInt = 1u
    @PublishedApi
    internal val entities: MutableMap<Entity, MutableMap<String, Component>> = mutableMapOf()
    private val systems: MutableList<System> = mutableListOf()

    val entityCount: Int get() = entities.size

    fun createEntity(): Entity {
        val entity = Entity(nextId++)
        entities[entity] = mutableMapOf()
        return entity
    }

    fun destroyEntity(entity: Entity) {
        entities.remove(entity)
    }

    fun <T : Component> addComponent(entity: Entity, component: T) {
        val key = component::class.simpleName ?: component::class.toString()
        entities[entity]?.put(key, component)
    }

    inline fun <reified T : Component> getComponent(entity: Entity): T? {
        val key = T::class.simpleName ?: T::class.toString()
        return entities[entity]?.get(key) as? T
    }

    inline fun <reified T : Component> hasComponent(entity: Entity): Boolean {
        val key = T::class.simpleName ?: T::class.toString()
        return entities[entity]?.containsKey(key) == true
    }

    fun <T : Component> removeComponent(entity: Entity, componentClass: String) {
        entities[entity]?.remove(componentClass)
    }

    inline fun <reified T : Component> query(): List<Pair<Entity, T>> {
        val key = T::class.simpleName ?: T::class.toString()
        val result = mutableListOf<Pair<Entity, T>>()
        for ((entity, components) in entities) {
            val component = components[key] as? T
            if (component != null) {
                result.add(entity to component)
            }
        }
        return result
    }

    fun query2(class1: String, class2: String): List<Entity> {
        val result = mutableListOf<Entity>()
        for ((entity, components) in entities) {
            if (components.containsKey(class1) && components.containsKey(class2)) {
                result.add(entity)
            }
        }
        return result
    }

    fun addSystem(system: System) {
        systems.add(system)
        systems.sortBy { it.priority }
    }

    fun removeSystem(system: System) {
        systems.remove(system)
    }

    fun initialize() {
        for (system in systems) {
            system.initialize(this)
        }
    }

    fun update(time: Time) {
        for (system in systems) {
            system.update(this, time)
        }
    }

    fun shutdown() {
        for (system in systems) {
            system.shutdown()
        }
    }
}
