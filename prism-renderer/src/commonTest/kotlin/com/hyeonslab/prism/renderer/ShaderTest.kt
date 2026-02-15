package com.hyeonslab.prism.renderer

import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import kotlin.test.Test

class ShaderTest {

  // --- Shaders constants ---

  @Test
  fun uniformsSize() {
    Shaders.UNIFORMS_SIZE shouldBe 128L
  }

  @Test
  fun materialUniformsSize() {
    Shaders.MATERIAL_UNIFORMS_SIZE shouldBe 16L
  }

  // --- Vertex shader ---

  @Test
  fun vertexShaderStage() {
    Shaders.VERTEX_SHADER.stage shouldBe ShaderStage.VERTEX
  }

  @Test
  fun vertexShaderEntryPoint() {
    Shaders.VERTEX_SHADER.entryPoint shouldBe "vs_main"
  }

  @Test
  fun vertexShaderCodeNotEmpty() {
    Shaders.VERTEX_SHADER.code.shouldNotBeEmpty()
  }

  @Test
  fun vertexShaderContainsEntryFunction() {
    Shaders.VERTEX_SHADER.code shouldContain "fn vs_main"
  }

  // --- Fragment shader ---

  @Test
  fun fragmentUnlitStage() {
    Shaders.FRAGMENT_UNLIT.stage shouldBe ShaderStage.FRAGMENT
  }

  @Test
  fun fragmentUnlitEntryPoint() {
    Shaders.FRAGMENT_UNLIT.entryPoint shouldBe "fs_main"
  }

  @Test
  fun fragmentUnlitCodeNotEmpty() {
    Shaders.FRAGMENT_UNLIT.code.shouldNotBeEmpty()
  }

  @Test
  fun fragmentUnlitContainsEntryFunction() {
    Shaders.FRAGMENT_UNLIT.code shouldContain "fn fs_main"
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
