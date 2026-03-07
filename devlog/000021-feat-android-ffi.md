# 000021 — feat/android-ffi
2026-02-26T21:15-08:00

**Agent:** Junie (gemini-3-flash-preview) @ repository branch feat/android-ffi
**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ repository branch feat/android-ffi — 2026-03-07T11:30-08:00, continuing from Junie's incomplete stub
**Intent:** Implement Android FFI support for the Prism Flutter plugin — full scene rendering, glTF loading, camera controls, and state queries via Dart FFI calling into a Kotlin/Native shared library.

## Plan (Junie)

1.  **Environment Check**: Verify wgpu4k Android native artifacts are in Maven local (Verified: `androidnativearm64` and `androidnativex64` present for `wgpu4k`, `wgpu4k-native`, and `wgpu4k-toolkit`).
2.  **Prism Native Support**:
    - Add `androidNativeArm64` and `androidNativeX64` targets to `prism-native/build.gradle.kts`.
    - Create `androidNativeWindow.def` cinterop to bridge NDK's `ANativeWindow_fromSurface`.
    - Implement `AndroidBridge.kt` in `prism-native` with both Dart FFI C symbols and JNI symbols for the JVM side to call.
3.  **Flutter Plugin Support**:
    - Add `bundleNativeAndroid` task to `prism-flutter/build.gradle.kts` to copy `.so` files to `jniLibs`.
    - Implement `PrismAndroidNative.kt` (JNI loader) and update `PrismAndroidPlatformView.kt` in `prism-flutter`.
    - Update `prism_engine_dispatch.dart` and `prism_engine_ffi.dart` to use FFI on Android.
4.  **Verification**:
    - Build `libprism.so` for Android.
    - Run the Flutter demo on an Android device.
    - Update CI.

## What Changed

Junie's WIP scaffold (carried to this worktree via git stash):
- `prism-math/build.gradle.kts`, `prism-core/build.gradle.kts`, `prism-ecs/build.gradle.kts`, `prism-renderer/build.gradle.kts`, `prism-scene/build.gradle.kts` — added `androidNativeArm64()` and `androidNativeX64()` targets
- `prism-native/build.gradle.kts` — conditional android native targets when `ANDROID_NDK_HOME`/`ANDROID_HOME` is set; cinterop for `androidNativeWindow.def`; `bundleNativeAndroid` Gradle task
- `prism-core/src/androidNativeMain/.../Platform.androidNative.kt` — `Platform` actual for Android Native using `gettimeofday`
- `prism-native/src/androidNativeMain/cinterop/androidNativeWindow.def` — NDK cinterop for `ANativeWindow_fromSurface`
- `prism-flutter/flutter_plugin/android/.../PrismAndroidNative.kt` — JNI loader (`System.loadLibrary("prism")`) with JNI declarations
- `prism-flutter/flutter_plugin/android/.../PrismAndroidPlatformView.kt` — `SurfaceView` + `Choreographer.FrameCallback` platform view factory
- `prism-flutter/flutter_plugin/android/.../PrismFlutterPlugin.kt` — plugin registers native view factory for `engine.prism.flutter/render_view_native`
- `prism-flutter/flutter_plugin/lib/src/prism_engine_ffi.dart` — `DynamicLibrary.open('libprism.so')` for Android
- `prism-flutter/flutter_plugin/lib/src/prism_render_view_mobile.dart` — Android uses `render_view_native` view type
- `prism-assets/src/appleMain/.../ImageDecoder.apple.kt` — renamed from `ImageDecoder.native.kt` (apple source set rename)
- `.github/workflows/ci.yml`, `build-all-docker.sh` — NDK setup steps

Claude's completions (2026-03-07T11:45-08:00):
- `prism-native/src/androidNativeMain/kotlin/com/hyeonslab/prism/native/AndroidBridge.kt` — full rewrite: fixed package from `engine.prism.native` → `com.hyeonslab.prism.native`; added `AndroidSurfaceState` data class, `androidScenes` and `pendingGlbPaths` atomic maps; implemented `prismLoadGltfFromPath` with pending-path queuing, full `prismRenderFrame` with SceneState scene dispatch, `prismResize` with aspect-ratio update, `prismDetachSurface` with shutdown/cleanup, camera APIs (`prism_orbit_by`, `prism_zoom`), state queries (`prism_get_fps`, `prism_toggle_pause`, `prism_get_pause_state`, `prism_is_renderer_ready`); mirrors IosBridge.kt
- `prism-assets/build.gradle.kts` — added `androidNativeArm64()` and `androidNativeX64()` targets so prism-assets is available to prism-native's androidNativeMain source set
- `prism-assets/src/androidNativeMain/kotlin/com/hyeonslab/prism/assets/ImageDecoder.androidNative.kt` — new stub matching linuxMain pattern
- `prism-flutter/flutter_plugin/lib/src/prism_engine_dispatch.dart` — removed `if (Platform.isAndroid) return null` guard from `resolveFlutterAssetPath()`; Android now uses the same Dart-extracts-to-temp-dir flow as iOS/macOS

## Decisions

- 2026-03-07T11:30-08:00 Moved Junie's WIP from main checkout to worktree via double-stash to maintain clean main branch.
- 2026-03-07T11:45-08:00 Added `androidNativeArm64/X64` to prism-assets unconditionally (same as prism-math, prism-core, etc.) rather than conditionally — prism-assets doesn't produce a shared lib so no NDK check is needed.
- 2026-03-07T11:45-08:00 `AndroidBridge.kt` mirrors `IosBridge.kt` exactly: `AndroidContext`/`androidContextRenderer` swapped in for the iOS equivalents; all other scene, camera, and state logic is identical.
- 2026-03-07T11:45-08:00 `prism-native/build.gradle.kts` sourceSets block has no duplicate target issue — `macosArm64()` etc. inside sourceSets are idempotent DSL getters.

## Issues

- None encountered. `ktfmtFormat`, `ktfmtCheck detektJvmMain jvmTest` all passed.

## Commits

- 0f1145f — feat: Android FFI bridge with full scene rendering
