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
2026-02-19T08:41-08:00 prism-assets/src/.../GltfAsset.kt — doc comment fix: imageData is parallel to textures, not images; default Material(WHITE) fallback for null node.material (glTF nodes without materials are valid)
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
2026-02-19T12:59-08:00 prism-renderer/src/.../WgpuRenderer.kt — `invalidateMaterial()` override: evicts from materialBindGroupCache + materialUniformBufferCache; calls `.close()` on evicted resources to prevent GPU leaks
2026-02-19T12:59-08:00 prism-demo-core/src/.../GltfDemoScene.kt — progressiveScope: CoroutineScope? param; when non-null uses loadStructure() + background decode loop with per-texture decode→initializeTexture→uploadTextureData→invalidateMaterial; buildTexToMaterialsMap() reverse index for targeted cache invalidation
2026-02-19T12:59-08:00 prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt — pass GlobalScope as progressiveScope; `suspendCoroutine` → `suspendCancellableCoroutine`; 4-byte chunked GLB copy via `int8ArrayReadInt32LE` @JsFun helper (4× fewer JS boundary crossings)
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — full rewrite: DamagedHelmet GLB fetch + progressive loading (progressiveScope = GlobalScope); drag-to-orbit (pointer events); auto-orbit via DemoStore; full-screen canvas with ResizeObserver; falls back to sphere-grid if GLB unavailable
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/resources/index.html — full-screen layout (100vw × 100vh, overflow hidden); title updated to DamagedHelmet glTF
2026-02-19T13:15-08:00 prism-demo-core/src/wasmJsMain/resources/DamagedHelmet.glb — copied from docs/ so webpack dev server includes it
2026-02-19T13:30-08:00 docs/gltf-demo.js — add auto-orbit when not dragging (0.305 rad/s ≈ 17.5 deg/s, matching WASM demo default); frame() now accepts RAF timestamp; lastTime tracks inter-frame dt
2026-02-19T13:30-08:00 docs/style.css — #gltf-demo becomes full-viewport section (min-height:100vh, position:relative, padding:0, flex align-end); canvas absolutely fills section; .gltf-demo-overlay gradient text overlay at bottom
2026-02-19T13:30-08:00 docs/index.html — #gltf-demo restructured: canvas + fallback are direct children of section (position:absolute); .gltf-demo-overlay wraps all text content at bottom of viewport

2026-02-19T20:30-08:00 prism-assets/src/commonMain/.../ImageDecoder.kt — add public `nativePixelBuffer: Any?` field to ImageData for zero-copy WASM GPU upload
2026-02-19T20:30-08:00 prism-assets/src/wasmJsMain/.../ImageDecoder.wasmJs.kt — zero-copy WASM decoder: return JS ArrayBuffer directly via ArrayBuffer.wrap() instead of copying 16MB pixel-by-pixel into Kotlin; eliminates ~4M JS interop calls per 2K texture
2026-02-19T20:30-08:00 prism-renderer/src/commonMain/.../WgpuRenderer.kt — add internal writeTextureFromArrayBuffer(); refactor uploadTextureData() to delegate to it
2026-02-19T20:30-08:00 prism-demo-core/src/commonMain/.../TextureUploadHelper.kt (NEW) — expect uploadDecodedImage() helper
2026-02-19T20:30-08:00 prism-demo-core/src/{jvm,ios,android,macos}Main/.../TextureUploadHelper.*.kt (NEW) — actual impls: all delegate to renderer.uploadTextureData() with pixels
2026-02-19T20:30-08:00 prism-demo-core/src/wasmJsMain/.../TextureUploadHelper.wasmJs.kt (NEW) — actual impl: zero-copy path via writeTextureFromArrayBuffer() when nativePixelBuffer is available
2026-02-19T20:30-08:00 prism-demo-core/src/commonMain/.../GltfDemoScene.kt — use uploadDecodedImage() in both progressive and non-progressive paths; add yield() between progressive texture uploads
2026-02-19T20:30-08:00 devlog/plans/000013-02-flutter-desktop-support.md — zero-copy WASM texture upload plan (Steps 1–8)

2026-02-19T17:45-08:00 prism-native-widgets/src/wasmJsMain/.../WasmInterop.kt (NEW) — internal @JsFun JS interop primitives (jsGetCanvasById, jsSetCanvasSize, jsWindowWidth/Height, jsNow, jsNextFrame, jsOnBeforeUnload, jsInstallPointerDrag, jsInstallResizeObserver) moved from demo PlatformBridge.kt
2026-02-19T17:45-08:00 prism-native-widgets/src/wasmJsMain/.../WasmUtils.kt (NEW) — public suspend fun fetchBytes(url): ByteArray? moved into SDK so consumers don't reimplement it
2026-02-19T17:45-08:00 prism-native-widgets/src/wasmJsMain/.../PrismSurface.wasmJs.kt — added canvas param, onPointerDrag/onResize/startRenderLoop methods, createPrismSurface(canvasId) overload; SDK handles all RAF/ResizeObserver/beforeunload boilerplate
2026-02-19T17:45-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — simplified to 5-step showcase; removed DemoStore/auto-orbit; added isAutoStartDisabled() check (pbr.html sets window.prismSkipAutoStart = true to opt out)
2026-02-19T17:45-08:00 prism-flutter/src/wasmJsMain/.../FlutterWasmEntry.kt — removed fetchGlbBytes + 4 @JsFun helpers; now imports fetchBytes from prism-native-widgets
2026-02-19T19:30-08:00 prism-demo-core/src/commonMain/.../MaterialPresetScene.kt (NEW) — createMaterialPresetScene(): 5 spheres in a row (Gold, Chrome, Worn Metal, Ceramic, Obsidian); orbit radius 5f
2026-02-19T19:30-08:00 prism-demo-core/src/commonMain/.../CornellBoxScene.kt (NEW) — createCornellBoxScene(): 5 Mesh.quad() walls (red left, green right, white back/floor/ceiling) + 2 spheres; orbit radius 7f; point light near ceiling simulates area light
2026-02-19T19:30-08:00 prism-demo-core/src/wasmJsMain/.../DemoExports.kt (NEW) — @JsExport prismStartPbr(canvasId, sceneName) and prismSetScene(sceneName); module-level pbrCanvas/pbrSurface/pbrScene state; scene switch calls surface.detach() to stop old render loop and release GPU before reinitializing
2026-02-19T19:30-08:00 docs/wasm/pbr.html (NEW) — standalone PBR demo page: 2-tab UI (Material Presets / Cornell Box), sets window.prismSkipAutoStart=true, polls for WASM exports, sizes canvas to window.innerWidth/Height before calling prismStartPbr
2026-02-19T19:30-08:00 docs/index.html — #demo section replaced with pbr.html iframe (full-viewport, same layout as #gltf-demo); glTF section renamed #gltf-demo; removed demo.js import
2026-02-19T19:30-08:00 docs/style.css — #demo added to #gltf-demo selector (full-viewport min-height:100vh, position:relative, flex align-end)

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
2026-02-19T20:30-08:00 nativePixelBuffer made public (not @PublishedApi internal) — @PublishedApi only allows cross-module access from inline functions; TextureUploadHelper.wasmJs.kt needs direct access from a non-inline function, so public is required
2026-02-19T20:30-08:00 Used as? ArrayBuffer (safe cast) in WASM actual to avoid detekt UnsafeCast rule — if cast fails (shouldn't happen), falls back to slow pixels path
2026-02-19T20:30-08:00 yield() added after each progressive texture upload — gives render loop a frame boundary between uploads, prevents multi-second jank mid-progressive-load

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
5d90533 — fix: apply remaining review fixes from glTF code review
195475d — docs: make glTF demo section full-viewport with auto-orbit
668d2a4 — chore: reconcile devlog conventions across AGENTS.md and CONVENTIONS.md
258992e — feat: update WASM demo to load DamagedHelmet.glb with pointer-drag orbit

2026-02-19T22:10-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — rewrite: replace isAutoStartDisabled() / @JsExport polling approach with window.prismPbrScene routing; add getPbrSceneName() / consumePendingSceneSwitch() @JsFun helpers; add startPbrScene() private suspend fun; main() routes to PBR or glTF based on prismPbrScene global
2026-02-19T22:10-08:00 docs/wasm/pbr.html — rewrite startup: set window.prismPbrScene='hero' instead of prismSkipAutoStart; remove polling block; switchScene() sets window.prismNextScene instead of calling prismSetScene export
2026-02-19T22:10-08:00 prism-demo-core/src/wasmJsMain/.../DemoExports.kt — deleted; logic absorbed into Main.kt startPbrScene()

## Decisions
2026-02-19T22:10-08:00 Abandoned @JsExport polling for PBR demo — webpack's async module wrapping (t.a() with lazy getters) captures const i before the await resolves, so the getter always returns undefined from JS even after WASM loads; switching to window.prismPbrScene global read inside main() bypasses webpack's module system entirely
2026-02-19T22:10-08:00 Scene switching via window.prismNextScene polled each frame — simpler than passing a Kotlin lambda reference to JS; render loop calls consumePendingSceneSwitch() each frame, detaches old surface, and launches new coroutine for next scene

## Issues
2026-02-19T22:10-08:00 @JsExport + webpack async module = undefined getters — DemoExports.kt exported prismStartPbr/prismSetScene via @JsExport @JsName; webpack module 525 wraps the Kotlin-generated .mjs (which uses top-level await) in t.a() with lazy getters `()=>i`; these getters capture `const i` from inside the try block but the getter is defined before the await, so i is in TDZ or captures undefined; pbr.html polling typeof mod.prismStartPbr always saw 'undefined' even after WASM loaded; confirmed via Python binary inspection that prismStartPbr IS in the WASM export section — the issue is purely webpack's module export wrapping, not the WASM binary

## Progress
- [x] Zero-copy WASM texture upload: nativePixelBuffer on ImageData, ArrayBuffer.wrap() in decoder, writeTextureFromArrayBuffer() in WgpuRenderer, uploadDecodedImage() expect/actual in prism-demo-core, yield() between uploads
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
- [x] Remaining review fixes: GltfAsset null material default, WgpuRenderer.invalidateMaterial .close() GPU leak
- [x] Reconcile devlog conventions (AGENTS.md, CONVENTIONS.md, global CLAUDE.md)
- [x] MaterialPresetScene + CornellBoxScene (5-sphere row, Cornell box walls + spheres)
- [x] PBR scene switcher on docs landing page (pbr.html iframe, window.prismPbrScene routing, window.prismNextScene switching)
2026-02-19T19:25-08:00 prism-renderer/src/commonMain/.../Mesh.kt — fix Mesh.quad() missing tangent data; also Mesh.triangle() still uses positionNormalUv (unchanged, not rendered by PBR pipeline directly)
2026-02-19T19:25-08:00 prism-demo-core/src/commonMain/.../CornellBoxScene.kt — redesign to match Wikipedia Cornell box: two white matte boxes (tall + short rotated ≈17°), single ceiling point light (intensity 150 at ceiling center), removed fill directional light
2026-02-19T19:25-08:00 docs/wasm/pbr.html — fix loading overlay not dismissing on scene switch: register new pointerdown listener per switch, 4s fallback timeout
2026-02-19T22:30-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — add startGltfScene() with lazy GLB fetch + cachedGlbData; startPbrScene dispatches to startGltfScene when next=='gltf'; glTF loop dispatches back to startPbrScene on switch
2026-02-19T22:30-08:00 docs/wasm/pbr.html — add 'Damaged Helmet' third button; toggle all 3 buttons in switchScene(); postMessage to parent page on scene switch; 10s timeout for glTF scene
2026-02-19T22:30-08:00 docs/index.html — merge gltf-demo section into unified #demo; add id attributes to overlay elements; add postMessage listener to update label/title/desc per scene; remove glTF nav link

## Commits (continued)
ab56feb — fix: fix Cornell box rendering and redesign to match Wikipedia reference
f39e668 — fix: match loading overlay to scene background color; add FPS counter
1951fe6 — fix: dismiss loading overlay on first rendered frame, clear canvas on scene switch
3e6e7e1 — refactor: replace per-frame JS polling with event-driven scene switcher
845d670 — refactor: move first-frame callback into PrismSurface, eliminating demo @JsFun glue
4706459 — perf: progressive material presets — async IBL + one sphere per frame
61e785b — fix: guard async IBL coroutine against scene-switch device close

2026-02-19T23:15-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — replace consumePendingSceneSwitch() @JsFun (polled 60×/sec) with @JsExport fun prismSwitchScene(name); zero JS interop per frame for scene checking
2026-02-19T23:15-08:00 docs/wasm/pbr.html — switchScene() calls globalThis['prism-demo-core'].prismSwitchScene(name) instead of window.prismNextScene
2026-02-19T23:30-08:00 prism-native-widgets/src/wasmJsMain/.../WasmInterop.kt — add jsNotifyFirstFrameReady() @JsFun (calls window.prismHideLoading if set)
2026-02-19T23:30-08:00 prism-native-widgets/src/wasmJsMain/.../PrismSurface.wasmJs.kt — add onFirstFrame: (() -> Unit)? param to startRenderLoop; fires after first frame + unconditionally calls jsNotifyFirstFrameReady()
2026-02-19T23:30-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — remove @JsFun notifyFirstFrameReady() and firstFrameNotified boilerplate; use onFirstFrame= lambda instead
2026-02-19T23:45-08:00 prism-demo-core/src/commonMain/.../DemoScene.kt — add pendingSetup: ArrayDeque<() -> Unit>; tick() and tickWithAngle() process one item per call before world.update()
2026-02-19T23:45-08:00 prism-demo-core/src/commonMain/.../MaterialPresetScene.kt — add progressiveScope: CoroutineScope? param; when non-null queues spheres in pendingSetup and launches async IBL at 64×32 samples (128× less work); when null sync (existing behavior)
2026-02-19T23:45-08:00 prism-renderer/src/commonMain/.../WgpuRenderer.kt — add brdfLutSamples: Int = 256 param to initializeIbl(), passed to IblGenerator.generate()
2026-02-19T23:45-08:00 prism-demo-core/src/wasmJsMain/.../Main.kt — pass progressiveScope = GlobalScope to createMaterialPresetScene()
2026-02-19T23:50-08:00 prism-demo-core/src/commonMain/.../MaterialPresetScene.kt — wrap initializeIbl() in try-catch(Throwable): prevents unhandled exception from crashing WASM runtime when user switches scenes before IBL completes

## Decisions
2026-02-19T23:50-08:00 Catch Throwable (not Exception) in async IBL guard — GPU exceptions in wgpu4k may not be Exception subclasses; Throwable covers all failure modes including Error
2026-02-19T23:15-08:00 @JsExport over window global for scene switch — @JsExport places function directly in the UMD bundle's exports object; JS calls it once per button click instead of Kotlin polling a window global 60×/sec
2026-02-19T23:30-08:00 jsNotifyFirstFrameReady() placed in PrismSurface (not demo code) — embed pages need a standard hook to dismiss loading overlays; making it first-class in the SDK means consumers never need to write @JsFun glue for this pattern

## Issues
2026-02-19T23:50-08:00 Cornell Box and Damaged Helmet tabs failed to load after progressive materials change — root cause: async IBL coroutine in GlobalScope had no exception handler; when surface.detach() closed the device mid-computation, initializeIbl() threw; unhandled GlobalScope exception in WASM can terminate the coroutine dispatcher, preventing the new scene coroutine from running; fixed by try-catch(Throwable)

2026-02-19T23:59-08:00 prism-assets/.../GlbReader.kt — add `binOffset: Int` to GlbContent; record byte offset of BIN chunk data in original GLB so callers can slice image ranges without copying
2026-02-19T23:59-08:00 prism-assets/.../GltfLoader.kt — add `rawTextureByteRanges: List<Pair<Int,Int>?>` to GltfLoadResult; loadStructure() computes (binOffset+bv.byteOffset, bv.byteLength) per texture; null for data-URI/external images
2026-02-19T23:59-08:00 prism-assets/.../ImageDecoder.wasmJs.kt — add `decodeFromJsBuffer(jsBuffer, offset, length)` to actual object; uses `jsBuffer.slice(offset, offset+length)` → Blob → createImageBitmap path; eliminates ~125K JS interop calls per texture (zero Kotlin↔JS copy)
2026-02-19T23:59-08:00 prism-native-widgets/.../WasmUtils.kt — add FetchBytesResult(bytes, nativeBuffer) + fetchBytesWithNativeBuffer(); fetches both Int8Array (for Kotlin copy) and the underlying ArrayBuffer in a single JS call
2026-02-19T23:59-08:00 prism-demo-core/.../GltfDemoScene.kt — move initializeIbl() into progressiveScope.launch at reduced resolution (64×32 BRDF LUT, 8px irradiance, 16px prefiltered); add nativeGlbBuffer: Any? param; progressive loop uses decodeTextureFromNativeBuffer zero-copy when range+buffer available
2026-02-19T23:59-08:00 prism-demo-core/src/commonMain/.../NativeTextureDecode.kt (NEW) — expect fun decodeTextureFromNativeBuffer(nativeBuffer, offset, length): ImageData?
2026-02-19T23:59-08:00 prism-demo-core/src/{wasmJs,jvm,android,ios,macos}Main/.../NativeTextureDecode.*.kt (NEW) — actuals: wasmJs delegates to ImageDecoder.decodeFromJsBuffer; all others return null
2026-02-19T23:59-08:00 prism-demo-core/.../Main.kt — use fetchBytesWithNativeBuffer; cache FetchBytesResult; pass nativeBuffer to createGltfDemoScene in both main() and startGltfScene()

## Decisions
2026-02-19T23:59-08:00 IBL async at reduced resolution for glTF scene — matches MaterialPresetScene pattern; 64×32 BRDF LUT is visually indistinguishable at demo quality; eliminates 1-2s blocking before first frame
2026-02-19T23:59-08:00 decodeTextureFromNativeBuffer placed in prism-demo-core not prism-assets — bridge depends on WASM-specific ImageDecoder.decodeFromJsBuffer; keeping it in demo avoids adding platform-specific SDK surface area; could be promoted to SDK (into ImageDecoder expect/actual) if consumers need it
2026-02-19T23:59-08:00 rawTextureByteRanges defaults to emptyList() — backward-compatible; callers on non-WASM paths get null ranges for all textures and fall back to rawTextureImageBytes copy path unchanged

2026-02-19T23:59-08:00 prism-assets/.../ImageDecoder.kt — promote decodeFromNativeBuffer into expect object ImageDecoder SDK surface; add actual suspend fun decodeFromNativeBuffer(nativeBuffer, offset, length) to all platforms: wasmJs delegates to decodeFromJsBuffer; jvmMain, androidMain, nativeMain return null
2026-02-19T23:59-08:00 prism-demo-core/src/{common,wasmJs,jvm,android,ios,macos}Main/.../NativeTextureDecode.*.kt — deleted; expect/actual bridge no longer needed in demo layer now that SDK provides it
2026-02-19T23:59-08:00 prism-demo-core/.../GltfDemoScene.kt — updated zero-copy decode call from decodeTextureFromNativeBuffer() to ImageDecoder.decodeFromNativeBuffer() directly

## Decisions
2026-02-19T23:59-08:00 Promoted decodeFromNativeBuffer into ImageDecoder SDK — eliminates the NativeTextureDecode expect/actual boilerplate from prism-demo-core; any future consumer of prism-assets gets the zero-copy path without writing their own bridge; null-returning actuals on JVM/Android/native have zero cost on those platforms
