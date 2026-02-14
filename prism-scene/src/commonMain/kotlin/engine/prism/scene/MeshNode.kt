package engine.prism.scene

import engine.prism.renderer.Material
import engine.prism.renderer.Mesh

class MeshNode(
    name: String = "MeshNode",
    var mesh: Mesh? = null,
    var material: Material? = null,
) : Node(name)
