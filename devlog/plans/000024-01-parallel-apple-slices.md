# Plan: parallelize Apple XCFramework slices in CI

## Thinking

The `Apple Targets` CI job today runs a single Gradle invocation that
serially builds:

```
iosSimulatorArm64Test
macosArm64Test
:prism-demo-core:assemblePrismDemoDebugXCFramework
:prism-ios:assemblePrismDebugXCFramework
:prism-native:linkDebugSharedIosSimulatorArm64
:prism-native:linkDebugSharedMacosArm64
```

Recent successful run (24959988333) shows that "Apple native tests +
XCFrameworks" step takes **~14m** out of a ~17m total job. The dominant
cost is Kotlin/Native link for the static frameworks — each XCFramework
has 10+ exported modules.

`org.gradle.parallel=true` is already set, but K/N link tasks for
different Apple slices (iosArm64 vs iosSimulatorArm64) live in the same
module (`prism-ios`, `prism-demo-core`) and historically don't parallelize
well within a single Gradle invocation due to memory pressure (4 GB
heap) and historical K/N compiler-singleton state.

The XCFramework structure is just a directory of per-slice
`.framework`s plus an `Info.plist`. `xcodebuild -create-xcframework
-framework <slice1> -framework <slice2> -output Foo.xcframework`
produces an identical artifact. So we can build each slice's `.framework`
on its own runner and merge them in a follow-up job — the user's
preferred approach.

Verified slice-level Gradle task names by listing `:prism-ios:tasks
--all`:

- `:prism-ios:linkDebugFrameworkIosArm64`
- `:prism-ios:linkDebugFrameworkIosSimulatorArm64`
- `:prism-demo-core:linkDebugFrameworkIosArm64`
- `:prism-demo-core:linkDebugFrameworkIosSimulatorArm64`

Each produces the per-slice `.framework` directory at
`<module>/build/bin/<target>/debugFramework/<Name>.framework/`.

The `prism-ios-demo` Xcode project (in `project.yml`) references
`prism-demo-core/build/XCFrameworks/debug/PrismDemo.xcframework`. So
the finalize job must place the merged XCFramework at exactly that
path before the iOS demo build step runs.

### Trade-off

- **Wall clock:** matrix slices run in parallel, max(slice times) +
  finalize merge ≈ 7-9 min vs the current 14 min Gradle step. Finalize
  job adds ~5 min for the iOS demo build (already in current job). New
  total ≈ 12-15 min vs current 17 min.
- **Cost:** 3 macOS runners during the slice phase + 1 macOS runner for
  finalize = 4 macOS-minute streams instead of 1. macOS runners are
  10× the linux rate; net macOS-minute spend per CI run is ~3-4×.
- The user explicitly asked for the slice-then-merge approach and is
  already aware of CI cost (recent commit `7e2afaf` reduced cache
  restore time).

### Design

Three jobs replace the existing `apple` job:

```
apple-slice  (matrix: target ∈ {iosArm64, iosSimulatorArm64, macosArm64})
   │
   └─► apple-finalize (needs: apple-slice)
         (assembles XCFrameworks, runs iOS demo build)
```

Per-slice tasks:

- **iosArm64**:
  `:prism-ios:linkDebugFrameworkIosArm64`,
  `:prism-demo-core:linkDebugFrameworkIosArm64`
- **iosSimulatorArm64**:
  `:prism-ios:linkDebugFrameworkIosSimulatorArm64`,
  `:prism-demo-core:linkDebugFrameworkIosSimulatorArm64`,
  `:prism-native:linkDebugSharedIosSimulatorArm64`,
  `iosSimulatorArm64Test` (multi-project umbrella)
- **macosArm64**:
  `:prism-native:linkDebugSharedMacosArm64`,
  `macosArm64Test`

Each slice job uploads:

- `prism-ios/build/bin/<target>/debugFramework/Prism.framework/`
- `prism-demo-core/build/bin/<target>/debugFramework/PrismDemo.framework/`
- (sim only) `prism-native/build/bin/iosSimulatorArm64/debugShared/`
- (mac only) `prism-native/build/bin/macosArm64/debugShared/`

Finalize job:

1. Downloads all artifacts.
2. Reconstructs the per-target `.framework` paths under
   `<module>/build/bin/<target>/debugFramework/`.
3. Calls `xcodebuild -create-xcframework -framework iosArm64.framework
   -framework iosSimulatorArm64.framework -output
   <module>/build/XCFrameworks/debug/<Name>.xcframework` for both
   `Prism` and `PrismDemo`.
4. Runs the existing header-verification logic against the merged
   `Prism.xcframework`.
5. Uploads `Prism-debug.xcframework`.
6. Runs xcodegen + xcodebuild for `prism-ios-demo` against the merged
   `PrismDemo.xcframework`.

The finalize job does **not** need wgpu4k or Gradle setup because it
doesn't invoke Gradle — only `xcodebuild` and `xcodegen`.

### Risks / unknowns

- The K/N link task may emit additional output files (e.g. `Headers/`,
  `Modules/`, `Resources/`) inside the per-slice `.framework`. We need
  to upload the entire framework directory tree and preserve symlinks,
  not just the binary. `actions/upload-artifact@v4` preserves the tree
  but flattens symlinks. Static frameworks shouldn't have symlinks
  (those are a dynamic-framework quirk), so this should be fine — but
  worth verifying on the first run.
- `iosSimulatorArm64Test` and `macosArm64Test` are aggregator tasks
  spanning all modules. If any module has its target-specific test
  fail, that slice job fails. Same as today, just split across runners.
- Configuration cache: each matrix runner has its own gradle/cache.
  Cache key is keyed on the same Kotlin/Native version, so all runners
  share the K/N toolchain cache. The Gradle home cache is
  per-runner-OS so all three macOS runners share via GHA.

## Plan

1. Refactor `.github/workflows/ci.yml`: replace the `apple` job with
   `apple-slice` (matrix) + `apple-finalize`.
2. In `apple-slice`:
   - Reuse the current setup steps (JDKs, Gradle, K/N toolchain cache,
     wgpu4k action).
   - Use a `case`/`if` block on `matrix.target` to invoke the right
     Gradle tasks per slice.
   - Upload the per-slice `.framework` directories (and `prism-native`
     dylibs where applicable) as artifacts named
     `apple-slice-<target>`.
3. In `apple-finalize`:
   - macos-26 runner.
   - Download all `apple-slice-*` artifacts.
   - Re-stage the framework directories under
     `<module>/build/bin/<target>/debugFramework/` so paths match what
     `xcodebuild -create-xcframework` expects.
   - Run `xcodebuild -create-xcframework` for `Prism` and `PrismDemo`.
   - Reuse the existing "Verify XCFramework headers" step.
   - Reuse the existing "Upload Prism XCFramework" step.
   - Reuse the existing xcodegen / iOS demo steps.
4. Drop `--configuration-cache` and `-Pkotlin.native.parallelThreads=3`
   tuning where no longer applicable; keep on slice jobs that still run
   Gradle.
5. Verify on a draft PR — all three slice jobs and the finalize job
   pass; merged XCFramework artifact looks identical to today's.
6. After merge, monitor wall-clock numbers in step summary.
