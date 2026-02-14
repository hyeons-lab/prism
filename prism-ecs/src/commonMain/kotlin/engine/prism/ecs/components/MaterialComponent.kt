package engine.prism.ecs.components

import engine.prism.ecs.Component
import engine.prism.renderer.Material

data class MaterialComponent(
    var material: Material? = null,
) : Component
