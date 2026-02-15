package com.hyeonslab.prism.demo

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.scene.CameraNode
import com.hyeonslab.prism.scene.LightNode
import com.hyeonslab.prism.scene.LightType
import com.hyeonslab.prism.scene.MeshNode
import com.hyeonslab.prism.scene.Scene

class DemoApp {
  val config = EngineConfig(appName = "Prism Demo", targetFps = 60, enableDebug = true)
  val engine = Engine(config)
  val scene = Scene("Demo Scene")

  private var rotationAngle = 0f

  fun setup() {
    engine.initialize()

    val cameraNode =
      CameraNode(
        name = "MainCamera",
        camera =
          Camera().apply {
            position = Vec3(0f, 2f, 5f)
            target = Vec3.ZERO
            fovY = 60f
            nearPlane = 0.1f
            farPlane = 100f
          },
      )
    scene.activeCamera = cameraNode
    scene.addNode(cameraNode)

    val lightNode =
      LightNode(
        name = "SunLight",
        lightType = LightType.DIRECTIONAL,
        color = Color.WHITE,
        intensity = 1f,
        direction = Vec3(-0.5f, -1f, -0.3f),
      )
    scene.addNode(lightNode)

    val cubeNode = MeshNode(name = "Cube", mesh = Mesh.cube())
    scene.addNode(cubeNode)
  }

  fun update(time: Time) {
    rotationAngle += time.deltaTime * 45f
    val cube = scene.findNode("Cube")
    cube?.transform =
      cube.transform.copy(
        rotation = Quaternion.fromEuler(0f, MathUtils.toRadians(rotationAngle), 0f)
      )
    scene.update(time.deltaTime)
  }

  fun shutdown() {
    engine.shutdown()
  }
}
