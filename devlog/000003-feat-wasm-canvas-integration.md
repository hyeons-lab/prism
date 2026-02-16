# 000003-feat-wasm-canvas-integration

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/wasm-canvas-integration`

## Intent

Add a browser entry point for the Prism demo that renders the same rotating lit cube as the JVM/GLFW demo, but via WebGPU in the browser using wgpu4k-toolkit's `canvasContextRenderer()`.

## What Changed

- **2026-02-15T00:00-08:00** `prism-demo/build.gradle.kts` — Added `wasmJsMain.dependencies` block with wgpu4k, wgpu4k-toolkit, and kotlinx-coroutines-core.
- **2026-02-15T00:00-08:00** `prism-demo/src/wasmJsMain/kotlin/.../Main.kt` — Raw WebGPU entry: gets canvas from DOM via `@JsFun` (with JS-level `instanceof HTMLCanvasElement` validation), creates `WGPUContext` via `canvasContextRenderer()`, sets up Engine + ECS world, runs render loop via `requestAnimationFrame` with `performance.now()` timing. Includes `CoroutineExceptionHandler` for init failures, try-catch in render loop, and `showError()` for DOM error feedback.
- **2026-02-15T00:00-08:00** `prism-demo/src/wasmJsMain/resources/index.html` — HTML host page with `<canvas id="prismCanvas">`, WebGPU feature detection, dark background styling.
- **2026-02-15T00:00-08:00** `README.md` — Updated Platform Support (Web WASM/JS -> Working), Milestone table (M6 -> Done).

## Decisions

- **2026-02-15T00:00-08:00** **Use `@JsFun` for JS interop instead of `kotlinx.browser`** — Avoids dependency, gives explicit control over the JS interop layer.
- **2026-02-15T00:00-08:00** **Use `GlobalScope.launch` for async init** — `runBlocking` not available in Kotlin/WASM.
- **2026-02-15T00:00-08:00** **Let Kotlin code get canvas from DOM** — Rather than letting `canvasContextRenderer(null)` create a detached canvas, get it from the HTML page.

## Research & Discoveries

- wgpu4k-toolkit uses `web.html.HTMLCanvasElement` (NOT `org.w3c.dom.HTMLCanvasElement`) — import must match.
- `@JsFun` external functions require `@OptIn(ExperimentalWasmJsInterop::class)`.
- `canvasContextRenderer` returns `CanvasContext` wrapping both `HTMLCanvasElement` and `WGPUContext`.

## Issues

- **Type mismatch on HTMLCanvasElement** — Initially imported `org.w3c.dom` but wgpu4k expects `web.html`. Fixed import.
- **Silent failure on init errors** — Bare `GlobalScope.launch` swallowed exceptions. Fixed with `CoroutineExceptionHandler`.
- **Runaway requestAnimationFrame on render errors** — No error handling in callback. Fixed with try-catch and `running = false`.
- **Unsafe getCanvasById return type** — Added JS-level `instanceof HTMLCanvasElement` validation.

## Lessons Learned

- In Kotlin/WASM, use `web.html.*` types (not `org.w3c.dom.*`) for wgpu4k-toolkit APIs.
- `runBlocking` is NOT available in Kotlin/WASM — use `GlobalScope.launch`.
- Always add `CoroutineExceptionHandler` to `GlobalScope.launch` in browser entry points.
- `@JsFun` return types are unchecked — validate with `instanceof` inside the JS body.

## Commits

- *(part of PR #5 squash)*

## Next Steps

- Consider migrating from `setInterval` to `requestAnimationFrame` for vsync-aligned rendering
