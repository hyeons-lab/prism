# 000004-fix-wasm-demo-review-feedback

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `fix/wasm-demo-review-feedback`

## Intent

Address Copilot review comments on PR #5 and PR #6 (WASM/Canvas WebGPU integration). Fix render loop timing, resource cleanup, accessibility, commit hash precision, and update documentation for merged WASM PRs.

## What Changed

- **2026-02-15T01:00-08:00** `prism-demo/src/wasmJsMain/kotlin/.../Main.kt` — Replaced `setInterval`/`clearInterval` with `requestAnimationFrame` for vsync-aligned rendering. Compute actual `deltaTime` from frame timestamps instead of hardcoded `1f/60f`. Add `world.shutdown()`/`engine.shutdown()` in error catch. Add `beforeunload` event listener for page-unload cleanup with double-shutdown guard. Switched to `Logger.withTag("Prism")`. Restructured as recursive `renderFrame()` with `running` flag. Later updated `requestAnimationFrame` to accept `(Double) -> Unit` for DOMHighResTimeStamp, moved `onBeforeUnload` before `world.initialize()`.
- **2026-02-15T01:00-08:00** `prism-demo/src/jvmMain/kotlin/.../GlfwMain.kt` — Switched to `Logger.withTag("Prism")`.
- **2026-02-15T01:00-08:00** `prism-demo/src/wasmJsMain/resources/index.html` — Added `aria-label` to canvas element for screen reader accessibility.
- **2026-02-15T01:00-08:00** `README.md` — Expanded wgpu4k commit hash from 8-char to full 40-char.
- **2026-02-15T10:00-08:00** `BUILD_STATUS.md` — Checked off WASM/Canvas. Expanded M6 to complete with details.
- **2026-02-15T10:00-08:00** `AGENTS.md` — Added WASM/Canvas to "What works" list.

## Decisions

- **2026-02-15T01:00-08:00** **Skip async WebGPU detection in HTML** — Kotlin-side `CoroutineExceptionHandler` already handles adapter request failures; calling `requestAdapter()` twice is wasteful.

## Commits

- `719506f` — fix: address PR #5 review feedback for WASM demo
- `ddd21cd` — docs: update devlog and status docs for merged WASM PRs
- `a1c7070` — fix: use requestAnimationFrame timestamp and register cleanup earlier
