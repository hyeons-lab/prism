# 000022 — chore/docs-wasm-rebuild
2026-03-07T19:05-08:00

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ repository branch chore/docs-wasm-rebuild
**Intent:** Rebuild committed `docs/wasm/` WASM artifacts from the latest main, picking up changes from PRs #45, #47, #48, and #49 that modified the Flutter/WASM integration. Also port Copilot review fixes from PR #49 that were not included in the merge.

## What Changed

- `docs/wasm/` — rebuilt WASM bundle from current main (PRs #45, #47, #48): new chunk hashes (840.js, 90.js, 3272a582cfb5eeea043f.wasm), updated prism-demo-core.js, removed stale old chunks
- `prism-assets/src/androidNativeMain/.../ImageDecoder.androidNative.kt` — `TODO(...)` → `return null` in `decode()` to avoid `NotImplementedError` crashing the texture-loading coroutine (Copilot fix 1 from PR #49)
- `prism-flutter/flutter_plugin/android/.../PrismAndroidPlatformView.kt` — added `attached` flag; `surfaceChanged` calls `nAttachSurface` only once and `nResize` on subsequent events; reset `attached` in `stopRendering()` (Copilot fix 2)
- `prism-native/build.gradle.kts` — replaced separate `androidNativeTargets.forEach` + `if (isMac)` blocks with `(nativeTargets + androidNativeTargets).forEach` so `linuxX64`/`mingwX64` also wire to `nativeMain` (Copilot fix 3)
- `prism-native/src/androidNativeMain/.../AndroidBridge.kt` — `@Suppress("UNUSED_PARAMETER")` on all four JNI wrapper functions (Copilot fix 4/5)
- `prism-flutter/build.gradle.kts` — `bundleNativeAndroidArm64`/`X64` tasks changed from `debugShared` to `releaseShared` (Copilot fix 6)
- `.github/workflows/ci.yml` — added "Get Xcode version" step and included `steps.xcode-version.outputs.version` in the DerivedData cache key; `restore-keys` also scoped to Xcode version so stale module cache from a prior Xcode install is never restored; runner updated to `macos-26`
- `.github/workflows/release.yml` — runner updated to `macos-26`
- `prism-native/build.gradle.kts` — added `applyDefaultHierarchyTemplate()`; replaced `val nativeMain by creating { dependsOn(commonMain) }` with `val nativeMain by getting` (the template creates and wires it); removed the explicit `(nativeTargets + androidNativeTargets).forEach { dependsOn(nativeMain) }` loop — the default hierarchy handles all targets including `androidNativeArm64/X64`
- `prism-demo-core/build.gradle.kts` — moved `linuxX64()` / `mingwX64()` out of the `else` branch so they are always declared (previously only on non-macOS); added `nativeMain.dependencies { implementation(libs.compose.runtime) }` so the Compose Compiler plugin (applied to all targets including linuxX64/mingwX64) finds Compose Runtime on the classpath
- `prism-demo-core/src/nativeMain/kotlin/.../TextureUploadHelper.native.kt` — new file; consolidated the identical `macosMain` and `iosMain` `uploadDecodedImage` actuals into a single `nativeMain` actual so linuxX64/mingwX64 are covered
- `prism-demo-core/src/macosMain/kotlin/.../TextureUploadHelper.macos.kt` — deleted; replaced by `nativeMain` actual above
- `prism-demo-core/src/iosMain/kotlin/.../TextureUploadHelper.ios.kt` — deleted; replaced by `nativeMain` actual above
- `prism-android-demo/build.gradle.kts` — broadened `tasks.matching` pattern to include all lint-related task names (not just `merge*Assets`); AGP lint model tasks (`lintAnalyze*`, `lintVitalAnalyze*`, `generate*LintReportModel`) also read from the assets source directory and need `downloadDemoAssets` declared as a dependency
- `detekt.yml` — added `wasmJs`, `apple`, `native`, `androidNative`, `macos`, `linux`, `mingw` to `MatchingDeclarationName.multiplatformTargets`; only `ios`, `android`, `js`, `jvm` were listed, causing `FileReader.wasmJs.kt` and similar platform-suffix files to fail the rule

## Decisions

- 2026-03-07T19:05-08:00 Porting Copilot review fixes from PR #49 into this branch since android-ffi was merged without them. These fixes are correctness issues (GPU resource leak, crash on progressive texture decode) and belong in main.
- 2026-03-07T19:05-08:00 Kept WASM rebuild and android fixes in one PR rather than splitting — both are housekeeping/correctness work targeting the same base.
- 2026-03-07T19:28-08:00 DerivedData cache key now includes Xcode version — the `restore-keys` fallback `xcode-dd-${{ runner.os }}-` was restoring stale module cache from pre-16.4 Xcode, causing mtime mismatch errors on `.pcm` files. Scoping to version ensures a fresh cache on Xcode upgrades.
- 2026-03-07T21:29-08:00 `prism-native` was the only module without `applyDefaultHierarchyTemplate()`. All other modules already called it. Adding it allows `nativeMain` to be obtained with `by getting` instead of `by creating`, and removes the explicit `forEach` wiring that conflicted with the template. The default hierarchy already includes `androidNativeArm64/X64` under `nativeMain`, so no manual edges are needed.
- 2026-03-07T21:29-08:00 `detekt.yml` `MatchingDeclarationName.multiplatformTargets` was missing 7 KMP platform suffixes actually used in the project. Kotlin's `expect`/`actual` convention uses `.platformSuffix.kt` filenames (e.g., `FileReader.wasmJs.kt`). Detekt must be told which suffixes to strip when comparing file names to declaration names.
- 2026-03-07T21:29-08:00 `./gradlew build` on macOS has two pre-existing failures unrelated to this PR: (a) `prism-demo-core:compileIosMainKotlinMetadata` fails due to Compose 1.10.1 KLIB duplicate unique-names between `org.jetbrains.compose` and `androidx.compose` transitive deps; (b) `prism-assets:linkDebugTestLinuxX64` fails because macOS LLD cannot link Linux binaries. Neither is in CI's dependency chain (CI runs specific tasks, not `build`).
- 2026-03-08T13:27-0700 `prism-demo-core` was conditionally declaring `linuxX64/mingwX64` only on non-macOS. This meant macOS builds silently omitted those targets while `prism-flutter-demo` (which has always declared them unconditionally) caused Gradle variant-resolution failure on macOS. Correct fix: add the targets unconditionally to `prism-demo-core` (they cross-compile fine on macOS). Adding them also required a consolidated `nativeMain` actual for `TextureUploadHelper` (replacing the identical macosMain/iosMain copies) and `nativeMain.dependencies { compose.runtime }` (Compose Compiler applies to all native targets).

## Issues

- None. `ktfmtFormat`, `ktfmtCheck detektJvmMain jvmTest` all passed.

## Commits

- 295bd47 — chore: rebuild WASM demo from latest main (PRs #45, #47, #48)
- 6c337c3 — fix: Android FFI Copilot review fixes (surfaceChanged, nativeMain wiring, JNI suppression, release .so)
- ffc6346 — fix: scope DerivedData cache key to Xcode version to prevent stale module cache
- 434406b — chore: update macOS CI runners to macos-26
- e39a6f4 — fix: hierarchy, lint deps, detekt multiplatform targets, flutter-demo target scope
- HEAD — fix: add linuxX64/mingwX64 to prism-demo-core, consolidate nativeMain actual
