# Plan: Auto-Generated JS and Native Bindings for Prism

## Thinking

Prism is KMP and compiles to multiple native targets. Two distinct binding surfaces are needed
for JavaScript/Flutter consumers, each with its own auto-generation mechanism:

| Runtime | Kotlin target | Bridge | Dart binding auto-gen |
|---------|--------------|--------|----------------------|
| Web / Flutter Web | `wasmJs` | `@JsExport` → `.mjs` | `generateTypeScriptDefinitions()` → `.d.ts` → Gradle script |
| iOS, macOS, Linux, Windows (Flutter native + desktop) | Kotlin/Native | `@CName` → `.h` | `ffigen` reads `.h`, generates `dart:ffi` bindings |
| Android | JVM Kotlin | MethodChannel (keep) | n/a — JVM doesn't expose a C ABI from the same code |

Currently the only bridge is a hand-written, Flutter-specific WASM entry point with 5 @JsExport
opaque command functions (prismInit, prismTogglePause, prismGetState, prismIsInitialized, prismShutdown),
and a MethodChannel handler for mobile.

The existing `prism-flutter` module already has a `wasmJs` target producing `prism-flutter.mjs`.
The new `prism-js` module will be a standalone WASM SDK (not Flutter-specific) with a richer API.

Both surfaces share the same opaque-handle registry pattern: complex Kotlin objects live on the
Kotlin side, identified by a `Long` handle ID. Only primitives (`Int`, `Float`, `Boolean`,
C strings / JS strings) cross the boundary.

Key findings from codebase exploration:
- `prism-ecs` does NOT include linuxX64/mingwX64 (commented out as "no platform code") — `prism-native` will need to account for this
- `prism-quality` convention plugin applies ktfmt (Google style, 100 char) and detekt (JVM+WASM only)
- Version catalog: Kotlin 2.3.0, AGP 8.13.0
- Existing WASM entry: `FlutterWasmEntry.kt` in `prism-flutter/src/wasmJsMain/` — already uses `@JsExport`

## Plan

### Step 1 — Annotate clean types with `@JsExport` in existing modules [DONE]

### Step 2 — Create `prism-js` module (WASM/TypeScript SDK) [DONE]

### Step 3 — Create `prism-native` module (C API bridge) [DONE]

### Step 4 — Auto-generate Dart FFI bindings from C header (`ffigen`) [DONE]

### Step 5 — Auto-generate Dart `@JS()` bindings from `.d.ts` (WASM surface) [DONE]

### Step 6 — Update `prism-flutter` to use generated bindings [DONE]

---

### Recent Improvements

#### 1. Resource Management & Memory Leak Prevention
- **Dart (FFI):** Added `NativeFinalizer` to `Engine`, `World`, `Node`, and `Scene` in `prism_sdk_ffi.dart`.
- **TypeScript (WASM):** Added `FinalizationRegistry` to SDK classes in `prism-sdk.mts`.
This ensures Kotlin objects are automatically released from the `Registry` when garbage collected on the consumer side.

#### 2. Thread Safety (macOS)
- Replaced `mutableMapOf` with `AtomicRef<Map<...>>` in `MacosBridge.kt`.
- Uses `kotlinx-atomicfu` to ensure safe concurrent access between Flutter UI (main thread) and Metal render loop.

#### 3. Architectural Consolidation
- Standardized macOS to use FFI for engine control, reducing reliance on `MethodChannel`.
- Renamed `prism_sdk_stub.dart` to `prism_sdk_ffi.dart` for clarity.

#### 4. Robustness & Debugging
- Added `kermit` logging to `Registry.get` in `prism-native` to warn when an object is not found for a given handle.

#### 5. CI/CD & Multi-platform Artifacts
- Added a `docker` job to `.github/workflows/ci.yml` using `build-all-docker.sh`.
- Automated builds for Linux (.so), Windows (.dll), WASM/JS, and Android APK.
- Uploads all artifacts to GitHub Actions for verification.
