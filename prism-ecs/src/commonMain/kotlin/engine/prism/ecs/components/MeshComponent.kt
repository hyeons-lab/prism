package engine.prism.ecs.components

import engine.prism.ecs.Component
import engine.prism.renderer.Mesh

data class MeshComponent(
    var mesh: Mesh? = null,
) : Component
