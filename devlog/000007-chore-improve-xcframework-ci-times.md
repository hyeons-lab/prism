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
- (pending)
