# 000013 — feat/pbr-materials

## Agent
Claude (claude-opus-4-6) @ repository:prism branch:feat/pbr-materials

## Intent
Implement M9: PBR materials pipeline — Cook-Torrance BRDF, ECS-driven lights, PBR textures, IBL, HDR tone mapping, and sphere grid demo.

## What Changed
- 2026-02-18 `prism-renderer/.../{Color,Material,Texture,Mesh,Renderer,LightData}.kt` — PBR data model: Color.toLinear/toSrgb, Material PBR fields, RGBA16_FLOAT format, sphere mesh, setMaterial/setLights/setCameraPosition, LightData GPU struct
- 2026-02-18 `prism-math/.../Mat4.kt` — toMat3(), normalMatrix()
- 2026-02-18 `prism-renderer/.../Shaders.kt` — PBR_VERTEX_SHADER, PBR_FRAGMENT_SHADER (Cook-Torrance BRDF, IBL), TONE_MAP shaders
- 2026-02-18 `prism-renderer/.../WgpuRenderer.kt` — multi-bind-group layout, PBR pipeline, HDR render target, tone map pass, IBL bind group, initializeIbl()
- 2026-02-18 `prism-renderer/.../IblGenerator.kt` — CPU-side BRDF LUT, irradiance cubemap, prefiltered specular env
- 2026-02-18 `prism-ecs/.../RenderSystem.kt` — rewritten for PBR API (lights query, setMaterial, setCameraPosition)
- 2026-02-18 `prism-demo-core/.../DemoScene.kt` — replaced rotating cube with 7×7 PBR sphere grid + 2 lights + IBL/HDR
- 2026-02-18 `prism-demo-core/.../DemoSceneState.kt` — replaced cubeColor with metallic/roughness/envIntensity PBR fields
- 2026-02-18 `prism-demo-core/.../ComposeDemoControls.kt` — PBR sliders (env intensity, metallic, roughness)
- 2026-02-18 `prism-demo-core/jvmMain/.../ComposeMain.kt` — removed cube rotation/color logic, call scene.tick()
- 2026-02-18 `prism-demo-core/iosMain/.../IosConstants.kt` — removed cubeEntity material color update
- 2026-02-18 `prism-demo-core/iosMain/.../ComposeIosEntry.kt` — removed initialColor parameter
- 2026-02-18 `AGENTS.md` — marked M9 complete, updated phase and test counts
- 2026-02-18 `BUILD_STATUS.md` — M9 milestone complete with full feature list

## Decisions
- 2026-02-18 Multi-bind-group layout (4 groups: scene, object, material, environment) — separates data by update frequency, matches standard PBR engine patterns
- 2026-02-18 CPU-side IBL generation — avoids compute shader complexity with wgpu4k; acceptable for small cubemaps (32-64px)
- 2026-02-18 Replace old RenderSystem in-place — single PBR pipeline handles all materials (metallic=0, roughness=1 approximates old Lambert look)
- 2026-02-18 Static sphere grid (no rotation) — PBR showcase benefits from stable lighting to see metallic/roughness gradient clearly
- 2026-02-18 DemoStore PBR fields (metallic/roughness/envIntensity) — UI sliders are cosmetic for M9; env intensity wiring to GPU deferred to future
- 2026-02-18 Share single Mesh.sphere() instance across all 49 spheres — GPU upload happens once (mesh.vertexBuffer set on first draw), subsequent draws reuse buffers

## Issues
- `Origin3D` type mismatch in cubemap face upload: wgpu4k expects `Origin3D(z = face)` not `arrayOf(0u, 0u, face)` — fixed using named constructor
- `Math.pow` not KMP-compatible: replaced with `kotlin.math.pow` extension throughout IblGenerator
- `shouldBeNull()` unavailable without explicit kotest import: used `shouldBe null` pattern instead
- wgpu4k `TextureDescriptor.mipLevelCount`, `TexelCopyTextureInfo.mipLevel`, `TextureViewDescriptor.mipLevelCount` confirmed present via source jar inspection (all `UInt`-typed with sensible defaults)
- Critical review found 7 issues after M9 completion — all fixed in follow-up commit:
  1. Irradiance scale `π/N` → `2π/N` (factor-of-2 physics error)
  2. Missing `1/π` in Lambertian diffuse IBL term in fragment shader
  3. IBL textures upgraded from RGBA8Unorm to RGBA16Float (irradiance + prefiltered) and RG16Float (BRDF LUT) — adds float16 encoding helpers
  4. Non-sRGB swapchain got no gamma encoding in tone map pass — added `ToneMapParams.applySrgb` uniform
  5. Tangent transformed by model matrix instead of normalMatrix — breaks TBN under non-uniform scale
  6. `LightData.innerAngle` field added to expose spot light inner cone; replaces hardcoded 80% in WGSL
  7. Redundant `setCameraPosition` call after `setCamera` in RenderSystem removed

- Critical review (second round) found additional issues after rebase onto main:
  8. `mat3x3f` in uniform buffer is WGSL spec-illegal (§13.4.1) — replaced with 3 vec4f columns
  9. Bitangent cross product wrong: cross(N,T) → cross(T,N)
  10. Emissive double-converts via toLinear() — emissive is linear HDR, not sRGB; fix: remove conversion
  11. Android ArrayBuffer.of(FloatArray) byte-swap: BIG_ENDIAN ByteBuffer on ARM64 — fix: use deprecated writeBuffer(buf, offset, FloatArray) overload everywhere
  12. IntArray index buffer same bug — fix: reinterpret via Float.fromBits before using writeBuffer
  13. hdrEnabled mid-frame toggle race — fix: snapshot hdrEnabledForFrame at beginRenderPass
  14. normalMatrix() crashes on zero-scale transforms — fix: guard with abs(det) < 1e-6 → Mat3.identity()
  15. Flutter demos referenced removed DemoIntent.SetCubeColor — compile errors; updated to PBR API
  16. docs/index.html: M9/M11 marked planned, Flutter marked planned, old module names — all updated
  17. docs/demo.js: spinning cube replaced with PBR sphere grid (Cook-Torrance BRDF)

## Commits
- 16213a2 docs: add devlog and plan for PBR materials (M9)
- 5fc2bfe feat: implement PBR rendering pipeline (Steps 1-4)
- 99b6905 feat: add HDR render target + Khronos PBR Neutral tone mapping (Step 5)
- c88e1e4 feat: add CPU-side IBL generation (Step 6)
- 9699a87 feat: add PBR sphere grid demo + PBR UI controls (Step 7)
- 7f0db1b docs: mark M9 PBR materials complete, update BUILD_STATUS + AGENTS
- 74c24b0 docs: update docs for M9 PBR materials completion
- 5e69d09 docs: fix ARCHITECTURE.md inaccuracies found in critical review
- 3629d8e fix: resolve critical review issues in PBR pipeline
- db80dc4 feat: update Flutter demos and docs for PBR (M9 + M11)

## Progress
- [x] Step 1: Data model expansion + math helpers
- [x] Step 2: UV sphere mesh generator with tangents
- [x] Step 3: PBR shaders + multi-bind-group renderer
- [x] Step 4: ECS light integration + update RenderSystem
- [x] Step 5: HDR render target + tone mapping pass
- [x] Step 6: IBL — procedural irradiance, prefiltered env, BRDF LUT
- [x] Step 7: PBR sphere grid demo + UI controls
- [x] Step 8: Platform validation + docs
