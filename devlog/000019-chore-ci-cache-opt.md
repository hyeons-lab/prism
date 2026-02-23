# 000019 — chore/ci-cache-opt

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ prism branch chore/ci-cache-opt
**Intent:** Reduce Gradle cache restore time in CI (currently ~13 min on the Apple job) by enabling `cache-cleanup: always`, excluding `transforms-*` from the Apple job's cached Gradle home, and adding a per-directory size diagnostic to both jobs' step summaries.

## What Changed

`2026-02-23T...` `.github/workflows/ci.yml` — Added `cache-cleanup: always` to `ci` and `apple` Setup Gradle steps; added `gradle-home-cache-excludes: caches/transforms-* / caches/kotlin-dce` to the `apple` job only; replaced the narrow `build-cache-1`-only diagnostic in `ci` with a broader Gradle Home Cache Size table covering `modules-2`, `transforms-3`, `transforms-4`, `build-cache-1`, and `kotlin-dce`; added the same broad diagnostic to `apple` after its build step.

## Decisions

`2026-02-23T...` Keep `gradle-home-cache-excludes` on Apple only — the Linux `ci` job needs transforms for Android-related detekt/compile tasks.
`2026-02-23T...` Extend the existing `ci` diagnostic rather than adding a second step, to avoid duplicate GITHUB_STEP_SUMMARY sections.
`2026-02-23T...` `docker` job left unchanged — it already uses `cache-read-only: true` so it never writes and cleanup is irrelevant.

## Commits

HEAD — chore: reduce Gradle cache restore time in CI
