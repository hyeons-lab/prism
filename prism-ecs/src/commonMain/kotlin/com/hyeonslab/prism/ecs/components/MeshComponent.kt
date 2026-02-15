package com.hyeonslab.prism.ecs.components

import com.hyeonslab.prism.ecs.Component
import com.hyeonslab.prism.renderer.Mesh

data class MeshComponent(var mesh: Mesh? = null) : Component
