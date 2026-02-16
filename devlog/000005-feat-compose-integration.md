# 000005-feat-compose-integration

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/compose-integration`

## Intent

Implement Compose Desktop integration with embedded 3D rendering using wgpu4k surface creation from AWT Canvas native handles. Create PrismPanel (AWT Canvas subclass), MVI architecture with Store<State, Event>, interactive Compose demo with Material3 controls, and fix macOS Metal sublayer sizing for correct resize behavior.

## What Changed

- **2026-02-15T08:45-08:00** `prism-renderer/.../WgpuRenderer.kt` — Added `surfacePreConfigured: Boolean` constructor parameter and `onResize` callback property.
- **2026-02-15T08:48-08:00** `prism-native-widgets/build.gradle.kts` — Added `jvmToolchain(25)`, jvmMain dependencies on wgpu4k, wgpu4k-toolkit, kotlinx-coroutines-core.
- **2026-02-15T08:50-08:00** `prism-native-widgets/.../PrismPanel.kt` — Complete rewrite from empty stub to full AWT Canvas subclass (~240 lines). Native handle extraction (macOS/Windows/Linux), wgpu surface creation, AwtRenderingContext, resize handling. Later: dual-path NSView resolution (JNA fast path + AWT reflection fallback), deferred `onReady`, sublayer approach replacing `setLayer`, `ObjCBridge` for JNA `objc_msgSend` calls, cached ObjC selectors, null checks, `@Suppress("ArrayPrimitive")`.
- **2026-02-15T08:53-08:00** `prism-compose/.../EngineState.kt` — Refactored multiple times: initial `ownsEngine` flag, then complete MVI rewrite as `EngineState` data class + `EngineStore` implementing `Store<EngineState, EngineStateEvent>` with `@Stable` annotation. `rememberEngineStore()` / `rememberExternalEngineStore()`.
- **2026-02-15T08:55-08:00** `prism-compose/.../PrismView.jvm.kt` — Full implementation using `SwingPanel` embedding `PrismPanel`. Changed from `delay(1)` to `delay(16)` for ~60fps.
- **2026-02-15T08:56-08:00** `prism-compose/build.gradle.kts` — Added `jvmToolchain(25)`, compose deps, kotlinx-coroutines-core, lifecycle-runtime-compose, kotlinx-coroutines-swing.
- **2026-02-15T08:58-08:00** `prism-demo/.../DemoSceneState.kt` — MVI pattern: `DemoUiState` (immutable data class), `DemoIntent` (sealed interface), `DemoStore` with `@Stable`. Implements `Store<DemoUiState, DemoIntent>`.
- **2026-02-15T08:59-08:00** `prism-demo/.../ComposeDemoControls.kt` — Material3 UI panel: FPS display, rotation speed slider, pause/resume button, 6 color preset buttons.
- **2026-02-15T09:01-08:00** `prism-demo/.../ComposeMain.kt` — JFrame + ComposePanel architecture (not Compose `application {}` which deadlocks on macOS). PrismPanel in BorderLayout.CENTER, ComposePanel in EAST. Render loop via `withFrameNanos`. `collectAsStateWithLifecycle()`.
- **2026-02-15T09:02-08:00** `prism-demo/build.gradle.kts` — Added `--add-opens` JVM args, `runCompose` task, lifecycle-runtime-compose, kotlinx-coroutines-swing, commonTest deps.
- **2026-02-15T09:04-08:00** `prism-core/.../Store.kt` — New `Store<State, Event>` interface with `val state: StateFlow<State>` and `fun dispatch(event: Event)`.
- **2026-02-15T09:04-08:00** `prism-compose/.../PrismView.kt` (expect) — Changed signature to `(EngineStore, Modifier)`.
- **2026-02-15T09:04-08:00** `prism-compose/.../PrismOverlay.kt` — Uses `store.state.map { it.isInitialized }.distinctUntilChanged()` to avoid per-frame recomposition. `collectAsStateWithLifecycle()`.
- **2026-02-15T09:04-08:00** `prism-compose/src/appleMain/.../PrismView.apple.kt` — Moved from `iosMain` to `appleMain` to cover both iOS and macOS native targets.
- **2026-02-15T09:04-08:00** `prism-demo/src/jvmMain/.../DemoScene.kt` — Shared factory: `createDemoScene()` used by both GLFW and Compose entry points.
- **2026-02-15T09:04-08:00** `prism-demo/.../ComposeDemoApp.kt` — Uses `rememberEngineStore()`, `collectAsStateWithLifecycle()`.
- **2026-02-15T09:04-08:00** `gradle/libs.versions.toml` — Added lifecycleCompose, lifecycle-runtime-compose. Sorted all sections alphabetically.

## Decisions

- **2026-02-15T08:48-08:00** **Use `Surface(nativeSurface, 0L)` with custom `AwtRenderingContext`** — Bypasses wgpu4k's `glfwGetWindowSize()` with Canvas dimensions.
- **2026-02-15T08:50-08:00** **Pre-configure surface before WgpuRenderer** — WgpuRenderer uses `surfacePreConfigured=true`.
- **2026-02-15T09:01-08:00** **JFrame + ComposePanel over application {}** — Compose `application {}` deadlocks on macOS with `-XstartOnFirstThread`. `ComposePanel` works inside JFrame without conflict.
- **2026-02-15T09:01-08:00** **Run Compose demo without -XstartOnFirstThread** — Use AWT reflection for native handle extraction instead.
- **2026-02-15T09:01-08:00** **Swing Timer then withFrameNanos for render loop** — Compose's `withFrameNanos` synchronizes with display refresh rate.
- **2026-02-15T09:04-08:00** **`Store<State, Event>` in prism-core** — Shared MVI interface for both EngineStore and DemoStore.
- **2026-02-15T09:04-08:00** **EngineState as data class** — Engine reference on Store, not in state. Makes state a pure value type.
- **2026-02-15T09:04-08:00** **iosMain -> appleMain** — `applyDefaultHierarchyTemplate()` creates `appleMain` covering both iOS and macOS.
- **2026-02-15T09:04-08:00** **Sublayer approach over setLayer** — `nsView.setLayer(metalLayer)` replaces window's backing layer, breaking AWT. Use `addSublayer` with frame positioning.
- **2026-02-15T09:04-08:00** **Direct JNA `objc_msgSend` over Rococoa extension** — Rococoa 0.0.1 doesn't expose needed CALayer methods.
- **2026-02-15T09:04-08:00** **`@Stable` on EngineStore and DemoStore** — Without it, Compose assumes instability and recomposes every frame.
- **2026-02-15T09:04-08:00** **`lifecycle-runtime-compose` in prism-compose** — Library composables use `collectAsStateWithLifecycle`, so dependency belongs in library module.
- **2026-02-15T09:04-08:00** **`kotlinx-coroutines-swing` for Desktop Main dispatcher** — Required by `collectAsStateWithLifecycle`.

## Research & Discoveries

- **macOS `-XstartOnFirstThread` constraints:** Makes main thread = AppKit thread. Required for GLFW and JNA `getComponentPointer()`. Compose `application {}` deadlocks. `SwingUtilities.invokeLater` doesn't dispatch.
- **AWT native handle extraction on macOS:** `Component.peer -> LWCanvasPeer -> LWWindowPeer -> CPlatformWindow.contentView -> CPlatformView.getAWTView()` -> NSView pointer. Requires `--add-opens`.
- **macOS LW AWT shares a single NSView for all components.** `resolveNsViewViaReflection()` returns the WINDOW's content NSView, not per-Canvas.
- **`setLayer:` replaces backing layer (layer-hosting mode)**, breaks AWT rendering. `setWantsLayer(true)` + `addSublayer` preserves AWT (layer-backed mode).
- **arm64 `objc_msgSend` CGRect:** 4 doubles go in FP registers d0-d3. JNA handles this correctly.
- **CALayer implicit animation:** 0.25s default. Use `CATransaction.setDisableActions(true)`.
- **Compose Desktop Dispatchers.Main = EDT.** Single-threaded, no synchronization needed.
- **wgpu4k `WGPUContext.close()`** has commented-out `renderingContext.close()` due to upstream crash.

## Issues

- **`application {}` + `-XstartOnFirstThread` deadlock** — Compose event loop conflicts with AppKit main thread. Solved with JFrame + ComposePanel.
- **NSView pointer null without `-XstartOnFirstThread`** — JNA/JAWT can't extract native handle. Solved via AWT reflection.
- **Initial 0x0 Canvas size** — `addNotify()` fires before layout. Solved by deferring `onReady` to first `componentResized`.
- **Inverted resize behavior** — CAMetalLayer covering entire window; canvas texture stretched to window bounds. Fixed with sublayer positioning.
- **`collectAsStateWithLifecycle` crash** — Missing `Dispatchers.Main`. Fixed with `kotlinx-coroutines-swing`.
- **`@Stable` missing on EngineStore** — Every composable recomposed unconditionally at 60fps.
- **PrismOverlay recomposing every frame** — Collected full `EngineState` but only needed `isInitialized`. Fixed with `map {}` + `distinctUntilChanged()`.
- **WGPU instance leak** — `WGPU.createInstance()` local not closed. Fixed: stored in field, closed in `removeNotify()`.

## Lessons Learned

- On macOS, `-XstartOnFirstThread` and `SwingUtilities.invokeLater` are incompatible.
- `ComposePanel` (Swing component) works without `-XstartOnFirstThread`, unlike Compose `application {}`.
- `nsView.setLayer()` replaces the backing layer; use `addSublayer` for Metal in AWT.
- JNA `objc_msgSend` on arm64 works correctly for struct-by-value args (CGRect) without variadic promotion.
- `@Stable` is critical for classes passed to composables that hold `MutableStateFlow` internally.
- Use `map {}` + `distinctUntilChanged()` when composable only needs a subset of StateFlow state.
- `collectAsStateWithLifecycle` needs platform-specific coroutines module (`kotlinx-coroutines-swing` on Desktop).
- Compose's `withFrameNanos` is the preferred render loop mechanism.

## Commits

- `6fd72c8` — feat: add Compose Desktop integration with embedded 3D rendering (Sessions 1-7 squashed)
- `84a6952` — refactor: MVI architecture for Compose layer with lifecycle-aware state
- `5296cc2` — fix: address PR #7 review feedback
