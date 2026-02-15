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

## Phase 2: WgpuRenderer Implementation 🚧

### Completed
- [x] WgpuRenderer class implementing Renderer interface
- [x] WGSL shaders for lit/unlit materials (Shaders.kt)
- [x] GlfwMain.kt demo entry point with GLFW windowing
- [x] Cube mesh with vertex/index buffers and depth testing

### Pending
- [ ] Complete RenderSurface implementations (native stubs are TODOs)
- [ ] WASM/Canvas integration for web
- [ ] Test WgpuRenderer on macOS/Linux/Windows
- [ ] Add unit tests for renderer

## Build Commands

```bash
# Build all modules (JVM)
./gradlew compileKotlinJvm

# Full build with tests
./gradlew build

# Run demo app (JVM Desktop)
./gradlew :prism-demo:run
```

## Prerequisites

- JDK 25 (for FFI in prism-demo) or JDK 21+ (for other modules)
- wgpu4k 0.2.0-SNAPSHOT in Maven local (see AGENTS.md for setup)
