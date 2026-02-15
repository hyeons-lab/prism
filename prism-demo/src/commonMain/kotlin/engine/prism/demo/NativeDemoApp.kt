package engine.prism.demo

import engine.prism.core.Engine
import engine.prism.core.EngineConfig
import engine.prism.math.Vec3
import engine.prism.renderer.Camera
import engine.prism.renderer.Color
import engine.prism.renderer.Mesh
import engine.prism.scene.CameraNode
import engine.prism.scene.LightNode
import engine.prism.scene.LightType
import engine.prism.scene.MeshNode
import engine.prism.scene.Scene
import engine.prism.widget.PrismSurface

/**
 * Demo using native widgets directly without Compose. This demonstrates how to integrate the engine
 * into a platform's native UI framework (Android XML, UIKit, SwiftUI, etc.)
 */
class NativeDemoApp {
  val engine = Engine(EngineConfig(appName = "Prism Native Demo"))
  val scene = Scene("Native Demo Scene")
  val surface = PrismSurface()

  fun start(width: Int, height: Int) {
    engine.initialize()
    surface.attach(engine)
    surface.resize(width, height)

    val camera =
      CameraNode(
        name = "Camera",
        camera =
          Camera().apply {
            position = Vec3(0f, 3f, 6f)
            target = Vec3.ZERO
          },
      )
    scene.activeCamera = camera
    scene.addNode(camera)

    scene.addNode(
      LightNode(name = "Light", lightType = LightType.POINT, color = Color.WHITE, intensity = 2f)
        .apply { transform = transform.copy(position = Vec3(3f, 5f, 3f)) }
    )

    scene.addNode(MeshNode(name = "Cube", mesh = Mesh.cube()))
  }

  fun stop() {
    surface.detach()
    engine.shutdown()
  }
}
