# Plan 000019-01 — Reduce Gradle Cache Restore Time

## Thinking

The Gradle cache restore step takes ~13 minutes in CI, primarily on the Apple (macos-15) runner.
Root causes:
1. `cache-cleanup` defaults to `on-success` — stale entries accumulate because cleanup is skipped
   on failed builds.
2. The Apple job caches `transforms-*/` (extracted AARs, dexed JARs, derived artifacts) even
   though it never runs Android tests. These can be hundreds of MB and are regenerated quickly.
3. No diagnostic exists to measure the `modules-2` vs `transforms-*` split.

Three targeted fixes address all three without changing build semantics:

Fix 1 — `cache-cleanup: always` on both jobs: evicts 7+-day-old entries before saving, even on
failure. Expected 20–40% reduction over several weeks.

Fix 2 — `gradle-home-cache-excludes` on Apple job only: strips `transforms-*` and `kotlin-dce`
from the cached archive. Potentially removes 200–800 MB immediately.

Fix 3 — Broader diagnostic step: replaces the existing narrow `build-cache-1` check with a table
covering `modules-2`, `transforms-3`, `transforms-4`, `build-cache-1`, and `kotlin-dce`, plus a
total `~/.gradle` size row. Added to both `ci` and `apple` jobs.

The `docker` job already has `cache-read-only: true` — no changes needed there.

## Plan

1. Open `.github/workflows/ci.yml` in the worktree.
2. **ci job — Setup Gradle step** (line ~37): add `cache-cleanup: always`.
3. **ci job — Gradle build cache diagnostics step** (line ~60): replace the narrow `build-cache-1`
   block with the full Gradle Home Cache Size table.
4. **apple job — Setup Gradle step** (line ~97): add `cache-cleanup: always` and
   `gradle-home-cache-excludes: caches/transforms-* / caches/kotlin-dce`.
5. **apple job** — Add a new "Gradle home cache size breakdown" step after the Apple build step
   (after line ~137, before the Verify XCFramework step).
6. Run `./gradlew ktfmtCheck detektJvmMain jvmTest` — N/A (pure YAML, no Kotlin changes).
7. Commit, push, draft PR.
