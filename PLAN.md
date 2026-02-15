  # Prism Engine — Project Plan

## 1. Project Overview

**Prism** is a modular, cross-platform 3D game engine built with Kotlin Multiplatform (KMP). It targets desktop (JVM), web (WASM/JS), mobile (iOS, Android), and native (macOS) — with a single shared codebase. The rendering backend uses **wgpu** (via wgpu4k) for cross-platform GPU access (Vulkan, Metal, DX12, WebGPU under the hood).

### Target Platforms
| Platform | Runtime | GPU Backend | Notes |
|----------|---------|-------------|-------|
| macOS Desktop | JVM | Metal (via wgpu) | JDK 21+ |
| Windows Desktop | JVM | DX12/Vulkan (via wgpu) | JDK 21+ |
| Linux Desktop | JVM | Vulkan (via wgpu) | JDK 21+ |
| Web | WASM/JS | WebGPU | Browser with WebGPU support |
| iOS | Kotlin/Native | Metal (via wgpu) | iOS 13.0+ |
| Android | Kotlin/Android | Vulkan (via wgpu + PanamaPort) | API 26+ (Android 8.0+) |
| macOS Native | Kotlin/Native | Metal (via wgpu) | macOS 11.0+ |

### Tech Stack
- **Language:** Kotlin 2.3.0, Kotlin Multiplatform
- **Build:** Gradle 9.1+ with Kotlin DSL
- **GPU:** wgpu4k (WebGPU bindings for Kotlin) — `io.ygdrasil:wgpu4k`
- **Shaders:** WGSL (WebGPU Shading Language)
- **UI Framework:** Jetpack Compose Multiplatform 1.10.0
- **Windowing (JVM):** GLFW via wgpu4k's glfw-native
- **Android FFI:** PanamaPort (Foreign Function & Memory API for Android 8.0+)
- **Async:** kotlinx-coroutines 1.10.2
- **Serialization:** kotlinx-serialization 1.9.0
- **Logging:** Kermit 2.0.8
- **Testing:** Kotest 6.0.7

---

## 2. Module Architecture

```
prism/
├── prism-math          # Vec2/3/4, Mat3/4, Quaternion, Transform
├── prism-core          # Engine, GameLoop, Time, Subsystem, Platform
├── prism-renderer      # Renderer interface + WgpuRenderer implementation
├── prism-scene         # Scene graph: Node, Scene, MeshNode, CameraNode, LightNode
├── prism-ecs           # Entity-Component-System: World, Entity, Component, System
├── prism-input         # InputManager, keyboard/mouse/touch events
├── prism-assets        # AssetManager, loaders (mesh, shader, texture)
├── prism-audio         # AudioEngine interface (stub, future implementation)
├── prism-native-widgets# PrismSurface — native rendering surfaces per platform
├── prism-compose       # PrismView, PrismOverlay, EngineState — Compose integration
├── prism-flutter       # Flutter bridge (minimal, future)
└── prism-demo          # Demo app: rotating cube with camera and lighting
```

### Module Dependency Graph
```
prism-math
  └─► prism-core
       └─► prism-renderer (wgpu4k lives here)
            ├─► prism-scene
            ├─► prism-ecs
            ├─► prism-assets
            └─► prism-native-widgets
                 └─► prism-compose
                      └─► prism-demo
prism-core
  └─► prism-input
  └─► prism-audio
```

---

## 3. Current State (What Exists)

| Module | Status | Lines | Notes |
|--------|--------|-------|-------|
| prism-math | **Complete** | ~450 | Vec2/3/4, Mat3/4, Quaternion, Transform, tests |
| prism-core | **Complete** | ~300 | Engine, GameLoop, Time, Subsystem, EngineConfig, Platform |
| prism-ecs | **Complete** | ~400 | World, Entity, Component, System, built-in components/systems |
| prism-renderer | **Interface only** | ~600 | All abstractions defined, no GPU backend |
| prism-scene | **Complete** | ~250 | Node hierarchy, Scene, CameraNode, MeshNode, LightNode |
| prism-input | **Complete** | ~200 | InputManager, events, key/mouse/touch |
| prism-assets | **Complete** | ~300 | AssetManager, loaders, FileReader per platform |
| prism-audio | **Stub** | ~100 | Interface + StubAudioEngine only |
| prism-compose | **Stub** | ~250 | Composables defined, platform actuals are empty |
| prism-native-widgets | **Stub** | ~350 | PrismSurface defined, all actuals empty |
| prism-demo | **Complete** | ~200 | DemoApp scene setup, waiting for renderer |
| prism-flutter | **Minimal** | ~80 | Basic bridge, not a priority |

**Total: ~4,200 lines of Kotlin**

---

## 4. What We're Building Now: GPU Rendering Backend

### Goal
Implement a working `WgpuRenderer` that can render the demo scene (rotating cube with camera and directional light) to a window on JVM/macOS, then extend to other platforms.

### wgpu4k API (from real examples)

The wgpu4k library follows WebGPU conventions. Key namespace: `io.ygdrasil.webgpu.*`

**Core types:**
- `WGPUContext` — wraps device + rendering context
- `GPUDevice` — the GPU device
- `GPUQueue` — command submission queue
- `GPURenderPipeline` — compiled render pipeline
- `GPUBuffer` — GPU memory buffer
- `GPUBindGroup` — resource bindings (uniforms, textures)
- `RenderingContext` — surface + texture format

**Resource creation pattern:**
```kotlin
// Buffers
device.createBuffer(BufferDescriptor(size, usage, mappedAtCreation))

// Shaders (WGSL)
device.createShaderModule(ShaderModuleDescriptor(code = wgslSource))

// Pipelines
device.createRenderPipeline(RenderPipelineDescriptor(
    vertex = VertexState(module, entryPoint, buffers),
    fragment = FragmentState(module, entryPoint, targets),
    primitive = PrimitiveState(topology, cullMode),
    depthStencil = DepthStencilState(format, depthCompare, depthWriteEnabled)
))

// Bind groups (for uniforms)
device.createBindGroup(BindGroupDescriptor(
    layout = pipeline.getBindGroupLayout(0u),
    entries = listOf(BindGroupEntry(binding, BufferBinding(buffer)))
))
```

**Render loop pattern:**
```kotlin
// 1. Write uniforms
device.queue.writeBuffer(uniformBuffer, 0u, ArrayBuffer.of(matrixData))

// 2. Encode commands
val encoder = device.createCommandEncoder().bind()
encoder.beginRenderPass(renderPassDescriptor) {
    setPipeline(pipeline)
    setBindGroup(0u, uniformBindGroup)
    setVertexBuffer(0u, verticesBuffer)
    draw(vertexCount)
    end()
}

// 3. Submit
device.queue.submit(listOf(encoder.finish().bind()))
```

**AutoClosable pattern:** All GPU resources use `.bind()` within `AutoClosableContext` for automatic cleanup.

### wgpu4k Setup Requirements
- **Maven group:** `io.ygdrasil`
- **Key artifacts:** `wgpu4k` (high-level), `wgpu4k-native` (low-level FFI)
- **JDK:** 21+ required (JDK 22+ preferred for native FFI on desktop)
- **Gradle:** 9.1+
- **Maven repo:** Maven Central (for stable releases like 0.1.1)
- **GLFW integration:** `io.ygdrasil:glfw-native:0.0.2` for desktop windowing
- **Android support:**
  - Requires PanamaPort: `com.github.vova7878:PanamaPort` for FFI
  - Android API 26+ (Android 8.0+)
  - Android Gradle Plugin 8.6.0+
  - compileSdk 35+
- **Gradle properties needed:**
  ```
  kotlin.mpp.enableCInteropCommonization=true
  kotlin.native.ignoreDisabledTargets=true
  ```

---

## 5. Implementation Plan

### Phase 1: Build System Setup
**Files to modify:**
- `gradle/libs.versions.toml` — add wgpu4k, glfw-native versions
- `settings.gradle.kts` — add wgpu4k Maven repo
- `gradle/wrapper/gradle-wrapper.properties` — upgrade to Gradle 9.1+
- `gradle.properties` — add required KMP properties, bump JVM args
- `prism-renderer/build.gradle.kts` — add wgpu4k dependency

**Steps:**
1. Upgrade Gradle wrapper to 9.1.0
2. Add wgpu4k Maven repository to settings.gradle.kts
3. Add wgpu4k artifacts to version catalog
4. Add wgpu4k dependency to prism-renderer
5. Run `./gradlew :prism-renderer:build` to validate compilation

### Phase 2: WgpuRenderer Core
**New file:** `prism-renderer/src/commonMain/kotlin/engine/prism/renderer/WgpuRenderer.kt`

Implements the `Renderer` interface using wgpu4k. This is the biggest piece of work.

**Implementation details:**

```
class WgpuRenderer(private val context: WGPUContext) : Renderer {
    // wgpu handles from context
    private val device: GPUDevice = context.device
    private val queue: GPUQueue = device.queue
    private val renderingContext: RenderingContext = context.renderingContext

    // Per-frame state
    private var commandEncoder: GPUCommandEncoder? = null
    private var currentRenderPass: GPURenderPassEncoder? = null
    private var currentSurfaceTexture: GPUTexture? = null

    // Cached resources
    private var depthTexture: GPUTexture? = null
    private var cameraUniformBuffer: GPUBuffer? = null
    private var modelUniformBuffer: GPUBuffer? = null
    private var cameraBindGroup: GPUBindGroup? = null

    // Current state
    private var currentCamera: Camera? = null
    private var currentPipeline: RenderPipeline? = null
    private var width: Int = 0
    private var height: Int = 0
}
```

**Key method implementations:**

| Method | wgpu4k mapping |
|--------|---------------|
| `initialize()` | Create depth texture, uniform buffers, default pipeline |
| `beginFrame()` | `renderingContext.getCurrentTexture()`, `device.createCommandEncoder()` |
| `endFrame()` | `encoder.finish()`, `device.queue.submit()` |
| `beginRenderPass()` | `encoder.beginRenderPass(descriptor)` with color + depth attachments |
| `endRenderPass()` | `renderPass.end()` |
| `bindPipeline()` | `renderPass.setPipeline(pipeline.handle)` |
| `drawMesh()` | Write model matrix to uniform, `setVertexBuffer`, `setIndexBuffer`, `drawIndexed` |
| `setCamera()` | Write view-projection to uniform buffer via `queue.writeBuffer()` |
| `createBuffer()` | `device.createBuffer(BufferDescriptor(...))` |
| `createTexture()` | `device.createTexture(TextureDescriptor(...))` |
| `createShaderModule()` | `device.createShaderModule(ShaderModuleDescriptor(code))` |
| `createPipeline()` | `device.createRenderPipeline(RenderPipelineDescriptor(...))` |
| `uploadMesh()` | Create vertex/index buffers with `mappedAtCreation`, write data, unmap |
| `resize()` | Recreate depth texture, update surface config |

### Phase 3: WGSL Shaders
**New file:** `prism-renderer/src/commonMain/kotlin/engine/prism/renderer/Shaders.kt`

Embedded WGSL shader source strings.

**Shader 1: Unlit (MVP + solid color)**
```wgsl
// Vertex
struct Uniforms {
    viewProjection: mat4x4<f32>,
    model: mat4x4<f32>,
}
@group(0) @binding(0) var<uniform> uniforms: Uniforms;

struct VertexInput {
    @location(0) position: vec3<f32>,
    @location(1) normal: vec3<f32>,
    @location(2) uv: vec2<f32>,
}
struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) normal: vec3<f32>,
    @location(1) uv: vec2<f32>,
}

@vertex fn vs_main(in: VertexInput) -> VertexOutput {
    var out: VertexOutput;
    out.position = uniforms.viewProjection * uniforms.model * vec4<f32>(in.position, 1.0);
    out.normal = (uniforms.model * vec4<f32>(in.normal, 0.0)).xyz;
    out.uv = in.uv;
    return out;
}

// Fragment
struct MaterialUniforms {
    baseColor: vec4<f32>,
}
@group(0) @binding(1) var<uniform> material: MaterialUniforms;

@fragment fn fs_main(in: VertexOutput) -> @location(0) vec4<f32> {
    // Simple directional light
    let lightDir = normalize(vec3<f32>(0.5, 1.0, 0.3));
    let ndotl = max(dot(normalize(in.normal), lightDir), 0.0);
    let ambient = 0.15;
    let diffuse = ndotl * 0.85;
    return vec4<f32>(material.baseColor.rgb * (ambient + diffuse), material.baseColor.a);
}
```

**Uniform buffer layout:**
- Bind group 0, binding 0: `Uniforms` (viewProjection: mat4, model: mat4) = 128 bytes
- Bind group 0, binding 1: `MaterialUniforms` (baseColor: vec4) = 16 bytes

### Phase 4: RenderSurface + Windowing (Multi-platform)

**JVM — Files to modify:**
- `prism-renderer/src/jvmMain/kotlin/engine/prism/renderer/RenderSurface.jvm.kt`
- `prism-demo/src/jvmMain/kotlin/Main.kt`

**JVM approach — GLFW window:**
1. Create GLFW window using `io.ygdrasil:glfw-native`
2. Create wgpu surface from GLFW window
3. Request adapter → device → queue
4. Package into `WGPUContext`
5. Pass to `WgpuRenderer`

**WASM — Files to modify:**
- `prism-renderer/src/wasmJsMain/kotlin/engine/prism/renderer/RenderSurface.wasmJs.kt`
- `prism-demo/src/wasmJsMain/kotlin/Main.kt`

**WASM approach — HTML Canvas + WebGPU:**
1. Get HTML canvas element by ID
2. Call `navigator.gpu.requestAdapter()` → `adapter.requestDevice()`
3. Create WebGPU surface from canvas context
4. Package into `WGPUContext`
5. Use `requestAnimationFrame` for the game loop (via `GameLoop.startExternal()` + `tick()`)

**iOS/macOS Native — Files to modify:**
- `prism-renderer/src/iosMain/kotlin/engine/prism/renderer/RenderSurface.ios.kt`
- `prism-renderer/src/macosArm64Main/kotlin/engine/prism/renderer/RenderSurface.macos.kt`
- `prism-demo/src/iosMain/kotlin/Main.kt`

**iOS/Native approach — Metal Layer:**
1. Create CAMetalLayer via platform interop
2. Create wgpu surface from Metal layer
3. Request adapter (Metal backend) → device → queue
4. Package into `WGPUContext`
5. Integrate with UIView (iOS) or NSView (macOS)

**Android — Files to modify:**
- `prism-renderer/src/androidMain/kotlin/engine/prism/renderer/RenderSurface.android.kt`
- `prism-demo/src/androidMain/kotlin/MainActivity.kt`
- Add PanamaPort dependency to support FFI on Android

**Android approach — SurfaceView + Vulkan:**
1. Add PanamaPort to support Project Panama FFI on Android
2. Create Android SurfaceView for rendering
3. Create wgpu surface from ANativeWindow (via JNI/FFI bridge)
4. Request adapter (Vulkan backend) → device → queue
5. Package into `WGPUContext`
6. Handle Android lifecycle (pause/resume)

**For Compose integration** (later milestone):
- `PrismView.jvm.kt` will embed a GLFW-backed rendering panel inside a Compose window
- Or use `SwingPanel` with AWT Canvas → native handle → wgpu surface
- `PrismView.android.kt` will use AndroidView with SurfaceView

### Phase 5: Wire Up RenderSystem
**File:** `prism-ecs/src/commonMain/kotlin/engine/prism/ecs/system/RenderSystem.kt`

Currently a stub. Implementation:
```kotlin
class RenderSystem(private val renderer: Renderer) : System {
    override val name = "RenderSystem"
    override val priority = 100

    override fun update(world: World, time: Time) {
        renderer.beginFrame()
        renderer.beginRenderPass(RenderPassDescriptor())

        // Set active camera
        world.query<CameraComponent>().firstOrNull()?.let { (_, cam) ->
            renderer.setCamera(cam.camera)
        }

        // Draw all mesh entities
        for ((entity, mesh) in world.query2(TransformComponent::class, MeshComponent::class)) {
            val transform = world.getComponent<TransformComponent>(entity)!!
            val meshComp = world.getComponent<MeshComponent>(entity)!!
            val material = world.getComponent<MaterialComponent>(entity)
            if (meshComp.mesh != null) {
                material?.material?.pipeline?.let { renderer.bindPipeline(it) }
                renderer.drawMesh(meshComp.mesh!!, transform.toTransform().toMatrix())
            }
        }

        renderer.endRenderPass()
        renderer.endFrame()
    }
}
```

### Phase 6: Demo App
**File:** `prism-demo/src/commonMain/kotlin/engine/prism/demo/DemoApp.kt`

Already sets up a scene with:
- Camera at (0, 2, 5) looking at origin
- Directional light
- Rotating cube entity with mesh + material + transform

Just needs the `WgpuRenderer` subsystem registered:
```kotlin
engine.addSubsystem(wgpuRenderer)
engine.addSubsystem(RenderSystem(wgpuRenderer))
```

**JVM entry point** (`prism-demo/src/jvmMain/`):
```kotlin
fun main() {
    // 1. Create GLFW window
    // 2. Create WGPUContext from window
    // 3. Create WgpuRenderer(context)
    // 4. Create Engine with renderer
    // 5. Run game loop
}
```

---

## 6. File Change Summary

### New Files
| File | Purpose |
|------|---------|
| `prism-renderer/.../WgpuRenderer.kt` | Main Renderer implementation using wgpu4k |
| `prism-renderer/.../Shaders.kt` | WGSL shader source strings |

### Modified Files
| File | Change |
|------|--------|
| `gradle/libs.versions.toml` | Add wgpu4k version + PanamaPort artifacts |
| `settings.gradle.kts` | Maven Central repository |
| `gradle/wrapper/gradle-wrapper.properties` | Upgrade to Gradle 9.1+ |
| `gradle.properties` | Add KMP properties, JVM args |
| `prism-renderer/build.gradle.kts` | Add wgpu4k + PanamaPort dependencies |
| `prism-renderer/.../RenderSurface.jvm.kt` | GLFW/wgpu surface creation |
| `prism-renderer/.../RenderSurface.wasmJs.kt` | WebGPU canvas surface creation |
| `prism-renderer/.../RenderSurface.ios.kt` | Metal layer surface creation |
| `prism-renderer/.../RenderSurface.android.kt` | Android SurfaceView + Vulkan |
| `prism-ecs/.../RenderSystem.kt` | Actual draw calls |
| `prism-demo/.../DemoApp.kt` | Wire up WgpuRenderer |
| `prism-demo/src/jvmMain/.../Main.kt` | GLFW window + engine bootstrap |
| `prism-demo/src/wasmJsMain/.../Main.kt` | Browser canvas + WebGPU |
| `prism-demo/src/iosMain/.../Main.kt` | iOS UIView integration |
| `prism-demo/src/androidMain/.../MainActivity.kt` | Android Activity + SurfaceView |

---

## 7. Milestone Targets

### M1: Triangle on Screen (JVM)
- wgpu4k compiles and links
- GLFW window opens
- Hardcoded triangle renders with solid color
- **Validates:** build setup, wgpu4k integration, shader compilation, render loop

### M2: Rotating Cube (JVM)
- Mesh.cube() uploaded to GPU
- Camera uniform buffer with view-projection matrix
- Model matrix uniform updated each frame for rotation
- Depth testing enabled
- **Validates:** uniform buffers, vertex layouts, depth buffer, transforms

### M3: Lit Cube with Materials
- Directional light in fragment shader
- Material baseColor applied
- Normal-based diffuse + ambient lighting
- **Validates:** material system, lighting, normal passing

### M4: ECS-Driven Rendering
- RenderSystem queries entities and draws them
- Multiple entities in scene
- Camera driven by CameraNode
- **Validates:** full ECS → renderer pipeline

### M5: Compose Integration (JVM)
- PrismView embeds rendering in Compose window
- PrismOverlay shows FPS counter on top
- **Validates:** Compose ↔ engine integration

### M6: Web/WASM Support
- Rotating cube renders in browser via WebGPU
- HTML Canvas integration
- requestAnimationFrame game loop
- **Validates:** WASM compilation, WebGPU backend, browser compatibility

### M7: iOS Native Support
- Rotating cube renders on iOS device/simulator
- CAMetalLayer integration
- Touch input handling
- **Validates:** Kotlin/Native compilation, Metal backend, iOS platform integration

### M8: Android Support
- PanamaPort integration for FFI support
- Rotating cube renders on Android device/emulator
- SurfaceView integration with Vulkan backend
- Touch input and lifecycle handling
- **Validates:** Android compilation, PanamaPort FFI bridge, Vulkan backend

---

## 8. Decisions Made
1. **wgpu4k version:** Using `wgpu4k` version 0.1.1 from Maven Central (high-level API, `io.ygdrasil.webgpu.*`). More ergonomic, matches the examples.
2. **Target platforms:** JVM Desktop (macOS/Windows/Linux), WASM, iOS, Android, macOS Native
3. **Platform priority:** Start with JVM (macOS/GLFW), then WASM, then mobile (iOS/Android)
4. **Gradle/JDK upgrade:** Approved — upgrading to Gradle 9.1+ and JDK 21+
5. **Android FFI:** Using PanamaPort (github.com/vova7878/PanamaPort) to enable Project Panama on Android 8.0+

## 9. Open Questions
1. **Compose + GLFW:** Can a GLFW-rendered surface be embedded inside a Compose Desktop window, or do we need two separate windowing approaches? Investigate `SwingPanel` approach later (M5).
2. **Suspend functions:** wgpu4k uses `suspend fun` for `initialize()` and `render()`. Our `Subsystem.initialize()` and `update()` are not suspend. We may need a `runBlocking` bridge or restructure.
3. **PanamaPort integration:** How seamlessly does PanamaPort work with wgpu4k's FFI layer? May need adapter layer to remap `java.lang.foreign` to `com.v7878.foreign`.
4. **Android Vulkan:** Does wgpu4k's Vulkan backend work on all Android devices, or only those with Vulkan support? Fallback strategy for older devices?

---

## 10. Verification Plan
1. `./gradlew :prism-renderer:build` — compiles with wgpu4k
2. `./gradlew :prism-demo:run` — opens GLFW window, renders triangle
3. Upgrade to rotating cube with camera
4. Add lighting + materials
5. Wire ECS RenderSystem, render multiple entities
6. Embed in Compose window with FPS overlay
