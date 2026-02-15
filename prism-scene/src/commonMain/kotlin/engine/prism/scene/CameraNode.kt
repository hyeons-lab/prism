package engine.prism.scene

import engine.prism.renderer.Camera

class CameraNode(name: String = "CameraNode", val camera: Camera = Camera()) : Node(name) {

  override fun update(deltaTime: Float) {
    camera.position = transform.position
    val forward = transform.rotation.rotateVec3(engine.prism.math.Vec3.FORWARD)
    camera.target = transform.position + forward
    camera.up = transform.rotation.rotateVec3(engine.prism.math.Vec3.UP)
    super.update(deltaTime)
  }
}
