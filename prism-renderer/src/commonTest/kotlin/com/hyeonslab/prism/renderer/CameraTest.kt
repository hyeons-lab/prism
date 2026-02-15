package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.math.Vec3
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.floats.shouldNotBeZero
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

class CameraTest {

  // --- Default values ---

  @Test
  fun defaultPosition() {
    Camera().position shouldBe Vec3(0f, 0f, 5f)
  }

  @Test
  fun defaultTarget() {
    Camera().target shouldBe Vec3.ZERO
  }

  @Test
  fun defaultUp() {
    Camera().up shouldBe Vec3.UP
  }

  @Test
  fun defaultFovY() {
    Camera().fovY shouldBe 60f
  }

  @Test
  fun defaultAspectRatio() {
    Camera().aspectRatio shouldBe 16f / 9f
  }

  @Test
  fun defaultNearPlane() {
    Camera().nearPlane shouldBe 0.1f
  }

  @Test
  fun defaultFarPlane() {
    Camera().farPlane shouldBe 1000f
  }

  @Test
  fun defaultIsNotOrthographic() {
    Camera().isOrthographic.shouldBeFalse()
  }

  // --- View matrix ---

  @Test
  fun viewMatrixIsNotIdentity() {
    Camera().viewMatrix() shouldNotBe Mat4.identity()
  }

  @Test
  fun viewMatrixHasNonZeroDeterminant() {
    Camera().viewMatrix().determinant().shouldNotBeZero()
  }

  // --- Projection matrix ---

  @Test
  fun perspectiveProjectionIsNotIdentity() {
    Camera().projectionMatrix() shouldNotBe Mat4.identity()
  }

  @Test
  fun orthographicProjectionDiffersFromPerspective() {
    val perspective = Camera()
    val ortho = Camera()
    ortho.isOrthographic = true

    perspective.projectionMatrix() shouldNotBe ortho.projectionMatrix()
  }

  @Test
  fun orthographicProjectionIsNotIdentity() {
    val cam = Camera()
    cam.isOrthographic = true
    cam.projectionMatrix() shouldNotBe Mat4.identity()
  }

  // --- View-projection matrix ---

  @Test
  fun viewProjectionCombinesBothMatrices() {
    val cam = Camera()
    val vp = cam.viewProjectionMatrix()
    // VP should differ from both view-only and projection-only
    vp shouldNotBe cam.viewMatrix()
    vp shouldNotBe cam.projectionMatrix()
  }

  @Test
  fun viewProjectionReflectsPositionChange() {
    val cam1 = Camera()
    val cam2 = Camera()
    cam2.position = Vec3(10f, 0f, 0f)

    cam1.viewProjectionMatrix() shouldNotBe cam2.viewProjectionMatrix()
  }

  // --- Custom camera configuration ---

  @Test
  fun cameraAtOriginLookingDown() {
    val cam = Camera()
    cam.position = Vec3.ZERO
    cam.target = Vec3(0f, -1f, 0f)
    cam.up = Vec3.FORWARD

    val view = cam.viewMatrix()
    view shouldNotBe Mat4.identity()
    view.determinant().shouldNotBeZero()
  }

  @Test
  fun changingAspectRatioAffectsProjection() {
    val cam1 = Camera()
    cam1.aspectRatio = 4f / 3f

    val cam2 = Camera()
    cam2.aspectRatio = 16f / 9f

    cam1.projectionMatrix() shouldNotBe cam2.projectionMatrix()
  }

  // --- toString ---

  @Test
  fun toStringPerspective() {
    val cam = Camera()
    val s = cam.toString()
    s shouldContain "perspective"
    s shouldContain "fov=60.0"
  }

  @Test
  fun toStringOrthographic() {
    val cam = Camera()
    cam.isOrthographic = true
    cam.toString() shouldContain "ortho"
  }
}
