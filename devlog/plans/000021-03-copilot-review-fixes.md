## Thinking

PR #48 received 12 Copilot review comments. After reading all affected files:
- 5 are real bugs to fix
- 2 need C-wrapper exports plus Dart updates
- The rest are either already correct, design suggestions, or require a major async
  refactor that's out of scope

### Real bugs (fix)

A. `encoder.finish()` returns a `GPUCommandBuffer` that is never closed — per-frame leak
   in the fallback clear pass in MacosBridge.kt and IosBridge.kt.

B. `NativeFinalizer` in prism_sdk_ffi.dart looks up `prism_destroy_*` as
   `Void Function(Pointer<Void>)` but the Kotlin exports take `Long` — UB. Need typed
   `*_ptr` C wrapper functions and updated Dart lookups.

C. Temp file in prism_engine_dispatch.dart uses `assetKey.split('/').last` — basename
   collision if two assets share a filename.

D. `fps` in prism_engine_web.dart reads `PrismWebEngine.lastCanvasId` instead of this
   engine's `_canvasId` — wrong FPS in multi-engine setups.

E. `WgpuRenderer` is used directly in SceneState.kt but `:prism-renderer` is not an
   explicit dependency in prism-native/build.gradle.kts.

### Already correct / skip

- Comments 2 & 3 (iOS/macOS NSNumber cast) — already use `(params?["engineHandle"] as? NSNumber)?.int64Value ?? 0`
- Comment 5 (resolveFlutterAssetPath exceptions) — already has `catch (_) { return null; }`
- Comment 7 (main.dart polling) — no polling loop in current code; `_setup()` calls `loadGltfFromPath` once
- Comment 8 (IBL/HDR decoupling) — design suggestion; SceneState always sets `hdrEnabled = true` first
- Comment 1 (runBlocking async loading) — valid concern but significant architectural change; out of scope

## Plan

### A — Command buffer leak (MacosBridge.kt + IosBridge.kt)

Both files, same change at line ~198:

```kotlin
// before
ctx.device.queue.submit(listOf(encoder.finish()))

// after
encoder.finish().use { cmdBuf -> ctx.device.queue.submit(listOf(cmdBuf)) }
```

### B — NativeFinalizer typed C wrappers

Add 4 `*_ptr` wrapper functions in NativeBridge.kt after existing destroy functions.
Add `import kotlinx.cinterop.COpaquePointer` and `import kotlinx.cinterop.rawValue`.

Update prism_sdk_ffi.dart finalizer lookups to use `*_ptr` symbols.

### C — Temp file collision (prism_engine_dispatch.dart)

```dart
// before
final fileName = assetKey.split('/').last;
// after
final fileName = assetKey.replaceAll('/', '_');
```

### D — Web FPS canvas affinity (prism_engine_web.dart)

```dart
// before
double get fps => PrismWebEngine.getFps(PrismWebEngine.lastCanvasId ?? '');
// after
double get fps =>
    PrismWebEngine.getFps(_canvasId ?? PrismWebEngine.lastCanvasId ?? '');
```

### E — Explicit prism-renderer dependency (prism-native/build.gradle.kts)

Add `implementation(project(":prism-renderer"))` to `nativeMain.dependencies { }`.

### Verification

1. `./gradlew ktfmtFormat`
2. `./gradlew ktfmtCheck detektJvmMain jvmTest`
3. Rebuild native: `JAVA_HOME=~/.sdkman/candidates/java/21.0.9-zulu ./gradlew :prism-native:linkReleaseSharedMacosArm64`
4. Push; CI should pass
