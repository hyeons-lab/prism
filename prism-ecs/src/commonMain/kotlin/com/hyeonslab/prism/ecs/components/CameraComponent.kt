package com.hyeonslab.prism.ecs.components

import com.hyeonslab.prism.ecs.Component
import com.hyeonslab.prism.renderer.Camera
data class CameraComponent(var camera: Camera = Camera()) : Component
