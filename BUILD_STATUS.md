# Prism Engine - Build Status

## Phase 1: Build System Setup ✅ Complete

### All Modules Compiling (JVM)
- ✅ prism-math
- ✅ prism-core
- ✅ prism-renderer
- ✅ prism-ecs
- ✅ prism-scene
- ✅ prism-input
- ✅ prism-audio
- ✅ prism-assets
- ✅ prism-native-widgets
- ✅ prism-compose
- ✅ prism-demo

### Configuration Applied
- [x] Gradle 9.2.0
- [x] wgpu4k 0.2.0-SNAPSHOT (Maven local)
- [x] KMP properties (enableCInteropCommonization, ignoreDisabledTargets)
- [x] `-Xexpect-actual-classes` flag on modules with expect/actual declarations
- [x] `@PublishedApi internal` for inline function field access (prism-core, prism-ecs)
- [x] Native platform implementations (Linux, macOS, Windows) for Platform and RenderSurface
- [x] JVM toolchain 25 for prism-demo (FFI support)

## Phase 2: WgpuRenderer Implementation ✅ Complete

### Completed
- [x] WgpuRenderer class implementing Renderer interface
- [x] WGSL shaders for lit/unlit materials (Shaders.kt)
- [x] GlfwMain.kt demo entry point with GLFW windowing
- [x] Cube mesh with vertex/index buffers and depth testing
- [x] Uniform buffers (VP + model matrices) and bind groups
- [x] Material uniform buffer (baseColor)
- [x] Surface configuration and present lifecycle

### Pending
- [ ] Complete RenderSurface implementations (native stubs are TODOs)
- [ ] WASM/Canvas integration for web

## Milestones

### M1: Triangle on Screen (JVM) ✅
- wgpu4k compiles and links
- GLFW window opens
- Render pipeline with WGSL shaders
- **Status:** Complete

### M2: Rotating Cube (JVM) ✅
- Mesh.cube() uploaded to GPU with vertex/index buffers
- Camera uniform buffer with view-projection matrix
- Model matrix uniform updated each frame (45 deg/sec rotation)
- Depth testing enabled
- **Status:** Complete — `./gradlew :prism-demo:jvmRun` renders a rotating cube

### M3: Lit Cube with Materials ✅
- Directional light in fragment shader
- Material baseColor applied (blue cube)
- Normal-based diffuse (0.85) + ambient (0.15) lighting
- Cornflower blue background
- **Status:** Complete — lighting and materials working in demo

### M4: ECS-Driven Rendering ✅
- WgpuRenderer resource lifecycle fixed (per-frame AutoClosableContext, surface.configure())
- CameraComponent added to ECS
- RenderSystem implemented (queries entities, drives WgpuRenderer)
- GlfwMain.kt rewritten to use Engine + World + ECS entities
- **Status:** Complete — same rotating cube, now driven through Engine + ECS + WgpuRenderer pipeline

### M5: Compose Integration (JVM) ⏳
### M6: Web/WASM Support ⏳
### M7: iOS Native Support ⏳
### M8: Android Support ⏳
### M9: PBR Materials ⏳
### M10: glTF Asset Loading ⏳

## Build Commands

```bash
# Build all modules (JVM)
./gradlew compileKotlinJvm

# Full build with tests
./gradlew build

# Run demo app (JVM Desktop)
./gradlew :prism-demo:jvmRun
```

## Prerequisites

- JDK 25 (for FFI in prism-demo) or JDK 21+ (for other modules)
- wgpu4k 0.2.0-SNAPSHOT in Maven local (build from ~/development/wgpu4k)
