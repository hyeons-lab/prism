package engine.prism.ecs.systems

import engine.prism.core.Time
import engine.prism.ecs.System
import engine.prism.ecs.World
import engine.prism.ecs.components.CameraComponent
import engine.prism.ecs.components.MaterialComponent
import engine.prism.ecs.components.MeshComponent
import engine.prism.ecs.components.TransformComponent
import engine.prism.math.Mat4
import engine.prism.renderer.PipelineDescriptor
import engine.prism.renderer.RenderPassDescriptor
import engine.prism.renderer.RenderPipeline
import engine.prism.renderer.Renderer
import engine.prism.renderer.Shaders
import engine.prism.renderer.VertexLayout

class RenderSystem(private val renderer: Renderer) : System {
    override val name: String = "RenderSystem"
    override val priority: Int = 100

    private var defaultPipeline: RenderPipeline? = null

    override fun initialize(world: World) {
        val shader =
            renderer.createShaderModule(Shaders.VERTEX_SHADER, Shaders.FRAGMENT_UNLIT, "Default")
        defaultPipeline =
            renderer.createPipeline(
                PipelineDescriptor(
                    shader = shader,
                    vertexLayout = VertexLayout.positionNormalUv(),
                    label = "Default Pipeline",
                )
            )
    }

    override fun update(world: World, time: Time) {
        val pipeline = defaultPipeline ?: return

        renderer.beginFrame()
        renderer.beginRenderPass(RenderPassDescriptor())

        // Set camera from the first CameraComponent found
        val cameras = world.query<CameraComponent>()
        if (cameras.isNotEmpty()) {
            renderer.setCamera(cameras.first().second.camera)
        }

        renderer.bindPipeline(pipeline)

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
            val modelMatrix =
                transform?.toTransform()?.toModelMatrix() ?: Mat4.identity()

            // Set material color if MaterialComponent is present
            val material = world.getComponent<MaterialComponent>(entity)
            if (material?.material != null) {
                renderer.setMaterialColor(material.material!!.baseColor)
            }

            renderer.drawMesh(mesh, modelMatrix)
        }

        renderer.endRenderPass()
        renderer.endFrame()
    }
}
