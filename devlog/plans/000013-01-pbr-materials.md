# 000013-01 — PBR Materials (M9)

## Context
Prism renders with simple Lambert diffuse + hardcoded ambient. Material already declares metallic/roughness but they're unused. M9 delivers Cook-Torrance BRDF, ECS lights, PBR textures, IBL, HDR tone mapping.

## Plan
See full plan in conversation transcript. 8 incremental steps:
1. Data model expansion (Material, Color, Mat4, LightData)
2. UV sphere mesh with tangents
3. PBR shaders + multi-bind-group renderer (largest step)
4. ECS light integration + RenderSystem update
5. HDR render target + tone mapping
6. IBL generation (BRDF LUT, procedural sky, irradiance, prefiltered)
7. Sphere grid demo + UI controls
8. Platform validation + docs
