# 000007-chore-improve-xcframework-ci-times

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/improve-xcframework-ci-times`

## Intent

Reduce Apple CI job build time (45-minute timeout). Optimize with: cache ~/.konan, switch to debug XCFramework, combine Gradle invocations, address PR feedback, upgrade Compose Multiplatform to 1.10.1, and fix K/N thread warning.

## What Changed

- **2026-02-15T18:58-08:00** `.github/workflows/ci.yml` — Added `~/.konan/` cache keyed on Kotlin version (not full `libs.versions.toml`). Combined `iosSimulatorArm64Test` + XCFramework into single Gradle invocation. Switched from release to debug XCFramework. Later: added empty-check + `exit 1` for Kotlin version extraction, removed broad `restore-keys`, added `-Pkotlin.native.parallelThreads=3` for macOS 3-vCPU runner.
- **2026-02-15T19:02-08:00** `gradle.properties` — Added `kotlin.incremental.native=true`.
- **2026-02-15T22:15-08:00** `gradle/libs.versions.toml` — Upgraded Compose Multiplatform 1.10.0 -> 1.10.1. Added direct artifact entries for compose-runtime/foundation/ui (1.10.1) and compose-material3 (1.10.0-alpha05).
- **2026-02-15T22:15-08:00** `prism-compose/build.gradle.kts`, `prism-demo/build.gradle.kts` — Replaced deprecated `compose.*` Gradle plugin accessors with version catalog references (`libs.compose.*`).
- **2026-02-15T22:15-08:00** `prism-demo/build.gradle.kts` — Added `@OptIn(ExperimentalKotlinGradlePluginApi::class)` on `jvm { mainRun { } }` block.

## Decisions

- **2026-02-15T18:58-08:00** **Debug XCFramework in CI** — CI verifies assembly, not distribution. Release linking is the slowest phase.
- **2026-02-15T19:02-08:00** **Key konan cache on Kotlin version** — `~/.konan/` only changes with Kotlin version changes, not unrelated dep bumps.
- **2026-02-15T22:15-08:00** **Use direct artifact coordinates, not BOM** — No Compose Multiplatform BOM exists yet. Direct coordinates in version catalog.
- **2026-02-15T22:15-08:00** **material3 version differs from other Compose libs** — Per 1.10.1 release notes, material3 is at 1.10.0-alpha05 while runtime/foundation/ui are at 1.10.1.
- **2026-02-15T22:30-08:00** **`-Pkotlin.native.parallelThreads=3` for apple CI** — macOS-latest runners have 3 vCPUs; KGP defaults to 4, triggering a compiler warning.

## Research & Discoveries

- `assemblePrismDemoDebugXCFramework` exists as a Gradle task.
- ccache not viable for K/N — it bundles its own LLVM without CC/CXX hooks.
- K/N tasks aren't `@CacheableTask` (KT-39564); XCFramework tasks aren't config-cache compatible (KT-43293).
- GitHub macOS-latest = macOS 15 arm64: 3 vCPUs, 7 GB RAM.
- `kotlin.native.parallelThreads` is the Gradle property controlling K/N backend threads (undocumented).
- Compose Multiplatform 1.10.1 version mapping: runtime/ui/foundation at 1.10.1, material3 at 1.10.0-alpha05.

## Issues

- **konan cache key too broad** — Initial `hashFiles('libs.versions.toml')` invalidates on any version change. Fixed to Kotlin version only.
- **material3:1.10.0 doesn't exist on Maven Central** — Checked 1.10.1 release page to find correct version (1.10.0-alpha05).
- **`--max-workers=3` did NOT fix K/N thread warning** — Controls Gradle parallelism, not K/N `-Xbackend-threads`. Fixed with `kotlin.native.parallelThreads` property.

## Commits

- `16cb14a` — chore: improve Apple CI build times for XCFramework compilation
- `699d7c5` — fix: address PR review feedback on Apple CI job
- `b8783ad` — fix: address PR feedback and upgrade Compose Multiplatform to 1.10.1
