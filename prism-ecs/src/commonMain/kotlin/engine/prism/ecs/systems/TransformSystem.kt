package engine.prism.ecs.systems

import engine.prism.core.Time
import engine.prism.ecs.System
import engine.prism.ecs.World

class TransformSystem : System {
    override val name: String = "TransformSystem"
    override val priority: Int = 0

    override fun update(world: World, time: Time) {
        // Transform system processes transform hierarchy updates
        // Currently a no-op — will compute world transforms from local transforms
    }
}
