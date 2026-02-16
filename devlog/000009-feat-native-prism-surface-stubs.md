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

## Next Steps
- PR review and merge (#24)
- Test actual GPU rendering on macOS native (requires display + GPU)
- Future: embed native surfaces into UI frameworks (AppKit, GTK, Win32) instead of standalone GLFW windows
