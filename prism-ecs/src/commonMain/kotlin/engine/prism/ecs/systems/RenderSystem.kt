package engine.prism.ecs.systems

import engine.prism.core.Time
import engine.prism.ecs.System
import engine.prism.ecs.World
import engine.prism.ecs.components.MeshComponent
import engine.prism.ecs.components.TransformComponent
import engine.prism.ecs.components.MaterialComponent
import engine.prism.renderer.Renderer

class RenderSystem(private val renderer: Renderer) : System {
    override val name: String = "RenderSystem"
    override val priority: Int = 100

    override fun update(world: World, time: Time) {
        // Query all entities with transform + mesh components and render them
        // This is a stub — actual rendering will be connected when wgpu4k is integrated
    }
}
