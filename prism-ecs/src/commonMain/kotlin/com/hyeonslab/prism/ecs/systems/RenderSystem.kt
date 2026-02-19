package com.hyeonslab.prism.ecs.systems

import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.System
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.LightComponent
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.LightData
import com.hyeonslab.prism.renderer.LightType
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.RenderPassDescriptor
import com.hyeonslab.prism.renderer.Renderer

/**
 * PBR render system that queries light and mesh entities each frame.
 *
 * The renderer's internal PBR pipeline is auto-bound during [Renderer.beginRenderPass]. This system
 * collects lights, sets camera/material uniforms, and issues draw calls.
 */
class RenderSystem(private val renderer: Renderer) : System {
  override val name: String = "RenderSystem"
  override val priority: Int = 100

  override fun initialize(world: World) {
    // PBR pipeline is created by the renderer in initialize().
    // No shader/pipeline creation needed here.
  }

  override fun update(world: World, time: Time) {
    renderer.beginFrame()
    renderer.beginRenderPass(RenderPassDescriptor())

    // Set camera from the first CameraComponent found
    val cameras = world.query<CameraComponent>()
    if (cameras.isNotEmpty()) {
      val cam = cameras.first().second.camera
      renderer.setCamera(cam)
    }

    // Collect lights from LightComponent entities
    val lightEntities = world.query<LightComponent>()
    val lightDataList =
      lightEntities.map { (entity, lightComp) ->
        val transform = world.getComponent<TransformComponent>(entity)
        val position = transform?.position ?: com.hyeonslab.prism.math.Vec3.ZERO
        LightData(
          type =
            when (lightComp.lightType) {
              com.hyeonslab.prism.ecs.components.LightType.DIRECTIONAL -> LightType.DIRECTIONAL
              com.hyeonslab.prism.ecs.components.LightType.POINT -> LightType.POINT
              com.hyeonslab.prism.ecs.components.LightType.SPOT -> LightType.SPOT
            },
          position = position,
          direction = lightComp.direction,
          color = lightComp.color,
          intensity = lightComp.intensity,
          range = lightComp.range,
          spotAngle = lightComp.spotAngle,
          innerAngle = lightComp.innerAngle,
        )
      }
    renderer.setLights(lightDataList)

    // Render all entities with a MeshComponent
    val meshEntities = world.query<MeshComponent>()
    for ((entity, meshComp) in meshEntities) {
      val mesh = meshComp.mesh ?: continue

      // Lazy-upload mesh to GPU if not yet uploaded
      if (mesh.vertexBuffer == null) {
        renderer.uploadMesh(mesh)
      }

      // Get model matrix from TransformComponent
      val transform = world.getComponent<TransformComponent>(entity)
      val modelMatrix = transform?.toTransform()?.toModelMatrix() ?: Mat4.identity()

      // Set material (PBR or default)
      val matComp = world.getComponent<MaterialComponent>(entity)
      val material = matComp?.material ?: Material(baseColor = Color.WHITE)
      renderer.setMaterial(material)

      renderer.drawMesh(mesh, modelMatrix)
    }

    renderer.endRenderPass()
    renderer.endFrame()
  }
}
