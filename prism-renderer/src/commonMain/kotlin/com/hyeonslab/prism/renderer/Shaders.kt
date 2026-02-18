package com.hyeonslab.prism.renderer

/**
 * Built-in PBR WGSL shader sources for the Prism engine.
 *
 * Bind group layout:
 * - Group 0 (Scene): SceneUniforms + Light storage buffer
 * - Group 1 (Object): ObjectUniforms (model + normalMatrix)
 * - Group 2 (Material): PBR uniforms + 5 textures + sampler
 * - Group 3 (Environment): IBL cubemaps + BRDF LUT + EnvironmentUniforms
 */
object Shaders {

  // --- Buffer sizes ---

  /** SceneUniforms: mat4x4f + vec3f + u32 + vec3f + pad = 96 bytes. */
  const val SCENE_UNIFORMS_SIZE = 96L

  /** ObjectUniforms: mat4x4f(64) + mat3x3f padded(48) = 112 bytes. */
  const val OBJECT_UNIFORMS_SIZE = 112L

  /** PBR MaterialUniforms: vec4f + 4xf32 + vec4f = 48 bytes. */
  const val PBR_MATERIAL_UNIFORMS_SIZE = 48L

  /** One Light struct = 64 bytes (16 floats). */
  const val LIGHT_STRIDE = 64L

  /** Maximum lights supported in the storage buffer. */
  const val MAX_LIGHTS = 16

  /** Light storage buffer size: MAX_LIGHTS * LIGHT_STRIDE. */
  const val LIGHT_BUFFER_SIZE = MAX_LIGHTS * LIGHT_STRIDE

  /** EnvironmentUniforms: 2xf32 + 2xf32 pad = 16 bytes. */
  const val ENV_UNIFORMS_SIZE = 16L

  // --- Legacy constants (kept for existing test compatibility) ---

  /** @deprecated Use [SCENE_UNIFORMS_SIZE] and [OBJECT_UNIFORMS_SIZE] instead. */
  const val UNIFORMS_SIZE = 128L

  /** @deprecated Use [PBR_MATERIAL_UNIFORMS_SIZE] instead. */
  const val MATERIAL_UNIFORMS_SIZE = 16L

  // --- PBR Shaders ---

  /**
   * PBR vertex shader with tangent support.
   *
   * Defines all shared structs and bindings (groups 0-3). Outputs world-space position, normal,
   * tangent, UV, and bitangent sign for TBN reconstruction in the fragment shader.
   */
  @Suppress("LongMethod")
  val PBR_VERTEX_SHADER =
    ShaderSource(
      code =
        """
        // ===== Structs =====

        struct SceneUniforms {
            viewProjection : mat4x4f,
            cameraPosition : vec3f,
            numLights : u32,
            ambientColor : vec3f,
            _pad : f32,
        };

        struct Light {
            position : vec3f,
            lightType : f32,
            direction : vec3f,
            intensity : f32,
            color : vec3f,
            range : f32,
            spotAngle : f32,
            _pad1 : f32,
            _pad2 : f32,
            _pad3 : f32,
        };

        struct ObjectUniforms {
            model : mat4x4f,
            normalMatrix : mat3x3f,
        };

        struct MaterialUniforms {
            baseColor : vec4f,
            metallic : f32,
            roughness : f32,
            occlusionStrength : f32,
            flags : u32,
            emissive : vec4f,
        };

        struct EnvironmentUniforms {
            envIntensity : f32,
            maxMipLevel : f32,
            _pad1 : f32,
            _pad2 : f32,
        };

        // ===== Bindings =====

        // Group 0: Scene (per-frame)
        @group(0) @binding(0)
        var<uniform> scene : SceneUniforms;
        @group(0) @binding(1)
        var<storage, read> lights : array<Light>;

        // Group 1: Object (per-draw)
        @group(1) @binding(0)
        var<uniform> objectData : ObjectUniforms;

        // Group 2: Material (per-material)
        @group(2) @binding(0)
        var<uniform> material : MaterialUniforms;
        @group(2) @binding(1)
        var materialSampler : sampler;
        @group(2) @binding(2)
        var baseColorTexture : texture_2d<f32>;
        @group(2) @binding(3)
        var metalRoughTexture : texture_2d<f32>;
        @group(2) @binding(4)
        var normalTexture : texture_2d<f32>;
        @group(2) @binding(5)
        var occlusionTexture : texture_2d<f32>;
        @group(2) @binding(6)
        var emissiveTexture : texture_2d<f32>;

        // Group 3: Environment (per-scene)
        @group(3) @binding(0)
        var<uniform> env : EnvironmentUniforms;
        @group(3) @binding(1)
        var envSampler : sampler;
        @group(3) @binding(2)
        var irradianceMap : texture_cube<f32>;
        @group(3) @binding(3)
        var prefilteredMap : texture_cube<f32>;
        @group(3) @binding(4)
        var brdfLut : texture_2d<f32>;

        // ===== Vertex =====

        struct VertexInput {
            @location(0) position : vec3f,
            @location(1) normal : vec3f,
            @location(2) uv : vec2f,
            @location(3) tangent : vec4f,
        };

        struct VertexOutput {
            @builtin(position) clipPosition : vec4f,
            @location(0) worldPosition : vec3f,
            @location(1) worldNormal : vec3f,
            @location(2) uv : vec2f,
            @location(3) worldTangent : vec3f,
            @location(4) bitangentSign : f32,
        };

        @vertex
        fn vs_main(in : VertexInput) -> VertexOutput {
            var out : VertexOutput;
            let worldPos = objectData.model * vec4f(in.position, 1.0);
            out.clipPosition = scene.viewProjection * worldPos;
            out.worldPosition = worldPos.xyz;
            out.worldNormal = objectData.normalMatrix * in.normal;
            out.uv = in.uv;
            out.worldTangent =
                (objectData.model * vec4f(in.tangent.xyz, 0.0)).xyz;
            out.bitangentSign = in.tangent.w;
            return out;
        }
        """
          .trimIndent(),
      stage = ShaderStage.VERTEX,
      entryPoint = "vs_main",
    )

  /**
   * PBR fragment shader: Cook-Torrance BRDF with direct lighting and IBL.
   *
   * Features:
   * - GGX/Trowbridge-Reitz normal distribution
   * - Smith-GGX geometry function (Schlick approximation)
   * - Fresnel-Schlick with roughness variant for IBL
   * - Directional, point, and spot light support
   * - Image-based lighting (irradiance + prefiltered env + BRDF LUT)
   * - Optional normal mapping via TBN matrix
   * - All 5 PBR texture slots with flag-based branching
   */
  @Suppress("LongMethod")
  val PBR_FRAGMENT_SHADER =
    ShaderSource(
      code =
        """
        const PI = 3.14159265359;

        // GGX/Trowbridge-Reitz NDF
        fn distributionGGX(NdotH : f32, roughness : f32) -> f32 {
            let a = roughness * roughness;
            let a2 = a * a;
            let NdotH2 = NdotH * NdotH;
            let denom = NdotH2 * (a2 - 1.0) + 1.0;
            return a2 / (PI * denom * denom);
        }

        // Schlick-GGX geometry for one direction
        fn geometrySchlickGGX(NdotX : f32, roughness : f32) -> f32 {
            let r = roughness + 1.0;
            let k = (r * r) / 8.0;
            return NdotX / (NdotX * (1.0 - k) + k);
        }

        // Smith geometry: combined for view and light
        fn geometrySmith(
            NdotV : f32, NdotL : f32, roughness : f32
        ) -> f32 {
            return geometrySchlickGGX(NdotV, roughness)
                 * geometrySchlickGGX(NdotL, roughness);
        }

        // Fresnel-Schlick
        fn fresnelSchlick(cosTheta : f32, F0 : vec3f) -> vec3f {
            let t = clamp(1.0 - cosTheta, 0.0, 1.0);
            return F0 + (1.0 - F0) * pow(t, 5.0);
        }

        // Fresnel-Schlick with roughness for IBL
        fn fresnelSchlickRoughness(
            cosTheta : f32, F0 : vec3f, roughness : f32
        ) -> vec3f {
            let maxR = max(vec3f(1.0 - roughness), F0);
            let t = clamp(1.0 - cosTheta, 0.0, 1.0);
            return F0 + (maxR - F0) * pow(t, 5.0);
        }

        @fragment
        fn fs_main(
            @location(0) worldPosition : vec3f,
            @location(1) worldNormal : vec3f,
            @location(2) uv : vec2f,
            @location(3) worldTangent : vec3f,
            @location(4) bitangentSign : f32,
        ) -> @location(0) vec4f {

            // Material flags
            let hasBaseColorTex = (material.flags & 1u) != 0u;
            let hasMetalRoughTex = (material.flags & 2u) != 0u;
            let hasNormalTex = (material.flags & 4u) != 0u;
            let hasOcclusionTex = (material.flags & 8u) != 0u;
            let hasEmissiveTex = (material.flags & 16u) != 0u;

            // Base color
            var baseColor = material.baseColor;
            if (hasBaseColorTex) {
                let texColor = textureSample(
                    baseColorTexture, materialSampler, uv
                );
                baseColor = baseColor * texColor;
            }
            let albedo = baseColor.rgb;

            // Metallic / Roughness
            var metallic = material.metallic;
            var roughness = material.roughness;
            if (hasMetalRoughTex) {
                let mr = textureSample(
                    metalRoughTexture, materialSampler, uv
                );
                metallic = metallic * mr.b;
                roughness = roughness * mr.g;
            }
            roughness = clamp(roughness, 0.04, 1.0);

            // Normal (with optional normal mapping)
            var N = normalize(worldNormal);
            if (hasNormalTex) {
                let ts = textureSample(
                    normalTexture, materialSampler, uv
                ).rgb;
                let tanN = ts * 2.0 - 1.0;
                let T = normalize(worldTangent);
                let B = cross(N, T) * bitangentSign;
                N = normalize(
                    T * tanN.x + B * tanN.y + N * tanN.z
                );
            }

            // View direction
            let V = normalize(scene.cameraPosition - worldPosition);
            let NdotV = max(dot(N, V), 0.001);

            // F0: 0.04 for dielectrics, albedo for metals
            let F0 = mix(vec3f(0.04), albedo, metallic);

            // ===== Direct lighting =====
            var Lo = vec3f(0.0);
            let lightCount = min(scene.numLights, ${MAX_LIGHTS}u);
            for (var i = 0u; i < lightCount; i++) {
                let light = lights[i];
                let lType = i32(light.lightType);

                var L : vec3f;
                var attenuation : f32 = 1.0;

                if (lType == 0) {
                    // Directional
                    L = normalize(-light.direction);
                } else {
                    // Point or Spot
                    let toLight = light.position - worldPosition;
                    let dist = length(toLight);
                    L = toLight / dist;
                    attenuation = 1.0 / (dist * dist + 0.0001);
                    if (light.range > 0.0) {
                        let ratio = dist / light.range;
                        let r4 = ratio * ratio * ratio * ratio;
                        attenuation *= max(1.0 - r4, 0.0);
                    }
                    if (lType == 2) {
                        // Spot
                        let spotCos = dot(
                            L, normalize(-light.direction)
                        );
                        let angRad = light.spotAngle * PI / 180.0;
                        let outerCos = cos(angRad);
                        let innerCos = cos(angRad * 0.8);
                        attenuation *= clamp(
                            (spotCos - outerCos)
                            / (innerCos - outerCos),
                            0.0, 1.0
                        );
                    }
                }

                let H = normalize(V + L);
                let NdotL = max(dot(N, L), 0.0);
                let NdotH = max(dot(N, H), 0.0);
                let HdotV = max(dot(H, V), 0.0);

                let D = distributionGGX(NdotH, roughness);
                let G = geometrySmith(NdotV, NdotL, roughness);
                let F = fresnelSchlick(HdotV, F0);

                let num = D * G * F;
                let den = 4.0 * NdotV * NdotL + 0.0001;
                let spec = num / den;

                let kS = F;
                let kD = (1.0 - kS) * (1.0 - metallic);

                let radiance =
                    light.color * light.intensity * attenuation;
                Lo += (kD * albedo / PI + spec)
                    * radiance * NdotL;
            }

            // ===== Ambient / IBL =====
            var ambient = vec3f(0.0);
            if (env.envIntensity > 0.0) {
                let F = fresnelSchlickRoughness(
                    NdotV, F0, roughness
                );
                let kS = F;
                let kD = (1.0 - kS) * (1.0 - metallic);

                let irr = textureSample(
                    irradianceMap, envSampler, N
                ).rgb;
                let diffuseIBL = irr * albedo;

                let R = reflect(-V, N);
                let mip = roughness * env.maxMipLevel;
                let pref = textureSampleLevel(
                    prefilteredMap, envSampler, R, mip
                ).rgb;
                let brdf = textureSample(
                    brdfLut, envSampler,
                    vec2f(NdotV, roughness)
                ).rg;
                let specIBL = pref * (F * brdf.x + brdf.y);

                ambient = (kD * diffuseIBL + specIBL)
                    * env.envIntensity;
            } else {
                // Fallback ambient when IBL is disabled
                ambient = scene.ambientColor * albedo
                    * (1.0 - metallic * 0.5);
            }

            // Occlusion
            var ao = 1.0;
            if (hasOcclusionTex) {
                ao = textureSample(
                    occlusionTexture, materialSampler, uv
                ).r;
                ao = 1.0 + material.occlusionStrength * (ao - 1.0);
            }
            ambient = ambient * ao;

            // Emissive
            var emissive = material.emissive.rgb;
            if (hasEmissiveTex) {
                emissive = emissive * textureSample(
                    emissiveTexture, materialSampler, uv
                ).rgb;
            }

            let color = Lo + ambient + emissive;
            return vec4f(color, baseColor.a);
        }
        """
          .trimIndent(),
      stage = ShaderStage.FRAGMENT,
      entryPoint = "fs_main",
    )

  // --- Tone Mapping Shaders ---

  /**
   * Fullscreen triangle vertex shader for the tone mapping pass.
   *
   * Uses the vertex_index trick (no vertex buffer needed): 3 vertices form a large triangle
   * covering the entire viewport, mapping UV [0,1]×[0,1] to screen space.
   */
  val TONE_MAP_VERTEX_SHADER =
    ShaderSource(
      code =
        """
        struct TmOutput {
            @builtin(position) pos : vec4f,
            @location(0) uv : vec2f,
        };

        @vertex
        fn tm_vs(@builtin(vertex_index) vi : u32) -> TmOutput {
            var out : TmOutput;
            // Fullscreen triangle covering NDC (-1,-1) to (1,1)
            let x = f32((vi << 1u) & 2u) * 2.0 - 1.0;
            let y = 1.0 - f32(vi & 2u) * 2.0;
            out.pos = vec4f(x, y, 0.0, 1.0);
            out.uv = vec2f((x + 1.0) * 0.5, (1.0 - y) * 0.5);
            return out;
        }
        """
          .trimIndent(),
      stage = ShaderStage.VERTEX,
      entryPoint = "tm_vs",
    )

  /**
   * Tone mapping fragment shader using Khronos PBR Neutral tone mapping.
   *
   * Reads linear HDR color from the HDR render target and maps it to LDR [0, 1]. Outputs linear
   * values suitable for sRGB swapchains (GPU applies gamma correction automatically).
   *
   * Reference: https://github.com/KhronosGroup/ToneMapping/tree/main/PBR_Neutral
   */
  val TONE_MAP_FRAGMENT_SHADER =
    ShaderSource(
      code =
        """
        @group(0) @binding(0) var hdrTexture : texture_2d<f32>;
        @group(0) @binding(1) var hdrSampler : sampler;

        // Khronos PBR Neutral tone mapping operator
        fn toneMapKhronosPbrNeutral(color : vec3f) -> vec3f {
            let startCompression : f32 = 0.8 - 0.04;
            let desaturation : f32 = 0.15;

            let x = min(color.r, min(color.g, color.b));
            let offset = select(0.04, x - 6.25 * x * x, x < 0.08);
            var c = color - vec3f(offset);

            let peak = max(c.r, max(c.g, c.b));
            if (peak < startCompression) {
                return c;
            }

            let d : f32 = 1.0 - startCompression;
            let newPeak = 1.0 - d * d / (peak + d - startCompression);
            c = c * (newPeak / peak);

            let g = 1.0 - 1.0 / (desaturation * (peak - newPeak) + 1.0);
            return mix(c, vec3f(newPeak), g);
        }

        @fragment
        fn tm_fs(@location(0) uv : vec2f) -> @location(0) vec4f {
            let hdr = textureSample(hdrTexture, hdrSampler, uv).rgb;
            let ldr = toneMapKhronosPbrNeutral(hdr);
            return vec4f(ldr, 1.0);
        }
        """
          .trimIndent(),
      stage = ShaderStage.FRAGMENT,
      entryPoint = "tm_fs",
    )
}
