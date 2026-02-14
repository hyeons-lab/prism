package engine.prism.math

/**
 * Represents a 3D transformation consisting of position, rotation, and scale.
 *
 * Transformations are applied in the order: Scale -> Rotate -> Translate (SRT).
 */
data class Transform(
    val position: Vec3 = Vec3.ZERO,
    val rotation: Quaternion = Quaternion.identity(),
    val scale: Vec3 = Vec3.ONE
) {

    /**
     * Constructs the model (local-to-world) matrix: T * R * S
     */
    fun toModelMatrix(): Mat4 {
        val t = Mat4.translation(position)
        val r = rotation.toMat4()
        val s = Mat4.scale(scale)
        return t * r * s
    }

    /** The forward direction vector (negative Z axis rotated by this transform's rotation). */
    val forward: Vec3
        get() = rotation.rotateVec3(Vec3.FORWARD)

    /** The right direction vector (positive X axis rotated by this transform's rotation). */
    val right: Vec3
        get() = rotation.rotateVec3(Vec3.RIGHT)

    /** The up direction vector (positive Y axis rotated by this transform's rotation). */
    val up: Vec3
        get() = rotation.rotateVec3(Vec3.UP)

    /**
     * Returns a new Transform translated by the given delta.
     */
    fun translate(delta: Vec3): Transform = copy(position = position + delta)

    /**
     * Returns a new Transform with an additional rotation applied.
     * The new rotation is `quaternion * this.rotation` so that the new rotation
     * is applied after the existing one in world space.
     */
    fun rotate(quaternion: Quaternion): Transform = copy(rotation = (quaternion * rotation).normalize())

    /**
     * Returns a new Transform with scale multiplied component-wise by the given factor.
     */
    fun scaledBy(factor: Vec3): Transform = copy(scale = scale * factor)
}
