# 000012: Android Rendering Support (M8)

**Agent:** Claude Code (claude-opus-4-6) @ prism branch android-support
**Intent:** Implement Android wgpu4k rendering support (issue #9), migrate to `com.android.kotlin.multiplatform.library`, add Android targets to all modules, create Android demo activity.

## What Changed

2026-02-16T14:00-08:00 AGENTS.md — Consolidated duplicate worktree workflow sections; added frozen-snapshot instruction; updated project status for M8 completion; updated Android platform description and tech stack.

2026-02-16T14:05-08:00 gradle/libs.versions.toml — Added `android-kotlin-multiplatform-library` plugin alias.

2026-02-16T14:05-08:00 build.gradle.kts — Added `android.kotlin.multiplatform.library` plugin declaration with `apply false`.

2026-02-16T14:10-08:00 prism-math, prism-core, prism-renderer, prism-native-widgets build.gradle.kts — Migrated from `com.android.library` + `androidTarget()` + top-level `android {}` to `com.android.kotlin.multiplatform.library` + `kotlin { android { } }`.

2026-02-16T14:15-08:00 prism-ecs, prism-scene, prism-input, prism-assets, prism-audio, prism-compose build.gradle.kts — Added Android targets using `com.android.kotlin.multiplatform.library` plugin with `kotlin { android { } }` configuration.

2026-02-16T14:15-08:00 prism-assets/src/androidMain/.../FileReader.android.kt — New actual for FileReader (same as JVM impl). Required because Android is now a separate target from JVM.

2026-02-16T14:15-08:00 prism-compose/src/androidMain/.../PrismView.android.kt — New actual for PrismView (placeholder Box). Required because Android is now a separate target from JVM.

2026-02-16T14:20-08:00 prism-native-widgets/src/androidMain/.../PrismSurface.android.kt — Replaced stub with real wgpu4k implementation. Added `createPrismSurface(surfaceHolder, width, height)` factory using `androidContextRenderer()`. Added wgpu4k + coroutines-android deps to androidMain sourceSet.

2026-02-16T14:25-08:00 prism-demo/build.gradle.kts — Added `com.android.application` plugin, `androidTarget()`, android application block (namespace, compileSdk, minSdk, targetSdk, applicationId). Added coroutines-android dep for androidMain.

2026-02-16T14:25-08:00 prism-demo/src/androidMain/AndroidManifest.xml — New manifest with PrismDemoActivity as launcher.

2026-02-16T14:25-08:00 prism-demo/src/androidMain/.../PrismDemoActivity.kt — New Activity with SurfaceView + Choreographer render loop + DemoScene integration.

## Decisions

2026-02-16T14:05-08:00 Used `android {}` (not `androidLibrary {}`) inside `kotlin {}` — `androidLibrary {}` is deprecated since AGP 8.12. The `android {}` block is the current recommended DSL.

2026-02-16T14:05-08:00 Kept `com.android.application` + `androidTarget()` for prism-demo — the new `com.android.kotlin.multiplatform.library` plugin only works for library modules, not application modules. There's no KMP replacement for `com.android.application` yet.

2026-02-16T14:20-08:00 Used wgpu4k-toolkit-android AAR directly instead of PanamaPort — wgpu4k already provides `androidContextRenderer()` that creates Vulkan surfaces from SurfaceHolder. No need for PanamaPort intermediary.

2026-02-16T14:25-08:00 Used Choreographer.FrameCallback for render loop — vsync-aligned like requestAnimationFrame on web and CADisplayLink on iOS. Consistent with other platform render loops.

2026-02-16T20:00-08:00 Forked webgpu-ktypes (third fork under hyeons-lab) rather than keeping deprecated `writeBuffer` workaround — proper fix at the source ensures all buffer types are correct, not just the ones we happened to use. All three forks (wgpu4k-native, wgpu4k, webgpu-ktypes) use the same branch naming convention and `com.hyeons-lab` group ID.

## Issues

2026-02-16T14:15-08:00 Adding Android target to prism-assets and prism-compose broke build — these modules have expect/actual declarations (FileReader, PrismView) that previously only needed jvmMain actuals. With the new KMP Android plugin, Android is a separate target from JVM and needs its own actuals. Fixed by creating androidMain actual files.

2026-02-16T15:00-08:00 Runtime crash on API 36 (Android 16): `InstantiationError: java.lang.foreign.MemorySegment`. Root cause: wgpu4k-native v27.0.4 ships Panama FFI shim classes in the `java.lang.foreign` package (JNA-backed `MemorySegment`, `SegmentAllocator`, etc.). On API 36, Android includes the real `java.lang.foreign` package in the boot classpath, which shadows the shim. The real `MemorySegment` is an interface and cannot be instantiated. This is an upstream wgpu4k-native issue — the shim approach works on API 28-34 but breaks on API 35+ where Android added its own `java.lang.foreign` support. **Fixed:** Forked and relocated shims to `com.hyeonslab.foreign`.

2026-02-16T17:00-08:00 Second runtime crash on API 36: `RuntimeException: Failed to get ByteBuffer address`. Root cause: `Queue.native.android.kt` uses `getDeclaredMethod("address")` reflection on `ByteBuffer`, which is a hidden API blocked on API 36. **Fixed:** Replaced with JNA `Native.getDirectBufferPointer()`.

2026-02-16T18:00-08:00 White screen on API 36 despite render loop running at 120fps. No exceptions. Render pipeline fully executing (configure → getCurrentTexture → render → submit → present). Root cause was TWO bugs: sRGB double-gamma encoding and ByteBuffer byte order. See "White Screen Root Cause: Two Bugs" section below.

2026-02-16T19:30-08:00 sRGB double-gamma: Android Vulkan preferred format is `RGBA8UnormSrgb`. Prism Color values are already sRGB-encoded → GPU applies gamma again → near-white output. Fixed with `srgbToLinear()` conversion detecting sRGB surface format.

2026-02-16T19:45-08:00 ByteBuffer byte order: `webgpu-ktypes` `ArrayBuffer.of(FloatArray)` uses `ByteBuffer.allocateDirect()` defaulting to BIG_ENDIAN. ARM64 Android is LITTLE_ENDIAN → all multi-byte GPU data garbled. Fixed in webgpu-ktypes fork with `.order(ByteOrder.nativeOrder())`.

2026-02-16T16:00-08:00 Forked wgpu4k-native to `hyeons-lab/wgpu4k-native`, branch `fix/android-api35-package-relocation`. Relocated 5 shim files from `java.lang.foreign` to `com.hyeonslab.foreign`. Updated imports in `ffi/FFI.kt`, `ffi/MemoryAllocator.android.kt`, `Structures.android.kt`, and generator template. Changed Maven group to `com.hyeons-lab` to avoid collision with upstream snapshots. Published to Maven local.

2026-02-16T16:30-08:00 Forked wgpu4k to `hyeons-lab/wgpu4k`, branch `fix/android-api35-wgpu4k-native-snapshot`. Updated `gradle/libs.versions.toml` to reference `com.hyeons-lab:wgpu4k-native` coordinates. Published to Maven local. First crash (InstantiationError) fixed.

2026-02-16T17:00-08:00 Second runtime crash on API 36: `RuntimeException: Failed to get ByteBuffer address`. Root cause: `Queue.native.android.kt` uses reflection (`getDeclaredMethod("address")`) to access hidden `ByteBuffer.address()` API, blocked on API 36. Fixed by replacing with `Pointer.nativeValue(Native.getDirectBufferPointer(this)).toULong()` (JNA public API). Committed to existing wgpu4k fork branch. Rebuilt and published.

2026-02-16T17:30-08:00 settings.gradle.kts — Added `includeGroup("com.hyeons-lab")` to mavenLocal content filter so Gradle can resolve the forked wgpu4k-native artifacts.

2026-02-16T18:00-08:00 APK installed and launched on Galaxy Fold (API 36). No crashes! Render loop running at 120fps. But display shows white screen — no cube, no blue background.

2026-02-16T19:30-08:00 WgpuRenderer.kt — Added `surfaceIsSrgb` detection and `srgbToLinear()` conversion for clear color and material color. Changed `alphaMode` to `Auto`, `usage` to `RenderAttachment` only. Fixed white screen caused by sRGB double-gamma.

2026-02-16T19:45-08:00 WgpuRenderer.kt — Switched from deprecated `writeBuffer(FloatArray/IntArray)` to `writeBuffer(ArrayBuffer.of(array))` using fixed webgpu-ktypes fork with correct byte order.

2026-02-16T20:00-08:00 Forked webgpu-ktypes to `hyeons-lab/webgpu-ktypes`, branch `fix/android-api35-package-relocation`. Added `.order(ByteOrder.nativeOrder())` to all multi-byte `ByteBuffer.allocateDirect()` calls. Changed group to `com.hyeons-lab`. Updated wgpu4k fork to reference new coordinates (`com.hyeons-lab:webgpu-ktypes:0.0.9-SNAPSHOT`). Upgraded both forks to Gradle 9.2.0.

## Debugging: White Screen Investigation

### Confirmed Working
- wgpu instance, adapter, device creation: OK (no errors in logs)
- `androidContextRenderer()` completes successfully with `SurfaceRenderingContext`
- `WgpuRenderer.initialize()` calls `surface.configure()` with correct format/alphaMode
- `DemoScene` initialized (logged)
- `Choreographer.FrameCallback` running at 120fps (logged every 60 frames)
- `scene.tick()` runs without exceptions (try/catch confirms no render errors)
- Full render pipeline executes: `beginFrame()` → `beginRenderPass(CORNFLOWER_BLUE)` → draw cube → `endRenderPass()` → `endFrame()` (submit + present)

### Hypothesis: SurfaceView Z-ordering
Android `SurfaceView` places its surface BEHIND the window by default. The framework punches a transparent "hole" through the window background so the surface shows through. If this hole-punching doesn't work correctly, the opaque white window background covers the Vulkan surface.

Supporting evidence:
- White = window background color (Activity default theme)
- wgpu4k-native's own Android demo uses `setWillNotDraw(false)` + `draw(Canvas)` + `invalidate()` which is a different rendering pattern tied to Android's view drawing system
- Vulkan `wgpuSurfacePresent()` presents to the ANativeWindow, which maps to the SurfaceView's dedicated Surface — this is behind the window by default

### Next Steps
1. Try `surfaceView.setZOrderOnTop(true)` — places Surface on top of window, bypassing hole-punching
2. If that fails, try custom SurfaceView subclass with `setWillNotDraw(false)` + `draw()` + `invalidate()` approach matching wgpu4k-native demo
3. If that fails, investigate whether `wgpuSurfaceConfigure` + `wgpuSurfacePresent` actually work correctly with `androidContextRenderer()` on API 36

### White Screen Root Cause: Two Bugs

**Bug 1: sRGB double-gamma encoding**
Android Vulkan preferred surface format is `RGBA8UnormSrgb`. Prism `Color` stores sRGB values (e.g., CORNFLOWER_BLUE = 0.392, 0.584, 0.929). When passed to an sRGB surface, the GPU applies gamma encoding again — `pow(srgb, 1/2.2)` on already-encoded values produces near-white. Pure 0.0/1.0 values are unaffected (identical in linear and sRGB space), which is why pure GREEN worked but CORNFLOWER_BLUE appeared white.

**Fix:** Added `surfaceIsSrgb` detection (`textureFormat.name.endsWith("Srgb")`) and `srgbToLinear()` conversion (standard IEC 61966-2-1 formula) applied to clear color and material color values.

**Bug 2: ByteBuffer byte order on Android ARM64**
`webgpu-ktypes` v0.0.9 `ArrayBuffer.of(FloatArray)` uses `ByteBuffer.allocateDirect()` which defaults to BIG_ENDIAN regardless of platform. ARM64 Android is LITTLE_ENDIAN. All multi-byte GPU buffer data (vertices, indices, uniforms) was byte-swapped — garbled geometry invisible to the renderer.

**Fix:** Forked webgpu-ktypes to `hyeons-lab/webgpu-ktypes`, added `.order(ByteOrder.nativeOrder())` to all `ByteBuffer.allocateDirect()` calls for multi-byte types (Short, Int, Float, Double, UShort, UInt). ByteArray/UByteArray not affected (single-byte, no endianness). Changed group to `com.hyeons-lab`. Updated wgpu4k fork to reference new coordinates.

2026-02-16T20:00-08:00 Both fixes confirmed working — rotating lit cube visible on Galaxy Fold (API 36).

### Known Upstream Bug (not blocking)
`Surface.native.kt:90` — `toAlphaMode()` uses `formatCount` instead of `alphaModeCount` to size the alpha modes array. Works because the array read still produces valid values, but technically reads wrong count.

### Upstream Patches Applied (updated)

| Repo | Fork | Branch | What |
|------|------|--------|------|
| wgpu4k-native | hyeons-lab/wgpu4k-native | `fix/android-api35-package-relocation` | Relocate `java.lang.foreign` shims → `com.hyeonslab.foreign`; change group to `com.hyeons-lab` |
| wgpu4k | hyeons-lab/wgpu4k | `fix/android-api35-wgpu4k-native-snapshot` | Reference `com.hyeons-lab` coordinates; fix `Queue.native.android.kt` ByteBuffer reflection; use `com.hyeons-lab:webgpu-ktypes` fork; upgrade Gradle to 9.2.0 |
| webgpu-ktypes | hyeons-lab/webgpu-ktypes | `fix/android-api35-package-relocation` | Fix ByteBuffer byte order (nativeOrder); change group to `com.hyeons-lab`; upgrade Gradle to 9.2.0 |

All three patches required for Android API 35+ (Android 15+). Published to Maven local.

## What Changed (continued)

2026-02-17 devlog/fork-dependencies.md — **NEW**: Standalone reference document explaining why we maintain forks of wgpu4k-native, wgpu4k, and webgpu-ktypes. Covers the full dependency chain, each fork's specific bug and fix, a summary table, and guidance on when forks can be dropped.

2026-02-17 AGENTS.md — Resolved merge conflict during rebase onto origin/main. Kept the android-support version that references the dedicated Git Worktree Workflow section (origin/main had inlined a duplicate worktree section under "Branching & Plan Workflow").

## Decisions (continued)

2026-02-17 Added fork-dependencies.md as a standalone devlog reference (not branch-numbered) — fork rationale is cross-cutting context that applies across branches and shouldn't be buried in a single branch's devlog. Supplements the detailed narrative in this file with a clean, skimmable reference.

2026-02-17 Rebased onto origin/main — origin/main had new commit (0fdf3ee: AGENTS.md worktree/devlog sync). Resolved one conflict in AGENTS.md.

## What Changed (continued 2)

2026-02-17 prism-android-demo/ — **NEW MODULE**: Thin Android application module extracted from prism-demo. Contains PrismDemoActivity.kt and AndroidManifest.xml (moved from prism-demo/src/androidMain/). build.gradle.kts uses `android.application` + `kotlin.android` plugins, depends on :prism-demo, :prism-native-widgets, kermit, wgpu4k, wgpu4k-toolkit, coroutines-android.

2026-02-17 prism-demo/build.gradle.kts — Replaced `android.application` plugin with `android.kotlin.multiplatform.library`. Replaced `androidTarget()` + top-level `android {}` block with `android {}` inside `kotlin {}` (library-style: namespace + compileSdk + minSdk only, no applicationId/versionCode/versionName). Removed prism-demo/src/androidMain/ directory (PrismDemoActivity.kt + AndroidManifest.xml moved to prism-android-demo).

2026-02-17 settings.gradle.kts — Added `include(":prism-android-demo")`.

## Decisions (continued 2)

2026-02-17 Extracted Android app into separate module — combining `kotlin.multiplatform` + `android.application` in one module is deprecated in AGP 8.x and will break in AGP 9. Mirrors the iOS pattern where prism-ios-demo (Xcode app) is separate from prism-demo (KMP framework).

2026-02-17 Removed Compose plugins from prism-android-demo — PrismDemoActivity is a plain Activity with SurfaceView, no Compose APIs. Adding Compose plugins without Compose Runtime on classpath causes compiler error. Can be added later if a Compose-based Android demo is needed.

2026-02-17 Added explicit dependencies (prism-native-widgets, kermit, wgpu4k, wgpu4k-toolkit) to prism-android-demo — prism-demo declares these as `implementation` (not `api`), so they're not transitively visible. PrismDemoActivity directly uses createPrismSurface, Logger, and WGPUContext from these libraries.

## Commits

404e069 — chore: add devlog and plan for Android rendering (M8)
d39c6ab — feat: implement Android rendering support (M8)
1493b90 — docs: document Android API 36 runtime crash (upstream wgpu4k-native issue)
040b33a — fix: resolve Android white screen with sRGB color correction and byte-order fix
0c7e177 — fix: use non-deprecated ArrayBuffer.of() for GPU buffer writes
290bcff — docs: update devlog with sRGB, byte-order fixes and webgpu-ktypes fork
b496769 — docs: update all docs for completed Android rendering (M8)
f567837 — build: update CI to build all three wgpu fork dependencies
c428573 — fix: exclude library entries from version extraction in CI
faeccfb — build: update wgpu4k-native to Kotlin 2.3.0 / JDK 25, increase CI timeout
9a3b0b5 — fix: address critical review findings
9425478 — fix: cancel initJob in stopRendering to prevent lifecycle race
9137acf — docs: add fork dependency rationale reference
