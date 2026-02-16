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

---

## Session 3 — Fix K/N thread warning on macOS CI (2026-02-15 PST, claude-opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/improve-xcframework-ci-times`

### Intent
Fix Kotlin/Native compiler warning "The number of threads 4 is more than the number of processors 3" appearing during XCFramework build on macOS CI runner.

### What Changed
- **[2026-02-15 PST]** `.github/workflows/ci.yml` — Added `-Pkotlin.native.parallelThreads=3` to the apple job's Gradle invocation to match the macOS-latest runner's 3 vCPUs. Also added `@OptIn(ExperimentalKotlinGradlePluginApi)` to suppress `mainRun` experimental API warning.
- **[2026-02-15 PST]** `prism-demo/build.gradle.kts` — Added `@OptIn(ExperimentalKotlinGradlePluginApi::class)` on the `jvm { mainRun { ... } }` block.

### Decisions
- **[2026-02-15 PST]** **Use `-Pkotlin.native.parallelThreads=3` in apple CI job** — macOS-latest runners have 3 vCPUs (M1). The KGP defaults `kotlin.native.parallelThreads` to 4 (hardcoded in `nativeCacheKindProperties.kt`), which is passed as `-Xbackend-threads=4` to the K/N compiler. On a 3-vCPU runner this triggers the warning. Setting to 3 matches the runner's actual vCPU count. Initial attempt with `--max-workers=3` didn't work because that controls Gradle parallelism, not the K/N compiler's internal thread pool.

### Issues
- **`--max-workers=3` did NOT fix the K/N thread warning** — `--max-workers` controls Gradle worker count, not the K/N compiler's `-Xbackend-threads` flag. The KGP sets `-Xbackend-threads` from `kotlin.native.parallelThreads` (default 4), completely independent of Gradle workers. Had to trace through `KotlinNativeLink.kt` → `getKonanParallelThreads()` → `PropertiesProvider.nativeParallelThreads` → `kotlin.native.parallelThreads` to find the correct property.

### Research & Discoveries
- GitHub-hosted macOS runners (macos-latest = macOS 15 arm64): 3 vCPUs, 7 GB RAM, 14 GB SSD
- GitHub-hosted ubuntu-latest: 4 vCPUs, 16 GB RAM, 14 GB SSD
- **`kotlin.native.parallelThreads`** is the Gradle property that controls K/N backend threads (undocumented in official Kotlin docs)
- KGP source chain: `PropertiesProvider.nativeParallelThreads` → `getKonanParallelThreads()` → defaults to 4 → passed as `-Xbackend-threads=N` to K/N compiler
- The K/N compiler's `SetupConfiguration.kt` checks `nThreads > availableProcessors` and warns if exceeded
- KT-70915 tracks the issue of K/N link tasks overloading machines when config cache enables parallel linking
- `mainRun` API in KGP requires `@OptIn(ExperimentalKotlinGradlePluginApi::class)`
