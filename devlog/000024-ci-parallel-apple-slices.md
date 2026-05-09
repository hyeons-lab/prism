# 000024 — ci/parallel-apple-slices

**Agent:** Claude (claude-opus-4-7) @ prism branch ci/parallel-apple-slices

## Intent

Cut wall-clock time of the `Apple Targets` CI job by compiling per-slice
Kotlin/Native frameworks (iosArm64, iosSimulatorArm64, macosArm64) on
parallel macOS runners and merging them into XCFrameworks via
`xcodebuild -create-xcframework` in a finalize job.

User explicitly framed the design: "compile them in parallel, and then
merge them — that would be faster".

Baseline (run 24959988333): "Apple native tests + XCFrameworks" step =
14m 12s out of a 17m total job.

## What Changed

- 2026-05-08T19:38-0700 `.github/workflows/ci.yml` — replace single
  `apple` job with `apple-slice` (matrix over `iosArm64`,
  `iosSimulatorArm64`, `macosArm64`) and `apple-finalize` (downloads
  slice `.framework`s, merges via `xcodebuild -create-xcframework`,
  verifies headers, uploads `Prism.xcframework`, runs xcodegen + iOS
  demo xcodebuild).
- 2026-05-08T19:38-0700 `devlog/plans/000024-01-parallel-apple-slices.md`
  — design doc for the split.

## Decisions

- 2026-05-08T19:38-0700 Three matrix slices (iosArm64,
  iosSimulatorArm64, macosArm64) rather than two (combine the two iOS
  slices into one runner). Reasoning: the dominant cost is K/N link of
  the iOS slices specifically; combining them defeats the parallelism
  win. macosArm64 has no XCFramework participation but it does host
  `macosArm64Test` and `prism-native` macOS dylib, which today share
  the same Gradle invocation as the iOS work — moving them onto their
  own runner removes them from the critical path of the iOS slice.
- 2026-05-08T19:38-0700 Merge XCFrameworks with raw `xcodebuild
  -create-xcframework` in the finalize job instead of running
  `assemblePrismDebugXCFramework` after staging artifacts. Reasoning:
  the assemble Gradle task would re-run K/N link tasks (or fail their
  up-to-date checks) on the finalize runner because the slice
  `.framework`s come from a different worktree. Bypassing Gradle in
  finalize keeps the merge purely IO + plist generation.
- 2026-05-08T19:38-0700 Finalize job does not install Gradle / wgpu4k /
  K/N toolchain. It only needs xcodegen + xcodebuild.
- 2026-05-08T19:38-0700 Slice artifacts uploaded as raw `.framework`
  directories (one artifact per slice). `actions/upload-artifact@v4`
  zips these but preserves the directory tree on download. Static
  frameworks have no symlinks so the round-trip should be safe.
- 2026-05-08T19:38-0700 Each slice keeps the existing K/N toolchain
  cache (`~/.konan` keyed on Kotlin version). All three slice runners
  hit the same GHA cache entry → single populate, three readers.

## Issues

- Outstanding: first push will validate that `actions/upload-artifact`
  preserves the framework tree well enough that `xcodebuild
  -create-xcframework` accepts it. If the round-trip drops bundle
  metadata (e.g. `Info.plist`, `Modules/module.modulemap`), the merge
  will fail and the slice upload step needs `tar` packaging.

## Commits

- HEAD — ci: parallelize Apple XCFramework slices
