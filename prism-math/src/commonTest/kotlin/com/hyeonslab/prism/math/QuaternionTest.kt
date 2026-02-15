package com.hyeonslab.prism.math

import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.test.Test
import kotlin.test.assertTrue

class QuaternionTest {

  private val epsilon = 1e-4f

  private fun Float.shouldBeApprox(expected: Float) {
    assertTrue(
      MathUtils.approximately(this, expected, epsilon),
      "Expected $expected but got $this (epsilon=$epsilon)",
    )
  }

  private fun Vec3.shouldBeApprox(expected: Vec3) {
    x.shouldBeApprox(expected.x)
    y.shouldBeApprox(expected.y)
    z.shouldBeApprox(expected.z)
  }

  private fun Quaternion.shouldBeApprox(expected: Quaternion) {
    // Quaternions q and -q represent the same rotation, so compare both
    val sameSign =
      MathUtils.approximately(x, expected.x, epsilon) &&
        MathUtils.approximately(y, expected.y, epsilon) &&
        MathUtils.approximately(z, expected.z, epsilon) &&
        MathUtils.approximately(w, expected.w, epsilon)
    val negSign =
      MathUtils.approximately(x, -expected.x, epsilon) &&
        MathUtils.approximately(y, -expected.y, epsilon) &&
        MathUtils.approximately(z, -expected.z, epsilon) &&
        MathUtils.approximately(w, -expected.w, epsilon)
    assertTrue(sameSign || negSign, "Expected $expected (or its negation) but got $this")
  }

  private fun Mat4.shouldBeApprox(expected: Mat4) {
    for (i in 0..15) {
      data[i].shouldBeApprox(expected.data[i])
    }
  }

  // --- Identity ---

  @Test
  fun identityQuaternion() {
    val q = Quaternion.identity()
    q.x.shouldBeApprox(0f)
    q.y.shouldBeApprox(0f)
    q.z.shouldBeApprox(0f)
    q.w.shouldBeApprox(1f)
  }

  @Test
  fun identityDoesNotRotate() {
    val v = Vec3(1f, 2f, 3f)
    val result = Quaternion.identity().rotateVec3(v)
    result.shouldBeApprox(v)
  }

  @Test
  fun identityTimesQuaternion() {
    val q = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 4f)
    val result = Quaternion.identity() * q
    result.shouldBeApprox(q)
  }

  // --- fromAxisAngle ---

  @Test
  fun fromAxisAngleZeroAngle() {
    val q = Quaternion.fromAxisAngle(Vec3.UP, 0f)
    q.shouldBeApprox(Quaternion.identity())
  }

  @Test
  fun fromAxisAngle90DegreesAroundY() {
    val q = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f)
    q.length().shouldBeApprox(1f)

    // Rotating (1,0,0) by 90 degrees around Y should give (0,0,-1)
    val result = q.rotateVec3(Vec3(1f, 0f, 0f))
    result.shouldBeApprox(Vec3(0f, 0f, -1f))
  }

  @Test
  fun fromAxisAngle180DegreesAroundZ() {
    val q = Quaternion.fromAxisAngle(Vec3(0f, 0f, 1f), PI.toFloat())

    // Rotating (1,0,0) by 180 degrees around Z should give (-1,0,0)
    val result = q.rotateVec3(Vec3(1f, 0f, 0f))
    result.shouldBeApprox(Vec3(-1f, 0f, 0f))
  }

  @Test
  fun fromAxisAngle360DegreesIsIdentity() {
    val q = Quaternion.fromAxisAngle(Vec3.RIGHT, 2f * PI.toFloat())
    // 360 degree rotation should not change anything
    val v = Vec3(0f, 1f, 0f)
    val result = q.rotateVec3(v)
    result.shouldBeApprox(v)
  }

  // --- Normalize ---

  @Test
  fun normalizeUnitQuaternion() {
    val q = Quaternion.identity()
    val n = q.normalize()
    n.length().shouldBeApprox(1f)
  }

  @Test
  fun normalizeScaledQuaternion() {
    val q = Quaternion(2f, 0f, 0f, 2f)
    val n = q.normalize()
    n.length().shouldBeApprox(1f)

    val expectedLen = sqrt(8f)
    n.x.shouldBeApprox(2f / expectedLen)
    n.w.shouldBeApprox(2f / expectedLen)
  }

  @Test
  fun normalizedQuaternionPreservesRotation() {
    val q = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 3f)
    // Scale it up
    val scaled = Quaternion(q.x * 5f, q.y * 5f, q.z * 5f, q.w * 5f)
    val normalized = scaled.normalize()

    val v = Vec3(1f, 0f, 0f)
    q.rotateVec3(v).shouldBeApprox(normalized.rotateVec3(v))
  }

  // --- Slerp ---

  @Test
  fun slerpAtZero() {
    val a = Quaternion.identity()
    val b = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f)

    val result = Quaternion.slerp(a, b, 0f)
    result.shouldBeApprox(a)
  }

  @Test
  fun slerpAtOne() {
    val a = Quaternion.identity()
    val b = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f)

    val result = Quaternion.slerp(a, b, 1f)
    result.shouldBeApprox(b)
  }

  @Test
  fun slerpAtHalf() {
    val a = Quaternion.identity()
    val b = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 2f)

    val mid = Quaternion.slerp(a, b, 0.5f)
    // At t=0.5, the rotation should be 45 degrees around Y
    val expected = Quaternion.fromAxisAngle(Vec3.UP, PI.toFloat() / 4f)
    mid.shouldBeApprox(expected)
  }

  @Test
  fun slerpProducesUnitQuaternion() {
    val a = Quaternion.fromAxisAngle(Vec3(1f, 1f, 0f).normalize(), 0.5f)
    val b = Quaternion.fromAxisAngle(Vec3(0f, 1f, 1f).normalize(), 1.5f)

    for (i in 0..10) {
      val t = i / 10f
      val result = Quaternion.slerp(a, b, t)
      result.length().shouldBeApprox(1f)
    }
  }

  // --- toMat4 ---

  @Test
  fun identityQuaternionToMat4() {
    val mat = Quaternion.identity().toMat4()
    mat.shouldBeApprox(Mat4.identity())
  }

  @Test
  fun rotationAroundYToMat4() {
    val angle = PI.toFloat() / 4f // 45 degrees
    val q = Quaternion.fromAxisAngle(Vec3.UP, angle)
    val qMat = q.toMat4()
    val rMat = Mat4.rotationY(angle)

    qMat.shouldBeApprox(rMat)
  }

  @Test
  fun rotationAroundXToMat4() {
    val angle = PI.toFloat() / 3f
    val q = Quaternion.fromAxisAngle(Vec3.RIGHT, angle)
    val qMat = q.toMat4()
    val rMat = Mat4.rotationX(angle)

    qMat.shouldBeApprox(rMat)
  }

  @Test
  fun rotationAroundZToMat4() {
    val angle = PI.toFloat() / 6f
    val q = Quaternion.fromAxisAngle(Vec3(0f, 0f, 1f), angle)
    val qMat = q.toMat4()
    val rMat = Mat4.rotationZ(angle)

    qMat.shouldBeApprox(rMat)
  }

  @Test
  fun toMat4RotatesVectorConsistently() {
    val q = Quaternion.fromAxisAngle(Vec3(1f, 1f, 1f).normalize(), 1.2f)
    val v = Vec3(1f, 0f, 0f)

    val rotatedByQuat = q.rotateVec3(v)
    val mat = q.toMat4()
    val rotatedByMat = (mat * Vec4(v.x, v.y, v.z, 1f)).toVec3()

    rotatedByQuat.shouldBeApprox(rotatedByMat)
  }

  // --- Conjugate and inverse ---

  @Test
  fun conjugateOfIdentity() {
    val q = Quaternion.identity()
    val c = q.conjugate()
    c.shouldBeApprox(Quaternion.identity())
  }

  @Test
  fun quaternionTimesConjugateIsIdentityForUnit() {
    val q = Quaternion.fromAxisAngle(Vec3.UP, 1.0f).normalize()
    val product = q * q.conjugate()
    product.shouldBeApprox(Quaternion.identity())
  }

  @Test
  fun inverseUndoesRotation() {
    val q = Quaternion.fromAxisAngle(Vec3(1f, 2f, 3f).normalize(), 0.8f)
    val v = Vec3(5f, -3f, 2f)

    val rotated = q.rotateVec3(v)
    val unrotated = q.inverse().rotateVec3(rotated)

    unrotated.shouldBeApprox(v)
  }

  // --- fromEuler ---

  @Test
  fun fromEulerZeroIsIdentity() {
    val q = Quaternion.fromEuler(0f, 0f, 0f)
    q.shouldBeApprox(Quaternion.identity())
  }

  @Test
  fun fromEulerPitchOnly() {
    val pitch = PI.toFloat() / 4f
    val q = Quaternion.fromEuler(pitch, 0f, 0f)
    val expected = Quaternion.fromAxisAngle(Vec3.RIGHT, pitch)
    q.shouldBeApprox(expected)
  }

  @Test
  fun fromEulerYawOnly() {
    val yaw = PI.toFloat() / 3f
    val q = Quaternion.fromEuler(0f, yaw, 0f)
    val expected = Quaternion.fromAxisAngle(Vec3.UP, yaw)
    q.shouldBeApprox(expected)
  }
}
