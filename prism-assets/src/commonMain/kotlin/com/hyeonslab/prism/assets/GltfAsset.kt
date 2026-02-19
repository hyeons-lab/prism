package com.hyeonslab.prism.assets

import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Transform
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.Texture

/**
 * A renderable node extracted from a glTF scene, flattened to world space.
 *
 * Each instance corresponds to one glTF mesh primitive (a single draw call).
 */
data class GltfNodeData(
  val name: String?,
  val worldTransform: Transform,
  val mesh: Mesh,
  val material: Material?,
)

/**
 * The result of loading a glTF 2.0 (.gltf / .glb) file.
 *
 * Contains pre-built Prism [Mesh] and [Material] objects ready for GPU upload, along with raw
 * [ImageData] for each texture that can be uploaded via [com.hyeonslab.prism.renderer.Renderer].
 *
 * Call [instantiateInWorld] to populate an ECS [World] with entities for each renderable node.
 */
class GltfAsset(
  /** All GPU textures referenced by the asset's materials. */
  val textures: List<Texture>,
  /**
   * Raw pixel data (RGBA8) for each texture, parallel to [textures]. Each entry is null if the
   * corresponding image failed to decode. Multiple textures may share the same source image — this
   * list is built per-texture (not per-image) to keep indexing consistent.
   */
  val imageData: List<ImageData?>,
  /** Renderable nodes from the default scene, with pre-multiplied world-space transforms. */
  val renderableNodes: List<GltfNodeData>,
) {
  /**
   * Creates ECS entities for every renderable node in the asset.
   *
   * Each entity gets [TransformComponent], [MeshComponent], and [MaterialComponent].
   *
   * @return List of created entities.
   */
  fun instantiateInWorld(world: World): List<Entity> =
    renderableNodes.map { node ->
      val entity = world.createEntity()
      world.addComponent(
        entity,
        TransformComponent(
          position = node.worldTransform.position,
          rotation = node.worldTransform.rotation,
          scale = node.worldTransform.scale,
        ),
      )
      world.addComponent(entity, MeshComponent(mesh = node.mesh))
      val material = node.material ?: Material(baseColor = Color.WHITE, label = "default")
      world.addComponent(entity, MaterialComponent(material = material))
      entity
    }
}
