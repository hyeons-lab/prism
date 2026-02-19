# 000014-feat-gltf-asset-loading

## Agent
Claude (claude-sonnet-4-6) @ repository:prism branch:feat/gltf-asset-loading

## Intent
Implement glTF 2.0 (.gltf + .glb) asset loading in prism-assets per issue #11. Full mesh, material, and scene hierarchy import with ECS entity creation.

## Progress
- [ ] Build setup: add kotlinx-serialization to prism-assets
- [ ] GltfTypes.kt — internal serializable JSON schema types
- [ ] GlbReader.kt — GLB binary container parser
- [ ] ImageDecoder.kt — expect/actual image decoding (RGBA8 pixels)
- [ ] GltfAsset.kt — public Prism type with meshes, materials, textures, scene nodes
- [ ] GltfLoader.kt — AssetLoader<GltfAsset> orchestrator
- [ ] Update MeshLoader.kt (remove gltf/glb extensions)
- [ ] Update AssetManager.kt (register GltfLoader)
- [ ] Unit tests (GlbReaderTest, GltfLoaderTest)
- [ ] Demo integration

## What Changed

## Decisions

## Issues

## Commits
