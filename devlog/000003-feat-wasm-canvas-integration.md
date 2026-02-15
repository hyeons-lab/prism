# 000003-feat-wasm-canvas-integration

## Session 1 — WASM/Canvas WebGPU Integration (2026-02-15 00:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/wasm-canvas-integration`

### Intent

Add a browser entry point for the Prism demo that renders the same rotating lit cube as the JVM/GLFW demo, but via WebGPU in the browser using wgpu4k-toolkit's `canvasContextRenderer()`.

### What Changed

- **[2026-02-15 00:00 PST]** `prism-demo/build.gradle.kts` — Added `wasmJsMain.dependencies` block with wgpu4k, wgpu4k-toolkit, and kotlinx-coroutines-core (needed since prism-renderer declares these as `implementation`, not `api`)
- **[2026-02-15 00:00 PST]** `prism-demo/src/wasmJsMain/kotlin/.../Main.kt` — Replaced Compose `CanvasBasedWindow` entry with raw WebGPU entry: gets canvas from DOM via `@JsFun` (with JS-level `instanceof HTMLCanvasElement` validation), creates `WGPUContext` via `canvasContextRenderer()`, sets up Engine + ECS world with camera and cube entities (same parameters as JVM demo), runs render loop via `setInterval` at ~60fps with `performance.now()` timing. Includes `CoroutineExceptionHandler` for init failures, try-catch in render loop with `clearInterval` on error, and `showError()` to surface errors in the DOM fallback div.
- **[2026-02-15 00:00 PST]** `prism-demo/src/wasmJsMain/resources/index.html` — Created HTML host page with `<canvas id="prismCanvas">`, WebGPU feature detection fallback message, dark background styling, and `prism-demo.js` script tag
- **[2026-02-15 00:00 PST]** `README.md` — Added wgpu4k pinned commit hash (`3fc6e327`, verified against local clone HEAD) to build instructions and tech stack table, updated Platform Support table (Web WASM/JS → Working), updated Milestone table (M6 → Done). Initially set to "In Progress" pending browser verification; promoted to Done after confirming the demo renders correctly in the browser.

### Decisions

- **[2026-02-15 00:00 PST]** **Use `@JsFun` for JS interop instead of `kotlinx.browser`** — Declared `getCanvasById`, `setInterval`, and `performanceNow` as `@JsFun` external functions. This avoids dependency on `kotlinx.browser` and gives explicit control over the JS interop layer.
- **[2026-02-15 00:00 PST]** **Use `GlobalScope.launch` for async init** — `canvasContextRenderer()` is a suspend function, and `runBlocking` is not available in Kotlin/WASM. `GlobalScope.launch` with `@OptIn(DelicateCoroutinesApi::class)` is the appropriate pattern for a top-level browser entry point.
- **[2026-02-15 00:00 PST]** **Use `setInterval` over `requestAnimationFrame`** — Simpler API for initial integration; can migrate to `requestAnimationFrame` later for vsync-aligned rendering.
- **[2026-02-15 00:00 PST]** **Let Kotlin code get canvas from DOM** — Rather than letting `canvasContextRenderer(null)` create a detached canvas, we get the canvas from the HTML page via `@JsFun("(id) => document.getElementById(id)")` and pass it explicitly. This ensures the canvas is properly positioned in the page layout.

### Research & Discoveries

- wgpu4k-toolkit uses `web.html.HTMLCanvasElement` (from the newer `kotlin-browser` package), NOT `org.w3c.dom.HTMLCanvasElement` (the legacy DOM API). Import must match.
- `@JsFun` external functions require `@OptIn(ExperimentalWasmJsInterop::class)` — applied as `@file:OptIn` to keep the code clean.
- `canvasContextRenderer` signature: `suspend fun canvasContextRenderer(htmlCanvas: HTMLCanvasElement? = null, deferredRendering: Boolean = false, width: Int? = null, height: Int? = null, onUncapturedError: GPUUncapturedErrorCallback? = null): CanvasContext`
- `CanvasContext` wraps both the `HTMLCanvasElement` and `WGPUContext`, similar to how `GLFWContext` wraps the window handler and `WGPUContext`.

### Issues

- **Type mismatch on HTMLCanvasElement:** Initially imported `org.w3c.dom.HTMLCanvasElement` but wgpu4k expects `web.html.HTMLCanvasElement`. Fixed by changing the import.
- **ExperimentalWasmJsInterop opt-in:** `@JsFun` annotations trigger warnings-as-errors without the opt-in. Fixed with `@file:OptIn(ExperimentalWasmJsInterop::class)`.
- **Disk space exhaustion:** Gradle daemon logs consumed 3.7GB in `~/.gradle/daemon/`. Cleaned daemon logs and build-cache-1 to free ~3.8GB.
- **Silent failure on init errors:** Initial version used bare `GlobalScope.launch` with no exception handler — WebGPU init failures (adapter not found, device lost, etc.) were silently swallowed. Fixed by adding `CoroutineExceptionHandler` that logs and surfaces errors in the DOM.
- **Runaway setInterval on render errors:** Initial version had no error handling in the `setInterval` callback — a single render error would produce an infinite stream of console errors at 60fps. Fixed by wrapping the callback body in try-catch that calls `clearInterval` on first error.
- **Unsafe getCanvasById return type:** `document.getElementById` returns `Element | null` but was declared as `HTMLCanvasElement?`. If the HTML element was not a canvas, the mismatch would surface deep in wgpu4k. Fixed by adding JS-level `instanceof HTMLCanvasElement` validation in the `@JsFun` body.
- **Premature "Done" status for M6:** README marked M6 and Platform Support as "Done"/"Working" before browser testing. Changed to "In Progress" since WASM compiles but hasn't been verified in a browser yet.

### Lessons Learned

- In Kotlin/WASM (wasmJs), use `web.html.*` types (not `org.w3c.dom.*`) when interacting with wgpu4k-toolkit's browser APIs.
- `@JsFun` requires `@OptIn(ExperimentalWasmJsInterop::class)` in Kotlin 2.3.0.
- Gradle daemon logs can silently consume gigabytes of disk space — periodically clean `~/.gradle/daemon/*/daemon-*.out.log`.
- `runBlocking` is NOT available in Kotlin/WASM — use `GlobalScope.launch` for top-level async entry points.
- Always add `CoroutineExceptionHandler` to `GlobalScope.launch` in browser entry points — otherwise exceptions vanish silently.
- `setInterval` callbacks need try-catch with `clearInterval` to avoid runaway error loops — unlike `requestAnimationFrame`, `setInterval` keeps firing regardless of callback failures.
- `@JsFun` return types are unchecked type assertions — when returning DOM subtypes like `HTMLCanvasElement`, validate with `instanceof` inside the JS body to catch type mismatches early.
- Don't mark milestones as "Done" until the feature is tested end-to-end on the target platform.

### Commits

- *(pending — awaiting user commit request)*

---

## Next Steps

- Test in Chrome 113+ / Edge 113+ with `./gradlew :prism-demo:wasmJsBrowserDevelopmentRun`
- Consider migrating from `setInterval` to `requestAnimationFrame` for vsync-aligned rendering
- Add touch/pointer input handling for mobile browser support
