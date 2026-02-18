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

## Decisions
- 2026-02-18 Multi-bind-group layout (4 groups: scene, object, material, environment) — separates data by update frequency, matches standard PBR engine patterns
- 2026-02-18 CPU-side IBL generation — avoids compute shader complexity with wgpu4k; acceptable for small cubemaps (32-64px)
- 2026-02-18 Replace old RenderSystem in-place — single PBR pipeline handles all materials (metallic=0, roughness=1 approximates old Lambert look)
- 2026-02-18 Static sphere grid (no rotation) — PBR showcase benefits from stable lighting to see metallic/roughness gradient clearly
- 2026-02-18 DemoStore PBR fields (metallic/roughness/envIntensity) — UI sliders are cosmetic for M9; env intensity wiring to GPU deferred to future
- 2026-02-18 Share single Mesh.sphere() instance across all 49 spheres — GPU upload happens once (mesh.vertexBuffer set on first draw), subsequent draws reuse buffers

## Issues

## Commits
- 16213a2 docs: add devlog and plan for PBR materials (M9)
- 5fc2bfe feat: implement PBR rendering pipeline (Steps 1-4)
- 99b6905 feat: add HDR render target + Khronos PBR Neutral tone mapping (Step 5)
- c88e1e4 feat: add CPU-side IBL generation (Step 6)

## Progress
- [x] Step 1: Data model expansion + math helpers
- [x] Step 2: UV sphere mesh generator with tangents
- [x] Step 3: PBR shaders + multi-bind-group renderer
- [x] Step 4: ECS light integration + update RenderSystem
- [x] Step 5: HDR render target + tone mapping pass
- [x] Step 6: IBL — procedural irradiance, prefiltered env, BRDF LUT
- [x] Step 7: PBR sphere grid demo + UI controls
- [ ] Step 8: Platform validation + docs
