# Prism Engine

A modular, cross-platform 3D game engine built with Kotlin Multiplatform and WebGPU.

[Website](https://hyeons-lab.github.io/prism/) | [GitHub](https://github.com/hyeons-lab/prism) | [Roadmap](PLAN.md)

> **This is an experiment.** Prism is being vibe-coded with [Claude](https://claude.ai) — the engine, the tests, the CI, and even this README. Every design decision, debug session, and dead end is captured in the [`devlog/`](devlog/) directory. AI-assisted PRs are welcome.

## Overview

Prism provides a unified API for building 3D applications across desktop (JVM), web (WASM/JS), mobile (iOS, Android), and native platforms using a single Kotlin codebase. The rendering backend uses [wgpu4k](https://github.com/wgpu4k/wgpu4k) for cross-platform GPU access via WebGPU (Vulkan, Metal, DX12, WebGPU).

## Features

- **Math library** — Vec2/3/4, Mat3/4, Quaternion, Transform with operator overloads
- **Entity-Component-System** — World, Entity, Component, System with query-based iteration
- **Renderer** — wgpu4k-backed pipeline with WGSL shaders, depth testing, and lighting
- **Scene graph** — Node hierarchy with MeshNode, CameraNode, LightNode
- **Engine core** — Subsystem architecture, game loop, frame timing
- **Input system** — Cross-platform keyboard/mouse/touch abstractions
- **Asset management** — Loader interfaces for meshes, shaders, and textures
- **Compose integration** — Embedded 3D rendering in Compose Desktop via MVI architecture (Store, State, Events)
- **Native rendering surfaces** — PrismSurface with suspend factory (`createPrismSurface()`) across JVM, iOS, macOS, Linux, Windows, WASM, and Android

## Architecture

```
prism-math              Vector/matrix math, transforms
  └─► prism-core        Engine core, subsystems, game loop
       ├─► prism-renderer    wgpu4k rendering backend + WGSL shaders
       │    ├─► prism-scene       Scene graph (Node, MeshNode, CameraNode)
       │    ├─► prism-ecs         Entity-Component-System
       │    └─► prism-assets      Asset loading and management
       ├─► prism-input       Cross-platform input handling
       └─► prism-audio       Audio engine interface (stub)

prism-renderer
  └─► prism-native-widgets   Platform-specific rendering surfaces
       └─► prism-compose     Compose Multiplatform integration

prism-demo                   Demo app (rotating lit cube via ECS) — see [prism-demo/README.md](prism-demo/README.md)
```

## Quick Start

### Prerequisites

- JDK 25 (for FFI in demo) or JDK 21+ (for other modules)
- Gradle 9.2.0 (wrapper included)

### Build wgpu4k from source

Prism depends on wgpu4k 0.2.0-SNAPSHOT (commit `3fc6e327`), which must be built and published to Maven local:

```bash
git clone https://github.com/wgpu4k/wgpu4k.git
cd wgpu4k
git checkout 3fc6e3297fee6b558efc6dcb29aec1a6629b0e90
./gradlew publishToMavenLocal
```

### Build and run

```bash
# Clone Prism
git clone https://github.com/hyeons-lab/prism.git
cd prism

# Build all modules
./gradlew build

# Run the demo (rotating lit cube)
./gradlew :prism-demo:jvmRun
```

### iOS (Swift Package Manager)

Add Prism to your iOS project via SPM:

**Xcode UI:** File > Add Package Dependencies > `https://github.com/hyeons-lab/prism`

**Package.swift:**
```swift
dependencies: [
    .package(url: "https://github.com/hyeons-lab/prism", from: "0.1.0")
]
```

Then in your Swift code:
```swift
import Prism
```

> **Note:** SPM support requires a published release. See the [Releases](https://github.com/hyeons-lab/prism/releases) page for available versions.

## Tech Stack

| Dependency | Version |
|---|---|
| Kotlin | 2.3.0 |
| Gradle | 9.2.0 |
| wgpu4k | 0.2.0-SNAPSHOT (`3fc6e327`) |
| Compose Multiplatform | 1.10.1 |
| kotlinx-coroutines | 1.10.2 |
| kotlinx-serialization | 1.9.0 |
| kotlinx-io | 0.8.2 |
| Kermit (logging) | 2.0.8 |
| Kotest | 6.0.7 |

## Platform Support

| Platform | Backend | Status |
|---|---|---|
| JVM Desktop (macOS) | Metal via wgpu4k + GLFW | Working |
| JVM Desktop (macOS) | Metal via wgpu4k + Compose/AWT | Working |
| JVM Desktop (Linux) | Vulkan via wgpu4k + GLFW | Planned |
| JVM Desktop (Windows) | DX12/Vulkan via wgpu4k + GLFW | Planned |
| Web (WASM/JS) | WebGPU | Working |
| iOS | Metal via wgpu4k + MTKView | Working |
| macOS Native | Metal via wgpu4k + GLFW | Working |
| Android | Vulkan via wgpu4k | Working |
| Flutter (iOS/Android) | Platform channels + native rendering | Planned |
| Flutter Web | WebGPU via HtmlElementView | Planned |

## Project Status

| Milestone | Description | Status |
|---|---|---|
| M1 | Triangle on screen (JVM) | Done |
| M2 | Rotating cube with camera | Done |
| M3 | Lit cube with materials | Done |
| M4 | ECS-driven rendering | Done |
| M5 | Compose integration (JVM) | Done |
| M6 | Web/WASM support | Done |
| M7 | iOS native support | Done |
| M7.5 | PrismSurface + macOS native demo | Done |
| M8 | Android support | Done |
| M9 | PBR materials | Planned |
| M10 | glTF asset loading | Planned |
| M11 | Flutter integration | Planned |

See [BUILD_STATUS.md](BUILD_STATUS.md) for detailed status and [PLAN.md](PLAN.md) for the full technical specification.

## Testing

Tests use `kotlin.test` with [Kotest](https://kotest.io/) assertion matchers. CI runs two jobs: `jvmTest` on Ubuntu and `macosArm64Test` + `iosSimulatorArm64Test` on macOS.

| Module | Tests | Coverage |
|---|---|---|
| prism-math | 75 | Vec3, Mat4, Quaternion |
| prism-renderer | 95 | Color, Mesh, VertexLayout, Camera, Shader |
| prism-demo | 8 | DemoStore (MVI reducer) |

## Contributing

```bash
# Format code (Google style via KtFmt)
./gradlew ktfmtFormat

# Run quality checks
./gradlew ktfmtCheck detektJvmMain jvmTest
```

Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new feature
fix: resolve bug
refactor: restructure code
docs: update documentation
test: add or update tests
```

See [AGENTS.md](AGENTS.md) for full coding standards and architecture details.

## License

[Apache License 2.0](LICENSE) — Copyright 2025-2026 Hyeons' Lab
