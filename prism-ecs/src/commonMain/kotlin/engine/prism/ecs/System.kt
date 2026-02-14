package engine.prism.ecs

import engine.prism.core.Time

interface System {
    val name: String
    val priority: Int get() = 0
    fun initialize(world: World) {}
    fun update(world: World, time: Time)
    fun shutdown() {}
}
