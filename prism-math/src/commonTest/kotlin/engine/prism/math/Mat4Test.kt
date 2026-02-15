package engine.prism.math

import kotlin.math.PI
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertTrue

class Mat4Test {

  private val epsilon = 1e-4f

  private fun Float.shouldBeApprox(expected: Float) {
    assertTrue(
      MathUtils.approximately(this, expected, epsilon),
      "Expected $expected but got $this (epsilon=$epsilon)",
    )
  }

  private fun Mat4.shouldBeApprox(expected: Mat4) {
    for (i in 0..15) {
      data[i].shouldBeApprox(expected.data[i])
    }
  }

  private fun Vec4.shouldBeApprox(expected: Vec4) {
    x.shouldBeApprox(expected.x)
    y.shouldBeApprox(expected.y)
    z.shouldBeApprox(expected.z)
    w.shouldBeApprox(expected.w)
  }

  // --- Identity ---

  @Test
  fun identityMatrixDiagonal() {
    val id = Mat4.identity()
    for (i in 0..3) {
      for (j in 0..3) {
        id[i, j].shouldBeApprox(if (i == j) 1f else 0f)
      }
    }
  }

  @Test
  fun identityTimesMatrixEqualsMatrix() {
    val id = Mat4.identity()
    val m = Mat4.translation(Vec3(3f, 4f, 5f))
    (id * m).shouldBeApprox(m)
  }

  @Test
  fun matrixTimesIdentityEqualsMatrix() {
    val id = Mat4.identity()
    val m = Mat4.scale(Vec3(2f, 3f, 4f))
    (m * id).shouldBeApprox(m)
  }

  // --- Translation ---

  @Test
  fun translationMatrix() {
    val t = Mat4.translation(Vec3(10f, 20f, 30f))
    // Translating the origin point
    val result = t * Vec4(0f, 0f, 0f, 1f)
    result.shouldBeApprox(Vec4(10f, 20f, 30f, 1f))
  }

  @Test
  fun translationDoesNotAffectDirections() {
    val t = Mat4.translation(Vec3(10f, 20f, 30f))
    // Directions have w=0, unaffected by translation
    val result = t * Vec4(1f, 0f, 0f, 0f)
    result.shouldBeApprox(Vec4(1f, 0f, 0f, 0f))
  }

  @Test
  fun translationTranslatesPoint() {
    val t = Mat4.translation(Vec3(5f, -3f, 2f))
    val point = Vec4(1f, 2f, 3f, 1f)
    val result = t * point
    result.shouldBeApprox(Vec4(6f, -1f, 5f, 1f))
  }

  // --- Perspective ---

  @Test
  fun perspectiveProjectionBasic() {
    val fov = (PI / 4.0).toFloat() // 45 degrees
    val aspect = 16f / 9f
    val near = 0.1f
    val far = 100f

    val p = Mat4.perspective(fov, aspect, near, far)

    // Check that the projection matrix has the correct structure
    val tanHalfFov = tan(fov / 2f)

    p[0, 0].shouldBeApprox(1f / (aspect * tanHalfFov))
    p[1, 1].shouldBeApprox(1f / tanHalfFov)
    p[3, 2].shouldBeApprox(-1f) // perspective divide marker
    p[3, 3].shouldBeApprox(0f)
  }

  @Test
  fun perspectiveMapsNearPlaneToNegativeOne() {
    val fov = (PI / 4.0).toFloat()
    val aspect = 1f
    val near = 1f
    val far = 100f

    val p = Mat4.perspective(fov, aspect, near, far)

    // A point on the near plane along the -Z axis (in view space)
    val nearPoint = p * Vec4(0f, 0f, -near, 1f)
    // After perspective divide, z should map to -1
    val ndcZ = nearPoint.z / nearPoint.w
    ndcZ.shouldBeApprox(-1f)
  }

  @Test
  fun perspectiveMapsFarPlaneToOne() {
    val fov = (PI / 4.0).toFloat()
    val aspect = 1f
    val near = 1f
    val far = 100f

    val p = Mat4.perspective(fov, aspect, near, far)

    // A point on the far plane along the -Z axis
    val farPoint = p * Vec4(0f, 0f, -far, 1f)
    val ndcZ = farPoint.z / farPoint.w
    ndcZ.shouldBeApprox(1f)
  }

  // --- Multiplication ---

  @Test
  fun multiplicationAssociativity() {
    val a = Mat4.rotationX(0.5f)
    val b = Mat4.translation(Vec3(1f, 2f, 3f))
    val c = Mat4.scale(Vec3(2f, 2f, 2f))

    val ab_c = (a * b) * c
    val a_bc = a * (b * c)

    ab_c.shouldBeApprox(a_bc)
  }

  @Test
  fun translationThenScale() {
    // Scale * Translation: scale the translation offsets
    val t = Mat4.translation(Vec3(1f, 0f, 0f))
    val s = Mat4.scale(Vec3(2f, 2f, 2f))

    val st = s * t
    val result = st * Vec4(0f, 0f, 0f, 1f)
    result.shouldBeApprox(Vec4(2f, 0f, 0f, 1f))
  }

  @Test
  fun multiplyVec4() {
    val s = Mat4.scale(Vec3(3f, 4f, 5f))
    val v = Vec4(1f, 1f, 1f, 1f)

    val result = s * v
    result.shouldBeApprox(Vec4(3f, 4f, 5f, 1f))
  }

  // --- Inverse ---

  @Test
  fun inverseOfIdentityIsIdentity() {
    val id = Mat4.identity()
    id.inverse().shouldBeApprox(id)
  }

  @Test
  fun inverseOfTranslation() {
    val t = Mat4.translation(Vec3(5f, -3f, 7f))
    val inv = t.inverse()
    val product = t * inv

    product.shouldBeApprox(Mat4.identity())
  }

  @Test
  fun inverseOfScale() {
    val s = Mat4.scale(Vec3(2f, 3f, 4f))
    val inv = s.inverse()
    val product = s * inv

    product.shouldBeApprox(Mat4.identity())
  }

  @Test
  fun inverseOfRotation() {
    val r = Mat4.rotationY(1.2f)
    val inv = r.inverse()
    val product = r * inv

    product.shouldBeApprox(Mat4.identity())
  }

  @Test
  fun inverseOfCompositeTransform() {
    val m = Mat4.translation(Vec3(3f, 4f, 5f)) * Mat4.rotationZ(0.7f) * Mat4.scale(Vec3(2f, 2f, 2f))
    val inv = m.inverse()
    val product = m * inv

    product.shouldBeApprox(Mat4.identity())
  }

  // --- Determinant ---

  @Test
  fun determinantOfIdentityIsOne() {
    Mat4.identity().determinant().shouldBeApprox(1f)
  }

  @Test
  fun determinantOfScale() {
    val s = Mat4.scale(Vec3(2f, 3f, 4f))
    // det of a scale matrix is the product of the scale factors
    s.determinant().shouldBeApprox(24f)
  }

  @Test
  fun determinantOfRotationIsOne() {
    val r = Mat4.rotationX(1.5f)
    r.determinant().shouldBeApprox(1f)
  }

  // --- Transpose ---

  @Test
  fun transposeOfIdentityIsIdentity() {
    Mat4.identity().transpose().shouldBeApprox(Mat4.identity())
  }

  @Test
  fun doubleTransposeIsOriginal() {
    val m = Mat4.translation(Vec3(1f, 2f, 3f))
    m.transpose().transpose().shouldBeApprox(m)
  }

  // --- LookAt ---

  @Test
  fun lookAtFromOriginAlongNegZ() {
    val view = Mat4.lookAt(eye = Vec3.ZERO, target = Vec3(0f, 0f, -1f), up = Vec3.UP)
    // Should be approximately identity (looking down -Z from origin with Y up)
    view.shouldBeApprox(Mat4.identity())
  }

  // --- Orthographic ---

  @Test
  fun orthographicMapsCorners() {
    val ortho = Mat4.orthographic(-1f, 1f, -1f, 1f, 0.1f, 100f)
    // A point at the near plane center
    val nearCenter = ortho * Vec4(0f, 0f, -0.1f, 1f)
    nearCenter.x.shouldBeApprox(0f)
    nearCenter.y.shouldBeApprox(0f)
    nearCenter.z.shouldBeApprox(-1f)
  }
}
