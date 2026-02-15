package com.hyeonslab.prism.demo

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.scene.CameraNode
import com.hyeonslab.prism.scene.LightNode
import com.hyeonslab.prism.scene.LightType
import com.hyeonslab.prism.scene.MeshNode
import com.hyeonslab.prism.scene.Scene
import com.hyeonslab.prism.widget.PrismSurface

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
