package com.hyeonslab.prism.scene

import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh

class MeshNode(name: String = "MeshNode", var mesh: Mesh? = null, var material: Material? = null) :
  Node(name)
