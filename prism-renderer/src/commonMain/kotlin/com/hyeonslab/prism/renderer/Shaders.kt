package com.hyeonslab.prism.renderer

/**
 * Built-in WGSL shader sources for the Prism engine.
 *
 * Uniform layout:
 * - Group 0, Binding 0: Uniforms (viewProjection mat4x4 + model mat4x4 = 128 bytes)
 * - Group 0, Binding 1: MaterialUniforms (baseColor vec4 = 16 bytes)
 */
object Shaders {

  /** Size of the Uniforms buffer in bytes (two mat4x4f = 2 * 64). */
  const val UNIFORMS_SIZE = 128L

  /** Size of the MaterialUniforms buffer in bytes (one vec4f = 16). */
  const val MATERIAL_UNIFORMS_SIZE = 16L

  /** Vertex shader: transforms positions by VP * M, passes world-space normal and UV. */
  val VERTEX_SHADER =
    ShaderSource(
      code =
        """
        struct Uniforms {
            viewProjection : mat4x4f,
            model : mat4x4f,
        };

        struct MaterialUniforms {
            baseColor : vec4f,
        };

        @group(0) @binding(0) var<uniform> uniforms : Uniforms;
        @group(0) @binding(1) var<uniform> material : MaterialUniforms;

        struct VertexInput {
            @location(0) position : vec3f,
            @location(1) normal : vec3f,
            @location(2) uv : vec2f,
        };

        struct VertexOutput {
            @builtin(position) clipPosition : vec4f,
            @location(0) worldNormal : vec3f,
            @location(1) uv : vec2f,
        };

        @vertex
        fn vs_main(in : VertexInput) -> VertexOutput {
            var out : VertexOutput;
            let worldPos = uniforms.model * vec4f(in.position, 1.0);
            out.clipPosition = uniforms.viewProjection * worldPos;
            // Transform normal by model matrix (upper-left 3x3, assumes uniform scale)
            out.worldNormal = (uniforms.model * vec4f(in.normal, 0.0)).xyz;
            out.uv = in.uv;
            return out;
        }
        """
          .trimIndent(),
      stage = ShaderStage.VERTEX,
      entryPoint = "vs_main",
    )

  /** Fragment shader: simple directional light with material base color. */
  val FRAGMENT_UNLIT =
    ShaderSource(
      code =
        """
        @fragment
        fn fs_main(
            @location(0) worldNormal : vec3f,
            @location(1) uv : vec2f,
        ) -> @location(0) vec4f {
            let lightDir = normalize(vec3f(0.5, 1.0, 0.8));
            let normal = normalize(worldNormal);
            let ndotl = max(dot(normal, lightDir), 0.0);
            let ambient = 0.15;
            let diffuse = ndotl * 0.85;
            let lighting = ambient + diffuse;
            return vec4f(material.baseColor.rgb * lighting, material.baseColor.a);
        }
        """
          .trimIndent(),
      stage = ShaderStage.FRAGMENT,
      entryPoint = "fs_main",
    )
}
