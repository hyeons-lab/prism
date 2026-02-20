# 000016 feat/prism-web

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ prism branch feat/prism-web

**Intent:** Create `prism-web` as a standalone wasmJs application module that owns the web
executable target, moving it out of `prism-demo-core`. `prism-demo-core` becomes a pure library
with no wasmJs binary output.

## Progress

- [x] Create worktree and devlog
- [x] Create plan file
- [ ] Create `prism-web` module (build.gradle.kts + source files)
- [ ] Move resources from `prism-demo-core` to `prism-web`
- [ ] Update `prism-demo-core` to library-only wasmJs
- [ ] Update `docs/wasm/pbr.html`
- [ ] Build + sync, format + validate
- [ ] Commit and push draft PR

## What Changed

<!-- populated as work progresses -->

## Decisions

2026-02-20T10:00-08:00 Source files placed in `wasmJsMain` (not `commonMain`) — prism-web is
wasmJs-only so `commonMain` would compile identically, but `wasmJsMain` is semantically clearer and
mirrors how `prism-demo-core` structured its web entry point.

2026-02-20T10:00-08:00 `uploadDecodedImage()` in prism-web is a plain `internal fun` (not
expect/actual) — prism-web targets only wasmJs so the WASM implementation is written directly
without the expect/actual indirection used in prism-demo-core.

2026-02-20T10:00-08:00 Scene files (`DemoScene`, `CornellBoxScene`, `MaterialPresetScene`,
`GltfDemoScene`) are duplicated in prism-web package `com.hyeonslab.prism.web` — intentional per
the plan; prism-web is designed as a reference for direct engine API usage without depending on
prism-demo-core.

## Issues

<!-- populated as issues arise -->

## Commits

<!-- populated as commits are made -->
