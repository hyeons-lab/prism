# 000004-fix-wasm-demo-review-feedback

## Session 1 — Address PR #5 Copilot review feedback (2026-02-15 01:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `fix/wasm-demo-review-feedback`

### Intent

Address 7 Copilot review comments on PR #5 (WASM/Canvas WebGPU integration). Fix render loop timing, resource cleanup, accessibility, and commit hash precision.

### What Changed

- **[2026-02-15 01:00 PST]** `prism-demo/src/wasmJsMain/kotlin/.../Main.kt` — Replaced `setInterval`/`clearInterval` with `requestAnimationFrame` for vsync-aligned rendering (comments #4). Compute actual `deltaTime` from frame timestamps instead of hardcoded `1f/60f` (comment #6). Add `world.shutdown()`/`engine.shutdown()` in error catch block (comment #1). Add `beforeunload` event listener for page-unload cleanup (comment #5). Restructured render loop as recursive `renderFrame()` local function with `running` flag for clean shutdown.
- **[2026-02-15 01:00 PST]** `prism-demo/src/wasmJsMain/resources/index.html` — Added `aria-label` to canvas element for screen reader accessibility (comment #7).
- **[2026-02-15 01:00 PST]** `README.md` — Expanded wgpu4k commit hash from 8-char (`3fc6e327`) to full 40-char (`3fc6e3297fee6b558efc6dcb29aec1a6629b0e90`) in `git checkout` command (comment #3).

### Decisions

- **[2026-02-15 01:00 PST]** **Skip async WebGPU detection in HTML (comment #2)** — Copilot suggested calling `requestAdapter()` in the HTML script to validate WebGPU before loading WASM. Declined because: (a) the Kotlin-side `CoroutineExceptionHandler` already handles adapter request failures with DOM error feedback, (b) calling `requestAdapter()` twice (once in JS, once in wgpu4k) is wasteful, (c) the simple `navigator.gpu` check covers the 99% case (browser with no WebGPU at all).

### Commits

- *(pending — awaiting user commit request)*
