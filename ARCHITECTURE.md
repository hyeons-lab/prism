# Prism Engine Architecture

This document describes the architecture of the Prism engine — a modular, cross-platform 3D game engine built with Kotlin Multiplatform. It covers design decisions, data flow, performance characteristics, and future improvement areas.

For build commands, contribution guidelines, and project status, see [AGENTS.md](AGENTS.md).

---

## Table of Contents

1. [Build System Architecture](#1-build-system-architecture)
2. [Module Architecture & Dependency Graph](#2-module-architecture--dependency-graph)
3. [Core Engine Layer (prism-core)](#3-core-engine-layer-prism-core)
4. [Math Layer (prism-math)](#4-math-layer-prism-math)
5. [ECS Layer (prism-ecs)](#5-ecs-layer-prism-ecs)
6. [Rendering Layer (prism-renderer)](#6-rendering-layer-prism-renderer)
7. [Scene Graph (prism-scene)](#7-scene-graph-prism-scene)
8. [Platform Surface Layer (prism-native-widgets)](#8-platform-surface-layer-prism-native-widgets)
9. [Compose Integration (prism-compose)](#9-compose-integration-prism-compose)
10. [Demo Architecture (prism-demo-core)](#10-demo-architecture-prism-demo-core)
11. [Platform-Specific Integration Patterns](#11-platform-specific-integration-patterns)
12. [Performance Characteristics & Future Improvements](#12-performance-characteristics--future-improvements)

---

## 1. Build System Architecture

### Gradle Configuration

Prism uses **Gradle 9.2** with Kotlin DSL. All dependency versions are centralized in `gradle/libs.versions.toml`.

**Root `build.gradle.kts`** declares plugins with `apply false` — each module opts in to the plugins it needs. Two quality tools are applied unconditionally to all subprojects via the `subprojects {}` block:

- **KtFmt** (`com.ncorti.ktfmt.gradle`): Google style, 100-character max line width
- **Detekt** (`io.gitlab.arturbosch.detekt`): Custom config from `detekt.yml` at project root, `buildUponDefaultConfig = false`, `maxIssues = 0`

All Kotlin compilations set `allWarningsAsErrors = true`.

### KMP Target Matrix

Each module declares its own targets individually. The full set across all modules:

| Target | Platform | Backend |
|--------|----------|---------|
| `jvm` | JVM Desktop | GLFW + Metal/Vulkan/DX12 via wgpu4k FFI |
| `wasmJs` | Web Browser | WebGPU API via wgpu4k JS bindings |
| `iosArm64` | iOS Device | MTKView + Metal via wgpu4k K/Native |
| `iosSimulatorArm64` | iOS Simulator | Same as iosArm64 |
| `macosArm64` | macOS Native | GLFW + Metal via wgpu4k K/Native |
| `linuxX64` | Linux Native | GLFW + Vulkan (compiles, untested) |
| `mingwX64` | Windows Native | GLFW + DX12/Vulkan (compiles, untested) |
| `android` | Android | Vulkan via wgpu4k-toolkit-android AAR |

Not every module declares every target — lower-level modules (prism-math, prism-core) have the broadest target coverage, while higher-level modules (prism-compose, prism-demo) target fewer platforms.

### wgpu4k Dependency

Prism depends on **wgpu4k 0.2.0-SNAPSHOT** (`io.ygdrasil:wgpu4k` + `io.ygdrasil:wgpu4k-toolkit`), built from source and published to Maven local. The `settings.gradle.kts` lists `mavenLocal()` first in dependency resolution — this is load-bearing.

A specific commit hash is tracked in `libs.versions.toml` (`wgpu4kCommit`), and CI uses a composite action (`.github/actions/setup-wgpu4k`) to clone, cache, and build wgpu4k reproducibly.

**JVM toolchain requirement:** wgpu4k's inline functions (e.g., `autoClosableContext`) require JVM target 25. Modules using wgpu4k must set `jvmToolchain(25)` in their `build.gradle.kts`. Without this, native FFI silently malfunctions — manifesting as a Rust "invalid texture" panic, not a Java compile error.

### Detekt JVM Target Split

Detekt's embedded Kotlin compiler maxes out at JVM target 22, but the project uses JVM 25 for wgpu4k FFI. The root build sets Detekt's `jvmTarget = "22"` and only wires `detektJvmMain` and `detektWasmJsMain` tasks into `check`. Metadata detekt tasks are excluded because they fail on JDK 25 as the host runtime. Since `detektJvmMain` transitively covers `commonMain` sources, coverage is preserved.

### CI Pipeline

GitHub Actions with two jobs:

- **`ci`** (ubuntu-latest, 30 min): `ktfmtCheck`, `detektMetadataCommonMain`, `detektJvmMain`, `jvmTest`
- **`apple`** (macos-15, 45 min): `macosArm64Test`, `iosSimulatorArm64Test`, XCFramework assembly (both `Prism` and `PrismDemo`), XCFramework header verification, artifact upload, xcodegen + xcodebuild iOS app build

Both jobs use the `.github/actions/setup-wgpu4k` composite action. The `apple` job additionally installs Rust cross-compilation targets for `aarch64-apple-ios` and `aarch64-apple-ios-sim`.

A separate **Release workflow** (`release.yml`, manual dispatch) builds a release XCFramework, packages it as a zip, computes the Swift Package checksum, updates `Package.swift`, commits, tags, and creates a GitHub Release with the attached artifact.

### iOS Distribution

The `prism-ios` module uses Kotlin's `XCFramework("Prism")` to build a static framework exporting all engine modules. Two slices: `iosArm64` (device) and `iosSimulatorArm64`. A `Package.swift` at the project root provides Swift Package Manager support with a binary target pointing to the release zip URL.

The `prism-ios-demo` Xcode project is generated via xcodegen (`project.yml`).

### Android

AGP 8.13.0, `compileSdk = 36`, `minSdk = 28`, `targetSdk = 36`. All library modules use `com.android.kotlin.multiplatform.library` with `kotlin { android {} }` DSL. Android rendering uses wgpu4k-toolkit-android AAR (JNI + native `libwgpu4k.so`) for Vulkan-backed rendering via `androidContextRenderer()`. Three upstream forks under `hyeons-lab` org (wgpu4k-native, wgpu4k, webgpu-ktypes) fix Android API 35+ compatibility issues (package relocation, ByteBuffer byte order, hidden API removal).

---

## 2. Module Architecture & Dependency Graph

### Layered Dependencies

```
prism-math                          (zero dependencies)
  └─► prism-core                    (engine lifecycle, timing, platform)
       ├─► prism-renderer           (GPU abstractions + wgpu4k impl)
       │    ├─► prism-scene         (scene graph nodes)
       │    ├─► prism-ecs           (entity-component-system)
       │    └─► prism-assets        (asset loading)
       ├─► prism-input              (keyboard/mouse/touch)
       └─► prism-audio              (stub interface)

prism-renderer
  └─► prism-native-widgets          (platform surfaces: GLFW, MTKView, Canvas)
       ├─► prism-compose            (Compose Multiplatform integration)
       └─► prism-flutter            (Flutter bridge: Android/iOS MethodChannel + Web WASM)

prism-ios                           (XCFramework aggregator, re-exports all modules)

prism-demo-core                     (depends on all engine modules)
```

### Module Responsibilities

| Module | Responsibility | ~Lines |
|--------|---------------|--------|
| `prism-math` | Vector/matrix math, quaternions, transforms | Pure computation, zero deps |
| `prism-core` | Engine coordinator, game loop, timing, platform abstraction, MVI store interface | Engine lifecycle |
| `prism-renderer` | GPU rendering interface + WgpuRenderer implementation, shaders, mesh/material/pipeline types | GPU abstraction |
| `prism-scene` | Scene graph with parent-child transform hierarchy | Node tree |
| `prism-ecs` | Entity-Component-System with query-based iteration | Game object model |
| `prism-input` | Cross-platform input event handling | Keyboard/mouse/touch |
| `prism-assets` | Asset lifecycle management with typed loaders | File I/O |
| `prism-audio` | Audio engine interface (stub) | Future work |
| `prism-native-widgets` | Platform-specific GPU surface creation (GLFW, MTKView, Canvas, AWT) | Surface layer |
| `prism-compose` | Compose Multiplatform integration with MVI state management | UI framework bridge |
| `prism-flutter` | Flutter bridge: PrismBridge + DemoStore MVI (Android/iOS), @JsExport WASM entry (web) | M11 |
| `prism-ios` | XCFramework aggregator for iOS/SPM distribution | Packaging |
| `prism-demo-core` | Demo app with PBR sphere grid across all platforms | Reference impl |

### Source Set Hierarchy

Each module follows KMP conventions. ~90% of code lives in `commonMain`. Platform-specific `actual` implementations live in target-specific source sets. The Kotlin default hierarchy template provides intermediate source sets (`nativeMain`, `appleMain`, `macosMain`, etc.) that share code across related platforms.

---

## 3. Core Engine Layer (prism-core)

### Engine

`Engine(config: EngineConfig)` is the central coordinator. It manages:

- **Subsystem registry**: `addSubsystem()`, `getSubsystem<T>()` (inline reified lookup)
- **Game loop**: Owned `GameLoop` instance, wired during `initialize()`
- **Time snapshot**: `time: Time` property updated each frame from the game loop

**Lifecycle:**
1. `initialize()` — idempotent (guarded by `initialized` flag). Calls `subsystem.initialize(engine)` on all registered subsystems. Wires game loop callbacks.
2. `shutdown()` — stops the game loop, shuts down subsystems in **reverse order** (dependencies clean up before their dependents).
3. Late registration: `addSubsystem()` called after `initialize()` immediately initializes the new subsystem.

The `subsystems` list is `@PublishedApi internal` — required because `getSubsystem<T>()` is an `inline reified` function that needs access to the backing collection.

### GameLoop

Implements the **fixed timestep accumulator** pattern (Gaffer on Games "Fix Your Timestep"):

```
accumulator += deltaTime
while (accumulator >= fixedTimeStep) {
    onFixedUpdate?.invoke(fixedTime)
    accumulator -= fixedTimeStep
}
onUpdate?.invoke(time)
onRender?.invoke(time)
```

**Anti-spiral-of-death clamp:** `deltaTime = (elapsedMillis / 1000f).coerceAtMost(0.25f)` — caps delta at 250ms to prevent a slow frame from causing a cascade of many fixed updates.

**Two operating modes:**

| Mode | Method | Use Case |
|------|--------|----------|
| Self-driven | `start()` | Blocking `while(running)` loop. JVM GLFW, native desktop. |
| Externally-driven | `startExternal()` + `tick()` | Platform manages the frame callback. WASM `requestAnimationFrame`, iOS `MTKViewDelegate`, Compose `withFrameNanos`. |

**Design decision:** Supporting both modes is essential because some platforms (WASM, iOS, Compose) do not allow blocking the main thread — they require the engine to yield control and be called back each frame.

### Time

Immutable `data class Time(deltaTime, totalTime, frameCount, fixedDeltaTime)`. A new instance is created each frame — no mutation. The `fixedDeltaTime` defaults to `1/60s`.

### Subsystem

```kotlin
interface Subsystem {
    val name: String
    fun initialize(engine: Engine)
    fun update(time: Time)
    fun shutdown()
}
```

Subsystems are the plugin mechanism for engine extensions. `InputManager`, `AssetManager`, `AudioEngine`, and `Renderer` all implement this interface.

### Platform

`expect object Platform` with two members: `name: String` and `currentTimeMillis(): Long`. Platform actuals:

| Platform | `currentTimeMillis()` Implementation |
|----------|--------------------------------------|
| JVM | `System.currentTimeMillis()` |
| Android | `System.currentTimeMillis()` |
| WASM | `js("Date.now()").toLong()` |
| iOS/macOS | `NSDate().timeIntervalSince1970 * 1000` |
| Linux | `gettimeofday` via C interop |
| Windows | `GetSystemTimeAsFileTime` (100ns intervals since 1601 → Unix ms) |

### Store (MVI Interface)

```kotlin
interface Store<State, Event> {
    val state: StateFlow<State>
    fun dispatch(event: Event)
}
```

All UI-engine communication flows through stores. Reducers are pure functions, state is immutable, events drive all transitions. `StateFlow` provides thread-safe state observation with automatic deduplication.

---

## 4. Math Layer (prism-math)

### Vector Types

`Vec2`, `Vec3`, `Vec4` are `data class` types with operator overloads (`+`, `-`, `*`, `/`, `unaryMinus`).

| Type | Fields | Notable Operations |
|------|--------|--------------------|
| `Vec2` | `x, y` | `dot`, `length`, `normalize`, `distanceTo` |
| `Vec3` | `x, y, z` | All of Vec2 + `cross`, `lerp`, component-wise `times(Vec3)` |
| `Vec4` | `x, y, z, w` | `dot`, `length`, `normalize`, `toVec3()` |

**Constants:** `ZERO`, `ONE`, `UP` (0,1,0), `DOWN`, `FORWARD` (0,0,-1), `BACK` (0,0,1), `RIGHT` (1,0,0), `LEFT`. Convention: right-handed coordinate system, Z points out of screen.

**Design decision:** Data classes rather than value classes. Value classes in KMP have limitations (no multi-field, restricted interface implementation across backends). Data classes provide structural equality, `copy()`, and destructuring. The trade-off is heap allocation per vector — acceptable for the current phase.

**Normalization safety:** All `normalize()` methods guard against near-zero length with `EPSILON = 1e-7f`, returning `ZERO` instead of NaN/Infinity.

### Mat3 and Mat4

Regular classes backed by `FloatArray` in **column-major** order (`data[col * N + row]`). This matches GPU convention (WebGPU, Metal, Vulkan) — the array can be uploaded directly to a uniform buffer without transposition.

Not `data class` — manual `equals()` via `contentEquals()` and `hashCode()` via `contentHashCode()` to compare array contents correctly.

**Mat4 factories:**
- `identity()`, `translation(Vec3)`, `scale(Vec3)`
- `rotationX/Y/Z(radians)`
- `lookAt(eye, target, up)` — right-handed view matrix
- `perspective(fovY, aspect, near, far)` — right-handed, **WebGPU depth range [0, 1]** (not OpenGL [-1, 1])
- `orthographic(left, right, bottom, top, near, far)` — same [0, 1] depth range

### Quaternion

`data class Quaternion(x, y, z, w)` — identity is `(0, 0, 0, 1)`.

Key operations:
- **Hamilton product** (`*`): `this * other` applies `other` first, then `this`
- **`rotateVec3(v)`**: Optimized Rodrigues formula (`t = 2 * cross(q.xyz, v); result = v + w*t + cross(q.xyz, t)`) — 15 multiplies vs 28+ for naive q*v*q^-1
- **`slerp(a, b, t)`**: Full SLERP with short-path handling and linear interpolation fallback for near-parallel quaternions (dot > 0.9995)
- **`fromAxisAngle`**, **`fromEuler`** (intrinsic Tait-Bryan ZYX)

### Transform

`data class Transform(position: Vec3, rotation: Quaternion, scale: Vec3)`.

- `toModelMatrix()` computes `Translation * Rotation * Scale` — allocates three intermediate Mat4 instances per call (not cached)
- `forward`, `right`, `up` — computed via `rotation.rotateVec3()` on basis vectors, per-access
- Immutable operations: `translate()`, `rotate()`, `scaledBy()` return new instances via `copy()`

---

## 5. ECS Layer (prism-ecs)

### Storage Model

```kotlin
class World {
    private var nextId: UInt = 1u
    @PublishedApi internal val entities: MutableMap<Entity, MutableMap<String, Component>> = mutableMapOf()
    private val systems: MutableList<System> = mutableListOf()
}
```

**Entity-centric sparse storage:** A nested `Map<Entity, Map<String, Component>>`. Components are keyed by `::class.simpleName`.

**Design decision:** Simplicity-first. No archetype management, no migration on component add/remove. Adequate for the current scene complexity (<100 entities). For scale (>1000 entities), archetype-based storage would provide significantly better cache locality and query performance.

### Entity

`data class Entity(val id: UInt)` — value-typed identifier. IDs start at 1 and increment monotonically. No recycling, no generation counter.

### Component

`interface Component` — pure marker interface. Any implementing class can be attached to entities.

**Built-in components:**

| Component | Key Fields | Notes |
|-----------|-----------|-------|
| `TransformComponent` | `position: Vec3`, `rotation: Quaternion`, `scale: Vec3` | Mutable `var` fields for in-place mutation. `toTransform(): Transform` bridge. |
| `MeshComponent` | `mesh: Mesh?` | Nullable — mesh may not be GPU-uploaded yet |
| `MaterialComponent` | `material: Material?` | Nullable |
| `CameraComponent` | `camera: Camera` | Wraps renderer `Camera` |
| `LightComponent` | `lightType`, `color`, `intensity`, `range`, `direction` | Mirrors `LightNode` in prism-scene |

### Queries

- `inline fun <reified T> query()` — returns `List<Pair<Entity, T>>`. Iterates ALL entities, checks for component key. **O(N) full scan**.
- `query2(class1, class2)` — two-component query, returns `List<Entity>` (not components). Less ergonomic — caller must follow up with `getComponent()`.

### System

```kotlin
interface System {
    val name: String
    val priority: Int  // default 0, sorted ascending
    fun initialize(world: World)
    fun update(world: World, time: Time)
    fun shutdown()
}
```

Systems run in priority order: `TransformSystem` (priority 0) before `RenderSystem` (priority 100).

**Built-in systems:**
- `TransformSystem` (priority 0): Stub — intended for parent-child transform propagation, not yet implemented.
- `RenderSystem` (priority 100): Drives the full render pass each frame (see [Rendering Layer](#6-rendering-layer-prism-renderer)).

---

## 6. Rendering Layer (prism-renderer)

### Renderer Interface

`Renderer` extends `Subsystem` with the full rendering API:

```
beginFrame() → beginRenderPass(descriptor) → setCamera() → bindPipeline() →
  [setMaterialColor() → drawMesh(mesh, transform)]* → endRenderPass() → endFrame()
```

Plus resource creation: `createBuffer()`, `createTexture()`, `createShaderModule()`, `createPipeline()`, `uploadMesh()`.

### WgpuRenderer

Single implementation using wgpu4k. Constructor takes `WGPUContext` (holds device, surface, renderingContext) and optional `surfacePreConfigured: Boolean` (true for AWT Canvas integration where the surface is already configured by `PrismPanel`).

**Initialization:**
1. Configure surface (unless `surfacePreConfigured`): query supported alpha modes, prefer `Inherit`, fall back to `Opaque`
2. Create depth texture (`Depth24Plus`, recreated on resize)
3. Create scene uniform buffer (96 bytes): VP matrix, camera position, ambient, light count
4. Create object uniform buffer (112 bytes): model matrix (64 bytes) + padded normalMatrix (mat3x3, 48 bytes with std140 padding)
5. Create material uniform buffer (48 bytes): baseColor, metallic, roughness, emissive, occlusion, texture flags
6. Create light storage buffer (1024 bytes): up to 16 lights at 64 bytes each
7. Create environment uniform buffer (16 bytes): env intensity, max mip level
8. Create default 1×1 textures: white (albedo/metallicRoughness/occlusion), flat-normal (128,128,255,255), black (emissive)
9. Create default bind groups for all 4 groups; IBL textures populated by `initializeIbl()`

### GPU Resource Model

| Resource | Lifetime | Size | Purpose |
|----------|----------|------|---------|
| Scene uniform buffer | Long-lived | 96 bytes | VP matrix, camera pos, light count, ambient |
| Object uniform buffer | Long-lived | 112 bytes | Model matrix (64B) + padded normalMatrix mat3x3 (48B) |
| Material uniform buffer | Long-lived | 48 bytes | PBR params: color, metallic, roughness, emissive |
| Light storage buffer | Long-lived | 1024 bytes | Up to 16 lights at 64 bytes each |
| Environment uniform buffer | Long-lived | 16 bytes | IBL intensity, max mip level |
| HDR color texture | Recreated on resize | surface W×H (RGBA16Float) | HDR intermediate render target |
| Depth texture | Recreated on resize | surface W×H | Depth testing |
| IBL textures (3) | Per scene (long-lived) | Varies | BRDF LUT (256×256), irradiance cubemap (16px), prefiltered env (32px, 5 mips) |
| Vertex/index buffers | Per mesh, long-lived | Varies | Geometry |
| Command encoder | Per frame (ephemeral) | — | GPU command recording |
| Render pass encoder | Per frame (ephemeral) | — | Draw call recording |
| Bind groups (4) | Per frame | — | Scene / object / material / environment bindings |

Ephemeral resources are tracked via wgpu4k's `AutoClosableContext` and cleaned up at frame end.

### Frame Flow

```
beginFrame()
  └─ AutoClosableContext {} opened
  └─ CommandEncoder created + .bind()

beginRenderPass(descriptor)
  └─ If hdrEnabled: color attachment = HDR texture (RGBA16Float, clear on load)
     else: color attachment = swapchain texture (clear on load)
  └─ depth-stencil attachment (clear depth=1.0)
  └─ RenderPassEncoder created + pipeline set

setCameraPosition(pos) + setCamera(camera) + setLights(lights)  [via RenderSystem]
  └─ queue.writeBuffer(sceneUniformBuffer, vpMatrix + cameraPos + lightCount + ambient, 96 bytes)
  └─ queue.writeBuffer(lightStorageBuffer, lightArray, numLights × 64 bytes)

setMaterial(material)  [per draw call, before drawMesh]
  └─ queue.writeBuffer(pbrMaterialUniformBuffer, pbr params, 48 bytes)
  └─ renderPass.setBindGroup(2u, defaultMaterialBindGroup) [group 2: PBR params + default textures]

drawMesh(mesh, transform)  [per draw call]
  └─ queue.writeBuffer(objectUniformBuffer, modelMatrix + paddedNormalMatrix, 112 bytes)
  └─ renderPass.setBindGroup(1u, objectBindGroup)     [group 1: model + normalMatrix]
  └─ renderPass.setVertexBuffer(0, mesh.vertexBuffer) [48 bytes/vertex: pos+normal+uv+tangent]
  └─ renderPass.drawIndexed(indexCount)
  Note: groups 0 (scene) and 3 (env) are set once in beginRenderPass(), not per draw call.

endRenderPass()
  └─ renderPass.end()

Tone mapping pass (if hdrEnabled)
  └─ New RenderPassEncoder targeting swapchain texture (clear on load)
  └─ toneMapPipeline (fullscreen triangle, no vertex buffer)
  └─ Samples HDR texture, applies Khronos PBR Neutral tone mapping → LDR output
  └─ renderPass.end()

endFrame()
  └─ commandEncoder.finish() → queue.submit()
  └─ surface.present()
  └─ AutoClosableContext closed (ephemeral resources freed)
```

**Design decision:** `surface.present()` is called **before** the `AutoClosableContext` closes ephemeral resources. The actual order in `endFrame()` is: `encoder.finish()` → `queue.submit()` → `surface.present()` → `ctx.close()`. Closing the context (and freeing ephemeral handles) before presenting would cause use-after-free.

### WGSL Shaders

Embedded as string constants in `Shaders.kt`. Two shader pairs: `PBR_VERTEX_SHADER` / `PBR_FRAGMENT_SHADER` for the main PBR pass, and `TONE_MAP_VERTEX_SHADER` / `TONE_MAP_FRAGMENT_SHADER` for the fullscreen HDR-to-LDR blit.

**Explicit 4-group bind group layout:**
```wgsl
@group(0) @binding(0) var<uniform> scene : SceneUniforms;   // 96 bytes: VP, cameraPos, lightCount, ambient
@group(0) @binding(1) var<storage, read> lights : LightArray; // 1024 bytes: up to 16 lights

@group(1) @binding(0) var<uniform> object : ObjectUniforms; // 128 bytes: model + padded normalMatrix

@group(2) @binding(0) var<uniform> material : MaterialUniforms; // 48 bytes: PBR params + texture flags
@group(2) @binding(1) var materialSampler : sampler;            // one shared sampler for all 5 PBR slots
@group(2) @binding(2..6): baseColorTex, metallicRoughnessTex, normalTex, occlusionTex, emissiveTex

@group(3) @binding(0) var<uniform> env : EnvironmentUniforms;  // 16 bytes: envIntensity, maxMipLevel
@group(3) @binding(1) var envSampler : sampler;                 // one shared clamp sampler
@group(3) @binding(2) var irradianceMap : texture_cube<f32>;
@group(3) @binding(3) var prefilteredEnv : texture_cube<f32>;
@group(3) @binding(4) var brdfLut : texture_2d<f32>;
```

**PBR vertex stage** (`vs_main`): Transforms position to clip space via `scene.viewProjection * object.model`. Transforms normal via `object.normalMatrix` (inverse-transpose mat3). Outputs world position, world normal, UV, and world tangent as varyings.

**PBR fragment stage** (`fs_main`): Cook-Torrance BRDF — GGX NDF (`D`), Smith-GGX geometry (`G`), Fresnel-Schlick (`F`). Loops over `scene.numLights` directional/point/spot lights. Adds IBL diffuse (irradiance cube sample) and specular (split-sum: prefiltered env + BRDF LUT). Flag-based texture sampling for all 5 PBR slots. Normal mapping via TBN matrix when `hasNormalTexture` flag is set. Outputs linear HDR colors (no clamping).

**Tone map vertex stage**: Generates fullscreen triangle from `vertex_index` — no vertex buffer needed.

**Tone map fragment stage**: Samples HDR texture, applies Khronos PBR Neutral tone mapping to map linear HDR → [0,1] LDR.

### Vertex Layout

Position (Float3, 12 bytes) + Normal (Float3, 12 bytes) + UV (Float2, 8 bytes) + Tangent (Float4, 16 bytes) = **48 bytes/vertex**. The tangent W component encodes the bitangent handedness sign (`+1` or `−1`) for TBN matrix reconstruction in the shader.

### Pipeline Configuration

TriangleList topology, back-face culling (CCW front face), Depth24Plus depth test with `Less` comparison, multisample count = 1, opaque blend mode.

### Mesh Factories

- `Mesh.triangle()` — 3 vertices, 3 indices
- `Mesh.quad()` — 4 vertices, 6 indices (2 triangles)
- `Mesh.cube()` — 24 vertices (4 per face for distinct normals), 36 indices, Uint32 index format
- `Mesh.sphere(stacks, slices, radius)` — UV sphere with tangents; `(stacks+1)×(slices+1)` vertices, `stacks×slices×6` indices. Seam duplication for correct UV wrapping. Tangent = ∂position/∂u normalized, W = +1.0.

---

## 7. Scene Graph (prism-scene)

### Node Hierarchy

`open class Node(name)` with parent-child relationships:

- `transform: Transform` — local transform (mutable)
- `parent: Node?` — set automatically by `addChild`/`removeChild`
- `children: List<Node>` — read-only view
- `isEnabled: Boolean` — disabled nodes (and subtrees) are skipped in `update()`

**`worldTransformMatrix()`**: Recursive traversal up the parent chain, computing `parent.worldTransformMatrix() * localMatrix`. No caching — O(depth) per call with intermediate Mat4 allocations.

**`addChild()`**: Detaches from existing parent before adopting, preventing dual parentage.

### Typed Nodes

- `MeshNode(name, mesh, material)` — renderable geometry
- `CameraNode(name, camera)` — synchronizes `Camera` from transform on each `update()` (position, target from forward vector, up vector)
- `LightNode(name, lightType, color, intensity, ...)` — light parameters

### Relationship to ECS

The scene graph and ECS are **parallel abstractions**. The demo app and all current rendering use the ECS path (`World` + `RenderSystem`). The scene graph provides an alternative API but is not currently wired into the rendering pipeline. They can coexist — a future bridge could sync scene graph nodes to ECS entities.

---

## 8. Platform Surface Layer (prism-native-widgets)

### PrismSurface

`expect class PrismSurface` with `attach(engine)`, `detach()`, `resize(width, height)`, `width`, `height`.

Each platform also provides a `suspend fun createPrismSurface(...)` factory that performs async GPU initialization (adapter request, surface configuration) and returns a ready-to-use surface.

**Design decision:** The suspend factory + expect/actual pattern lets each platform do platform-specific async GPU init before returning. The caller provides the coroutine context — no `runBlocking` inside PrismSurface.

### Platform Actuals

| Platform | Surface Source | Context Type | Factory Signature |
|----------|---------------|-------------|-------------------|
| JVM (GLFW) | `glfwContextRenderer(w, h, title)` | `GLFWContext` | `createPrismSurface(width, height, title)` |
| iOS | `iosContextRenderer(mtkView, w, h)` | `IosContext` | `createPrismSurface(view: MTKView, width, height)` |
| macOS Native | `glfwContextRenderer(w, h, title)` | `GLFWContext` | `createPrismSurface(width, height, title)` |
| WASM | `canvasContextRenderer(canvas, w, h)` | `CanvasContext` | `createPrismSurface(canvas: HTMLCanvasElement, width, height)` |
| Linux Native | `glfwContextRenderer(w, h, title)` | `GLFWContext` | `createPrismSurface(width, height, title)` |
| MinGW Native | `glfwContextRenderer(w, h, title)` | `GLFWContext` | `createPrismSurface(width, height, title)` |
| Android | `androidContextRenderer(holder, w, h)` | `AndroidContext` | `createPrismSurface(holder: SurfaceHolder, width, height)` |

All GLFW-based surfaces start with a hidden window. Callers must call `glfwShowWindow()` after setup.

### PrismPanel (JVM AWT Integration)

`PrismPanel` is a heavyweight AWT `Canvas` subclass for embedding Metal rendering in Swing/Compose Desktop. It is the most complex platform implementation.

**Surface creation flow:**
1. `addNotify()` (Canvas peer created) triggers `initializeWgpuSurface()`
2. Creates WGPU instance, requests adapter and device (via `runBlocking`)
3. Platform-specific native handle extraction for surface creation
4. Configures surface, assembles `WGPUContext`, fires `onReady` callback

**macOS metal layer integration uses the sublayer approach:**
1. Resolve NSView pointer via JNA `Native.getComponentPointer()` or deep AWT reflection chain
2. `nsView.setWantsLayer(true)` — make layer-backed
3. Get window's backing `CALayer`, create `CAMetalLayer`, add as **sublayer** (never `setLayer`)
4. Set sublayer frame to Canvas bounds with `CATransaction` animation suppression

`AwtRenderingContext` wraps `RenderingContext` to read Canvas dimensions instead of GLFW window size.

---

## 9. Compose Integration (prism-compose)

### MVI Architecture

All Compose UI-engine communication flows through an MVI store:

```
EngineStore : Store<EngineState, EngineStateEvent>
```

**EngineState** (immutable data class): `time: Time`, `fps: Float`, `surfaceWidth/Height: Int`, `isInitialized: Boolean`.

**EngineStateEvent** (sealed interface): `Initialized`, `Disposed`, `SurfaceResized(width, height)`, `FrameTick(time, fps)`.

**Reducer:** Pure function inside `EngineStore.dispatch()` — updates `MutableStateFlow<EngineState>` via `_state.update { reduce(it, event) }`.

Two constructors: one creates and owns an Engine (calls shutdown on dispose), one wraps an existing Engine (external ownership).

### Composable API

- **`rememberEngineStore(config)`** — lifecycle-aware store creation. `DisposableEffect` initializes engine on enter, shuts down on dispose.
- **`rememberExternalEngineStore(engine)`** — wraps existing engine, does not manage lifecycle.
- **`PrismView(store, modifier)`** — expect/actual composable embedding the GPU surface:
  - **JVM**: `SwingPanel` wrapping `PrismPanel`. Render loop via `withFrameNanos` (Compose vsync-synchronized). Smoothed FPS (90% old + 10% current EMA).
  - **iOS/WASM**: Stubs (logging only — real rendering handled outside PrismView by platform-specific code).
- **`PrismOverlay(store, modifier, content)`** — `Box` overlaying arbitrary Compose content on PrismView. Only shows PrismView after `Initialized` event.
- **`PrismTheme(store, content)`** — `CompositionLocalProvider` providing Engine via `LocalEngine`.

**Design decision:** MVI over callbacks — all UI-engine communication goes through the store. No direct mutation. Thread-safe via StateFlow.

---

## 10. Demo Architecture (prism-demo-core)

### Shared Logic (commonMain)

**`DemoScene`** — data holder for `Engine`, `World`, `WgpuRenderer`, `cameraEntity`. Two tick methods:
- `tick(deltaTime, elapsed, frameCount)` — calls `world.update(time)`
- `tickWithAngle(deltaTime, elapsed, frameCount, angle)` — kept for iOS/macOS platform compat; angle is unused in the static sphere grid scene

**`createDemoScene(wgpuContext, width, height, surfacePreConfigured)`** factory:
1. Creates `WgpuRenderer`, enables HDR (`hdrEnabled = true`), calls `initializeIbl()`, adds to `Engine`, initializes
2. Creates `World`, adds `RenderSystem`
3. Creates camera entity: position `(0, 0, 12)`, target `(0,0,0)`, FOV 45, near 0.1, far 100
4. Creates directional light (warm white 1.0/0.95/0.8, intensity 2.0, direction −0.5/−1/−0.5) and point light (cool white 0.8/0.9/1.0, intensity 80, range 20, at position 5/5/5)
5. Creates 7×7 sphere grid using a shared `Mesh.sphere()` instance — metallic 0→1 on X axis, roughness 0.04→1.0 on Y axis, spacing 1.5 units
6. Initializes World (triggers RenderSystem → PBR shader compilation + 4-group pipeline creation)

**`DemoStore`** — `Store<DemoUiState, DemoIntent>` managing rotation speed (default 45 deg/s), pause state, metallic (0.5), roughness (0.5), envIntensity (1.0), and FPS display.

### Platform Entry Points

All converge on `DemoScene`:

| Platform | Entry Point | Loop Mechanism |
|----------|------------|----------------|
| JVM GLFW | `GlfwMain.kt` | `while(running)` + `glfwPollEvents()` |
| JVM Compose | `ComposeMain.kt` | `JFrame` + `PrismPanel` + Compose `withFrameNanos` |
| WASM | `Main.kt` | `GlobalScope.launch` + recursive `requestAnimationFrame` |
| iOS Native | `IosDemoController.kt` | `MTKViewDelegateProtocol.drawInMTKView` (60fps Metal display link) |
| iOS Compose | `ComposeIosEntry.kt` | `UIKitView` + `ComposeRenderDelegate` |
| Android | `PrismDemoActivity.kt` | `Choreographer.FrameCallback` (vsync-aligned) |
| macOS Native | `MacosDemoMain.kt` | `while(running)` + `glfwPollEvents()` + AppKit `NSPanel` controls |
| Flutter Android | `PrismPlatformView.kt` | `SurfaceView` + `Choreographer.FrameCallback` via MethodChannel |
| Flutter iOS | `PrismPlatformView.swift` | `MTKView` + `configureDemo` via MethodChannel |
| Flutter Web | `FlutterWasmEntry.kt` | `@JsExport` + recursive `requestAnimationFrame` via Dart JS interop |

### ECS Rendering Pipeline (End-to-End)

1. **Setup**: `World.addSystem(RenderSystem(renderer))` → `World.initialize()` → `RenderSystem.initialize()` compiles PBR WGSL shaders, creates explicit 4-group pipeline layout
2. **Each frame**: `world.update(time)` → `RenderSystem.update()`:
   - `beginFrame()` → `beginRenderPass()` (renders to HDR texture when `hdrEnabled`; cornflower blue clear)
   - Query `CameraComponent` → `setCamera()` (writes VP matrix) + `setCameraPosition()` (writes camera position to scene uniforms)
   - Query `LightComponent` entities → build `List<LightData>` → `setLights()` (writes light array + light count)
   - For each `MeshComponent` entity: lazy-upload mesh, compute model + normal matrices from `TransformComponent`, `setMaterial()`, `drawMesh()`
   - `endRenderPass()` → tone-map pass (HDR → LDR) → `endFrame()` (submit + present)
3. **Scene**: static PBR sphere grid — no per-frame transform mutations; all motion is driven by scene tick without animation

---

## 11. Platform-Specific Integration Patterns

### macOS AWT + Metal

- **Sublayer approach** — never use `nsView.setLayer(metalLayer)` (replaces window's backing layer, breaks AWT). Instead: `nsView.setWantsLayer(true)` → get backing layer → `addSublayer(metalLayer)` → `setFrame` to Canvas bounds.
- **CATransaction animation suppression** — `CALayer.setFrame:` triggers a 0.25s implicit animation. Wrap in `CATransaction.begin()`/`setDisableActions(true)`/`commit()`.
- **ObjCBridge** — JNA `objc_msgSend` calls for `layer`, `addSublayer:`, `setFrame:`, `removeFromSuperlayer`. On arm64, CGRect (4 doubles) is passed in FP registers d0-d3; JNA `Function.invoke` with `Double` args handles this.
- **`-XstartOnFirstThread`** — required on macOS for JNA `Native.getComponentPointer(canvas)` to return non-null.

### iOS

- **Lazy wgpu init** — done in `viewDidAppear`, not `viewDidLoad`, to avoid double GPU memory allocation. MTKView starts paused, unpaused after init.
- **Thread safety** — Metal render delegate calls `tickDemoFrame` on the Metal thread. FPS dispatch to Compose/UIKit uses `NSOperationQueue.mainQueue.addOperationWithBlock` for thread-safe state updates.
- **SharedDemoTime** — thread-safe elapsed time tracker using `Mutex.tryLock()` with stale-value fallback. Handles pause/resume and speed changes without angle jumps via offset accumulation.

### WASM

- **No `runBlocking`** — must use `GlobalScope.launch` for top-level async entry points.
- **`@JsFun` externals** — `performance.now()` for high-resolution timing, DOM element access.
- **`web.html.HTMLCanvasElement`** — wgpu4k uses this type, NOT `org.w3c.dom.HTMLCanvasElement`.

### Android

- **SurfaceView + Choreographer** — `SurfaceHolder.Callback` for surface lifecycle, `Choreographer.FrameCallback` for vsync-aligned render loop (like `requestAnimationFrame` on web).
- **wgpu4k-toolkit-android AAR** — provides `androidContextRenderer(surfaceHolder, width, height)` which creates Vulkan surface from `ANativeWindow`. No PanamaPort needed.
- **sRGB surface handling** — Android Vulkan preferred format is `RGBA8UnormSrgb`. WgpuRenderer detects sRGB format and applies `srgbToLinear()` conversion to clear color and material color to prevent double-gamma encoding.
- **Three upstream forks** — required for API 35+ (Android 15+): wgpu4k-native (relocate `java.lang.foreign` shims), wgpu4k (fix ByteBuffer reflection), webgpu-ktypes (fix `ByteBuffer.allocateDirect()` byte order to native order).
- **Known limitation** — API 35+ `java.lang.foreign` boot classpath shadowing. Worked around via package relocation in wgpu4k-native fork.

### macOS Native

- GLFW creates a hidden window; `glfwShowWindow()` called after setup.
- AppKit `NSPanel` for floating controls (speed slider, pause button) using `@ObjCAction` annotated handlers.
- `CACurrentMediaTime()` for timing, `glfwPollEvents()` dispatches AppKit events.

---

## 12. Performance Characteristics & Future Improvements

### Current Performance Profile

| Area | Current State | Bottleneck |
|------|--------------|------------|
| **Rendering** | Single pipeline, single bind group, no batching | Material color written per `drawMesh`, bind group recreated per `bindPipeline` |
| **ECS queries** | O(N) full entity scan | Map-of-maps, no indexing. Fine for <1000 entities |
| **Math** | Pure Kotlin Float32, scalar operations | No SIMD, heap-allocated vectors |
| **Transform** | `toModelMatrix()` allocates 3 intermediate Mat4 per call | Not cached |
| **Scene graph** | `worldTransformMatrix()` recursive with no caching | O(depth) per node, no dirty flags |
| **Shading** | PBR Cook-Torrance BRDF, up to 16 lights, IBL | 4-bind-group layout; bind groups recreated per draw, no caching |
| **Culling** | None | Every entity is drawn every frame |
| **Textures** | Depth + IBL (BRDF LUT, irradiance, prefiltered env) | PBR texture slots ready; no loaded albedo/normal/metallicRoughness textures yet |
| **Memory** | Mesh CPU data retained after GPU upload | Can release after `uploadMesh()` |

### Planned Improvements

**Rendering:**
- Batch by material to reduce `queue.writeBuffer` calls
- Cache bind groups by pipeline + buffer combination
- Multiple render passes (shadow maps, post-processing)
- Instanced rendering for repeated geometry
- Frustum and occlusion culling

**Materials:**
- Texture loading pipeline (albedo, normal, metallic-roughness) from glTF assets or disk
- Spotlight support in PBR shader
- Runtime IBL from loaded HDR environment maps

**Assets:**
- glTF 2.0 loading (geometry, materials, animations)
- Actual texture decoding (PNG, JPEG)
- OBJ mesh parsing

**ECS:**
- Archetype-based storage for O(1) component access and cache-friendly iteration
- Type-safe multi-component queries
- Component change detection / dirty flags

**Math:**
- Platform-specific SIMD for hot paths (JVM vector API, native intrinsics)
- Object pooling for temporary vectors/matrices in tight loops

**Platform:**
- Frame rate limiting in GameLoop
- Transform hierarchy in ECS (TransformSystem)
- Compute shader support
