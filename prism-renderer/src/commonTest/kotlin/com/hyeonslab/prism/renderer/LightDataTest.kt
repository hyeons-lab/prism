package com.hyeonslab.prism.renderer

import com.hyeonslab.prism.math.Vec3
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LightDataTest {

  @Test
  fun toFloatArraySize() {
    val light = LightData()
    light.toFloatArray().size shouldBe LightData.FLOAT_COUNT
  }

  @Test
  fun toFloatArrayDirectionalLayout() {
    val light =
      LightData(
        type = LightType.DIRECTIONAL,
        position = Vec3(1f, 2f, 3f),
        direction = Vec3(0f, -1f, 0f),
        color = Color(0.8f, 0.6f, 0.4f),
        intensity = 2.5f,
        range = 50f,
        spotAngle = 30f,
      )
    val arr = light.toFloatArray()

    // position.xyz + type
    arr[0] shouldBe 1f
    arr[1] shouldBe 2f
    arr[2] shouldBe 3f
    arr[3] shouldBe 0f // DIRECTIONAL = 0

    // direction.xyz + intensity
    arr[4] shouldBe 0f
    arr[5] shouldBe -1f
    arr[6] shouldBe 0f
    arr[7] shouldBe 2.5f

    // color.rgb + range
    arr[8] shouldBe 0.8f
    arr[9] shouldBe 0.6f
    arr[10] shouldBe 0.4f
    arr[11] shouldBe 50f

    // spotAngle + padding
    arr[12] shouldBe 30f
    arr[13] shouldBe 0f
    arr[14] shouldBe 0f
    arr[15] shouldBe 0f
  }

  @Test
  fun lightTypeValues() {
    LightType.DIRECTIONAL.value shouldBe 0
    LightType.POINT.value shouldBe 1
    LightType.SPOT.value shouldBe 2
  }

  @Test
  fun pointLightType() {
    val light = LightData(type = LightType.POINT, position = Vec3(5f, 10f, 5f))
    val arr = light.toFloatArray()
    arr[3] shouldBe 1f // POINT = 1
  }

  @Test
  fun sizeConstants() {
    LightData.FLOAT_COUNT shouldBe 16
    LightData.SIZE_BYTES shouldBe 64L
  }

  @Test
  fun defaultValues() {
    val light = LightData()
    light.type shouldBe LightType.DIRECTIONAL
    light.position shouldBe Vec3.ZERO
    light.direction shouldBe Vec3(0f, -1f, 0f)
    light.color shouldBe Color.WHITE
    light.intensity shouldBe 1f
    light.range shouldBe 10f
    light.spotAngle shouldBe 45f
  }
}
