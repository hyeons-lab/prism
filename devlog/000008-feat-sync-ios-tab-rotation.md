# 000008-feat-sync-ios-tab-rotation

## Session 1 — Fix iOS demo bugs, sync tab rotation, and polish UI (2026-02-15 20:30 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/sync-ios-tab-rotation`

### Intent
Fix multiple iOS demo bugs (module name collision preventing Xcode build, Compose tab crash, MTKView delegate getting GC'd) and add quality-of-life improvements (synced rotation between Native and Compose tabs, UI polish, safe area handling).

### What Changed
- **[2026-02-15 20:30 PST]** `ios-demo/project.yml` — Renamed target from `PrismDemo` to `PrismDemoApp` to fix module name collision (Swift was ignoring `import PrismDemo` because the app target had the same module name as the framework). Added `PRODUCT_NAME: PrismDemoApp`. Switched XCFramework path from `release/` to `debug/` (matching CI change from PR #19).
- **[2026-02-15 20:30 PST]** `ios-demo/Sources/Info.plist` — Added `CADisableMinimumFrameDurationOnPhone = true` to fix Compose tab crash (`kotlin.IllegalStateException: Info.plist doesn't have CADisableMinimumFrameDurationOnPhone`). Required by Compose Multiplatform on ProMotion-capable iPhones.
- **[2026-02-15 20:30 PST]** `ios-demo/Sources/SceneDelegate.swift` — Changed Compose tab icon from `slider.horizontal.3` to `square.stack.3d.up` (the slider icon looked like a settings panel).
- **[2026-02-15 20:45 PST]** `prism-demo/src/iosMain/.../IosConstants.kt` — New file. Added:
  - `sharedDemoStore: DemoStore` — single store shared by both tabs so pause/speed/color state is unified
  - `SharedDemoTime` object — tracks pause-aware elapsed time with `elapsed()`, `angle()`, and `syncPause()`. Handles speed changes smoothly by accumulating an angle offset when speed changes.
- **[2026-02-15 20:45 PST]** `prism-demo/src/iosMain/.../IosDemoController.kt` — Fixed MTKView delegate GC: added `renderDelegate` parameter to `IosDemoHandle` as a strong reference (MTKView.delegate is WEAK in UIKit). `DemoRenderDelegate` now reads pause/speed from `sharedDemoStore` and computes rotation via `SharedDemoTime.angle()`.
- **[2026-02-15 20:45 PST]** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Fixed delegate GC: added `renderDelegate` to `remember` state. Uses `sharedDemoStore` instead of creating a local `DemoStore()`. Replaced incremental rotation (`+= deltaTime * speed`) with absolute angle via `SharedDemoTime.angle()`. Added `WindowInsets.safeDrawing` padding to avoid Dynamic Island/notch clipping.
- **[2026-02-15 20:50 PST]** `prism-demo/src/commonMain/.../ComposeDemoControls.kt` — Replaced two rows of text-labeled color buttons with a single row of 32dp circular color swatches using `CircleShape` and `Arrangement.SpaceEvenly`.
- **[2026-02-15 20:50 PST]** `README.md` — Updated iOS platform status from "Planned" to "Working", M7 milestone from "Planned" to "Done", added cross-reference to `prism-demo/README.md`.
- **[2026-02-15 20:50 PST]** `prism-demo/README.md` — New file. Documents all demo platforms (JVM Desktop, Web, iOS), shared code architecture, build/run instructions for iOS simulator, and interactive controls reference.

### Decisions
- **[2026-02-15 20:30 PST]** **Rename Xcode target to `PrismDemoApp`** — The Kotlin XCFramework module name is `PrismDemo` and cannot be easily changed (it's the `baseName` used by all iOS exports). Renaming the Swift app target avoids the collision with minimal impact.
- **[2026-02-15 20:30 PST]** **Strong reference for MTKView delegates** — MTKView.delegate is a WEAK reference in UIKit. Kotlin/Native GC collected the delegate objects after a few seconds, stopping rotation. Storing the delegate as a constructor parameter in `IosDemoHandle` (Native tab) and as `remember` state (Compose tab) ensures it survives.
- **[2026-02-15 20:45 PST]** **Shared DemoStore via top-level val** — Both tabs read from the same store. The Compose tab's UI controls (pause, speed slider, color picker) affect the Native tab too. Simpler than separate stores with a sync mechanism.
- **[2026-02-15 20:45 PST]** **SharedDemoTime with angle offset tracking** — Rather than tracking paused intervals independently per delegate, a single object freezes/unfreezes elapsed time. The `angle()` method accumulates an offset on speed changes so the cube doesn't jump — both tabs see the same angle at all times.
- **[2026-02-15 20:45 PST]** **Absolute angle instead of incremental in Compose** — Switched from `rotationAngle += deltaTime * speed` to computing angle from shared elapsed time. Eliminates floating-point drift between tabs and guarantees identical angles.
- **[2026-02-15 20:50 PST]** **Debug XCFramework path in project.yml** — Matches the CI change from PR #19 (debug XCFramework for development, release for distribution).

### Issues
- **"Cannot find type 'IosDemoHandle' in scope"** — Root cause was NOT a missing type but a module name collision. The Xcode target `PrismDemo` and the Kotlin framework `PrismDemo` had the same module name. Swift ignored `import PrismDemo` as a self-import. Fixed by renaming the target to `PrismDemoApp`.
- **Compose tab crash on ProMotion iPhones** — `IllegalStateException: Info.plist doesn't have CADisableMinimumFrameDurationOnPhone`. Required by Compose Multiplatform for 120Hz displays. Fixed by adding the key to Info.plist.
- **Rotation stops after a few seconds** — MTKView.delegate is WEAK in UIKit. Kotlin/Native GC collected the delegates since nothing held a strong reference. Fixed by storing delegates explicitly.

### Lessons Learned
- MTKView.delegate is a WEAK reference — any Kotlin/Native object set as a delegate needs a strong reference elsewhere or K/N GC will collect it.
- Swift module name collision is silent — `import PrismDemo` is ignored when the app module is also named `PrismDemo`, with no compiler error, just "cannot find type" for every imported symbol.
- `CADisableMinimumFrameDurationOnPhone` is required by Compose Multiplatform's iOS rendering on ProMotion devices.

### Commits
- `952ac1e` — feat: sync iOS tab rotation, fix delegate GC, and polish demo UI

---

## Session 2 — Rename ios-demo, sync color, and review fixes (2026-02-15 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/sync-ios-tab-rotation`

### Intent
Rename `ios-demo/` to `prism-ios-demo/` for naming consistency, sync cube color between Native and Compose iOS tabs, fix review nits (stale KDoc, missing cleanup, docs).

### What Changed
- **[2026-02-15 PST]** `ios-demo/` → `prism-ios-demo/` — Renamed directory via `git mv` to follow `prism-*` naming convention.
- **[2026-02-15 PST]** `.gitignore` — Updated `ios-demo` → `prism-ios-demo` in Xcode gitignore patterns.
- **[2026-02-15 PST]** `AGENTS.md` — Updated xcodegen command path from `ios-demo` to `prism-ios-demo`.
- **[2026-02-15 PST]** `BUILD_STATUS.md` — Updated 2 references: xcodegen path and M7 description.
- **[2026-02-15 PST]** `prism-demo/README.md` — Updated 4 path references from `ios-demo` to `prism-ios-demo`. Fixed JVM entry point table (GLFW is default, added `runCompose`, noted Compose Desktop needs IDE config). Updated color description: now synced across tabs.
- **[2026-02-15 PST]** `prism-demo/src/commonMain/.../DemoScene.kt` — Updated `createDemoScene()` KDoc: "GLFW and Compose" → "GLFW, Compose, WASM, and iOS". Removed "native iOS" from `tick()` KDoc.
- **[2026-02-15 PST]** `prism-demo/src/iosMain/.../IosDemoController.kt` — Added cube color sync from `sharedDemoStore` in `DemoRenderDelegate.drawInMTKView()` so Native tab reflects Compose color picker changes.
- **[2026-02-15 PST]** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Added `renderDelegate = null` in `DisposableEffect.onDispose` to release the delegate before shutting down the scene.
- **[2026-02-15 PST]** `prism-demo/src/commonMain/.../ComposeDemoControls.kt` — Replaced `ButtonDefaults.TextButtonContentPadding` with `PaddingValues(0.dp)` on the empty color swatch buttons.
- **[2026-02-15 PST]** `devlog/000006-feat-ios-native-support.md` — Added correction note: `ios-demo/` renamed to `prism-ios-demo/`.

### Decisions
- **[2026-02-15 PST]** **Sync cube color to Native tab** — Both tabs now read `cubeColor` from `sharedDemoStore` per frame. The color picker UI remains on the Compose tab only, but changes are reflected on the Native tab immediately.

> **Correction:** Session 1 references to `ios-demo/` in "What Changed" and elsewhere now refer to `prism-ios-demo/` after the rename in this session.

---

## Session 3 — Rename Xcode project to PrismiOSDemo (2026-02-15 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/sync-ios-tab-rotation`

### Intent
Rename the `prism-ios-demo/` Xcode project from `PrismDemo`/`PrismDemoApp` to `PrismiOSDemo` to eliminate confusion with the KMP `prism-demo` module. Update bundle ID, documentation, and add README explaining the relationship between the two projects.

### What Changed
- **[2026-02-15 PST]** `prism-ios-demo/project.yml` — Renamed project `PrismDemo` → `PrismiOSDemo`, target `PrismDemoApp` → `PrismiOSDemo`, product name `PrismDemoApp` → `PrismiOSDemo`, bundle ID `com.hyeonslab.prism.demo` → `com.hyeonslab.prism.ios.demo`.
- **[2026-02-15 PST]** `prism-demo/README.md` — Updated xcodebuild scheme `PrismDemoApp` → `PrismiOSDemo`, xcodeproj `PrismDemo.xcodeproj` → `PrismiOSDemo.xcodeproj`, app name `PrismDemoApp.app` → `PrismiOSDemo.app`, bundle ID `com.hyeonslab.prism.demo` → `com.hyeonslab.prism.ios.demo`. Added note explaining the relationship between `prism-demo` (KMP module) and `prism-ios-demo` (Swift consumer app).
- **[2026-02-15 PST]** `prism-ios-demo/README.md` — New file. Documents the iOS demo app, its relationship to the KMP `prism-demo` module, build/run instructions, and project structure.
- **[2026-02-15 PST]** `IosConstants.kt` — Replaced separate `syncPause()`/`elapsed()`/`angle()` methods with a single `tick()` inline method that acquires the Mutex once per frame for atomic pause+elapsed+angle computation. Added `tickDemoFrame()` shared function that both render delegates call, deduplicating rotation, material, FPS, and ECS update logic.
- **[2026-02-15 PST]** `IosDemoController.kt` — Simplified `DemoRenderDelegate.drawInMTKView()` to call shared `tickDemoFrame()`. Added KDoc on `renderDelegate` parameter explaining why it exists (prevents GC). Removed 7 unused imports.
- **[2026-02-15 PST]** `ComposeIosEntry.kt` — Simplified `ComposeRenderDelegate.drawInMTKView()` to call shared `tickDemoFrame()`. Removed unnecessary `remember { sharedDemoStore }` (top-level val doesn't need remember). Removed 8 unused imports.
- **[2026-02-15 PST]** `ComposeDemoControls.kt` — Added `contentDescription` accessibility labels to color swatch buttons.
- **[2026-02-15 PST]** `prism-ios-demo/project.yml` — Fixed `bundleIdPrefix` from `com.hyeonslab.prism` to `com.hyeonslab.prism.ios` to match the explicit `PRODUCT_BUNDLE_IDENTIFIER`.

### Decisions
- **[2026-02-15 PST]** **`PrismiOSDemo` as new name** — Clear disambiguation: `PrismDemo` is the KMP XCFramework module name (used by `import PrismDemo` in Swift), while `PrismiOSDemo` is the iOS app project/target/scheme name.
- **[2026-02-15 PST]** **New bundle ID `com.hyeonslab.prism.ios.demo`** — Adds `.ios` segment to distinguish from any future demo apps on other platforms.
- **[2026-02-15 PST]** **Swift `import PrismDemo` unchanged** — This imports the KMP XCFramework module, not the app. No Swift source changes needed.
- **[2026-02-15 PST]** **Mutex with single `tick()` over separate methods** — `drawInMTKView` is a synchronous ObjC callback — cannot use `suspend`. Replaced separate `syncPause()`/`elapsed()`/`angle()` (each acquiring the lock independently, allowing inconsistent reads between calls) with a single `tick()` that acquires once and computes all values atomically. Cached fields for contention fallback don't use `@Volatile` — at 60fps a single frame of staleness is imperceptible.
- **[2026-02-15 PST]** **Shared `tickDemoFrame()` function** — Eliminated code duplication between `DemoRenderDelegate` and `ComposeRenderDelegate`. Both delegates now call `tickDemoFrame()` which handles rotation, material color, FPS dispatch, and ECS update. This also fixes the Native tab not dispatching FPS updates.
- **[2026-02-15 PST]** **Generic simulator destination** — Replaced `iPhone 16 Pro` with `generic/platform=iOS Simulator` in both READMEs for broader Xcode version compatibility.

### Commits
- `e051248` — refactor: rename iOS Xcode project to PrismiOSDemo and clarify demo roles
- `066f48a` — fix: address PR review — thread safety, material alloc, accessibility
- `93aff45` — docs: update devlog with mutex decision rationale
- `7d93d93` — fix: skip action on contended mutex, use generic simulator destination

---

## Session 4 — Improve CI workflow for Apple builds (2026-02-15 23:08 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/ci-apple-improvements`

### Intent
Harden the Apple CI pipeline with 6 improvements: deduplicate wgpu4k build logic into a composite action, pin macOS runner, add macOS native tests, verify both XCFramework slices, upload artifact, and add xcodegen+xcodebuild verification.

**[2026-02-15 23:08 PST]** Created implementation plan → [devlog/plans/000008-01-ci-improvements.md](plans/000008-01-ci-improvements.md)

### What Changed
- **[2026-02-15 23:08 PST]** `.github/actions/setup-wgpu4k/action.yml` — New composite action encapsulating 6 wgpu4k build steps (version extraction, Maven cache, Rust toolchain, Rust cache, build from source, cache diagnostics). Accepts optional `rust-targets` input for iOS cross-compilation. Outputs `version`, `commit`, `cache-hit`.
- **[2026-02-15 23:08 PST]** `.github/workflows/ci.yml` — 6 improvements:
  1. Replaced 6 wgpu4k steps in `ci` job (lines 43–119) with single `uses: ./.github/actions/setup-wgpu4k`
  2. Replaced 6 wgpu4k steps in `apple` job (lines 194–254) with composite action + `rust-targets` input
  3. Pinned macOS runner from `macos-latest` to `macos-15`
  4. Added `macosArm64Test` to test step, renamed "iOS tests" → "Apple native tests"
  5. Replaced single-header check with loop over both `ios-arm64` and `ios-arm64-simulator` slices with summary table
  6. Added `Upload XCFramework` step (actions/upload-artifact@v4, 14-day retention)
  7. Added `Generate Xcode project` step (brew install xcodegen + xcodegen generate)
  8. Added `Build iOS app` step (xcodebuild with iphonesimulator SDK, CODE_SIGNING_ALLOWED=NO, `set -o pipefail`)
- **[2026-02-15 23:08 PST]** `prism-assets/src/nativeMain/.../FileReader.native.kt` — New `actual FileReader` using `kotlinx.io.files.SystemFileSystem` with proper resource cleanup (`use {}`). Replaces iosMain TODO stub, covers all native targets.
- **[2026-02-15 23:08 PST]** `prism-assets/src/iosMain/.../FileReader.ios.kt` — Deleted (superseded by nativeMain).

### Decisions
- **[2026-02-15 23:08 PST]** **Composite action over reusable workflow** — Composite actions can be referenced with `uses: ./.github/actions/...` in the same repo without needing `workflow_call` triggers. Simpler for deduplication within a single workflow file.
- **[2026-02-15 23:08 PST]** **No `timeout-minutes` in composite steps** — GitHub Actions doesn't support `timeout-minutes` on composite action steps. Job-level timeouts (30min ci, 45min apple) provide the safety net.
- **[2026-02-15 23:08 PST]** **Pin to `macos-15`** — `macos-latest` can shift between versions without notice. Pinning prevents unexpected Xcode/SDK changes from breaking Metal-dependent builds.
- **[2026-02-15 23:30 PST]** **nativeMain FileReader using kotlinx.io** — Replaced iosMain TODO stub with a proper `nativeMain` actual using `kotlinx.io.files.SystemFileSystem`. Covers all native targets (iOS, macOS, Linux, Windows). Filed #22 as tracking issue.

### Issues
- **macosArm64Test compilation failure** — Adding `macosArm64Test` exposed a pre-existing gap: `prism-assets` declares `macosArm64()` but only had an `actual FileReader` in `iosMain`. Fixed by creating `nativeMain/FileReader.native.kt` using `kotlinx.io.files.SystemFileSystem`, which covers all native targets. Deleted the iosMain TODO stub. Filed #22 for tracking.
- **xcodebuild exit code swallowed by `tail`** — `xcodebuild ... 2>&1 | tail -30 || EXIT=$?` captured `tail`'s exit code (always 0), not `xcodebuild`'s. A failing build would silently pass CI. Fixed by adding `set -o pipefail`.
- **FileReader resource leak** — Initial `nativeMain` implementation didn't close the `RawSource` from `SystemFileSystem.source()`. Fixed by wrapping in `.use { }`.

### Lessons Learned
- `set -o pipefail` is essential when piping a command through `tail`/`head` in CI — without it, only the last command's exit code is captured.
- `kotlinx.io.files.SystemFileSystem` works across all Kotlin/Native targets, making it a good default for `nativeMain` file access instead of per-platform stubs.

### Commits
- `39907d3` — chore: harden Apple CI with composite action and xcodebuild verification
