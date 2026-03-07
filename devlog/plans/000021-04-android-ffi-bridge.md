## Thinking

The Junie agent's WIP left a nearly-complete scaffold but with several critical gaps:

1. `AndroidBridge.kt` is a stub — only renders a clear-colour pass, no SceneState, glTF, camera, or state APIs.
2. Wrong package (`engine.prism.native` instead of `com.hyeonslab.prism.native`).
3. `prism-assets` missing `androidNativeArm64` / `androidNativeX64` targets → compile failure.
4. `prism-assets` missing `ImageDecoder` actual for `androidNativeMain`.
5. `prism_engine_dispatch.dart` guards `resolveFlutterAssetPath` with `if (Platform.isAndroid) return null`, which breaks FFI asset loading.

The iOS bridge (`IosBridge.kt`) is the reference to mirror — it uses `AndroidContext` / `androidContextRenderer` instead of `IosContext` / `iosContextRenderer`, and JNI symbols in addition to the `@CName` Dart FFI exports.

The `nativeMain` sourceset in `prism-native` already contains `SceneState.kt` / `NativeBridge.kt` / `Registry.kt` and is shared by all native targets including `androidNativeMain`. So `AndroidBridge.kt` just needs to import from `com.hyeonslab.prism.native` directly.

The `applyDefaultHierarchyTemplate()` already used in `prism-assets` will automatically create `androidNativeMain` as an intermediate source set when the `androidNativeArm64()` and `androidNativeX64()` targets are added. An `ImageDecoder` stub matching the `linuxMain` pattern is sufficient since real texture decode on Android isn't needed for a first release.

The `PrismFlutterPlugin.kt` in `engine.prism.flutter` package references new classes in `com.hyeonslab.prism.flutter` — that file is the JVM plugin entry point and keeps the old package to match the Flutter plugin manifest; the new `PrismAndroidNative.kt` / `PrismAndroidPlatformView.kt` live in the correct `com.hyeonslab.prism.flutter` package.

## Plan

1. **Fix `prism-assets/build.gradle.kts`** — add `androidNativeArm64()` and `androidNativeX64()` unconditionally (like `linuxX64()`) since the targets are always registered by other modules.

2. **Create `prism-assets/src/androidNativeMain/.../ImageDecoder.androidNative.kt`** — stub matching `linuxMain` pattern.

3. **Rewrite `prism-native/src/androidNativeMain/.../AndroidBridge.kt`** — fix package, add full SceneState integration, glTF loading, camera/input, state queries, JNI wrappers. Mirror `IosBridge.kt` exactly, substituting `AndroidContext` / `androidContextRenderer` for the iOS equivalents.

4. **Fix `prism_engine_dispatch.dart`** — remove `if (Platform.isAndroid) return null` guard.

5. **Run `ktfmtFormat`**, then `ktfmtCheck detektJvmMain jvmTest` to verify no regressions.

6. **Commit and push** with conventional commit message; update devlog and draft PR.
