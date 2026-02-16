# 000009-feat-native-prism-surface-stubs

## Session 1 — Implement native PrismSurface with wgpu4k (2026-02-16 01:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/native-prism-surface-stubs`

### Intent
Complete all native PrismSurface implementations so they create real wgpu surfaces instead of being logging-only stubs. Also fix RenderSurface actuals that throw `TODO()` at runtime. Addresses issue #13.

### What Changed

- **[2026-02-16 01:05 PST]** `prism-native-widgets/build.gradle.kts` — Added `nativeMain.dependencies` block with wgpu4k, wgpu4k-toolkit, and kotlinx-coroutines-core. Previously only jvmMain had these.

- **[2026-02-16 01:06 PST]** `prism-native-widgets/src/iosMain/.../PrismSurface.ios.kt` — Full implementation: accepts `MTKView?` via primary constructor (default null for commonMain compat). Deferred surface creation via `iosContextRenderer()` on first valid `resize()`. Exposes `wgpuContext` property. Uses `runBlocking` to bridge suspend API.

- **[2026-02-16 01:06 PST]** `prism-native-widgets/src/{macosMain,linuxMain,mingwMain}/.../PrismSurface.{macos,linux,mingw}.kt` — All three desktop native implementations use `glfwContextRenderer()` from wgpu4k-toolkit. Creates standalone GLFW window with platform-appropriate backend (Metal/X11/HWND). Same deferred-init pattern. Expose `wgpuContext` via `GLFWContext`.

- **[2026-02-16 01:07 PST]** `prism-renderer/src/{jvmMain,macosArm64Main,linuxX64Main,mingwX64Main,wasmJsMain}/.../RenderSurface.*.kt` — Removed `TODO()` throws from all 5 actuals. Replaced with Kermit logging + dimension tracking, matching the existing iOS RenderSurface pattern. Each now logs "surface managed externally" since the actual GPU surface is owned by PrismSurface/PrismPanel.

### Decisions

- **[2026-02-16 01:02 PST]** **Deferred init on first valid resize()** — Surface creation happens when both width > 0 and height > 0, not during attach(). Matches PrismPanel's JVM pattern where AWT Canvas doesn't have dimensions at construction time.

- **[2026-02-16 01:02 PST]** **wgpuContext as platform-specific property only** — Not added to expect declaration since JVM and WASM actuals manage surfaces differently (PrismPanel / HTML Canvas). Only accessible from platform-specific code.

- **[2026-02-16 01:02 PST]** **iOS constructor takes MTKView? with default null** — Preserves no-arg construction for commonMain callers. iOS-specific callers pass the MTKView explicitly.

- **[2026-02-16 01:02 PST]** **Desktop native uses GLFW standalone windows** — `glfwContextRenderer()` creates its own GLFW window. Embedding into AppKit/GTK/Win32 is deferred to future work.

### Research & Discoveries

- wgpu4k-toolkit's `glfwContextRenderer()` is in `desktopNativeMain` source set — available to macOS, Linux, and MinGW targets
- wgpu4k-toolkit's `iosContextRenderer()` is in `iosMain` source set — available to iosArm64 and iosSimulatorArm64
- Both are `suspend` functions; K/N has `runBlocking` available (unlike WASM)
- `GLFWContext` and `IosContext` both implement `AutoCloseable` with proper cleanup

### Issues
None — clean implementation, all targets compile.

### Commits
- `d19821d` — feat: implement native PrismSurface with wgpu4k and fix RenderSurface TODO stubs

---

## Session 2 — Wire PrismSurface into demos, suspend factory, Android + macOS demo (2026-02-16 09:50 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/native-prism-surface-stubs`

### Intent
Refactor PrismSurface to use suspend factory functions (eliminate `runBlocking` from inside PrismSurface), wire all demo consumers through PrismSurface, add Android target support across the dependency chain, fix macOS native demo AppKit compilation, and add a macOS native demo with AppKit floating controls panel.

### What Changed

- **[2026-02-16 09:55 PST]** `prism-native-widgets/src/iosMain/.../PrismSurface.ios.kt` — Refactored: constructor now takes `IosContext?` (default null) instead of `MTKView?`. Removed `runBlocking` from `resize()`. Added top-level `suspend fun createPrismSurface(view, width, height)` factory that calls `iosContextRenderer()` in caller's coroutine context.

- **[2026-02-16 09:55 PST]** `prism-native-widgets/src/{macosMain,linuxMain,mingwMain}/.../PrismSurface.{macos,linux,mingw}.kt` — Refactored: constructor takes `GLFWContext?` (default null). Removed `runBlocking` from `resize()`. Exposed `windowHandler: CValuesRef<GLFWwindow>?` property. Added `suspend fun createPrismSurface(width, height, title)` factory.

- **[2026-02-16 09:55 PST]** `prism-native-widgets/src/jvmMain/.../PrismSurface.jvm.kt` — Replaced stub with real implementation: `GLFWContext?` constructor, exposes `wgpuContext: WGPUContext?` and `windowHandler: Long?`. Added `suspend fun createPrismSurface(width, height, title)` with `@Suppress("RedundantSuspendModifier")` for API consistency.

- **[2026-02-16 09:55 PST]** `prism-native-widgets/src/wasmJsMain/.../PrismSurface.wasmJs.kt` — Replaced stub with `CanvasContext?` wrapper. Added `suspend fun createPrismSurface(canvas, width, height)`.

- **[2026-02-16 09:55 PST]** `prism-native-widgets/build.gradle.kts` — Added `wasmJsMain` deps (wgpu4k, wgpu4k-toolkit). Added `android-library` plugin, `androidTarget()`, and `android {}` block.

- **[2026-02-16 10:10 PST]** `prism-{math,core,renderer}/build.gradle.kts` — Added `android-library` plugin, `androidTarget()`, and `android {}` block to all three modules in the dependency chain.

- **[2026-02-16 10:10 PST]** `gradle/libs.versions.toml` — Bumped AGP from 8.12.3 to 8.13.0 (required by maven-publish plugin with Android library modules).

- **[2026-02-16 10:10 PST]** `prism-core/src/androidMain/.../Platform.android.kt` — **NEW**: Android actual for `expect object Platform` with `name = "Android"` and `System.currentTimeMillis()`.

- **[2026-02-16 10:10 PST]** `prism-renderer/src/androidMain/.../RenderSurface.android.kt` — **NEW**: Android actual for `expect class RenderSurface` — dimension-only stub matching JVM pattern.

- **[2026-02-16 10:10 PST]** `prism-native-widgets/src/androidMain/.../PrismSurface.android.kt` — **NEW**: Android actual for `PrismSurface` — stub with logging, no wgpu4k integration yet (M8 scope).

- **[2026-02-16 10:15 PST]** `prism-demo/build.gradle.kts` — Added `prism-native-widgets` dependency to `commonMain.dependencies`.

- **[2026-02-16 10:15 PST]** `prism-demo/src/jvmMain/.../GlfwMain.kt` — Wired through `createPrismSurface()`: replaces direct `glfwContextRenderer()` call. Uses `surface.windowHandler!!` and `surface.wgpuContext!!`. Calls `surface.detach()` on shutdown.

- **[2026-02-16 10:15 PST]** `prism-demo/src/iosMain/.../IosDemoController.kt` — Wired through `createPrismSurface(view, w, h)`. `IosDemoHandle` now holds `PrismSurface` instead of `IosContext`. Shutdown calls `surface.detach()`.

- **[2026-02-16 10:15 PST]** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Wired through `createPrismSurface(view, w, h)`. State holds `PrismSurface?` instead of `IosContext?`. DisposableEffect calls `surface?.detach()`.

- **[2026-02-16 10:15 PST]** `prism-demo/src/wasmJsMain/.../Main.kt` — Wired through `createPrismSurface(canvas, w, h)`. Shutdown calls `surface.detach()`.

- **[2026-02-16 10:30 PST]** `prism-demo/src/macosMain/.../MacosDemoMain.kt` — Fixed AppKit compilation errors: changed property assignments to setter method calls (`setFloatingPanel`, `setEditable`, `setBordered`, `setContinuous`, `setBezelStyle`, `setDrawsBackground`, `setBecomesKeyOnlyIfNeeded`). Changed `@ObjCAction` params from `Any?` to `NSSlider`/`NSButton` (ObjC types required).

### Decisions

- **[2026-02-16 09:55 PST]** **Suspend factory pattern** — `createPrismSurface()` is a top-level suspend function on each platform. The caller handles the coroutine context (e.g., `runBlocking` in `main()`, `LaunchedEffect` in Compose). No `runBlocking` inside PrismSurface itself.

- **[2026-02-16 10:10 PST]** **Add Android to full dependency chain** — Adding `androidTarget()` to prism-native-widgets required also adding it to prism-math, prism-core, and prism-renderer (transitive `api()` dependencies). Android actuals created for `Platform` (prism-core) and `RenderSurface` (prism-renderer).

- **[2026-02-16 10:10 PST]** **AGP 8.13.0 bump** — Required by `com.vanniktech.maven.publish` plugin when the Android library plugin is applied. Brings deprecation warnings about `com.android.library` → `com.android.kotlin.multiplatform.library` migration for AGP 9.0, but functional for now.

- **[2026-02-16 10:30 PST]** **macOS demo in macosMain, not macosArm64Main** — KMP's default hierarchy template makes `macosMain` a parent of `macosArm64Main`, so code in `macosMain` is compiled for `macosArm64`. Putting the demo there avoids duplication if macosX64 is added later.

- **[2026-02-16 10:10 PST]** **JVM PrismSurface suppress RedundantSuspendModifier** — `glfwContextRenderer()` is declared `suspend` on JVM but doesn't actually suspend. Detekt flags this. Suppressed with comment explaining API consistency with native targets.

### Research & Discoveries

- wgpu4k publishes Android artifacts (`wgpu4k-android`, `wgpu4k-toolkit-android`) so the full chain resolves
- K/N ObjC interop: boolean properties like `isFloatingPanel`, `isEditable` are often not settable as Kotlin properties — need to use setter methods like `setFloatingPanel(true)`, `setEditable(false)`
- K/N `@ObjCAction` methods require ObjC object types as parameters (e.g., `NSSlider`), not `Any?`
- `NSPanel(contentRect:styleMask:backing:defer:)` constructor works in K/N with named params matching the ObjC selector

### Issues

- **macosMain vs macosArm64Main duplicate** — Initially created MacosDemoMain.kt in both `macosArm64Main` and `macosMain`, causing duplicate `main()`. Resolved by deleting the `macosArm64Main` copy since `macosMain` is inherited.

### Lessons Learned

- K/N ObjC boolean property assignment (`obj.isFoo = true`) often fails — always try setter methods first (`obj.setFoo(true)`)
- `maven-publish` plugin has strict AGP version requirements when Android library is detected — check compatibility before adding `android-library` plugin to KMP modules
- When adding a new KMP target to a module, check all transitive `api()` dependencies — they all need the same target or the build fails at resolution time

---

## Session 3 — Execute suspend factory plan + verify build (2026-02-16 02:10 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/native-prism-surface-stubs`

### Intent
Execute the approved implementation plan from Session 2: refactor all PrismSurface actuals to suspend factory pattern, wire demo consumers through `createPrismSurface()`, and add macOS native demo.

### What Changed
All changes described in Session 2 were implemented and verified. Key adjustments from the plan:

- **[2026-02-16 01:55 PST]** All 6 PrismSurface actuals (iOS, macOS, Linux, MinGW, JVM, WASM) refactored to suspend factory pattern — constructors accept platform context with default null, `resize()` only updates dimensions, `detach()` closes context.
- **[2026-02-16 02:00 PST]** All 4 demo entry points wired through `createPrismSurface()` — GlfwMain.kt, IosDemoController.kt, ComposeIosEntry.kt, WASM Main.kt.
- **[2026-02-16 02:05 PST]** `MacosDemoMain.kt` — Created macOS native demo with AppKit floating NSPanel controls.
- **[2026-02-16 02:07 PST]** Fixed K/N ObjCAction parameters: `Any?` → `NSSlider`/`NSButton` (ObjC types required). Fixed ObjC property setters: used explicit `setEditable(false)` form instead of property assignment.
- **[2026-02-16 02:08 PST]** Added `@Suppress("RedundantSuspendModifier")` to JVM `createPrismSurface` — detekt flags it because LWJGL's `glfwContextRenderer` doesn't truly suspend on JVM.

### Verification
- `./gradlew ktfmtFormat` — PASS
- `./gradlew ktfmtCheck detektJvmMain` — PASS
- `./gradlew :prism-renderer:jvmTest :prism-demo:jvmTest` — PASS
- `./gradlew :prism-demo:compileKotlinMacosArm64 :prism-demo:compileKotlinIosArm64 :prism-demo:compileKotlinWasmJs` — PASS
- `./gradlew :prism-demo:linkDebugExecutableMacosArm64` — PASS (native executable links)

### Commits
- `174d093` — feat: wire PrismSurface into demos with suspend factory, add Android + macOS native

---

## Session 4 — Fix double-close vulnerability in PrismSurface (2026-02-16 17:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/native-prism-surface-stubs`

### Intent
Fix a double-close vulnerability found during critical review: `detach()` could be called multiple times, each calling `close()` on the underlying wgpu context (GLFW/iOS/Canvas). Double-closing GLFW windows is undefined behavior.

### What Changed

- **[2026-02-16 17:00 PST]** `PrismSurface.{jvm,ios,macos,linux,mingw,wasmJs}.kt` — All 6 platform implementations converted from `private val` constructor properties to mutable backing fields (`private var _glfwContext`, `_iosContext`, `_canvasContext`). `detach()` now nulls the backing field after `close()`, preventing double-close. Properties (`wgpuContext`, `windowHandler`) delegate to the backing field, so they return `null` after detach.

### Issues
- **JVM was in broken state** — Previous edit changed constructor from `private val glfwContext` to plain `glfwContext` param and added `_glfwContext` backing field, but `detach()` still referenced `glfwContext` (now a non-property constructor param). This was a compile error. Fixed by updating `detach()` to use `_glfwContext`.

### Verification
- `./gradlew ktfmtFormat` — PASS
- `./gradlew ktfmtCheck detektJvmMain` — PASS
- `./gradlew :prism-renderer:jvmTest :prism-demo:jvmTest` — PASS
- `./gradlew :prism-demo:compileKotlinMacosArm64 :prism-demo:compileKotlinIosArm64 :prism-demo:compileKotlinWasmJs` — PASS

### Commits
- `2c30579` — fix: prevent double-close in MinGW and WASM PrismSurface detach()

---

## Next Steps
- Commit and PR review (#24)
- Test macOS native demo: `./gradlew :prism-demo:runDebugExecutableMacosArm64`
- Test JVM GLFW demo: `./gradlew :prism-demo:run`
- Android: wire wgpu4k rendering when PanamaPort is integrated (M8)
- Future: `com.android.kotlin.multiplatform.library` migration when AGP 9.0 lands
