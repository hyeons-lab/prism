# 000014-feat-gltf-asset-loading

## Agent
Claude (claude-sonnet-4-6) @ repository:prism branch:feat/gltf-asset-loading

## Intent
Implement glTF 2.0 (.gltf + .glb) asset loading in prism-assets per issue #11. Full mesh, material, and scene hierarchy import with ECS entity creation.

## Progress
- [x] Build setup: add kotlinx-serialization to prism-assets
- [x] GltfTypes.kt — internal serializable JSON schema types
- [x] GlbReader.kt — GLB binary container parser
- [x] ImageDecoder.kt — expect/actual image decoding (RGBA8 pixels)
- [x] GltfAsset.kt — public Prism type with meshes, materials, textures, scene nodes
- [x] GltfLoader.kt — AssetLoader<GltfAsset> orchestrator
- [x] Update MeshLoader.kt (remove gltf/glb extensions)
- [x] Update AssetManager.kt (register GltfLoader)
- [x] Unit tests (GlbReaderTest 7 tests, GltfLoaderTest 6 tests)
- [ ] Demo integration (load a Khronos sample .glb)

## What Changed
2026-02-19 prism-assets/build.gradle.kts — add kotlin.serialization plugin, kotlinx-serialization-json, prism-ecs, coroutines-test deps
2026-02-19 prism-assets/src/commonMain/.../GltfTypes.kt — internal @Serializable data classes for glTF 2.0 JSON schema
2026-02-19 prism-assets/src/commonMain/.../GlbReader.kt — GLB binary container parser with magic validation
2026-02-19 prism-assets/src/commonMain/.../ImageDecoder.kt — expect declaration + ImageData type
2026-02-19 prism-assets/src/{jvm,android,native,wasmJs}Main/.../ImageDecoder.*.kt — platform actuals
2026-02-19 prism-assets/src/commonMain/.../GltfAsset.kt — public asset type with instantiateInWorld()
2026-02-19 prism-assets/src/commonMain/.../GltfLoader.kt — full glTF/GLB loader, mesh/material/scene traversal
2026-02-19 prism-assets/src/commonMain/.../MeshLoader.kt — removed gltf/glb from supported extensions
2026-02-19 prism-assets/src/commonMain/.../AssetManager.kt — register GltfLoader
2026-02-19 prism-renderer/src/commonMain/.../Renderer.kt — add uploadTextureData() default method
2026-02-19 prism-renderer/src/commonMain/.../WgpuRenderer.kt — implement uploadTextureData() via device.queue.writeTexture

## Decisions
2026-02-19 GltfAsset stores renderableNodes (flat list of GltfNodeData) rather than a scene tree — simpler for ECS which has no hierarchy
2026-02-19 ImageDecoder.native returns null (stub) rather than throwing — allows textured glTF to load without crashing on native (factors will still work)
2026-02-19 Used expect/actual for ImageDecoder rather than Coil — prism-assets is a low-level engine module; a UI-focused image loading library is a heavyweight dep for raw pixel extraction

## Issues
2026-02-19 GlbReader.read() returned JSON with trailing space padding — fixed by .trimEnd() on decoded string

## Commits
89e524d — chore: add devlog and plan for glTF 2.0 asset loading (M10)
754bf3e — feat: implement glTF 2.0 asset loading (M10)
