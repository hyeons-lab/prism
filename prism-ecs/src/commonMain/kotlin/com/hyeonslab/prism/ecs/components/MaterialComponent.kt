package com.hyeonslab.prism.ecs.components

import com.hyeonslab.prism.ecs.Component
import com.hyeonslab.prism.renderer.Material

data class MaterialComponent(var material: Material? = null) : Component
