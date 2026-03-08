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

## Decisions

- 2026-03-07T19:05-08:00 Porting Copilot review fixes from PR #49 into this branch since android-ffi was merged without them. These fixes are correctness issues (GPU resource leak, crash on progressive texture decode) and belong in main.
- 2026-03-07T19:05-08:00 Kept WASM rebuild and android fixes in one PR rather than splitting — both are housekeeping/correctness work targeting the same base.
- 2026-03-07T19:28-08:00 DerivedData cache key now includes Xcode version — the `restore-keys` fallback `xcode-dd-${{ runner.os }}-` was restoring stale module cache from pre-16.4 Xcode, causing mtime mismatch errors on `.pcm` files. Scoping to version ensures a fresh cache on Xcode upgrades.

## Issues

- None. `ktfmtFormat`, `ktfmtCheck detektJvmMain jvmTest` all passed.

## Commits

- 295bd47 — chore: rebuild WASM demo from latest main (PRs #45, #47, #48)
- 6c337c3 — fix: Android FFI Copilot review fixes (surfaceChanged, nativeMain wiring, JNI suppression, release .so)
- ffc6346 — fix: scope DerivedData cache key to Xcode version to prevent stale module cache
- HEAD — chore: update macOS CI runners to macos-26
