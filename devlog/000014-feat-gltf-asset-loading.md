# 000014-feat-gltf-asset-loading

## Agent
Claude (claude-sonnet-4-6) @ repository:prism branch:feat/gltf-asset-loading

## Intent
Implement glTF 2.0 (.gltf + .glb) asset loading in prism-assets per issue #11. Full mesh, material, and scene hierarchy import with ECS entity creation. Also add a full-featured DamagedHelmet demo across Flutter web, standalone WASM, and the docs landing page.

## What Changed
2026-02-18T23:49-08:00 devlog/000014-feat-gltf-asset-loading.md — create branch devlog
2026-02-18T23:49-08:00 devlog/plans/000014-01-gltf-asset-loading.md — create implementation plan
2026-02-19T00:13-08:00 prism-assets/build.gradle.kts — add kotlin.serialization plugin, kotlinx-serialization-json, prism-ecs, coroutines-test deps
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../GltfTypes.kt — internal @Serializable data classes for glTF 2.0 JSON schema
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../GlbReader.kt — GLB binary container parser with magic validation
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../ImageDecoder.kt — expect declaration + ImageData type
2026-02-19T00:13-08:00 prism-assets/src/{jvm,android,native,wasmJs}Main/.../ImageDecoder.*.kt — platform actuals (JVM: javax.imageio; wasmJs: stub returning null; native: stub returning null)
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../GltfAsset.kt — public asset type with instantiateInWorld()
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../GltfLoader.kt — full glTF/GLB loader, mesh/material/scene traversal
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../MeshLoader.kt — removed gltf/glb from supported extensions
2026-02-19T00:13-08:00 prism-assets/src/commonMain/.../AssetManager.kt — register GltfLoader
2026-02-19T00:13-08:00 prism-renderer/src/commonMain/.../Renderer.kt — add uploadTextureData() default method
2026-02-19T00:13-08:00 prism-renderer/src/commonMain/.../WgpuRenderer.kt — implement uploadTextureData() via device.queue.writeTexture
2026-02-19T00:22-08:00 prism-assets/src/iosMain/.../ImageDecoder.ios.kt — CGImage decoder (UIImage → RGBA8 pixels)
2026-02-19T00:22-08:00 prism-assets/src/macosArm64Main/.../ImageDecoder.macos.kt — CGImage decoder (NSImage → RGBA8 pixels)
2026-02-19T06:41-08:00 prism-assets/src/commonMain/.../ImageDecoder.kt — add unpremultiplyAlpha() and premultiplied: Boolean flag to ImageData
2026-02-19T07:21-08:00 prism-renderer/src/commonMain/.../WgpuRenderer.kt — per-material texture bind groups: textureViewCache, materialBindGroupCache, materialUniformBufferCache; getOrCreateMaterialBindGroup() lazy-creates GPU bind group with actual texture views for materials that have non-null textures
2026-02-19T07:21-08:00 prism-demo-core/src/commonMain/.../DemoScene.kt — made orbitRadius a constructor parameter (default 12f)
2026-02-19T07:21-08:00 prism-demo-core/src/commonMain/.../GltfDemoScene.kt (NEW) — createGltfDemoScene() factory: parse GLB, upload textures, instantiate nodes into ECS world, orbit radius 3.5f; uploadGltfTextures() private extension avoids circular dep
2026-02-19T07:21-08:00 prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt — fetch GLB via JS fetch, use createGltfDemoScene, remove PBR setter exports, add glbUrl param to prismInit
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/android/.../PrismPlatformView.kt — complete rewrite: fixed stale M11-before-M9 code (cubeEntity/cubeColor refs removed), load GLB from flutter_assets/, drag-to-rotate via OnTouchListener
2026-02-19T07:21-08:00 prism-demo-core/src/iosMain/.../IosDemoController.kt — add configureDemoWithGltf(), loadBundleAssetBytes(), orbitBy() on IosDemoHandle
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/ios/Classes/PrismPlatformView.swift — call configureDemoWithGltf(), add UIPanGestureRecognizer for drag-to-rotate
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/example/lib/main.dart — remove PBR sliders, add loading indicator (polls isInitialized()), keep FPS chip + pause button
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/lib/src/prism_engine_channel.dart — remove setMetallic/Roughness/EnvIntensity
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/lib/src/prism_engine_web.dart — remove setMetallic/Roughness/EnvIntensity
2026-02-19T07:21-08:00 prism-flutter/flutter_plugin/lib/src/prism_web_plugin.dart — remove JS bindings for removed WASM exports; update prismInit to pass glbUrl
2026-02-19T07:32-08:00 docs/gltf-demo.js (NEW) — pure WebGPU JS glTF renderer for landing page: loads DamagedHelmet.glb, parses GLB binary, PBR with 5 textures + normal mapping, drag-to-orbit, scroll-to-zoom, DPR-aware resize
2026-02-19T07:32-08:00 docs/index.html — add #gltf-demo section with DamagedHelmet canvas; update M10 milestone to done
2026-02-19T07:32-08:00 docs/DamagedHelmet.glb — Khronos sample model added to docs
2026-02-19T07:42-08:00 prism-demo-core/src/commonMain/.../GltfDemoScene.kt — apply initial azimuth offset matching DamagedHelmet's 180° Y rotation; fix orbit direction
2026-02-19T07:42-08:00 docs/gltf-demo.js — correct initial azimuth to match node rotation; chunk type validation added
2026-02-19T08:41-08:00 prism-renderer/src/.../Texture.kt — `val descriptor` → `var descriptor` (allows post-load sRGB format assignment); TextureDescriptor gets separate `minFilter`/`magFilter` fields
2026-02-19T08:41-08:00 prism-renderer/src/.../Renderer.kt — add `initializeTexture(texture: Texture)` default method (in-place GPU init); add `invalidateMaterial(material)` default method
2026-02-19T08:41-08:00 prism-assets/src/.../ImageDecoder.kt — `unpremultiplyAlpha()` bounds check: `require(size % 4 == 0)`
2026-02-19T08:41-08:00 prism-assets/src/.../GlbReader.kt — require totalLength >= 12; require chunkLength >= 0; `trimEnd(' ')` (spec-correct padding trim)
2026-02-19T08:41-08:00 prism-assets/src/.../GltfLoader.kt — texture format assignment (sRGB for albedo/emissive, UNORM for normals/MR/occlusion); normalized int accessor support (UBYTE/USHORT/BYTE/SHORT); primitive mode check (skip non-TRIANGLES); scene graph cycle detection; bounds check for accessor reads; negative scale decompose (determinant check); separate minFilter/magFilter from sampler; imageData now parallel to textures (not images)
2026-02-19T08:41-08:00 prism-assets/src/.../GltfAsset.kt — doc comment fix: imageData is parallel to textures, not images; default material for null node.material
2026-02-19T08:41-08:00 prism-demo-core/src/.../GltfDemoScene.kt — use `renderer.initializeTexture(assetTexture)` instead of createTexture + handle copy
2026-02-19T08:41-08:00 prism-renderer/src/.../WgpuRenderer.kt — dynamic object UBO pool (lazy-growing array of buffer+bindgroup pairs, reset index per beginRenderPass); all materials routed through getOrCreateMaterialBindGroup(); per-frame material uniform refresh; sampler cache keyed by SamplerKey(min/mag/wrapU/wrapV); `initializeTexture()` override; `invalidateMaterial()` override; resize guard (width/height <= 0)
2026-02-19T08:41-08:00 prism-flutter/flutter_plugin/ios/Classes/PrismPlatformView.swift — `showErrorLabel()` overlays label (tag 999) without removing mtkView
2026-02-19T08:41-08:00 prism-flutter/flutter_plugin/example/lib/main.dart — pause button debounce (750ms window after toggle, suppress state poll)
2026-02-19T08:41-08:00 prism-flutter/flutter_plugin/lib/src/prism_web_plugin.dart — 15-second timeout for WASM module load
2026-02-19T08:41-08:00 docs/gltf-demo.js — `accessorData()` handles byteStride (strided fallback); sRGB swap chain warning
2026-02-19T08:41-08:00 prism-assets/src/commonTest/.../GltfLoaderTest.kt — 8 new tests: mode rejection, cycle detection, normalized UBYTE UVs, negative scale, sRGB/UNORM format assignment, imageData parallel to textures
2026-02-19T08:41-08:00 prism-assets/src/commonTest/.../GlbReaderTest.kt — 3 new tests: negative totalLength, negative chunkLength, space-padded JSON trim
2026-02-19T08:41-08:00 prism-assets/src/commonTest/.../UnpremultiplyTest.kt — 2 new tests: non-multiple-of-4 throws, empty array no-op
2026-02-19T12:08-08:00 prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt — correct drag direction (negate dx for azimuth); fix WASM pre-load race for prismGetState/prismTogglePause
2026-02-19T12:08-08:00 prism-flutter/flutter_plugin/android/.../PrismPlatformView.kt — touch listener returns `true` for ACTION_UP/ACTION_CANCEL; correct drag direction (negate dx)
2026-02-19T12:24-08:00 prism-assets/src/wasmJsMain/.../ImageDecoder.wasmJs.kt — implement WASM image decoder via createImageBitmap + OffscreenCanvas (was null stub)
2026-02-19T12:50-08:00 prism-demo-core/src/iosMain/.../IosDemoController.kt — fix NSData init call (use `dataWithBytes:length:` pattern); add auto-orbit during pause for visible pause effect
2026-02-19T12:59-08:00 prism-assets/src/.../GltfLoader.kt — GltfLoadResult class; loadStructure() fast-path parses GLB structure without image decode; extractRawImageBytes() private helper extracts raw PNG/JPEG bytes per texture
2026-02-19T12:59-08:00 prism-renderer/src/.../Renderer.kt — `invalidateMaterial(material)` default method (already added in review pass; confirmed present)
2026-02-19T12:59-08:00 prism-renderer/src/.../WgpuRenderer.kt — `invalidateMaterial()` override: evicts from materialBindGroupCache + materialUniformBufferCache
2026-02-19T12:59-08:00 prism-demo-core/src/.../GltfDemoScene.kt — progressiveScope: CoroutineScope? param; when non-null uses loadStructure() + background decode loop with per-texture decode→initializeTexture→uploadTextureData→invalidateMaterial; buildTexToMaterialsMap() reverse index for targeted cache invalidation
2026-02-19T12:59-08:00 prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt — pass GlobalScope as progressiveScope; `suspendCoroutine` → `suspendCancellableCoroutine`; 4-byte chunked GLB copy via `int8ArrayReadInt32LE` @JsFun helper (4× fewer JS boundary crossings)
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — full rewrite: DamagedHelmet GLB fetch + progressive loading (progressiveScope = GlobalScope); drag-to-orbit (pointer events); auto-orbit via DemoStore; full-screen canvas with ResizeObserver; falls back to sphere-grid if GLB unavailable
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/resources/index.html — full-screen layout (100vw × 100vh, overflow hidden); title updated to DamagedHelmet glTF
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/resources/DamagedHelmet.glb — copied from docs/ so webpack dev server includes it
2026-02-19T13:30-08:00 docs/gltf-demo.js — add auto-orbit when not dragging (0.305 rad/s ≈ 17.5 deg/s, matching WASM demo default); frame() now accepts RAF timestamp; lastTime tracks inter-frame dt
2026-02-19T13:30-08:00 docs/style.css — #gltf-demo becomes full-viewport section (min-height:100vh, position:relative, padding:0, flex align-end); canvas absolutely fills section; .gltf-demo-overlay gradient text overlay at bottom
2026-02-19T13:30-08:00 docs/index.html — #gltf-demo restructured: canvas + fallback are direct children of section (position:absolute); .gltf-demo-overlay wraps all text content at bottom of viewport

## Decisions
2026-02-18T23:49-08:00 GltfAsset stores renderableNodes (flat list of GltfNodeData) rather than a scene tree — simpler for ECS which has no hierarchy
2026-02-18T23:49-08:00 Used expect/actual for ImageDecoder rather than Coil — prism-assets is a low-level engine module; a UI-focused image loading library is a heavyweight dep for raw pixel extraction
2026-02-19T00:13-08:00 ImageDecoder.native returns null (stub) rather than throwing — allows textured glTF to load without crashing on native; geometry still renders
2026-02-19T07:21-08:00 uploadGltfTextures() placed in GltfDemoScene.kt (prism-demo-core) not Renderer.kt — prism-assets already depends on prism-renderer; putting it on Renderer would create a circular dependency
2026-02-19T07:21-08:00 Drag-to-rotate via native platform handlers only (OnTouchListener Android, UIPanGestureRecognizer iOS, pointer events WASM) — no Flutter MethodChannel orbit API needed per user feedback
2026-02-19T07:21-08:00 Per-material bind group caching keyed by Material value equality — Material is a data class, Texture uses reference equality for equals(), so caching is correct
2026-02-19T08:41-08:00 Object UBO pool pattern chosen over per-frame recreate — pool grows lazily (max ~256 draws), reset index each beginRenderPass; avoids GPU allocation per frame
2026-02-19T08:41-08:00 All materials routed through getOrCreateMaterialBindGroup() — eliminates the shared pbrMaterialUniformBuffer overwrite race; simple, cache-friendly
2026-02-19T08:41-08:00 SamplerKey data class for sampler cache — most models use 1-2 unique samplers; cache eliminates redundant createSampler() calls
2026-02-19T12:59-08:00 No Material field mutability needed for progressive loading — WgpuRenderer already checks `it.handle != null` in getOrCreateMaterialBindGroup, so placeholder (handle=null) textures fall through to default views; invalidateMaterial() evicts the bind group so next setMaterial() call rebuilds with real texture views
2026-02-19T12:59-08:00 texture.descriptor updated to real dimensions before initializeTexture() — avoids allocating a 1×1 GPU texture that can't receive writeTexture with the full image pixels
2026-02-19T12:59-08:00 buildTexToMaterialsMap() scans renderableNodes to build texture→materials reverse map — allows targeted invalidation without scanning all materials every frame

## Issues
2026-02-19T00:13-08:00 GlbReader.read() returned JSON with trailing space padding — fixed by .trimEnd() on decoded string
2026-02-19T07:21-08:00 Android PrismPlatformView.kt (M11) had stale pre-M9 code (createDemoScene with initialColor param, cubeEntity, DemoUiState.cubeColor) — M11 was implemented before M9 PBR; fixed by complete rewrite
2026-02-19T12:08-08:00 Drag direction was inverted on all platforms — dx maps to azimuth, positive dx should decrease azimuth (drag right → rotate left from viewer's perspective); fixed by negating dx multiplier
2026-02-19T12:24-08:00 WASM ImageDecoder was a null stub — glTF textures never displayed on web; fixed with createImageBitmap + OffscreenCanvas implementation
2026-02-19T12:50-08:00 iOS NSData init call used wrong API form — fixed to use `dataWithBytes:length:` pattern

## Commits
89e524d — chore: add devlog and plan for glTF 2.0 asset loading (M10)
754bf3e — feat: implement glTF 2.0 asset loading (M10)
7db8a80 — feat: implement iOS/macOS CGImage decoder for glTF texture loading
b26f287 — feat: add unpremultiply option to ImageDecoder (default: false)
8d4babf — feat: replace Flutter PBR sphere grid with interactive DamagedHelmet glTF demo
00cadc5 — feat: add DamagedHelmet glTF demo to landing page
59ed885 — fix: apply glTF node rotation to DamagedHelmet and correct initial camera azimuth
5f88225 — fix: address all review issues in glTF asset loading branch
55e0096 — fix: correct drag-to-orbit direction and WASM pre-load race conditions
5b32857 — fix: implement WASM image decoder for glTF texture support
8a55501 — chore: update devlog with final commits and bug fixes
39bf082 — fix: iOS NSData init form, add auto-orbit for visible pause effect
50139a2 — feat: progressive glTF texture loading on Flutter web
c744dd9 — chore: update devlog with missing commits and progressive loading notes

## Progress
- [x] Build setup: add kotlinx-serialization to prism-assets
- [x] GltfTypes.kt — internal serializable JSON schema types
- [x] GlbReader.kt — GLB binary container parser
- [x] ImageDecoder.kt — expect/actual image decoding (RGBA8 pixels)
- [x] GltfAsset.kt — public Prism type with meshes, materials, textures, scene nodes
- [x] GltfLoader.kt — AssetLoader<GltfAsset> orchestrator
- [x] Update MeshLoader.kt (remove gltf/glb extensions)
- [x] Update AssetManager.kt (register GltfLoader)
- [x] Unit tests (GlbReaderTest, GltfLoaderTest, UnpremultiplyTest)
- [x] Demo integration (DamagedHelmet.glb via GltfDemoScene + Flutter)
- [x] Review fixes: loader correctness, renderer architecture, platform integration, docs
- [x] Fix drag direction inversion on all platforms (web, Android, iOS)
- [x] Fix WASM race conditions (prismGetState/prismTogglePause before WASM loaded)
- [x] Implement WASM ImageDecoder (was null stub — textures now display on web)
- [x] Progressive glTF texture loading (Flutter web: render geometry immediately, textures stream in)
- [x] Update prism-demo-core WASM demo (full-screen DamagedHelmet, auto-orbit, ResizeObserver)
- [x] Update docs/index.html (full-viewport glTF section, auto-orbit in gltf-demo.js)
