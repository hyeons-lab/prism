# 000004-fix-wasm-demo-review-feedback

## Session 1 — Address PR #5 Copilot review feedback (2026-02-15 01:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `fix/wasm-demo-review-feedback`

### Intent

Address 7 Copilot review comments on PR #5 (WASM/Canvas WebGPU integration). Fix render loop timing, resource cleanup, accessibility, and commit hash precision.

### What Changed

- **[2026-02-15 01:00 PST]** `prism-demo/src/wasmJsMain/kotlin/.../Main.kt` — Replaced `setInterval`/`clearInterval` with `requestAnimationFrame` for vsync-aligned rendering (comment #4). Compute actual `deltaTime` from frame timestamps instead of hardcoded `1f/60f` (comment #6). Add `world.shutdown()`/`engine.shutdown()` in error catch block (comment #1). Add `beforeunload` event listener for page-unload cleanup (comment #5). Added double-shutdown guard (`if (!running) return@onBeforeUnload`) so `beforeunload` is a no-op if the render loop already stopped on error. Switched from `Logger.i("Prism")` to `Logger.withTag("Prism")` stored as module-level `log` val for consistent tagged logging. Restructured render loop as recursive `renderFrame()` local function with `running` flag for clean shutdown.
- **[2026-02-15 01:00 PST]** `prism-demo/src/jvmMain/kotlin/.../GlfwMain.kt` — Switched from `Logger.i("Prism")` to `Logger.withTag("Prism")` stored as module-level `log` val, matching the WASM entry point pattern.
- **[2026-02-15 01:00 PST]** `prism-demo/src/wasmJsMain/resources/index.html` — Added `aria-label` to canvas element for screen reader accessibility (comment #7).
- **[2026-02-15 01:00 PST]** `README.md` — Expanded wgpu4k commit hash from 8-char (`3fc6e327`) to full 40-char (`3fc6e3297fee6b558efc6dcb29aec1a6629b0e90`) in `git checkout` command (comment #3).

### Decisions

- **[2026-02-15 01:00 PST]** **Skip async WebGPU detection in HTML (comment #2)** — Copilot suggested calling `requestAdapter()` in the HTML script to validate WebGPU before loading WASM. Declined because: (a) the Kotlin-side `CoroutineExceptionHandler` already handles adapter request failures with DOM error feedback, (b) calling `requestAdapter()` twice (once in JS, once in wgpu4k) is wasteful, (c) the simple `navigator.gpu` check covers the 99% case (browser with no WebGPU at all).

### Commits

- `719506f` — fix: address PR #5 review feedback for WASM demo

### PR

- [PR #6](https://github.com/hyeons-lab/prism/pull/6) — fix: address PR #5 review feedback for WASM demo

---

## Session 2 — Update devlog and documentation for merged WASM PRs (2026-02-15 10:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `fix/wasm-demo-review-feedback`

### Intent

PR #5 and PR #6 are merged to main. The devlog was missing the final commit hash, Logger.withTag changes, and double-shutdown guard details. Several documentation files still showed WASM/M6 as incomplete.

### What Changed

- **[2026-02-15 10:00 PST]** `devlog/000004-fix-wasm-demo-review-feedback.md` — Backfilled Session 1: added `GlfwMain.kt` Logger.withTag entry, added double-shutdown guard and Logger.withTag details to Main.kt entry, replaced pending commit placeholder with `719506f`, added PR #6 link.
- **[2026-02-15 10:00 PST]** `BUILD_STATUS.md` — Checked off WASM/Canvas under Phase 2 Pending. Expanded M6 from `⏳` to `✅` with bullet points (canvasContextRenderer, WebGPU detection, requestAnimationFrame, cleanup). Added `./gradlew :prism-demo:wasmJsBrowserDevelopmentRun` to Build Commands section.
- **[2026-02-15 10:00 PST]** `AGENTS.md` — Added `✅ WASM/Canvas WebGPU integration (M6 complete)` to "What works" list. Removed `⏭️ WASM/Canvas integration for web` from "What's next" list.

### Commits

- `ddd21cd` — docs: update devlog and status docs for merged WASM PRs
