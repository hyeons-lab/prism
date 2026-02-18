package com.hyeonslab.prism.renderer

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlin.test.Test

class ShaderTest {

  // --- PBR Shader constants ---

  @Test
  fun sceneUniformsSize() {
    Shaders.SCENE_UNIFORMS_SIZE shouldBe 96L
  }

  @Test
  fun objectUniformsSize() {
    Shaders.OBJECT_UNIFORMS_SIZE shouldBe 112L
  }

  @Test
  fun pbrMaterialUniformsSize() {
    Shaders.PBR_MATERIAL_UNIFORMS_SIZE shouldBe 48L
  }

  @Test
  fun lightBufferSize() {
    Shaders.LIGHT_BUFFER_SIZE shouldBe Shaders.MAX_LIGHTS * Shaders.LIGHT_STRIDE
  }

  @Test
  fun envUniformsSize() {
    Shaders.ENV_UNIFORMS_SIZE shouldBe 16L
  }

  // --- Legacy constants ---

  @Test
  fun legacyUniformsSize() {
    Shaders.UNIFORMS_SIZE shouldBe 128L
  }

  @Test
  fun legacyMaterialUniformsSize() {
    Shaders.MATERIAL_UNIFORMS_SIZE shouldBe 16L
  }

  // --- PBR Vertex shader ---

  @Test
  fun pbrVertexShaderStage() {
    Shaders.PBR_VERTEX_SHADER.stage shouldBe ShaderStage.VERTEX
  }

  @Test
  fun pbrVertexShaderEntryPoint() {
    Shaders.PBR_VERTEX_SHADER.entryPoint shouldBe "vs_main"
  }

  @Test
  fun pbrVertexShaderCodeNotEmpty() {
    Shaders.PBR_VERTEX_SHADER.code.shouldNotBeEmpty()
  }

  @Test
  fun pbrVertexShaderContainsEntryFunction() {
    Shaders.PBR_VERTEX_SHADER.code shouldContain "fn vs_main"
  }

  @Test
  fun pbrVertexShaderContainsBindings() {
    Shaders.PBR_VERTEX_SHADER.code shouldContain "@group(0)"
    Shaders.PBR_VERTEX_SHADER.code shouldContain "@group(1)"
    Shaders.PBR_VERTEX_SHADER.code shouldContain "@group(2)"
    Shaders.PBR_VERTEX_SHADER.code shouldContain "@group(3)"
  }

  // --- PBR Fragment shader ---

  @Test
  fun pbrFragmentShaderStage() {
    Shaders.PBR_FRAGMENT_SHADER.stage shouldBe ShaderStage.FRAGMENT
  }

  @Test
  fun pbrFragmentShaderEntryPoint() {
    Shaders.PBR_FRAGMENT_SHADER.entryPoint shouldBe "fs_main"
  }

  @Test
  fun pbrFragmentShaderCodeNotEmpty() {
    Shaders.PBR_FRAGMENT_SHADER.code.shouldNotBeEmpty()
  }

  @Test
  fun pbrFragmentShaderContainsBrdfFunctions() {
    Shaders.PBR_FRAGMENT_SHADER.code shouldContain "fn distributionGGX"
    Shaders.PBR_FRAGMENT_SHADER.code shouldContain "fn geometrySmith"
    Shaders.PBR_FRAGMENT_SHADER.code shouldContain "fn fresnelSchlick"
  }

  // --- Tone map vertex shader ---

  @Test
  fun toneMapVertexShaderStage() {
    Shaders.TONE_MAP_VERTEX_SHADER.stage shouldBe ShaderStage.VERTEX
  }

  @Test
  fun toneMapVertexShaderEntryPoint() {
    Shaders.TONE_MAP_VERTEX_SHADER.entryPoint shouldBe "tm_vs"
  }

  @Test
  fun toneMapVertexShaderCodeNotEmpty() {
    Shaders.TONE_MAP_VERTEX_SHADER.code.shouldNotBeEmpty()
  }

  @Test
  fun toneMapVertexShaderContainsEntryFunction() {
    Shaders.TONE_MAP_VERTEX_SHADER.code shouldContain "fn tm_vs"
  }

  // --- Tone map fragment shader ---

  @Test
  fun toneMapFragmentShaderStage() {
    Shaders.TONE_MAP_FRAGMENT_SHADER.stage shouldBe ShaderStage.FRAGMENT
  }

  @Test
  fun toneMapFragmentShaderEntryPoint() {
    Shaders.TONE_MAP_FRAGMENT_SHADER.entryPoint shouldBe "tm_fs"
  }

  @Test
  fun toneMapFragmentShaderCodeNotEmpty() {
    Shaders.TONE_MAP_FRAGMENT_SHADER.code.shouldNotBeEmpty()
  }

  @Test
  fun toneMapFragmentShaderContainsToneMapFunction() {
    Shaders.TONE_MAP_FRAGMENT_SHADER.code shouldContain "fn toneMapKhronosPbrNeutral"
  }

  @Test
  fun toneMapFragmentShaderContainsGroup0Binding() {
    Shaders.TONE_MAP_FRAGMENT_SHADER.code shouldContain "@group(0)"
  }

  // --- ShaderSource ---

  @Test
  fun shaderSourceDefaultEntryPoint() {
    val source = ShaderSource("code", ShaderStage.COMPUTE)
    source.entryPoint shouldBe "main"
  }

  // --- ShaderModule ---

  @Test
  fun shaderModuleConstruction() {
    val vs = ShaderSource("vs", ShaderStage.VERTEX, "vs_main")
    val fs = ShaderSource("fs", ShaderStage.FRAGMENT, "fs_main")
    val module = ShaderModule(vs, fs, label = "test")

    module.vertexSource shouldBe vs
    module.fragmentSource shouldBe fs
    module.label shouldBe "test"
  }

  @Test
  fun shaderModuleHandleStartsNull() {
    val vs = ShaderSource("vs", ShaderStage.VERTEX)
    val fs = ShaderSource("fs", ShaderStage.FRAGMENT)
    ShaderModule(vs, fs).handle.shouldBeNull()
  }

  @Test
  fun shaderModuleToString() {
    val vs = ShaderSource("vs", ShaderStage.VERTEX, "vs_main")
    val fs = ShaderSource("fs", ShaderStage.FRAGMENT, "fs_main")
    val module = ShaderModule(vs, fs, label = "myShader")

    module.toString() shouldBe
      "ShaderModule(vertex='vs_main', fragment='fs_main', label='myShader')"
  }
}
