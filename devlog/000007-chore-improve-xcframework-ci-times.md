# 000007-chore-improve-xcframework-ci-times

## Session 1 — Improve Apple CI build times (2026-02-15 18:58 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/improve-xcframework-ci-times`

### Intent
Reduce the `apple` CI job build time, which has a 45-minute timeout. The XCFramework builds 11 modules x 2 iOS targets with release linking being the bottleneck (~10x slower than debug due to LLVM optimization passes). Three optimizations: cache ~/.konan, switch to debug XCFramework, and combine Gradle invocations.

### What Changed
- **[2026-02-15 18:58 PST]** `.github/workflows/ci.yml` — Four changes to the `apple` job:
  1. Added `Get Kotlin version` step + `actions/cache@v4` for `~/.konan/` (K/N compiler + LLVM toolchains + target sysroots, ~7 GB). Keyed on the extracted Kotlin version (not the full `libs.versions.toml` hash) so it only invalidates when the Kotlin compiler version changes.
  2. Combined separate `iosSimulatorArm64Test` and `assemblePrismDemoXCFramework` steps into a single Gradle invocation (`iOS tests + XCFramework`). This reuses klib compilation outputs and avoids a second JVM warmup + configuration phase.
  3. Switched from `assemblePrismDemoXCFramework` (release) to `assemblePrismDemoDebugXCFramework`. CI only verifies the XCFramework assembles — it doesn't ship it. Debug skips LLVM optimization passes and LTO.
  4. Updated the `Verify XCFramework exports` step path from `release/` to `debug/`.
- **[2026-02-15 19:02 PST]** `gradle.properties` — Added `kotlin.incremental.native=true` to enable incremental K/N klib compilation for local development builds.

### Decisions
- **[2026-02-15 18:58 PST]** **Use debug XCFramework in CI** — CI builds the XCFramework to verify it assembles, not for distribution. Release linking is the single slowest phase. A release build can be added as a separate on-demand workflow if needed.
- **[2026-02-15 19:02 PST]** **Key konan cache on Kotlin version, not libs.versions.toml hash** — The `~/.konan/` directory only contains the K/N compiler and platform sysroots. It only changes when the Kotlin version changes, not when unrelated deps (kotest, kermit, AGP, etc.) are bumped. Using `hashFiles('gradle/libs.versions.toml')` would cause unnecessary cache misses.
- **[2026-02-15 19:02 PST]** **Enable `kotlin.incremental.native=true`** — Experimental incremental K/N klib compilation. Helps local iterative builds (not clean CI). Added uncommented so all developers get the benefit by default.

### Issues
- **konan cache key was too broad** — Initial implementation used `hashFiles('gradle/libs.versions.toml')` which invalidates on any version catalog change. Fixed to extract the Kotlin version via grep and use that as the key.
- **No `--continue` flag on combined Gradle invocation** — If `iosSimulatorArm64Test` fails, Gradle won't execute the XCFramework task. This matches the original behavior (separate steps, no `if: always()` on the second step), so it's consistent. A test failure masking a potential XCFramework build failure is acceptable for CI.

### Research & Discoveries
- `assemblePrismDemoDebugXCFramework` task confirmed to exist via `./gradlew :prism-demo:tasks --all`
- ccache is not viable for K/N — it bundles its own LLVM/Clang without CC/CXX hooks
- K/N tasks aren't `@CacheableTask` so Gradle remote build cache doesn't help (KT-39564)
- XCFramework tasks aren't configuration-cache compatible (KT-43293)
- `grep '^kotlin '` correctly extracts `2.3.0` from `libs.versions.toml` (tested locally)

### Commits
- `16cb14a` — chore: improve Apple CI build times for XCFramework compilation

---

## Session 2 — Address PR feedback and upgrade Compose (2026-02-15 22:00 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/improve-xcframework-ci-times`

### Intent
Address Copilot review comments on PR #19 and fix Gradle deprecation warnings for Compose Multiplatform dependency accessors.

### What Changed
- **[2026-02-15 22:00 PST]** `.github/workflows/ci.yml` — Added empty-check + `exit 1` for the Kotlin version extraction step (mirrors the wgpu4k version step pattern). Removed broad `restore-keys` from the konan cache to prevent accumulating old Kotlin/Native toolchains when the Kotlin version bumps.
- **[2026-02-15 22:15 PST]** `gradle/libs.versions.toml` — Upgraded Compose Multiplatform from 1.10.0 to 1.10.1. Added direct artifact entries for `compose-runtime`, `compose-foundation`, `compose-ui` (at 1.10.1) and `compose-material3` (at 1.10.0-alpha05, per the 1.10.1 release's version mapping).
- **[2026-02-15 22:15 PST]** `prism-compose/build.gradle.kts`, `prism-demo/build.gradle.kts` — Replaced deprecated `compose.runtime`, `compose.foundation`, `compose.ui`, `compose.material3` Gradle plugin accessors with version catalog references (`libs.compose.*`).

### Decisions
- **[2026-02-15 22:15 PST]** **Use direct artifact coordinates, not BOM** — No Compose Multiplatform BOM exists yet (planned by JetBrains but not released). Direct coordinates with explicit versions in the catalog are the recommended approach for 1.10.x.
- **[2026-02-15 22:15 PST]** **material3 version differs from other Compose libs** — Per the [1.10.1 release notes](https://github.com/JetBrains/compose-multiplatform/releases/tag/v1.10.1), material3 is at `1.10.0-alpha05` while runtime/foundation/ui are at `1.10.1`. This is expected — material3 follows its own versioning scheme.

### Issues
- **material3:1.10.0 doesn't exist on Maven Central** — Initial attempt to use `composeMultiplatform` version ref for all Compose artifacts failed because material3 uses a different version. Discovered via build failure. Checked the 1.10.1 release page to find the correct version mapping.
- **Compose Multiplatform BOM not available** — Investigated adding a BOM per user request. Web search and Maven Central/JetBrains repos confirmed no BOM exists yet. The `compose.*` plugin accessors handle version mapping internally, but are deprecated. Direct coordinates are the migration path.

### Research & Discoveries
- Compose Multiplatform 1.10.1 version mapping (from release notes): runtime/ui/foundation/material at 1.10.1, material3 at 1.10.0-alpha05
- `org.jetbrains.compose.material3:material3` is NOT published at the same version as the plugin — it has its own release cadence
- JetBrains plans to provide a Compose Multiplatform BOM in the future but hasn't released one yet

### Commits
- `699d7c5` — fix: address PR review feedback on Apple CI job
- `b8783ad` — fix: address PR feedback and upgrade Compose Multiplatform to 1.10.1
