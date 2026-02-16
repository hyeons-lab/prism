# 000009-feat-native-prism-surface-stubs

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/native-prism-surface-stubs`

## Intent

Complete all native PrismSurface implementations with real wgpu surfaces, refactor to suspend factory pattern, wire all demo consumers through PrismSurface, add Android targets, create macOS native demo with AppKit controls, and clean up dead code.

## What Changed

- **2026-02-16T01:05-08:00** `prism-native-widgets/build.gradle.kts` — Added `nativeMain.dependencies` (wgpu4k, wgpu4k-toolkit, coroutines), `wasmJsMain` deps, `android-library` plugin, `androidTarget()`, and `android {}` block.
- **2026-02-16T01:06-08:00** `prism-native-widgets/src/iosMain/.../PrismSurface.ios.kt` — Full implementation: `IosContext?` constructor (default null), `suspend fun createPrismSurface(view, width, height)` factory calling `iosContextRenderer()`, exposes `wgpuContext`. `detach()` nulls backing field to prevent double-close.
- **2026-02-16T01:06-08:00** `prism-native-widgets/src/{macosMain,linuxMain,mingwMain}/.../PrismSurface.{macos,linux,mingw}.kt` — All three desktop native implementations: `GLFWContext?` constructor, `suspend fun createPrismSurface(width, height, title)` factory via `glfwContextRenderer()`, exposes `wgpuContext` and `windowHandler`. Null-on-close pattern in `detach()`.
- **2026-02-16T01:07-08:00** `prism-native-widgets/src/jvmMain/.../PrismSurface.jvm.kt` — Real implementation replacing stub: `GLFWContext?` constructor, exposes `wgpuContext: WGPUContext?` and `windowHandler: Long?`. `suspend fun createPrismSurface(width, height, title)` with KDoc explaining suspend is for API consistency.
- **2026-02-16T01:07-08:00** `prism-native-widgets/src/wasmJsMain/.../PrismSurface.wasmJs.kt` — `CanvasContext?` wrapper with `suspend fun createPrismSurface(canvas, width, height)` factory via `canvasContextRenderer()`.
- **2026-02-16T01:07-08:00** `prism-native-widgets/src/androidMain/.../PrismSurface.android.kt` — **NEW**: Android actual — stub with logging, no wgpu4k integration yet (M8 scope).
- **2026-02-16T10:10-08:00** `prism-{math,core,renderer}/build.gradle.kts` — Added `android-library` plugin, `androidTarget()`, and `android {}` block to all three modules in the dependency chain.
- **2026-02-16T10:10-08:00** `gradle/libs.versions.toml` — Bumped AGP from 8.12.3 to 8.13.0 (required by maven-publish plugin with Android library modules).
- **2026-02-16T10:10-08:00** `prism-core/src/androidMain/.../Platform.android.kt` — **NEW**: Android actual for `expect object Platform`.
- **2026-02-16T10:15-08:00** `prism-demo/build.gradle.kts` — Added `prism-native-widgets` dependency to `commonMain.dependencies`. Added `macosArm64` executable binary with entry point.
- **2026-02-16T10:15-08:00** `prism-demo/src/jvmMain/.../GlfwMain.kt` — Wired through `createPrismSurface()`. Uses `checkNotNull()` on `wgpuContext` and `windowHandler`. Calls `surface.detach()` on shutdown.
- **2026-02-16T10:15-08:00** `prism-demo/src/iosMain/.../IosDemoController.kt` — Wired through `createPrismSurface(view, w, h)`. `IosDemoHandle` holds `PrismSurface` instead of `IosContext`. Uses `checkNotNull()`.
- **2026-02-16T10:15-08:00** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Wired through `createPrismSurface(view, w, h)`. State holds `PrismSurface?` instead of `IosContext?`. Uses `checkNotNull()`.
- **2026-02-16T10:15-08:00** `prism-demo/src/wasmJsMain/.../Main.kt` — Wired through `createPrismSurface(canvas, w, h)`. Uses `checkNotNull()`. Calls `surface.detach()` on shutdown/error.
- **2026-02-16T10:30-08:00** `prism-demo/src/macosMain/.../MacosDemoMain.kt` — **NEW**: macOS native demo with GLFW/Metal window + AppKit floating `NSPanel` controls (speed slider, pause button). Uses `createPrismSurface()`, `checkNotNull()`, `@Volatile` shared state, and `scene.tickWithAngle()`.
- **2026-02-16T10:30-08:00** `prism-demo/src/iosMain/.../IosConstants.kt` — `tickDemoFrame()` now delegates rotation + ECS update to `scene.tickWithAngle()` instead of manually manipulating components. `deltaTime` zeroed when paused for consistency with macOS demo. Removed unused imports.
- **2026-02-16T11:00-08:00** `prism-demo/src/commonMain/.../DemoScene.kt` — Added `tickWithAngle(deltaTime, elapsed, frameCount, angle)` for variable-speed/pause scenarios. `tick()` delegates to it.
- **2026-02-16T11:00-08:00** `prism-renderer/src/{commonMain,jvmMain,iosMain,macosArm64Main,linuxX64Main,mingwX64Main,wasmJsMain,androidMain}/.../RenderSurface.*.kt` — **DELETED**: All 8 files (expect + 7 actuals). Dead code superseded by PrismSurface — all actuals were logging-only dimension stubs.
- **2026-02-16T11:00-08:00** `AGENTS.md`, `BUILD_STATUS.md`, `PLAN.md`, `README.md` — Updated documentation for PrismSurface, Android targets, macOS native demo, and RenderSurface removal.

## Decisions

- **2026-02-16T01:02-08:00** **wgpuContext as platform-specific property only** — Not added to expect declaration since JVM and WASM actuals manage surfaces differently (PrismPanel / HTML Canvas). Only accessible from platform-specific code.
- **2026-02-16T01:02-08:00** **Desktop native uses GLFW standalone windows** — `glfwContextRenderer()` creates its own GLFW window. Embedding into AppKit/GTK/Win32 is deferred to future work.
- **2026-02-16T09:55-08:00** **Suspend factory pattern** — `createPrismSurface()` is a top-level suspend function on each platform. The caller handles the coroutine context. No `runBlocking` inside PrismSurface itself.
- **2026-02-16T10:10-08:00** **Add Android to full dependency chain** — Adding `androidTarget()` to prism-native-widgets required also adding it to prism-math, prism-core, and prism-renderer (transitive `api()` dependencies).
- **2026-02-16T10:30-08:00** **macOS demo in macosMain, not macosArm64Main** — KMP's default hierarchy template makes `macosMain` a parent of `macosArm64Main`. Avoids duplication if macosX64 is added later.
- **2026-02-16T11:00-08:00** **tickWithAngle() for variable-speed scenarios** — `DemoScene.tick()` assumes constant speed (angle = elapsed * speed). For pause/resume and variable speed, `tickWithAngle()` takes an explicit angle so callers manage accumulation.
- **2026-02-16T11:00-08:00** **Delete RenderSurface** — All 8 actuals were logging-only stubs since PrismSurface owns the GPU surface. Not released yet, so safe to remove entirely.
- **2026-02-16T11:00-08:00** **checkNotNull() over !!** — Factory functions guarantee non-null wgpuContext/windowHandler, but the type system doesn't express this. `checkNotNull()` with descriptive messages is preferred over `!!` for debuggability.
- **2026-02-16T11:00-08:00** **@Volatile on shared state** — MacosDemoMain's `rotationSpeedDegrees` and `isPaused` are accessed from the GLFW main thread only, but `@Volatile` added defensively for future refactoring safety.

## Research & Discoveries

- wgpu4k-toolkit's `glfwContextRenderer()` is in `desktopNativeMain` source set — available to macOS, Linux, and MinGW targets
- wgpu4k-toolkit's `iosContextRenderer()` is in `iosMain` source set — available to iosArm64 and iosSimulatorArm64
- K/N ObjC interop: boolean properties like `isFloatingPanel`, `isEditable` are often not settable as Kotlin properties — need to use setter methods like `setFloatingPanel(true)`
- K/N `@ObjCAction` methods require ObjC object types as parameters (e.g., `NSSlider`), not `Any?`
- `maven-publish` plugin has strict AGP version requirements when Android library is detected

## Issues

- **macosMain vs macosArm64Main duplicate** — Initially created MacosDemoMain.kt in both source sets, causing duplicate `main()`. Fixed by keeping only `macosMain`.
- **JVM double-close** — Previous edit changed constructor from `private val glfwContext` to plain param and added `_glfwContext` backing field, but `detach()` still referenced the constructor param. Fixed by updating `detach()` to use the backing field.

## Lessons Learned

- K/N ObjC boolean property assignment (`obj.isFoo = true`) often fails — always try setter methods first (`obj.setFoo(true)`)
- When adding a new KMP target to a module, check all transitive `api()` dependencies — they all need the same target
- `DemoScene.tick()` with constant `rotationSpeed` doesn't support mid-run speed changes — need incremental angle accumulation for variable-speed scenarios

## Commits

- `097cfff` — feat: implement native PrismSurface with wgpu4k and fix RenderSurface TODO stubs
- `8c76fcf` — feat: wire PrismSurface into demos with suspend factory, add Android + macOS native
- `4c9f674` — fix: prevent double-close in MinGW and WASM PrismSurface detach()
- `f4eb5be` — docs: update all documentation for PrismSurface, Android targets, and macOS native demo
- `ed89a37` — refactor: extract tickWithAngle, replace !! with checkNotNull, delete RenderSurface

## Next Steps

- PR review (#24)
- Test macOS native demo: `./gradlew :prism-demo:runDebugExecutableMacosArm64`
- Test JVM GLFW demo: `./gradlew :prism-demo:jvmRun`
- Android: wire wgpu4k rendering when PanamaPort is integrated (M8)
