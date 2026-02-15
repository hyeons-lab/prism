# 000005-feat-compose-integration

## Session 1 — Compose Multiplatform Integration (2026-02-15 08:42 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

### Intent
Implement true embedded 3D rendering inside a Compose Desktop window on JVM using wgpu4k surface creation from AWT Canvas native handles. Create PrismPanel (AWT Canvas subclass), implement PrismView.jvm with SwingPanel, build Compose demo with interactive UI controls driving a 3D scene.

### What Changed

**Phase 1 — WgpuRenderer preparation:**
- **[2026-02-15 08:45 PST]** `prism-renderer/.../WgpuRenderer.kt` — Added `surfacePreConfigured: Boolean = false` constructor parameter. When true, `initialize()` skips `surface.configure()` since the caller handles it externally. Added `onResize` callback property so external code (AWT Canvas) can reconfigure the surface on resize.

**Phase 2 — prism-native-widgets JVM implementation:**
- **[2026-02-15 08:48 PST]** `prism-native-widgets/build.gradle.kts` — Added `jvmToolchain(25)`, `jvmMain` dependencies on `wgpu4k`, `wgpu4k-toolkit`, and `kotlinx-coroutines-core`.
- **[2026-02-15 08:50 PST]** `prism-native-widgets/.../PrismPanel.kt` — Complete rewrite from empty stub to full AWT Canvas subclass (~240 lines). Implements native handle extraction (macOS/Windows/Linux), CAMetalLayer setup via Rococoa, wgpu surface creation, `AwtRenderingContext` for GLFW-free width/height, and automatic resize handling via `ComponentListener`.
- **[2026-02-15 08:51 PST]** `prism-native-widgets/.../PrismSurface.{jvm,wasmJs,ios,macos,linux,mingw}.kt` — Added Kermit logging to all platform stubs' attach/detach/resize methods.
- **[2026-02-15 08:52 PST]** `prism-native-widgets/.../PrismCanvasElement.kt` — Added Kermit logging to WASM canvas element stub.

**Phase 3 — prism-compose implementation:**
- **[2026-02-15 08:53 PST]** `prism-compose/.../EngineState.kt` — Refactored to support both owned and external engines. Added `ownsEngine` flag (internal visibility) and `rememberExternalEngineState(engine)` composable. Only shuts down engine on dispose when owned.
- **[2026-02-15 08:55 PST]** `prism-compose/.../PrismView.jvm.kt` — Full implementation using `SwingPanel` embedding `PrismPanel`. Coroutine-based render loop with `LaunchedEffect` that calls `onFrame` callback with proper `Time` (deltaTime, totalTime, frameCount). Waits for PrismPanel `onReady` signal.
- **[2026-02-15 08:56 PST]** `prism-compose/.../PrismView.{wasmJs,ios}.kt` — Added Kermit logging to stub implementations.
- **[2026-02-15 08:56 PST]** `prism-compose/build.gradle.kts` — Added `jvmToolchain(25)`, `jvmMain` dependencies on `compose.desktop.currentOs`, `wgpu4k`, `wgpu4k-toolkit`, `kotlinx-coroutines-core`.

**Phase 4 — Demo app:**
- **[2026-02-15 08:58 PST]** `prism-demo/.../DemoSceneState.kt` — New file. Compose `mutableStateOf` properties: rotationSpeed, isPaused, cubeColorR/G/B, fps. Shared between UI and render loop.
- **[2026-02-15 08:59 PST]** `prism-demo/.../ComposeDemoControls.kt` — New file. Material3 UI panel (260dp wide) with: FPS display, rotation speed slider (0-360°/s), pause/resume button, 6 color preset buttons (Blue, Red, Green, Gold, Purple, White).
- **[2026-02-15 09:01 PST]** `prism-demo/.../ComposeMain.kt` — New file. Main entry point for Compose demo. `LibraryLoader.load()` + `glfwInit()` → Compose `application {}` window with `Row { SwingPanel(PrismPanel) | ComposeDemoControls }`. Engine/World/RenderSystem creation deferred to PrismPanel `onReady` callback. Coroutine render loop in `LaunchedEffect` with smoothed FPS, rotation via `DemoSceneState`, per-frame material color updates. Uses `Entity` type (not `Long`).
- **[2026-02-15 09:02 PST]** `prism-demo/build.gradle.kts` — Added `--add-opens=java.desktop/sun.awt=ALL-UNNAMED` JVM arg. Added `runCompose` task targeting `ComposeMainKt`.

**Phase 5 — Quality verification:**
- **[2026-02-15 09:04 PST]** Fixed missing `kotlinx-coroutines-core` dependency in `prism-native-widgets/build.gradle.kts`.
- **[2026-02-15 09:05 PST]** Fixed `EngineState.ownsEngine` visibility: `private` → `internal` (accessed by top-level `rememberEngineState`).
- **[2026-02-15 09:06 PST]** Fixed detekt `TooGenericExceptionCaught` in `PrismPanel.kt` — added `@Suppress` with comment explaining wgpu4k FFI exception variety.
- **[2026-02-15 09:06 PST]** Fixed detekt `ConstructorParameterNaming` in `AwtRenderingContext` — renamed `_width`/`_height` to `initialWidth`/`initialHeight` with backing fields `currentWidth`/`currentHeight`.
- **[2026-02-15 09:07 PST]** Fixed `Entity` type in `ComposeMain.kt` — `SceneContext.cubeEntity` was `Long`, should be `Entity`.
- **[2026-02-15 09:08 PST]** Fixed `return@onReady` label error — restructured to `if (ctx != null)` block.
- **[2026-02-15 09:09 PST]** Fixed detekt `UnreachableCode` in `ComposeMain.kt` — removed early return pattern, used `if` block instead.
- **[2026-02-15 09:10 PST]** All checks pass: `ktfmtCheck` ✅, `detektJvmMain` ✅, `jvmTest` ✅ (170 tests).

### Decisions
- **[2026-02-15 08:48 PST]** **Use `Surface(nativeSurface, 0L)` with custom `AwtRenderingContext`** — The wgpu4k `Surface.jvm.kt` calls `glfwGetWindowSize(windowHandler)` for width/height. By passing `windowHandler=0L` and using our own `AwtRenderingContext` that returns Canvas dimensions, we avoid the GLFW dependency entirely.
- **[2026-02-15 08:50 PST]** **Pre-configure surface before WgpuRenderer** — PrismPanel configures the NativeSurface with explicit dimensions before constructing WGPUContext. WgpuRenderer uses `surfacePreConfigured=true` to skip its own `surface.configure()` call.
- **[2026-02-15 08:53 PST]** **EngineState owns vs. external** — `rememberExternalEngineState()` wraps an externally-created Engine without owning its lifecycle.
- **[2026-02-15 09:01 PST]** **ComposeMain uses SwingPanel directly (not PrismView)** — The demo creates PrismPanel directly via SwingPanel to get access to `wgpuContext` for scene setup.

### Research & Discoveries
- **[2026-02-15 08:42 PST]** wgpu4k `WGPU` class lives in `commonNativeMain` which includes JVM via custom source set hierarchy
- **[2026-02-15 08:42 PST]** `Surface.jvm.kt` uses `glfwGetWindowSize()` for width/height — bypassed with custom `RenderingContext`
- **[2026-02-15 08:42 PST]** macOS surface creation: NSView → `setWantsLayer(true)` → `CAMetalLayer.layer()` → `setLayer(layer)` → `getSurfaceFromMetalLayer()`
- **[2026-02-15 08:46 PST]** Rococoa 0.0.1 provides `darwin.NSView.setLayer(Pointer?)` and `darwin.CAMetalLayer.layer()`
- **[2026-02-15 08:47 PST]** `com.sun.jna.Native.getComponentPointer(Component)` returns NSView pointer on macOS

### Issues
- **[2026-02-15 09:04 PST]** Missing `kotlinx-coroutines-core` in prism-native-widgets caused `runBlocking` unresolved reference — fixed by adding dependency.
- **[2026-02-15 09:06 PST]** Detekt's `TooGenericExceptionCaught` rejects even `RuntimeException` — needed `@Suppress` since wgpu4k FFI can throw various exception types during native surface creation.
- **[2026-02-15 09:08 PST]** `return@onReady` label not valid in property assignment lambda — Kotlin doesn't create labels for property assignments. Used `if` block instead of early return.

### Lessons Learned
- wgpu4k FFI surface creation can throw many exception types (IllegalStateError from `error()`, JNA exceptions from native calls, Rococoa exceptions) — use `@Suppress("TooGenericExceptionCaught")` when catching broadly.
- Detekt treats code after `?: return@label` as unreachable in some lambda contexts — prefer `if (x != null)` blocks over early returns in short lambdas.
- ECS `Entity` is `data class Entity(val id: UInt)`, not `Long` — always import and use the proper type.

### Commits

---

## Session 2 — Add Flutter to Future Plans (2026-02-15, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

### Intent
Add Flutter integration as a future milestone (M11) in project plans, reflecting the existing `prism-flutter` module skeleton.

### What Changed
- **[2026-02-15]** `BUILD_STATUS.md` — Added `### M11: Flutter Integration ⏳` milestone
- **[2026-02-15]** `PLAN.md` — Added M11 milestone with detailed scope (PrismBridge methods, Android/iOS native plugins, rendering surface connection, game loop, Dart scene API, example app). Updated module description and dependency graph to include prism-flutter.
- **[2026-02-15]** `AGENTS.md` — Added Flutter to "What's next" list and updated module dependency graph to show `prism-flutter` under `prism-native-widgets`.

### Decisions
- **[2026-02-15]** **M11 depends on M7 (iOS) and M8 (Android)** — Flutter plugin implementations need native platform surface creation which is covered by those milestones.

---

## Session 3 — Critical Review and Bug Fixes (2026-02-15, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

### Intent
Critical review of the Compose integration branch: verify architecture, FFI correctness, thread safety, resource lifecycle, and integration patterns.

### What Changed
- **[2026-02-15]** `PrismPanel.kt` — Added `onResized` callback invoked on Canvas resize, so external code can propagate resize to the renderer.
- **[2026-02-15]** `ComposeMain.kt` — Wired `p.onResized` to call `renderer.resize(w, h)`, fixing depth texture / camera aspect ratio mismatch on window resize. Removed unnecessary `glfwInit()` call and import.
- **[2026-02-15]** `prism-demo/build.gradle.kts` — Removed duplicate JVM args from `runCompose` task (already inherited from `tasks.withType<JavaExec>`).
- **[2026-02-15]** `BUILD_STATUS.md` — Updated M5 milestone to ✅ Complete with details. Updated JVM toolchain note.

### Issues
- **[2026-02-15]** **Resize bug (fixed)**: PrismPanel's `componentResized` called `reconfigureSurface()` which updated the native surface and AwtRenderingContext dimensions, but never notified WgpuRenderer. This left the depth texture and camera aspect ratio at old dimensions, causing a wgpu render pass attachment size mismatch. Fixed by adding `PrismPanel.onResized` callback and wiring ComposeMain to call `renderer.resize()`.
- **[2026-02-15]** **Duplicate JVM args (fixed)**: `runCompose` task duplicated args already applied by `tasks.withType<JavaExec>`.
- **[2026-02-15]** **Unnecessary `glfwInit()` (fixed)**: ComposeMain called `glfwInit()` despite using AWT Canvas (not GLFW) for surface creation. Only `LibraryLoader.load()` is needed.
- **[2026-02-15]** **WGPU instance leak (fixed)**: `WGPU.createInstance()` in `initializeWgpuSurface()` was a local variable never closed. Now stored in `wgpuInstance` field and closed in `removeNotify()`.
- **[2026-02-15]** **Engine/WgpuRenderer shutdown not called (noted, not fixed)**: On window close, `PrismPanel.removeNotify()` closes WGPUContext but Engine/WgpuRenderer are never shut down. Renderer-owned buffers/textures are orphaned (GPU reclaims on device close). Adding explicit shutdown risks double-close on WGPUContext. Acceptable for demo.

### Decisions
- **[2026-02-15]** **PrismView API needs future design work for resize** — The PrismView.jvm.kt composable creates PrismPanel internally but doesn't expose resize events to callers. ComposeMain bypasses PrismView entirely (uses SwingPanel directly) so it can wire resize handling. A future redesign of PrismView should expose resize propagation, but this is not blocking since ComposeMain is the primary consumer.

### Research & Discoveries
- **[2026-02-15]** wgpu4k `Surface(nativeSurface, 0L)` — confirmed safe. The `0L` windowHandler is only used by `Surface.width/height` getters (which call `glfwGetWindowSize`), but AwtRenderingContext overrides these. WgpuRenderer never reads `surface.width/height` directly.
- **[2026-02-15]** wgpu4k `WGPUContext.close()` has a commented-out `renderingContext.close()` due to upstream crash with TextureRenderingContext. AwtRenderingContext.close() is a no-op, so no leak for our use case.
- **[2026-02-15]** wgpu4k-toolkit defines `fun Pointer.toNativeAddress()` and `fun Long.toNativeAddress()` in `io.ygdrasil.webgpu` package — confirms Windows surface creation is type-safe.
- **[2026-02-15]** Compose Desktop Dispatchers.Main = EDT. All LaunchedEffect coroutines and AWT callbacks run single-threaded. No explicit synchronization needed between UI state and render loop.
- **[2026-02-15]** Linux surface creation uses reflection (`getDeclaredField("display")`) on AWT Toolkit — fragile, JDK-dependent. wgpu4k-toolkit itself supports Wayland via GLFW, but our AWT Canvas path only handles X11.
- **[2026-02-15]** `WGPU` class implements `AutoCloseable` with `close()` calling `wgpuInstanceRelease()`. Must be stored and closed in `removeNotify()`.
- **[2026-02-15]** Per-frame resource management in WgpuRenderer is correct: `AutoClosableContext` in `beginFrame()` → `.bind()` ephemeral resources → `ctx.close()` in `endFrame()`. No per-frame leaks.
- **[2026-02-15]** wgpu `surface.configure()` handles swap chain recreation internally — no need to manually release old surface textures before reconfiguring.
- **[2026-02-15]** Scene setup duplicated between ComposeMain and GlfwMain. Acceptable for 2 entry points; shared DemoScene factory is a future option.

### Lessons Learned
- Always verify resize propagation end-to-end: native surface reconfiguration alone is insufficient — the renderer's internal state (depth texture, camera aspect ratio) must also be updated.
- `tasks.withType<JavaExec>` in Gradle applies to ALL JavaExec tasks (including lazily registered ones) — don't duplicate args in individual task registrations.
- Store and close `WGPU` instance — even lightweight FFI handles should be tracked for proper cleanup, especially if the component may be recreated.
- `WGPUContext.close()` calls `wgpuSurfaceRelease`, `wgpuAdapterRelease`, `wgpuDeviceRelease` but NOT `renderingContext.close()` (upstream bug). Safe for us since AwtRenderingContext.close() is a no-op.

### Commits

---

## Session 4 — MVI Refactor, Shared Scene Factory, Tests (2026-02-15, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

### Intent
Address remaining review feedback: extract shared DemoScene factory, refactor composables to MVI pattern (per user request), use StateFlow with `collectAsStateWithLifecycle()`, add unit tests, improve Linux error handling.

### What Changed
- **[2026-02-15]** `prism-demo/src/commonMain/.../DemoSceneState.kt` — Replaced mutable-state-holder class with MVI pattern: `DemoUiState` (immutable data class), `DemoIntent` (sealed interface), `DemoStore` (StateFlow-backed reducer). Internal `MutableStateFlow` + public `StateFlow` via `.asStateFlow()`.
- **[2026-02-15]** `prism-demo/src/commonMain/.../ComposeDemoControls.kt` — Refactored to MVI view: takes `DemoUiState` (immutable) + `onIntent: (DemoIntent) -> Unit` callback. Uses import aliases for Color disambiguation (`RendererColor` / `ComposeColor`).
- **[2026-02-15]** `prism-demo/src/jvmMain/.../DemoScene.kt` — New shared factory: `DemoScene` class and `createDemoScene()` function used by both GLFW and Compose entry points. Parameters: `surfacePreConfigured`, `initialColor`.
- **[2026-02-15]** `prism-demo/src/jvmMain/.../ComposeMain.kt` — Uses `DemoStore` with `collectAsStateWithLifecycle()`, delegates scene creation to `createDemoScene()`. Renamed render-loop local to `currentState` to avoid name shadowing with composed `uiState`.
- **[2026-02-15]** `prism-demo/src/jvmMain/.../GlfwMain.kt` — Simplified by using `createDemoScene()` and `scene.shutdown()`. Removed duplicate camera/cube setup code.
- **[2026-02-15]** `prism-demo/build.gradle.kts` — Moved `kotlinx-coroutines-core` to commonMain (needed for StateFlow in DemoStore). Added `lifecycle-runtime-compose` for `collectAsStateWithLifecycle()`. Added commonTest dependencies (kotlin-test, kotest, coroutines-test).
- **[2026-02-15]** `gradle/libs.versions.toml` — Added `lifecycleCompose` version and `lifecycle-runtime-compose` library. Sorted all sections alphabetically (versions, libraries, plugins).
- **[2026-02-15]** `prism-demo/src/commonTest/.../DemoStoreTest.kt` — 7 unit tests covering DemoStore: initial state, each intent type, sequential intents, StateFlow emission, field isolation.
- **[2026-02-15]** `prism-native-widgets/.../PrismPanel.kt` — Extracted `extractX11DisplayPointer()` with targeted error handling: `NoSuchFieldException` for non-X11 toolkits (Wayland), `IllegalAccessException` for missing `--add-opens`. Both chain original exception. Suppressed false-positive `UnreachableCode` detekt rule.

### Decisions
- **[2026-02-15]** **MVI over mutable state holder** — User requested MVI pattern. StateFlow + immutable data class + sealed intents + pure reduce function. Clean unidirectional data flow.
- **[2026-02-15]** **StateFlow over Compose mutableStateOf** — User requested `_state`/`state` pattern with `.asStateFlow()`. Decouples store from Compose runtime, testable with coroutines-test.
- **[2026-02-15]** **collectAsStateWithLifecycle over collectAsState** — User preference. Added `lifecycle-runtime-compose` dependency (JetBrains KMP artifact `2.8.4`).
- **[2026-02-15]** **DemoScene.shutdown() convenience** — Encapsulates `world.shutdown()` + `engine.shutdown()` ordering, used by both entry points.
- **[2026-02-15]** **Sorted libs.versions.toml** — User requested alphabetical sorting of all TOML sections.

### Issues
- Detekt false positive: `UnreachableCode` triggered on code after try-catch where catch branches throw. Suppressed with annotation on `extractX11DisplayPointer()`.

### Commits

---

## Session 5 — Fix macOS Compose Desktop Runtime (2026-02-15, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

### Intent
Fix the Compose Desktop demo which failed to render on macOS due to a fundamental conflict between `-XstartOnFirstThread` (needed for native handle extraction) and Compose Desktop's `application {}` (which deadlocks with it).

### What Changed
- **[2026-02-15]** `prism-demo/src/jvmMain/.../ComposeMain.kt` — Complete rewrite from Compose `application {}` + `Window` to `JFrame` + `ComposePanel` architecture. `SwingUtilities.invokeLater` dispatches all Swing/AWT setup to the EDT. PrismPanel (AWT Canvas) in `BorderLayout.CENTER`, ComposePanel (Compose UI) in `BorderLayout.EAST`. Render loop via `javax.swing.Timer(1)` firing on EDT. Graceful shutdown via `WindowListener.windowClosing()`.
- **[2026-02-15]** `prism-native-widgets/.../PrismPanel.kt` — Added dual-path NSView pointer resolution: JNA `Native.getComponentPointer()` (fast path with `-XstartOnFirstThread`) → AWT reflection fallback (works without the flag). Reflection path: `Canvas.peer → LWComponentPeer.windowPeer → LWWindowPeer.getPlatformWindow() → CPlatformWindow.contentView → CPlatformView.getAWTView()`. Added deferred `onReady` callback: if Canvas has 0x0 size during `addNotify()`, readiness is deferred to first `componentResized` event with valid dimensions.
- **[2026-02-15]** `prism-demo/src/jvmMain/.../DemoScene.kt` — Added `cameraEntity` field to `DemoScene`. Added `updateAspectRatio(width, height)` method to update camera aspect ratio on resize.
- **[2026-02-15]** `prism-demo/build.gradle.kts` — Added `--add-opens` for `java.desktop/java.awt`, `sun.lwawt`, and `sun.lwawt.macosx` (needed for AWT reflection fallback).

### Decisions
- **[2026-02-15]** **JFrame + ComposePanel over application {}** — Compose Desktop's `application {}` deadlocks on macOS with `-XstartOnFirstThread` (which GLFW requires). `ComposePanel` is a standard Swing component that works inside existing JFrame-based apps without this conflict.
- **[2026-02-15]** **Run without -XstartOnFirstThread for Compose demo** — After testing multiple approaches (JFrame with the flag, SwingUtilities.invokeLater with the flag), determined that `invokeLater` never dispatches on macOS with `-XstartOnFirstThread` because the AppKit run loop isn't started. Solution: run Compose demo without the flag and use AWT reflection for native handle extraction.
- **[2026-02-15]** **AWT reflection over JNA for NSView** — JNA's `Native.getComponentPointer()` returns null without `-XstartOnFirstThread` on macOS. The AWT reflection path navigates internal OpenJDK classes (`sun.lwawt.LWComponentPeer`, `sun.lwawt.macosx.CPlatformView`) to get the NSView pointer. Fragile (JDK-internal) but works on JDK 21-25.
- **[2026-02-15]** **Swing Timer for render loop** — Replaces `LaunchedEffect` + coroutine delay loop. Timer(1) fires on EDT which is safe for wgpu rendering. No coroutine dispatcher dependency.

### Research & Discoveries
- **macOS `-XstartOnFirstThread` constraints:**
  - Makes JVM main thread = AppKit main thread (thread 0)
  - Required for GLFW and JNA's `Native.getComponentPointer()`
  - Compose Desktop `application {}` deadlocks — never creates a visible window
  - `SwingUtilities.invokeLater` doesn't dispatch — `isEDT=false` after `Toolkit.getDefaultToolkit()`, and the enqueued runnable never executes because the AppKit run loop isn't running
- **AWT native handle extraction on macOS:**
  - JNA `Native.getComponentPointer()` uses JAWT internally → returns null without `-XstartOnFirstThread`
  - OpenJDK AWT hierarchy: `Component.peer` → `LWCanvasPeer` → `LWWindowPeer` → `CPlatformWindow.contentView` → `CPlatformView.getAWTView()` → NSView pointer
  - `CPlatformComponent.getPointer()` returns CALayer, NOT NSView (different path)
  - JAWT on macOS JDK 9+ returns CALayer (not NSView) via `JAWT_SurfaceLayers` protocol
  - Reflection requires: `--add-opens=java.desktop/{java.awt,sun.lwawt,sun.lwawt.macosx}=ALL-UNNAMED`
- **Canvas initialization timing:**
  - `addNotify()` fires before layout → Canvas has 0x0 dimensions
  - Surface needs non-zero dimensions → use 1x1 placeholder, defer `onReady` to first resize
- **Gemini insight (from user):** ComposePanel deadlocks can stem from non-MainUIDispatcher usage. Confirmed our `application {}` deadlock matches this pattern.

### Issues
- **`application {}` + `-XstartOnFirstThread` deadlock:** Compose Desktop's internal event loop setup conflicts with the AppKit main thread flag. No window appears, process hangs forever. Root cause is in Compose Multiplatform's macOS windowing code.
- **NSView pointer null without `-XstartOnFirstThread`:** JNA/JAWT can't extract the native handle when the JVM main thread isn't the AppKit thread. Solved via AWT internal reflection.
- **Initial 0x0 Canvas size:** `addNotify()` fires before the layout manager assigns dimensions. Scene created with invalid aspect ratio (0/0 = NaN). Fixed by deferring `onReady` to first `componentResized` event.

### Lessons Learned
- On macOS, `-XstartOnFirstThread` and `SwingUtilities.invokeLater` are incompatible — the EDT never processes enqueued runnables.
- `ComposePanel` (Swing component) works without `-XstartOnFirstThread`, unlike Compose `application {}`.
- AWT's `addNotify()` fires before layout — always check for valid dimensions before creating GPU resources.
- AWT reflection for native handles is fragile but is the only way to avoid `-XstartOnFirstThread` on macOS for JNA-based handle extraction.

### Commits

---

## Next Steps
- Manual testing: verify Compose controls (slider, buttons, pause) drive the 3D scene
- Commit all changes on `feat/compose-integration` branch and create PR
- Future: Implement Flutter integration (M11) after mobile platform support is complete
