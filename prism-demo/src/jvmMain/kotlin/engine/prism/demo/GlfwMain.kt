package engine.prism.demo

import co.touchlab.kermit.Logger
import engine.prism.math.Mat4
import engine.prism.math.Vec3
import engine.prism.renderer.Camera
import engine.prism.renderer.Shaders
import ffi.LibraryLoader
import io.ygdrasil.webgpu.ArrayBuffer
import io.ygdrasil.webgpu.BindGroupDescriptor
import io.ygdrasil.webgpu.BindGroupEntry
import io.ygdrasil.webgpu.BufferBinding
import io.ygdrasil.webgpu.BufferDescriptor
import io.ygdrasil.webgpu.ColorTargetState
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.DepthStencilState
import io.ygdrasil.webgpu.Extent3D
import io.ygdrasil.webgpu.FragmentState
import io.ygdrasil.webgpu.GPUBufferUsage
import io.ygdrasil.webgpu.GPUCompareFunction
import io.ygdrasil.webgpu.GPUCullMode
import io.ygdrasil.webgpu.GPUIndexFormat
import io.ygdrasil.webgpu.GPULoadOp
import io.ygdrasil.webgpu.GPUPrimitiveTopology
import io.ygdrasil.webgpu.GPUStoreOp
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.GPUVertexFormat
import io.ygdrasil.webgpu.MultisampleState
import io.ygdrasil.webgpu.PrimitiveState
import io.ygdrasil.webgpu.RenderPassColorAttachment
import io.ygdrasil.webgpu.RenderPassDepthStencilAttachment
import io.ygdrasil.webgpu.RenderPassDescriptor
import io.ygdrasil.webgpu.RenderPipelineDescriptor
import io.ygdrasil.webgpu.ShaderModuleDescriptor
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.TextureDescriptor
import io.ygdrasil.webgpu.VertexAttribute
import io.ygdrasil.webgpu.VertexBufferLayout
import io.ygdrasil.webgpu.VertexState
import io.ygdrasil.webgpu.autoClosableContext
import io.ygdrasil.webgpu.beginRenderPass
import io.ygdrasil.webgpu.glfwContextRenderer
import kotlin.math.PI
import kotlinx.coroutines.runBlocking
import org.lwjgl.glfw.GLFW.glfwPollEvents
import org.lwjgl.glfw.GLFW.glfwShowWindow
import org.lwjgl.glfw.GLFW.glfwWindowShouldClose
import io.ygdrasil.webgpu.Color as WGPUColor

fun main() = runBlocking {
    Logger.i("Prism") { "Starting Prism GLFW Demo..." }
    LibraryLoader.load()

    val glfwContext = glfwContextRenderer(width = 800, height = 600, title = "Prism Demo")
    val wgpuContext = glfwContext.wgpuContext
    val surface = wgpuContext.surface
    val device = wgpuContext.device
    val renderingContext = wgpuContext.renderingContext

    // Configure surface
    val format = renderingContext.textureFormat
    val alphaMode =
        if (surface.supportedAlphaMode.contains(CompositeAlphaMode.Inherit)) {
            CompositeAlphaMode.Inherit
        } else {
            CompositeAlphaMode.Opaque
        }
    surface.configure(
        SurfaceConfiguration(
            device = device,
            format = format,
            usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
            alphaMode = alphaMode,
        )
    )

    // WGSL shader code (combined vertex + fragment)
    val shaderCode = "${Shaders.VERTEX_SHADER.code}\n\n${Shaders.FRAGMENT_UNLIT.code}"
    val shaderModule = device.createShaderModule(ShaderModuleDescriptor(code = shaderCode))

    // Depth texture
    val depthTexture =
        device.createTexture(
            TextureDescriptor(
                size = Extent3D(renderingContext.width, renderingContext.height),
                format = GPUTextureFormat.Depth24Plus,
                usage = GPUTextureUsage.RenderAttachment,
            )
        )
    val depthTextureView = depthTexture.createView()

    // Render pipeline
    val renderPipeline =
        device.createRenderPipeline(
            RenderPipelineDescriptor(
                vertex =
                    VertexState(
                        entryPoint = "vs_main",
                        module = shaderModule,
                        buffers =
                            listOf(
                                VertexBufferLayout(
                                    arrayStride = 32uL,
                                    attributes =
                                        listOf(
                                            VertexAttribute(
                                                shaderLocation = 0u,
                                                offset = 0uL,
                                                format = GPUVertexFormat.Float32x3,
                                            ),
                                            VertexAttribute(
                                                shaderLocation = 1u,
                                                offset = 12uL,
                                                format = GPUVertexFormat.Float32x3,
                                            ),
                                            VertexAttribute(
                                                shaderLocation = 2u,
                                                offset = 24uL,
                                                format = GPUVertexFormat.Float32x2,
                                            ),
                                        ),
                                )
                            ),
                    ),
                fragment =
                    FragmentState(
                        entryPoint = "fs_main",
                        module = shaderModule,
                        targets = listOf(ColorTargetState(format = format)),
                    ),
                primitive =
                    PrimitiveState(
                        topology = GPUPrimitiveTopology.TriangleList,
                        cullMode = GPUCullMode.Back,
                    ),
                depthStencil =
                    DepthStencilState(
                        format = GPUTextureFormat.Depth24Plus,
                        depthWriteEnabled = true,
                        depthCompare = GPUCompareFunction.Less,
                    ),
                multisample = MultisampleState(count = 1u, mask = 0xFFFFFFFu),
            )
        )

    // Uniform buffer (VP + model = 128 bytes)
    val uniformBuffer =
        device.createBuffer(
            BufferDescriptor(
                size = 128uL,
                usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
            )
        )

    // Material uniform buffer (baseColor vec4 = 16 bytes)
    val materialBuffer =
        device.createBuffer(
            BufferDescriptor(
                size = 16uL,
                usage = GPUBufferUsage.Uniform or GPUBufferUsage.CopyDst,
            )
        )

    // Write material color (blue)
    device.queue.writeBuffer(
        materialBuffer,
        0u,
        ArrayBuffer.of(floatArrayOf(0.3f, 0.5f, 0.9f, 1f)),
    )

    // Bind group
    val bindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                layout = renderPipeline.getBindGroupLayout(0u),
                entries =
                    listOf(
                        BindGroupEntry(
                            binding = 0u,
                            resource = BufferBinding(buffer = uniformBuffer),
                        ),
                        BindGroupEntry(
                            binding = 1u,
                            resource = BufferBinding(buffer = materialBuffer),
                        ),
                    ),
            )
        )

    // Cube mesh data
    val cube = engine.prism.renderer.Mesh.cube()
    val vertexBuffer =
        device.createBuffer(
            BufferDescriptor(
                size = (cube.vertices.size * Float.SIZE_BYTES).toULong(),
                usage = GPUBufferUsage.Vertex or GPUBufferUsage.CopyDst,
            )
        )
    device.queue.writeBuffer(vertexBuffer, 0u, ArrayBuffer.of(cube.vertices))

    val indexBuffer =
        device.createBuffer(
            BufferDescriptor(
                size = (cube.indices.size * Int.SIZE_BYTES).toULong(),
                usage = GPUBufferUsage.Index or GPUBufferUsage.CopyDst,
            )
        )
    device.queue.writeBuffer(indexBuffer, 0u, ArrayBuffer.of(cube.indices))

    // Camera
    val camera = Camera()
    camera.position = Vec3(2f, 2f, 4f)
    camera.target = Vec3(0f, 0f, 0f)
    camera.fovY = 60f
    camera.aspectRatio = 800f / 600f
    camera.nearPlane = 0.1f
    camera.farPlane = 100f

    glfwShowWindow(glfwContext.windowHandler)
    Logger.i("Prism") { "Window opened — entering render loop" }

    val startTime = System.nanoTime()
    val rotationSpeed = PI.toFloat() / 4f

    while (!glfwWindowShouldClose(glfwContext.windowHandler)) {
        glfwPollEvents()

        val elapsed = (System.nanoTime() - startTime) / 1_000_000_000f
        val angle = elapsed * rotationSpeed
        val modelMatrix = Mat4.rotationY(angle)

        // Write VP matrix
        val vpMatrix = camera.viewProjectionMatrix()
        device.queue.writeBuffer(uniformBuffer, 0u, ArrayBuffer.of(vpMatrix.data))
        // Write model matrix at offset 64
        device.queue.writeBuffer(uniformBuffer, 64u, ArrayBuffer.of(modelMatrix.data))

        autoClosableContext {
            val surfaceTexture = renderingContext.getCurrentTexture()
            val surfaceView = surfaceTexture.createView().bind()

            val encoder = device.createCommandEncoder().bind()

            encoder.beginRenderPass(
                RenderPassDescriptor(
                    colorAttachments =
                        listOf(
                            RenderPassColorAttachment(
                                view = surfaceView,
                                loadOp = GPULoadOp.Clear,
                                storeOp = GPUStoreOp.Store,
                                clearValue = WGPUColor(0.392, 0.584, 0.929, 1.0),
                            )
                        ),
                    depthStencilAttachment =
                        RenderPassDepthStencilAttachment(
                            view = depthTextureView,
                            depthClearValue = 1.0f,
                            depthLoadOp = GPULoadOp.Clear,
                            depthStoreOp = GPUStoreOp.Store,
                        ),
                )
            ) {
                setPipeline(renderPipeline)
                setBindGroup(0u, bindGroup)
                setVertexBuffer(0u, vertexBuffer)
                setIndexBuffer(indexBuffer, GPUIndexFormat.Uint32)
                drawIndexed(cube.indices.size.toUInt())
                end()
            }

            val commandBuffer = encoder.finish().bind()
            device.queue.submit(listOf(commandBuffer))
        }

        surface.present()
    }

    Logger.i("Prism") { "Shutting down..." }
    depthTexture.close()
    bindGroup.close()
    uniformBuffer.close()
    materialBuffer.close()
    vertexBuffer.close()
    indexBuffer.close()
    shaderModule.close()
    renderPipeline.close()
    glfwContext.close()
}
