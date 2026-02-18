# 000013 — feat/pbr-materials

## Agent
Claude (claude-opus-4-6) @ repository:prism branch:feat/pbr-materials

## Intent
Implement M9: PBR materials pipeline — Cook-Torrance BRDF, ECS-driven lights, PBR textures, IBL, HDR tone mapping, and sphere grid demo.

## What Changed

## Decisions
- 2026-02-18 Multi-bind-group layout (4 groups: scene, object, material, environment) — separates data by update frequency, matches standard PBR engine patterns
- 2026-02-18 CPU-side IBL generation — avoids compute shader complexity with wgpu4k; acceptable for small cubemaps (32-64px)
- 2026-02-18 Replace old RenderSystem in-place — single PBR pipeline handles all materials (metallic=0, roughness=1 approximates old Lambert look)

## Issues

## Commits

## Progress
- [ ] Step 1: Data model expansion + math helpers
- [ ] Step 2: UV sphere mesh generator with tangents
- [ ] Step 3: PBR shaders + multi-bind-group renderer
- [ ] Step 4: ECS light integration + update RenderSystem
- [ ] Step 5: HDR render target + tone mapping pass
- [ ] Step 6: IBL — procedural irradiance, prefiltered env, BRDF LUT
- [ ] Step 7: PBR sphere grid demo + UI controls
- [ ] Step 8: Platform validation + docs
