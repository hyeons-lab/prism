# 000009-feat-spm-release-workflow

## Session 1 — SPM Support + GitHub Release Workflow (2026-02-16 00:20 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/spm-release-workflow`

### Intent
Enable iOS consumers to integrate Prism as a library via Swift Package Manager. The existing `PrismDemo.xcframework` bundles demo code with engine code and is only available as a 14-day CI artifact. This creates a proper `Prism.xcframework` via a new `prism-ios` Gradle module, adds SPM binary target support, and a GitHub Actions release workflow so iOS projects can add Prism with a single SPM dependency pointing to tagged GitHub releases.

### What Changed

- **[2026-02-16 00:25 PST]** `prism-ios/build.gradle.kts` — New KMP module that aggregates all engine libraries (prism-math, prism-core, prism-renderer, prism-scene, prism-ecs, prism-input, prism-assets, prism-audio, prism-compose) into a single `Prism.xcframework` via `export()`. iOS-only (iosArm64 + iosSimulatorArm64), static framework.
- **[2026-02-16 00:26 PST]** `prism-ios/src/iosMain/kotlin/com/hyeonslab/prism/Prism.kt` — Minimal source file required for Kotlin/Native to generate a framework binary. Without at least one source file, all compile/link tasks resolve as NO-SOURCE. Later replaced with KotlinPoet build-time generation (see Session 2).
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
- `447fc03` — feat: add SPM support with prism-ios XCFramework and release workflow

---

## Session 2 — Review Fixes (2026-02-16 01:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/spm-release-workflow`

### Intent
Address PR review feedback from Copilot reviewer (6 inline comments on PR #23).

### What Changed
- **[2026-02-16 01:05 PST]** `prism-ios/build.gradle.kts` — Replaced hand-maintained Prism.kt with KotlinPoet build-time generation sourcing VERSION from gradle.properties `VERSION_NAME`. Added `prism-native-widgets` to export/api lists.
- **[2026-02-16 01:10 PST]** `.github/workflows/release.yml` — Reordered: update Package.swift and retag *before* creating the GitHub Release, so SPM version resolution reads correct URL/checksum from the tag. Moved tag to updated commit via `git tag -f`.
- **[2026-02-16 01:10 PST]** `.github/workflows/ci.yml` — Renamed "Verify XCFramework exports" to "Verify XCFramework headers" (step only checks headers, not exported types).
- **[2026-02-16 01:10 PST]** `devlog/000009-feat-spm-release-workflow.md` — Fixed wrong file path for Prism.kt, added session 2.

### Decisions
- **[2026-02-16 01:05 PST]** **Branch-based SPM resolution (reverted in Session 3)** — SPM resolves Package.swift from the tag, not main. Since the checksum can't be known before building, and force-pushing tags is bad practice, we initially used `branch: "main"` for SPM consumers. This was later corrected — see Session 3.

### Commits
- `46b5896` — refactor: generate Prism.kt with KotlinPoet from gradle.properties

---

## Session 3 — Fix release workflow for correct SPM version resolution (2026-02-16 02:32 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/spm-release-workflow`

### Intent
Fix the fundamental flaw in the release workflow: SPM resolves `Package.swift` from the **tagged commit**, not from `main`. The v* tag-push trigger creates a chicken-and-egg problem — the tag points to a commit with placeholder URL/checksum, so version-based SPM resolution (`from: "0.1.0"`) always fails. The fix: use `workflow_dispatch` so the workflow itself creates the tag **after** updating Package.swift. Plan: [devlog/plans/000009-02-fix-release-workflow.md](plans/000009-02-fix-release-workflow.md).

### What Changed
- **[2026-02-16 02:32 PST]** `.github/workflows/release.yml` — Complete rewrite: changed trigger from `on: push: tags: ["v*"]` to `on: workflow_dispatch` with `version` input. Reordered steps: build XCFramework → zip + checksum → update Package.swift → commit → create tag → push tag + commit → create GitHub Release. Added version validation (semver format, no duplicate tags). Checkout now targets `main` explicitly (no detached tag HEAD).
- **[2026-02-16 02:33 PST]** `README.md` — Changed SPM instructions from `branch: "main"` to `from: "0.1.0"` since version-based resolution now works correctly. Updated note to link to Releases page.
- **[2026-02-16 02:33 PST]** `AGENTS.md` — Changed release command from `git tag v0.1.0 && git push --tags` to `gh workflow run release.yml -f version=0.1.0`.
- **[2026-02-16 02:34 PST]** `.github/workflows/ci.yml` — No additional changes needed (step rename to "Verify XCFramework headers" was already done in Session 2).
- **[2026-02-16 02:40 PST]** `prism-ios/README.md` — Updated Distribution section from old tag-push description to `workflow_dispatch` approach.

### Decisions
- **[2026-02-16 02:32 PST]** **workflow_dispatch over tag-push** — The workflow must create the tag, not react to it. `workflow_dispatch` with a version input lets us: build → update Package.swift → commit → tag the updated commit → push → release. The tag now points to a commit where Package.swift has the correct URL and checksum.
- **[2026-02-16 02:32 PST]** **Version validation** — Added semver regex check and duplicate tag detection as a guard rail since `workflow_dispatch` accepts freeform input.
- **[2026-02-16 02:32 PST]** **Fail if Package.swift unchanged** — Changed `exit 0` (silent skip) to `exit 1` (fail) when Package.swift has no changes, since that indicates a bug in the sed patterns.

### Issues
- **Session 2 branch-based workaround was wrong** — The Session 2 decision to use `branch: "main"` was a workaround for the wrong problem. The real fix is making the tag point to the right commit, not avoiding version-based resolution. With `workflow_dispatch`, the tag is created after Package.swift is updated, so `from: "0.1.0"` resolves correctly.

### Lessons Learned
- SPM binary target resolution reads Package.swift from the **tagged commit** for version-based deps. The tag must point to a commit where Package.swift already has the correct URL/checksum. Any workflow that creates the tag before knowing the checksum is fundamentally broken for version-based SPM.

### Commits
- `15075f5` — fix: use workflow_dispatch for release to fix SPM version resolution

---

## Session 4 — Simplify devlog convention (2026-02-16 08:40 PST, claude-opus-4-6)

### Intent
Reduce devlog instructions in AGENTS.md from ~145 inline lines to a concise ~40-line dedicated file.

### What Changed
- **[2026-02-16 08:40 PST]** `DEVLOG.md` — New file with concise devlog convention (~40 lines): formats, per-session sections, rules
- **[2026-02-16 08:40 PST]** `AGENTS.md` — Replaced ~145-line inline devlog section with 1-line reference to DEVLOG.md
- **[2026-02-16 08:40 PST]** `devlog/README.md` — Replaced 37-line verbose README with concise quick-reference table (14 lines)

### Commits
- `1bcc6a5` — docs: extract devlog convention into DEVLOG.md and simplify

---

## Next Steps
- Trigger `gh workflow run release.yml -f version=0.1.0-rc.1` to test end-to-end
- Verify: tag `v0.1.0-rc.1` points to a commit where Package.swift has correct URL/checksum
- `swift package resolve` in a test project with `from: "0.1.0-rc.1"` resolves correctly
