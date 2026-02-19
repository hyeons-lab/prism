package com.hyeonslab.prism.renderer

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MaterialTest {

  @Test
  fun defaultValues() {
    val m = Material()
    m.baseColor shouldBe Color.WHITE
    m.metallic shouldBe 0f
    m.roughness shouldBe 0.5f
    m.emissive shouldBe Color.BLACK
    m.occlusionStrength shouldBe 1.0f
    m.shader shouldBe null
    m.albedoTexture shouldBe null
    m.normalTexture shouldBe null
    m.metallicRoughnessTexture shouldBe null
    m.occlusionTexture shouldBe null
    m.emissiveTexture shouldBe null
    m.label shouldBe ""
    m.pipeline shouldBe null
  }

  @Test
  fun customPbrFields() {
    val m =
      Material(
        baseColor = Color.RED,
        metallic = 0.8f,
        roughness = 0.2f,
        emissive = Color(1f, 0.5f, 0f),
        occlusionStrength = 0.7f,
      )
    m.metallic shouldBe 0.8f
    m.roughness shouldBe 0.2f
    m.emissive shouldBe Color(1f, 0.5f, 0f, 1f)
    m.occlusionStrength shouldBe 0.7f
  }

  @Test
  fun dataClassCopy() {
    val m1 = Material(metallic = 0.5f)
    val m2 = m1.copy(roughness = 0.1f, emissive = Color.WHITE)
    m2.metallic shouldBe 0.5f
    m2.roughness shouldBe 0.1f
    m2.emissive shouldBe Color.WHITE
  }
}
