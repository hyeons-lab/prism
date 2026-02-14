package engine.prism.demo

import engine.prism.core.Engine
import engine.prism.core.EngineConfig
import engine.prism.core.Time
import engine.prism.math.Quaternion
import engine.prism.math.Vec3
import engine.prism.renderer.Camera
import engine.prism.renderer.Color
import engine.prism.renderer.Mesh
import engine.prism.scene.CameraNode
import engine.prism.scene.LightNode
import engine.prism.scene.LightType
import engine.prism.scene.MeshNode
import engine.prism.scene.Scene

class DemoApp {
    val config = EngineConfig(
        appName = "Prism Demo",
        targetFps = 60,
        enableDebug = true,
    )
    val engine = Engine(config)
    val scene = Scene("Demo Scene")

    private var rotationAngle = 0f

    fun setup() {
        engine.initialize()

        val cameraNode = CameraNode(
            name = "MainCamera",
            camera = Camera().apply {
                position = Vec3(0f, 2f, 5f)
                target = Vec3.ZERO
                fovY = 60f
                nearPlane = 0.1f
                farPlane = 100f
            },
        )
        scene.activeCamera = cameraNode
        scene.addNode(cameraNode)

        val lightNode = LightNode(
            name = "SunLight",
            lightType = LightType.DIRECTIONAL,
            color = Color.WHITE,
            intensity = 1f,
            direction = Vec3(-0.5f, -1f, -0.3f),
        )
        scene.addNode(lightNode)

        val cubeNode = MeshNode(
            name = "Cube",
            mesh = Mesh.cube(),
        )
        scene.addNode(cubeNode)
    }

    fun update(time: Time) {
        rotationAngle += time.deltaTime * 45f
        val cube = scene.findNode("Cube")
        cube?.transform = cube.transform.copy(
            rotation = Quaternion.fromEuler(0f, engine.prism.math.MathUtils.toRadians(rotationAngle), 0f),
        )
        scene.update(time.deltaTime)
    }

    fun shutdown() {
        engine.shutdown()
    }
}
