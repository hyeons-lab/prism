package engine.prism.ecs.components

import engine.prism.ecs.Component
import engine.prism.renderer.Camera

data class CameraComponent(var camera: Camera = Camera()) : Component
