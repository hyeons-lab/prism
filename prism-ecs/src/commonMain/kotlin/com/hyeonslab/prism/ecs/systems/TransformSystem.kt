package com.hyeonslab.prism.ecs.systems

import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.System
import com.hyeonslab.prism.ecs.World

class TransformSystem : System {
  override val name: String = "TransformSystem"
  override val priority: Int = 0

  override fun update(world: World, time: Time) {
    // Transform system processes transform hierarchy updates
    // Currently a no-op — will compute world transforms from local transforms
  }
}
