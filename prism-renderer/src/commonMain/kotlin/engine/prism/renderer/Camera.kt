package engine.prism.renderer

import engine.prism.math.Mat4
import engine.prism.math.Vec3
import kotlin.math.PI

/**
 * A camera that produces view and projection matrices for rendering.
 *
 * Supports both perspective and orthographic projection modes.
 * Uses a right-handed coordinate system consistent with the engine's math module.
 */
class Camera {

    /** World-space position of the camera. */
    var position: Vec3 = Vec3(0f, 0f, 5f)

    /** World-space point the camera is looking at. */
    var target: Vec3 = Vec3.ZERO

    /** World-space up direction. */
    var up: Vec3 = Vec3.UP

    /** Vertical field of view in degrees (used for perspective projection). */
    var fovY: Float = 60f

    /** Width-to-height aspect ratio of the viewport. */
    var aspectRatio: Float = 16f / 9f

    /** Distance to the near clipping plane. Must be > 0. */
    var nearPlane: Float = 0.1f

    /** Distance to the far clipping plane. Must be > [nearPlane]. */
    var farPlane: Float = 1000f

    /** When true, uses orthographic projection instead of perspective. */
    var isOrthographic: Boolean = false

    /** Half-height of the orthographic view volume (in world units). */
    var orthoSize: Float = 10f

    /**
     * Computes the view matrix (world-to-camera transform) using a right-handed look-at.
     */
    fun viewMatrix(): Mat4 = Mat4.lookAt(position, target, up)

    /**
     * Computes the projection matrix based on the current projection mode.
     *
     * - Perspective: uses [fovY] (converted to radians), [aspectRatio], [nearPlane], [farPlane].
     * - Orthographic: uses [orthoSize], [aspectRatio], [nearPlane], [farPlane].
     */
    fun projectionMatrix(): Mat4 {
        return if (isOrthographic) {
            val halfHeight = orthoSize
            val halfWidth = halfHeight * aspectRatio
            Mat4.orthographic(
                left = -halfWidth,
                right = halfWidth,
                bottom = -halfHeight,
                top = halfHeight,
                near = nearPlane,
                far = farPlane,
            )
        } else {
            val fovRadians = fovY * (PI.toFloat() / 180f)
            Mat4.perspective(
                fovY = fovRadians,
                aspect = aspectRatio,
                near = nearPlane,
                far = farPlane,
            )
        }
    }

    /**
     * Computes the combined view-projection matrix: projection * view.
     */
    fun viewProjectionMatrix(): Mat4 = projectionMatrix() * viewMatrix()

    override fun toString(): String {
        val mode = if (isOrthographic) "ortho(size=$orthoSize)" else "perspective(fov=$fovY)"
        return "Camera(pos=$position, target=$target, $mode, aspect=$aspectRatio, near=$nearPlane, far=$farPlane)"
    }
}
