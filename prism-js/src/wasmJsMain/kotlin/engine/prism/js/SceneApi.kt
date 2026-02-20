@file:OptIn(ExperimentalJsExport::class)

package engine.prism.js

import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.scene.CameraNode
import com.hyeonslab.prism.scene.LightNode
import com.hyeonslab.prism.scene.MeshNode
import com.hyeonslab.prism.scene.Node
import com.hyeonslab.prism.scene.Scene
import kotlin.js.JsExport

/** Creates a Scene and returns its handle. */
@JsExport
fun prismCreateScene(name: String): String = Registry.put(Scene(name))

/** Advances the scene by [deltaTime] seconds (updates all nodes). */
@JsExport
fun prismSceneUpdate(handle: String, deltaTime: Float) {
    Registry.get<Scene>(handle)?.update(deltaTime)
}

/** Destroys a scene handle. */
@JsExport
fun prismDestroyScene(handle: String) {
    Registry.remove(handle)
}

/** Creates a plain Node and returns its handle. */
@JsExport
fun prismCreateNode(name: String): String = Registry.put(Node(name))

/** Creates a MeshNode and returns its handle. */
@JsExport
fun prismCreateMeshNode(name: String): String = Registry.put(MeshNode(name))

/** Creates a CameraNode and returns its handle. */
@JsExport
fun prismCreateCameraNode(name: String): String = Registry.put(CameraNode(name))

/** Creates a LightNode and returns its handle. */
@JsExport
fun prismCreateLightNode(name: String): String = Registry.put(LightNode(name))

/** Adds a node as a direct child of the scene root. */
@JsExport
fun prismSceneAddNode(sceneHandle: String, nodeHandle: String) {
    val scene = Registry.get<Scene>(sceneHandle) ?: return
    val node = Registry.get<Node>(nodeHandle) ?: return
    scene.addNode(node)
}

/** Sets the world-space position of a node. */
@JsExport
fun prismNodeSetPosition(handle: String, x: Float, y: Float, z: Float) {
    val node = Registry.get<Node>(handle) ?: return
    node.transform = node.transform.copy(position = Vec3(x, y, z))
}

/** Sets the rotation of a node from a quaternion (x, y, z, w). */
@JsExport
fun prismNodeSetRotation(handle: String, x: Float, y: Float, z: Float, w: Float) {
    val node = Registry.get<Node>(handle) ?: return
    node.transform = node.transform.copy(rotation = Quaternion(x, y, z, w))
}

/** Sets the uniform scale of a node. */
@JsExport
fun prismNodeSetScale(handle: String, x: Float, y: Float, z: Float) {
    val node = Registry.get<Node>(handle) ?: return
    node.transform = node.transform.copy(scale = Vec3(x, y, z))
}

/** Sets the active camera for rendering. [cameraNodeHandle] must refer to a CameraNode. */
@JsExport
fun prismSceneSetActiveCamera(sceneHandle: String, cameraNodeHandle: String) {
    val scene = Registry.get<Scene>(sceneHandle) ?: return
    scene.activeCamera = Registry.get<CameraNode>(cameraNodeHandle)
}

/** Destroys a node handle. */
@JsExport
fun prismDestroyNode(handle: String) {
    Registry.remove(handle)
}
