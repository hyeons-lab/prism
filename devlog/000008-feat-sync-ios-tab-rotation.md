# 000008-feat-sync-ios-tab-rotation

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/sync-ios-tab-rotation`
**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/ci-apple-improvements` (Session 4)

## Intent

Fix multiple iOS demo bugs (module name collision, Compose tab crash, MTKView delegate GC), sync rotation and color between Native and Compose tabs, rename `ios-demo/` to `prism-ios-demo/`, rename Xcode project to PrismiOSDemo, and harden Apple CI with composite action and xcodebuild verification.

## What Changed

- **2026-02-15T20:30-08:00** `ios-demo/project.yml` — Renamed target `PrismDemo` -> `PrismDemoApp` to fix module name collision. Switched XCFramework from release to debug. Later renamed to `PrismiOSDemo` with new bundle ID `com.hyeonslab.prism.ios.demo`.
- **2026-02-15T20:30-08:00** `ios-demo/Sources/Info.plist` — Added `CADisableMinimumFrameDurationOnPhone = true` for ProMotion-capable iPhones (Compose requirement).
- **2026-02-15T20:45-08:00** `prism-demo/src/iosMain/.../IosConstants.kt` — `sharedDemoStore: DemoStore` shared by both tabs. `SharedDemoTime` object with pause-aware elapsed time, angle computation with smooth speed-change offset. Later replaced separate methods with single `tick()` acquiring Mutex once. Added `tickDemoFrame()` shared by both render delegates.
- **2026-02-15T20:45-08:00** `prism-demo/src/iosMain/.../IosDemoController.kt` — Fixed MTKView delegate GC: `renderDelegate` parameter in `IosDemoHandle` as strong reference. Reads pause/speed/color from `sharedDemoStore`. Simplified to call `tickDemoFrame()`.
- **2026-02-15T20:45-08:00** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Fixed delegate GC: `renderDelegate` in `remember` state. Uses `sharedDemoStore`. Absolute angle via `SharedDemoTime`. `WindowInsets.safeDrawing` padding. Simplified to call `tickDemoFrame()`.
- **2026-02-15T20:50-08:00** `prism-demo/src/commonMain/.../ComposeDemoControls.kt` — Circular color swatches (32dp) replacing text-labeled buttons. Added `contentDescription` accessibility labels.
- **2026-02-15T20:50-08:00** `README.md` — Updated iOS platform status to "Working", M7 to "Done".
- **2026-02-15T20:50-08:00** `prism-demo/README.md` — New file documenting all demo platforms, build/run instructions.
- **2026-02-15T21:00-08:00** `ios-demo/` -> `prism-ios-demo/` — Renamed directory via `git mv`.
- **2026-02-15T21:00-08:00** `.gitignore`, `AGENTS.md`, `BUILD_STATUS.md`, `prism-demo/README.md` — Updated all `ios-demo` -> `prism-ios-demo` references.
- **2026-02-15T21:00-08:00** `prism-ios-demo/README.md` — New file documenting iOS demo app and its relationship to KMP `prism-demo`.
- **2026-02-15T23:08-08:00** Created plan -> [devlog/plans/000008-01-ci-improvements.md](plans/000008-01-ci-improvements.md)
- **2026-02-15T23:08-08:00** `.github/actions/setup-wgpu4k/action.yml` — New composite action for wgpu4k build (6 steps). Accepts `rust-targets` input for iOS cross-compilation.
- **2026-02-15T23:08-08:00** `.github/workflows/ci.yml` — Replaced wgpu4k steps in both jobs with composite action. Pinned `macos-15`. Added `macosArm64Test`. XCFramework slice verification loop. Upload artifact. xcodegen + xcodebuild verification with `set -o pipefail`.
- **2026-02-15T23:08-08:00** `prism-assets/src/nativeMain/.../FileReader.native.kt` — New `actual FileReader` using `kotlinx.io.files.SystemFileSystem` with `.use {}`. Replaces iosMain TODO stub, covers all native targets.
- **2026-02-15T23:08-08:00** `prism-assets/src/iosMain/.../FileReader.ios.kt` — Deleted (superseded by nativeMain).

## Decisions

- **2026-02-15T20:30-08:00** **Rename Xcode target to PrismDemoApp** — Kotlin XCFramework module name `PrismDemo` cannot be easily changed; rename Swift target instead.
- **2026-02-15T20:30-08:00** **Strong reference for MTKView delegates** — MTKView.delegate is WEAK in UIKit. K/N GC collected delegates after seconds.
- **2026-02-15T20:45-08:00** **Shared DemoStore via top-level val** — Both tabs read from same store. Simpler than separate stores with sync.
- **2026-02-15T20:45-08:00** **Absolute angle instead of incremental** — Eliminates floating-point drift between tabs.
- **2026-02-15T21:00-08:00** **Mutex with single `tick()` over separate methods** — Atomic computation in synchronous ObjC callback (cannot use suspend).
- **2026-02-15T21:00-08:00** **Shared `tickDemoFrame()`** — Eliminates duplication between both render delegates.
- **2026-02-15T23:08-08:00** **Composite action over reusable workflow** — `uses: ./.github/actions/...` simpler for same-repo dedup.
- **2026-02-15T23:08-08:00** **Pin to `macos-15`** — Prevents unexpected Xcode/SDK changes.
- **2026-02-15T23:08-08:00** **nativeMain FileReader using kotlinx.io** — Covers all native targets, replacing iosMain TODO stub.

## Issues

- **"Cannot find type 'IosDemoHandle' in scope"** — Module name collision: Xcode target and KMP framework both named `PrismDemo`. Swift ignored `import PrismDemo` as self-import. Fixed by renaming target.
- **Compose tab crash on ProMotion iPhones** — Missing `CADisableMinimumFrameDurationOnPhone`. Required by Compose Multiplatform for 120Hz.
- **Rotation stops after seconds** — MTKView.delegate is WEAK; K/N GC collected delegates. Fixed with strong references.
- **macosArm64Test compilation failure** — `prism-assets` had `actual FileReader` only in `iosMain`. Fixed with `nativeMain` using kotlinx.io.
- **xcodebuild exit code swallowed by `tail`** — Fixed with `set -o pipefail`.
- **FileReader resource leak** — Initial nativeMain impl didn't close `RawSource`. Fixed with `.use {}`.

## Lessons Learned

- MTKView.delegate is WEAK — K/N objects need explicit strong reference.
- Swift module name collision is silent — `import X` ignored when app module is also `X`.
- `CADisableMinimumFrameDurationOnPhone` required by Compose Multiplatform iOS on ProMotion devices.
- `set -o pipefail` essential when piping through `tail`/`head` in CI.
- `kotlinx.io.files.SystemFileSystem` works across all K/N targets for `nativeMain` file access.

## Commits

- `952ac1e` — feat: sync iOS tab rotation, fix delegate GC, and polish demo UI
- `e051248` — refactor: rename iOS Xcode project to PrismiOSDemo and clarify demo roles
- `066f48a` — fix: address PR review — thread safety, material alloc, accessibility
- `93aff45` — docs: update devlog with mutex decision rationale
- `7d93d93` — fix: skip action on contended mutex, use generic simulator destination
- `39907d3` — chore: harden Apple CI with composite action and xcodebuild verification
