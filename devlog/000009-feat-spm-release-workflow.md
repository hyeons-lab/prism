# 000009-feat-spm-release-workflow

## Session 1 — SPM Support + GitHub Release Workflow (2026-02-16 00:20 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/spm-release-workflow`

### Intent
Enable iOS consumers to integrate Prism as a library via Swift Package Manager. The existing `PrismDemo.xcframework` bundles demo code with engine code and is only available as a 14-day CI artifact. This creates a proper `Prism.xcframework` via a new `prism-ios` Gradle module, adds SPM binary target support, and a GitHub Actions release workflow so iOS projects can add Prism with a single SPM dependency pointing to tagged GitHub releases.

### What Changed

- **[2026-02-16 00:25 PST]** `prism-ios/build.gradle.kts` — New KMP module that aggregates all engine libraries (prism-math, prism-core, prism-renderer, prism-scene, prism-ecs, prism-input, prism-assets, prism-audio, prism-compose) into a single `Prism.xcframework` via `export()`. iOS-only (iosArm64 + iosSimulatorArm64), static framework.
- **[2026-02-16 00:26 PST]** `prism-ios/src/iosMain/kotlin/engine/prism/Prism.kt` — Minimal source file required for Kotlin/Native to generate a framework binary. Without at least one source file, all compile/link tasks resolve as NO-SOURCE.
- **[2026-02-16 00:25 PST]** `Package.swift` — SPM package manifest with binary target pointing to GitHub releases. URL and checksum are placeholders updated by the release workflow.
- **[2026-02-16 00:26 PST]** `.github/workflows/release.yml` — Release workflow triggered on `v*` tag push. Builds release XCFramework, zips it, computes SHA-256 checksum, creates GitHub Release, and updates Package.swift on main with the new URL and checksum.
- **[2026-02-16 00:27 PST]** `settings.gradle.kts` — Added `include(":prism-ios")`.
- **[2026-02-16 00:28 PST]** `.github/workflows/ci.yml` — Updated apple job to also build `assemblePrismDebugXCFramework` alongside existing `assemblePrismDemoDebugXCFramework`, with verification of both frameworks and separate artifact uploads.
- **[2026-02-16 00:29 PST]** `README.md` — Added "iOS (Swift Package Manager)" section under Quick Start with Xcode UI, Package.swift, and Swift import instructions.
- **[2026-02-16 00:30 PST]** `AGENTS.md` — Added `prism-ios` to module list and release/XCFramework build commands to Build Commands section.

### Decisions

- **[2026-02-16 00:25 PST]** **iOS-only module, no JVM/WASM targets** — prism-ios exists purely as an XCFramework aggregator. Adding desktop/web targets would be pointless overhead.
- **[2026-02-16 00:25 PST]** **Static framework** — Simpler deployment for consumers, no need to embed dynamic libraries.
- **[2026-02-16 00:26 PST]** **Minimal source file needed** — Kotlin/Native requires at least one source file to produce a framework binary. Without it, compile/link tasks resolve as NO-SOURCE and the XCFramework assembly is skipped.
- **[2026-02-16 00:26 PST]** **Direct commit to main for Package.swift** — SPM reads Package.swift from the default branch. The release workflow pushes directly; if branch protection blocks it, the commit will fail gracefully.
- **[2026-02-16 00:28 PST]** **Separate artifact uploads in CI** — Split single PrismDemo upload into two (Prism + PrismDemo) for clarity.

### Issues

- **NO-SOURCE on initial build** — First attempt with an empty module (only commonMain/kotlin directory, no source files) resulted in all Kotlin/Native tasks being NO-SOURCE. Fixed by adding `Prism.kt` with a version object in `iosMain`.

### Lessons Learned

- Kotlin/Native framework linking requires at least one source file in the target's source set. A module with only `api()`/`export()` dependencies but no source produces NO-SOURCE for compile/link/assemble tasks.

### Commits
- (pending)

---

## Next Steps
- Tag `v0.1.0-rc.1` to test the full release workflow end-to-end
- Verify GitHub Release has zip attached and Package.swift on main is updated
- Test adding the package in a fresh Xcode project with `import Prism`
