# 000009-feat-spm-release-workflow

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/spm-release-workflow`

## Intent

Enable iOS consumers to integrate Prism as a library via Swift Package Manager. Create a proper `Prism.xcframework` via a new `prism-ios` Gradle module, add SPM binary target support, a GitHub Actions release workflow, and simplify devlog conventions.

## What Changed

- **2026-02-16T00:25-08:00** `prism-ios/build.gradle.kts` — New KMP module that aggregates all engine libraries into a single `Prism.xcframework` via `export()`. iOS-only (iosArm64 + iosSimulatorArm64), static framework. Later updated with KotlinPoet build-time generation sourcing VERSION from `gradle.properties`, and added `prism-native-widgets` to export/api lists.
- **2026-02-16T00:25-08:00** `Package.swift` — SPM package manifest with binary target pointing to GitHub releases. URL and checksum are placeholders updated by the release workflow.
- **2026-02-16T00:26-08:00** `.github/workflows/release.yml` — Release workflow. Initially triggered on `v*` tag push, later rewritten to `workflow_dispatch` with `version` input to fix SPM version resolution. Final flow: build XCFramework → zip + checksum → update Package.swift → commit → create tag → push → create GitHub Release. Includes version validation (semver format, no duplicate tags).
- **2026-02-16T00:27-08:00** `settings.gradle.kts` — Added `include(":prism-ios")`.
- **2026-02-16T00:28-08:00** `.github/workflows/ci.yml` — Updated apple job to also build `assemblePrismDebugXCFramework` alongside existing `assemblePrismDemoDebugXCFramework`, with verification of both frameworks and separate artifact uploads. Renamed "Verify XCFramework exports" to "Verify XCFramework headers".
- **2026-02-16T00:29-08:00** `README.md` — Added "iOS (Swift Package Manager)" section. Later changed SPM instructions from `branch: "main"` to `from: "0.1.0"` since version-based resolution now works correctly.
- **2026-02-16T00:30-08:00** `AGENTS.md` — Added `prism-ios` to module list, release/XCFramework build commands, and changed release command to `gh workflow run release.yml -f version=0.1.0`.
- **2026-02-16T02:40-08:00** `prism-ios/README.md` — Updated Distribution section from old tag-push description to `workflow_dispatch` approach.
- **2026-02-16T08:40-08:00** `devlog/CONVENTIONS.md` — New file with concise devlog convention (~40 lines): formats, per-session sections, rules. Extracted from ~145-line inline section in AGENTS.md.
- **2026-02-16T08:40-08:00** `devlog/README.md` — Replaced 37-line verbose README with concise quick-reference table (14 lines).
- **2026-02-16T09:30-08:00** `devlog/000001-initial-scaffolding.md` through `devlog/000008-feat-sync-ios-tab-rotation.md` — Flattened all 8 existing devlog files to new convention: removed `## Session N` headers, converted timestamps to ISO 8601, collapsed multi-session content into single flat sections.
- **2026-02-16T09:40-08:00** `AGENTS.md` — Renamed "Session Logs" section to "Development Logs" to match flat devlog convention. Updated to reference "AI coding agents" instead of "Claude Code".
- **2026-02-16T09:50-08:00** `devlog/CONVENTIONS.md` — Generalized from Claude Code-specific to any AI coding agent: updated intro, Agent line format, and cross-references.

## Decisions

- **2026-02-16T00:25-08:00** **iOS-only module, no JVM/WASM targets** — prism-ios exists purely as an XCFramework aggregator.
- **2026-02-16T00:25-08:00** **Static framework** — Simpler deployment for consumers, no need to embed dynamic libraries.
- **2026-02-16T00:26-08:00** **Minimal source file needed** — Kotlin/Native requires at least one source file to produce a framework binary. Without it, compile/link tasks resolve as NO-SOURCE.
- **2026-02-16T00:26-08:00** **Direct commit to main for Package.swift** — SPM reads Package.swift from the default branch. The release workflow pushes directly.
- **2026-02-16T01:05-08:00** **KotlinPoet generation over hand-maintained Prism.kt** — Replaced hand-maintained version constant with build-time generation from `gradle.properties`.
- **2026-02-16T02:32-08:00** **workflow_dispatch over tag-push** — The workflow must create the tag, not react to it. `workflow_dispatch` with a version input lets us build → update Package.swift → commit → tag the updated commit → push → release. The tag now points to a commit where Package.swift has the correct URL and checksum.
- **2026-02-16T02:32-08:00** **Fail if Package.swift unchanged** — Changed `exit 0` (silent skip) to `exit 1` (fail) when Package.swift has no changes, since that indicates a bug in the sed patterns.

## Issues

- **NO-SOURCE on initial build** — First attempt with an empty module (no source files) resulted in all Kotlin/Native tasks being NO-SOURCE. Fixed by adding `Prism.kt` with a version object in `iosMain`.
- **SPM version resolution with tag-push was fundamentally broken** — SPM resolves Package.swift from the **tagged commit**, not from `main`. The v* tag-push trigger creates a chicken-and-egg problem — the tag points to a commit with placeholder URL/checksum. Initially worked around with `branch: "main"`, then properly fixed by switching to `workflow_dispatch` so the tag is created after Package.swift is updated. Plan: [devlog/plans/000009-02-fix-release-workflow.md](plans/000009-02-fix-release-workflow.md).

## Lessons Learned

- Kotlin/Native framework linking requires at least one source file in the target's source set. A module with only `api()`/`export()` dependencies but no source produces NO-SOURCE for compile/link/assemble tasks.
- SPM binary target resolution reads Package.swift from the **tagged commit** for version-based deps. Any workflow that creates the tag before knowing the checksum is fundamentally broken for version-based SPM.

## Commits

- `447fc03` — feat: add SPM support with prism-ios XCFramework and release workflow
- `46b5896` — refactor: generate Prism.kt with KotlinPoet from gradle.properties
- `15075f5` — fix: use workflow_dispatch for release to fix SPM version resolution
- `1bcc6a5` — docs: extract devlog convention into DEVLOG.md and simplify
- `40174de` — docs: update devlog with session 4 and fill pending commit hash
- `df06df0` — docs: flatten devlog format, move conventions to devlog/CONVENTIONS.md, use ISO 8601
- `1bde5f6` — docs: flatten devlog files 000001-000008 to new convention

## Next Steps

- Trigger `gh workflow run release.yml -f version=0.1.0-rc.1` to test end-to-end
- Verify: tag `v0.1.0-rc.1` points to a commit where Package.swift has correct URL/checksum
- `swift package resolve` in a test project with `from: "0.1.0-rc.1"` resolves correctly
