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

### Step 1 — Annotate clean types with `@JsExport` in existing modules

Add `@JsExport` (with `@OptIn(ExperimentalJsExport::class)`) directly to types that have only
primitive fields — these are clean for both surfaces and trigger no `allWarningsAsErrors` warnings.

- **prism-math/src/commonMain/** — Vec2, Vec3, Vec4, Mat3, Mat4, Quaternion, Transform, Color
- **prism-core/src/commonMain/** — Time, EngineConfig
- **prism-ecs/src/commonMain/** — TransformComponent, LightComponent, CameraComponent
- **prism-renderer/src/commonMain/** — All enums (LightType, BlendMode, CullMode, etc.), LightData, VertexAttribute, RenderPassDescriptor

For Entity: `id: UInt` has no C or TypeScript equivalent. Add `@JsName("id") val jsId: Int get() = id.toInt()` for WASM; the C API will use `long` handles instead.

Skip any type that contains wgpu4k opaque handles (`GpuBuffer`, `RenderPipeline`, `Texture`,
`ShaderModule`) — those stay Kotlin-side only.

---

### Step 2 — Create `prism-js` module (WASM/TypeScript SDK)

**`settings.gradle.kts`**: add `include(":prism-js")`

**`prism-js/build.gradle.kts`**:
```kotlin
plugins { id("prism-quality"); kotlin("multiplatform") }

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
        outputModuleName.set("prism")
        generateTypeScriptDefinitions()   // ← auto-generates prism.d.ts
    }
    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":prism-math"))
            implementation(project(":prism-core"))
            implementation(project(":prism-scene"))
            implementation(project(":prism-ecs"))
            implementation(project(":prism-renderer"))
        }
    }
}
```

Bridge files under `prism-js/src/wasmJsMain/kotlin/engine/prism/js/`:
- **`EngineApi.kt`** — `prismCreateEngine`, `prismDestroyEngine`, `prismEngineInitialize`, `prismEngineGetTime`, `prismEngineIsInitialized`
- **`SceneApi.kt`** — `prismCreateScene`, `prismCreate{Node,MeshNode,CameraNode,LightNode}`, `prismSceneAddNode`, `prismNodeSetPosition/Rotation/Scale`, `prismSetActiveCamera`
- **`EcsApi.kt`** — `prismCreateWorld`, `prismWorldCreateEntity`, per-component-type add/get/query
- **`RendererApi.kt`** — `prismInitSurface`, `prismSurfaceRenderFrame`, `prismSurfaceSetMaterial`, `prismSurfaceResize`, `prismSurfaceDestroy`
- **`MeshApi.kt`** — `prismMeshTriangle/Quad/Cube/Sphere`, `prismMeshFromArrays`

---

### Step 3 — Create `prism-native` module (C API bridge)

**`settings.gradle.kts`**: add `include(":prism-native")`

Targets: `iosArm64`, `iosSimulatorArm64`, `macosArm64`, `linuxX64`, `mingwX64` — each with `binaries.sharedLib("prism")`.

Note: `prism-ecs` does not support linuxX64/mingwX64, so `prism-native` will depend on prism-scene
and prism-core only, mirroring the API without ECS on desktop Linux/Windows targets, OR we re-enable
those targets in prism-ecs. Evaluate during implementation.

Bridge files under `prism-native/src/nativeMain/kotlin/engine/prism/native/`:
- **`Registry.kt`** — `HandleRegistry<T>` mapping `Long` IDs to Kotlin objects
- **`NativeBridge.kt`** — `@CName`-annotated C API surface

---

### Step 4 — Auto-generate Dart FFI bindings from C header (`ffigen`)

Add `prism-flutter/flutter_plugin/ffigen.yaml`. Wire `generateFfiBindings` Gradle task in
`prism-native/build.gradle.kts`. Add `ffiPlugin: true` to pubspec.yaml.

---

### Step 5 — Auto-generate Dart `@JS()` bindings from `.d.ts` (WASM surface)

Add `generateDartJsBindings` Gradle task to `prism-flutter/build.gradle.kts`. Remove hand-written
`@JS()` declarations from `prism_web_plugin.dart`.

---

### Step 6 — Update `prism-flutter` to use generated bindings

- Keep MethodChannel for Android
- Use generated `@JS()` bindings for web
- New `prism_engine_ffi.dart` for iOS/macOS/Linux/Windows
- Conditional export in `prism_engine.dart`
