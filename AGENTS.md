# AGENTS.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Prism is a modular, cross-platform 3D game engine built with Kotlin Multiplatform (KMP). It provides a unified API for building 3D applications across desktop (JVM), web (WASM/JS), mobile (iOS, Android), and native platforms (macOS, Linux, Windows) using a single shared codebase. The rendering backend uses **wgpu4k** for cross-platform GPU access (Vulkan, Metal, DX12, WebGPU).

- **Group ID**: engine.prism
- **Current Version**: 0.1.0-SNAPSHOT
- **Repository**: prism
- **Documentation**: See PLAN.md for detailed technical specification

## Build Commands

```bash
# Build entire project
./gradlew build

# Build specific module
./gradlew :prism-renderer:build
./gradlew :prism-core:build

# Format code (Google style via KtFmt)
./gradlew ktfmtFormat

# Check formatting + static analysis
./gradlew ktfmtCheck detektJvmMain

# Run tests
./gradlew test                          # All tests
./gradlew :prism-math:test              # Module-specific tests
./gradlew :prism-core:jvmTest           # Platform-specific tests
./gradlew :prism-core:macosArm64Test    # Native tests (macOS)

# Clean build
./gradlew clean build

# Run demo apps (JVM Desktop)
./gradlew :prism-demo-core:jvmRun              # GLFW window (Metal/Vulkan)
./gradlew :prism-demo-core:runCompose           # Compose Desktop with embedded 3D

# Build demo for specific platform
./gradlew :prism-demo-core:jvmJar            # JVM executable JAR
./gradlew :prism-demo-core:wasmJsBrowserDistribution  # WASM for web
./gradlew assemblePrismDemoDebugXCFramework        # iOS XCFramework (debug)

# Run macOS native demo (GLFW + AppKit controls)
./gradlew :prism-demo-core:runDebugExecutableMacosArm64

# Generate Xcode project (requires xcodegen: brew install xcodegen)
cd prism-ios-demo && xcodegen generate

# Full CI quality check
./gradlew ktfmtCheck detektJvmMain jvmTest

# Build Prism.xcframework (iOS distribution)
./gradlew :prism-ios:assemblePrismDebugXCFramework    # Debug
./gradlew :prism-ios:assemblePrismReleaseXCFramework  # Release

# Create a release (triggers GitHub Actions release workflow)
gh workflow run release.yml -f version=0.1.0

# Check Gradle dependencies
./gradlew dependencies --configuration commonMainCompileClasspath
```

### Environment Setup

**Required:**
- JDK 21 or later (JDK 22+ recommended for desktop FFI)
- Gradle 9.1+ (wrapper included)

**Platform-specific:**
- **macOS/iOS:** Xcode 14+ with Command Line Tools
- **Android:** Android SDK with NDK, API level 28+
- **Web:** Modern browser with WebGPU support (Chrome 113+, Firefox Nightly)

Use JetBrains Runtime from Android Studio (if available):
```bash
export JAVA_HOME=/Applications/Android\ Studio.app/Contents/jbr/Contents/Home
```

## Architecture

### Kotlin Multiplatform Structure

```
prism/
├── prism-math          # Vector/matrix math, transforms (Vec2/3/4, Mat3/4, Quaternion)
├── prism-core          # Engine core (Engine, GameLoop, Time, Subsystem, Platform)
├── prism-renderer      # Rendering abstractions + WgpuRenderer implementation
├── prism-scene         # Scene graph (Node, Scene, CameraNode, MeshNode, LightNode)
├── prism-ecs           # Entity-Component-System (World, Entity, Component, System)
├── prism-input         # Input handling (InputManager, keyboard/mouse/touch)
├── prism-assets        # Asset management (AssetManager, loaders for meshes/shaders/textures)
├── prism-audio         # Audio engine interface (stub for future implementation)
├── prism-native-widgets# Platform-specific rendering surfaces (PrismSurface)
├── prism-compose       # Jetpack Compose Multiplatform integration
├── prism-flutter       # Flutter bridge (Android/iOS MethodChannel + Web WASM)
├── prism-ios           # iOS XCFramework aggregator (SPM distribution)
├── prism-demo-core     # Demo shared code (KMP library, all platforms)
├── prism-android-demo  # Android app (consumes prism-demo-core)
└── prism-ios-demo      # iOS Swift app (consumes prism-demo-core XCFramework)
```

### Module Source Structure (KMP)

Each module follows this pattern:
```
module-name/src/
├── commonMain/      # ~90% of code - platform-agnostic APIs
├── commonTest/      # Shared tests
├── jvmMain/         # JVM Desktop platform code
├── wasmJsMain/      # Web/WASM platform code
├── androidMain/     # Android platform code
├── iosMain/         # iOS platform code
├── macosArm64Main/  # macOS Native platform code
├── linuxX64Main/    # Linux Native platform code
├── mingwX64Main/    # Windows Native platform code
└── *Test/           # Platform-specific tests
```

### Key Abstractions

**Core Engine (prism-core):**
- `Engine` - Main engine coordinator, subsystem management
- `GameLoop` - Frame timing and update loop orchestration
- `Time` - Delta time, FPS tracking, total elapsed time
- `Subsystem` - Plugin interface for engine extensions
- `Platform` - Platform detection and capabilities
- `Store<State, Event>` - MVI store interface (`state: StateFlow<State>`, `dispatch(event)`)

**Math (prism-math):**
- `Vec2`, `Vec3`, `Vec4` - Vector types with operators
- `Mat3`, `Mat4` - Matrix types with transforms
- `Quaternion` - Rotation representation
- `Transform` - Position + rotation + scale composite
- `MathUtils` - Common math operations (lerp, clamp, deg/rad conversion)

**Rendering (prism-renderer):**
- `Renderer` - Core rendering interface
- `WgpuRenderer` - WebGPU/wgpu4k implementation (in progress)
- `Mesh`, `Material`, `Shader` - Graphics primitives
- `Camera`, `RenderPass`, `Pipeline` - Rendering pipeline
- `Buffer`, `Texture`, `VertexAttribute` - GPU resources

**Scene Graph (prism-scene):**
- `Node` - Base scene node with transform hierarchy
- `Scene` - Root scene container
- `MeshNode` - Renderable mesh in scene
- `CameraNode` - Camera in scene
- `LightNode` - Light source in scene

**ECS (prism-ecs):**
- `World` - Entity container and query system
- `Entity` - Unique identifier for game objects
- `Component` - Data-only behavior attachments
- `System` - Logic processors over component queries
- Built-in components: `TransformComponent`, `MeshComponent`, `MaterialComponent`, `LightComponent`
- Built-in systems: `RenderSystem`, `TransformSystem`

**Input (prism-input):**
- `InputManager` - Cross-platform input handling
- `InputEvent` - Event types (key, mouse, touch)
- `Key`, `MouseButton` - Input enumerations

**Assets (prism-assets):**
- `AssetManager` - Asset lifecycle management
- `AssetLoader` - Generic asset loading interface
- `MeshLoader`, `ShaderLoader`, `TextureLoader` - Specific loaders
- `FileReader` - Platform-specific file I/O

**Compose Integration (prism-compose):**
- `EngineState` - Immutable data class (time, fps, surfaceWidth/Height, isInitialized)
- `EngineStateEvent` - Sealed interface (Initialized, Disposed, SurfaceResized, FrameTick)
- `EngineStore` - MVI store implementing `Store<EngineState, EngineStateEvent>` with pure reducer
- `PrismView` - Stateless expect/actual composable: embeds GPU surface, takes `EngineStore`, dispatches events
- `PrismOverlay` - Composable wrapper: 3D surface + UI content overlay
- `PrismTheme` - CompositionLocal provider for Engine via `LocalEngine`
- `rememberEngineStore()` / `rememberExternalEngineStore()` - Lifecycle-aware store creation

**Native Widgets (prism-native-widgets):**
- `PrismPanel` - AWT Canvas subclass with wgpu surface creation from native handles (macOS/Windows/Linux)
- `AwtRenderingContext` - Custom RenderingContext bypassing GLFW's glfwGetWindowSize()
- `PrismSurface` - Platform-specific rendering surface wrapping wgpu context (GLFW on JVM/desktop native, MTKView on iOS, Canvas on WASM)
- `createPrismSurface()` - Suspend factory function per platform; creates wgpu context and returns ready-to-use PrismSurface

### Platform Implementations

- **JVM Desktop (GLFW)**: GLFW windowing (via wgpu4k's glfw-native) with Metal (macOS), Vulkan/DX12 (Windows/Linux)
- **JVM Desktop (Compose)**: AWT Canvas embedded via SwingPanel, wgpu surface from native handle, Compose `withFrameNanos` render loop
- **Web/WASM**: HTML Canvas with WebGPU API, requestAnimationFrame game loop
- **iOS Native (MTKView)**: MTKView + MTKViewDelegateProtocol via Kotlin/Native ObjC interop, wgpu4k `iosContextRenderer`
- **iOS Compose**: UIKitView embedding MTKView in Compose hierarchy, DemoStore MVI with Material3 controls
- **macOS Native (GLFW)**: GLFW windowing with Metal backend via wgpu4k `glfwContextRenderer`, AppKit NSPanel for floating controls
- **Android**: SurfaceView + wgpu4k `androidContextRenderer` with Vulkan backend, Choreographer-driven render loop
- **Linux/Windows Native**: GLFW windowing with Vulkan/DX12 via wgpu4k `glfwContextRenderer` (compiles, untested)
- **Flutter (Android)**: SurfaceView + Choreographer via PrismBridge, MethodChannel control API, consumes KMP artifacts from Maven local
- **Flutter (iOS)**: MTKView + configureDemo via PrismBridge, MethodChannel control API, consumes PrismDemo.xcframework via podspec
- **Flutter Web**: HtmlElementView canvas + Kotlin/WASM @JsExport functions, Dart JS interop via conditional imports, requestAnimationFrame render loop

### Module Dependency Graph

```
prism-math
  └─► prism-core
       ├─► prism-renderer (depends on wgpu4k)
       │    ├─► prism-scene
       │    ├─► prism-ecs
       │    └─► prism-assets
       ├─► prism-input
       └─► prism-audio

prism-renderer
  └─► prism-native-widgets (platform surfaces)
       ├─► prism-compose (Compose integration)
       └─► prism-flutter (Flutter bridge)

prism-demo-core
  └─► all engine modules
prism-android-demo
  └─► prism-demo-core + prism-native-widgets
prism-ios-demo (Swift)
  └─► prism-demo-core (via XCFramework)
```

## Code Quality

**All warnings are treated as errors.** The build enforces:
- KtFmt (Google style, 100-char line width)
- Detekt with maxIssues=0
- Kotlin `allWarningsAsErrors=true`
- `./gradlew ktfmtFormat` runs formatting on all source files

**Coding Standards:**
- Do not use star imports (e.g., `import foo.*`). Always use explicit imports.
- Use `expect`/`actual` for platform-specific implementations, with `-Xexpect-actual-classes` compiler flag
- Inline functions in commonMain must be `public` or `internal` (not `private`)
- Use `suspend` functions for async operations, leverage kotlinx-coroutines
- Prefer immutable data classes for components and value types
- Use Kermit for logging: `Logger.i`, `Logger.w`, `Logger.e`

## Tech Stack

- **Language:** Kotlin 2.3.0
- **Build System:** Gradle 9.2.0 with Kotlin DSL
- **GPU Backend:** wgpu4k 0.2.0-SNAPSHOT (io.ygdrasil:wgpu4k + wgpu4k-toolkit) - WebGPU bindings (built from source, Maven local)
- **Shader Language:** WGSL (WebGPU Shading Language)
- **UI Framework:** Jetpack Compose Multiplatform 1.10.1
- **Windowing (JVM):** GLFW via wgpu4k's glfw-native
- **Android FFI:** wgpu4k-toolkit-android AAR (JNI + native `libwgpu4k.so`)
- **Async:** kotlinx-coroutines 1.10.2
- **Serialization:** kotlinx-serialization 1.9.0
- **I/O:** kotlinx-io 0.8.2
- **Logging:** Kermit 2.0.8
- **Testing:** kotlin.test + Kotest 6.0.7 assertions (shouldBe, shouldContain, plusOrMinus, etc.)

### SDK Versions (Android)
- minSdk: 28 (Android 9.0 Pie)
- compileSdk: 36
- targetSdk: 36
- AGP: 8.13.0

### wgpu4k Setup

Prism depends on **wgpu4k 0.2.0-SNAPSHOT**, which must be built from source and published to Maven local.

**License:** wgpu4k is licensed under Apache 2.0. See https://github.com/wgpu4k/wgpu4k

**Steps to build:**
```bash
# Clone wgpu4k
git clone https://github.com/wgpu4k/wgpu4k.git
cd wgpu4k

# Build and publish to Maven local
./gradlew publishToMavenLocal
```

**Why SNAPSHOT?** The published releases on Maven Central may not include all the APIs Prism needs. Building from source ensures access to the latest wgpu4k-toolkit features (GLFW integration, `autoClosableContext`, `glfwContextRenderer`).

**Verification:** After building, confirm the artifacts exist:
```bash
ls ~/.m2/repository/io/ygdrasil/wgpu4k/0.2.0-SNAPSHOT/
ls ~/.m2/repository/io/ygdrasil/wgpu4k-toolkit/0.2.0-SNAPSHOT/
```

## Contribution Guidelines

### Commit Messages

Follow conventional commits format:
```
<type>: <description>

[optional body with bullet points]
```

Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`

Example:
```
feat: implement WgpuRenderer with basic triangle rendering

- Add WgpuRenderer class implementing Renderer interface
- Add WGSL shader source for unlit materials
- Implement beginFrame/endFrame with wgpu4k command encoding
- Add JVM platform integration with GLFW windowing
```

### Pull Request Descriptions

**IMPORTANT:** When creating or updating PRs, the PR description MUST match the commit message body exactly. This ensures consistency when squashing commits.

Format:
```
<Summary paragraph describing what was changed>

- Bullet point detailing specific change
- Another bullet point for another change
- Additional changes as needed
```

Example:
```
Implement core WgpuRenderer backend for JVM platform with GLFW windowing and basic render pipeline.

- Add WgpuRenderer implementation using wgpu4k API
- Create WGSL shaders for unlit materials with lighting
- Add JVM platform-specific RenderSurface with GLFW window creation
- Integrate with RenderSystem for ECS-driven rendering
- Add demo app entry point for rotating cube scene
```

## Branching & Plan Workflow

### Write a Plan Before Starting

> All feature work uses git worktrees — see [Git Worktree Workflow](#git-worktree-workflow) in the Development Workflow section below.

Before writing code, create a plan file in `devlog/plans/`:

1. **Create worktree** (see above)
2. **Set up devlog directory** (see [Devlog in New Projects](#devlog-in-new-projects) below)
3. **Create branch devlog**: `devlog/NNNNNN-<branch-name>.md`
   - Check highest `NNNNNN` in existing `devlog/NNNNNN-*.md` files (exclude `plans/`), increment
   - `<branch-name>`: Git branch name with `/` replaced by `-`
4. **Create plan file**: `devlog/plans/NNNNNN-NN-<description>.md`
   - `NN` is a required two-digit sequence number (01, 02, ...)
   - Structure: `## Thinking` (exploratory reasoning) then `## Plan` (actionable steps)
   - Plans are append-only; note deviations in the devlog
5. **Commit and push** the devlog and plan files, then **draft PR** via `gh pr create --draft`
6. **Write code**, format, validate, commit, push
7. **Update PR description** to match final commit body

### Plan Versioning

Plans are **append-only**. If the plan changes during execution:
- Note deviations in the branch devlog file
- For major pivots, create a new plan file with an incremented sequence number (e.g., `000010-02-...`)

### Devlog in New Projects

Always create devlogs, even in projects that don't have a `devlog/` directory yet. When starting a new feature in a project without an existing `devlog/` directory:

1. **Ask the user**: "This project doesn't track devlogs yet. Would you like to add `devlog/` to the repository, or keep it local-only?"
2. **If tracked**: Create `devlog/` and `devlog/plans/`, commit them normally.
3. **If local-only**: Create `devlog/` and `devlog/plans/`, then add `devlog/` to `.gitignore`. Devlog files stay local and are never committed unless the user later asks to start tracking them.

Skip the question if the project already has a committed `devlog/` directory.

### Devlog Conventions

One flat file per branch: `devlog/NNNNNN-<branch-name>.md`. Update proactively as you work — do not wait for the user to ask. See `devlog/CONVENTIONS.md` for the full specification.

**Timestamps:** ISO 8601 with UTC offset — e.g. `2026-02-14T10:30-08:00`. Always include time, not just date.

**Required sections** (omit if empty):
- **Agent:** `Agent Name (model-id) @ repository branch branch-name` — add a new line with timestamp if agent or model changes mid-branch
- **Intent:** User's goal or problem being solved
- **What Changed:** `timestamp path/to/file — what and why` (record final state, not iterations; group similar files)
- **Decisions:** `timestamp Decision — reasoning`
- **Issues:** Problems, failed attempts, and resolutions. Log what you tried, why it failed, and what you learned.
- **Commits:** `hash — message`

**Optional sections** (omit if empty): Progress, Research & Discoveries, Lessons Learned, Next Steps

**Rules:**
- One flat file per branch — append to existing sections as work progresses; don't split into sessions
- Track "why" not just "what" — capture reasoning, not file diffs
- Append-only across conversations — append new entries to existing sections rather than rewriting them
- Never log secrets — no API keys, tokens, credentials, PII, or private URLs; use placeholders like `<API_KEY>` instead
- On merge conflict: rebase onto main and renumber your devlog file

## Current Project Status

**Phase:** PBR materials + Flutter integration complete (M9, M11), all platforms operational

**What works:**
- ✅ All math operations (Vec2/3/4, Mat3/4, Quaternion, Transform)
- ✅ Engine core with subsystem architecture + `Store<State, Event>` MVI interface
- ✅ ECS with query system and built-in components
- ✅ Scene graph with node hierarchy
- ✅ Input event system (interfaces defined)
- ✅ Asset management (interfaces defined)
- ✅ wgpu4k integration in prism-renderer module
- ✅ WgpuRenderer with WGSL shaders, depth testing, lighting
- ✅ GLFW windowing on JVM Desktop (macOS Metal)
- ✅ ECS-driven rendering pipeline (M4 complete)
- ✅ Demo app: rotating lit cube via Engine + ECS + WgpuRenderer (M4); PBR sphere grid demo (M9)
- ✅ Compose Desktop integration with MVI architecture (M5 complete)
- ✅ PrismPanel: AWT Canvas with native handle → wgpu surface (macOS Metal, Windows/Linux stubs)
- ✅ Compose demo: Material3 UI controls driving 3D scene via EngineStore/DemoStore
- ✅ Unit tests: 236 tests across prism-math (80), prism-renderer (146), prism-demo-core (10)
- ✅ CI: GitHub Actions with ktfmtCheck, detekt, jvmTest
- ✅ WASM/Canvas WebGPU integration (M6 complete)
- ✅ iOS native rendering via MTKView + wgpu4k iosContextRenderer (M7 complete)
- ✅ iOS Compose demo: UIKitView embedding MTKView with DemoStore MVI + Material3 controls
- ✅ iOS app with UITabBarController: Native (MTKView) + Compose tabs
- ✅ Shared DemoScene.tick() deduplicating rotation logic across all platforms
- ✅ PrismSurface suspend factory pattern: all 7 platform actuals (JVM, iOS, macOS, Linux, MinGW, WASM, Android)
- ✅ All demo consumers wired through `createPrismSurface()` (JVM GLFW, iOS native, iOS Compose, WASM)
- ✅ macOS native demo with AppKit floating controls panel (NSPanel + NSSlider + NSButton)
- ✅ Android build targets added to all modules (migrated to `com.android.kotlin.multiplatform.library`)
- ✅ Android wgpu4k rendering via Vulkan with SurfaceView + Choreographer render loop (M8 complete)
- ✅ Android demo APK with rotating lit cube
- ✅ PBR materials: Cook-Torrance BRDF (GGX NDF, Smith-GGX, Fresnel-Schlick), IBL, HDR tone mapping (M9 complete)
- ✅ Multi-bind-group pipeline (4 groups: scene, object, material, environment)
- ✅ CPU-side IBL: BRDF LUT, irradiance cubemap, prefiltered specular env (5 mip levels)
- ✅ HDR render target (RGBA16Float) + Khronos PBR Neutral tone mapping
- ✅ PBR sphere grid demo: 7×7 spheres with metallic/roughness gradient
- ✅ Android Compose demo: SurfaceView + DemoStore MVI + Material3 controls (matching JVM/iOS Compose demos)
- ✅ Flutter Android plugin: SurfaceView + Choreographer + PrismBridge MethodChannel (M11)
- ✅ Flutter iOS plugin: MTKView + configureDemo + PrismBridge MethodChannel (M11)
- ✅ Flutter Web: Kotlin/WASM @JsExport + HtmlElementView canvas + Dart JS interop (M11)
- ✅ Flutter example app: Material3 UI controls (speed slider, color buttons, pause/resume)

**What's in progress:**

**What's next:**
- ⏭️ glTF 2.0 asset loading

See BUILD_STATUS.md and PLAN.md for detailed implementation plan.

## Development Workflow

### Git Worktree Workflow

All feature work MUST use git worktrees. Do not switch branches in the main checkout. The `worktrees/` directory is gitignored — all worktrees live there to keep the project root clean.

> **Important:** Create the worktree and branch _before_ entering plan mode or starting any exploration. This gives you a frozen snapshot of the codebase — parallel work in other worktrees won't change files while you're reading them.

**Starting a new feature:**
1. From the main checkout (`~/development/prism`), fetch and create a worktree:
   ```bash
   git fetch origin
   git worktree add worktrees/<branch-name> -b <type>/<branch-name> origin/main
   cd worktrees/<branch-name>
   git branch --unset-upstream
   ```
   Branch names should describe the work (e.g. `feat/pbr-materials`, `fix/wasm-depth-buffer`).
   The `--unset-upstream` step is required because Git automatically tracks `origin/main` when branching from a remote ref. The correct upstream will be set on first `git push -u origin <type>/<branch-name>`.
2. Do all work inside the worktree directory.
3. Create a draft PR immediately:
   ```bash
   gh pr create --draft --title "WIP: <description>" --body ""
   ```
4. As you implement, update the PR title and description to reflect the current state:
   ```bash
   gh pr edit <number> --title "<final title>" --body "<description>"
   ```

**Finishing:**
- Push, mark PR as ready, and get it merged.
- Only after the PR is merged, clean up:
  ```bash
  cd ~/development/prism
  git worktree remove worktrees/<branch-name>
  git branch -d <type>/<branch-name>
  git pull origin main
  ```

**Rules:**
- Never commit directly to `main`. All changes go through pull requests.
- Never delete a worktree whose PR is still open.
- Keep the main checkout on `main` — use it only for creating worktrees and housekeeping.
- Always create the worktree first, before planning or exploring. Do all work — including reading code and writing plans — inside the worktree.

### Adding a New Feature

1. **Create worktree:** Follow the Git Worktree Workflow above
2. **Plan:** Determine which module(s) need changes
3. **Interface:** Define abstractions in `commonMain` first
4. **Implement:** Add platform-specific `actual` implementations
5. **Test:** Write tests in `commonTest` and platform-specific test sources
6. **Format:** Run `./gradlew ktfmtFormat`
7. **Validate:** Run `./gradlew ktfmtCheck detektJvmMain jvmTest`
8. **Commit:** Use conventional commit format
9. **PR:** Match PR description to commit body

### Debugging Tips

**JVM Desktop:**
- Run with `-Dwgpu.backend=vulkan` (Linux/Windows) or `-Dwgpu.backend=metal` (macOS)
- Enable wgpu validation layers: `-Dwgpu.validation=true`
- Increase heap: `-Xmx4096m` (already in gradle.properties)

**WASM:**
- Use browser DevTools console for errors
- Check WebGPU support: `navigator.gpu !== undefined`
- Enable WebGPU in Firefox: `about:config` → `dom.webgpu.enabled`

**Native (iOS/macOS):**
- Use Xcode Instruments for Metal debugging
- Check Metal validation layers in scheme settings
- Use `Logger.d` for cross-platform logging (Kermit)

## Known Issues & Workarounds

1. **Inline function visibility:** Inline functions in commonMain must be public/internal, not private (fixed in prism-core, needs fixing in prism-ecs)
2. **expect/actual warnings:** Add `-Xexpect-actual-classes` to module build.gradle.kts (needed in prism-assets, prism-native-widgets, prism-ecs)
3. **wgpu4k suspend functions:** wgpu4k uses `suspend` for some APIs; bridge with `runBlocking` or make engine loop coroutine-based
4. **WASM build size:** Use `-Xwasm-enable-array-range-checks=false` for smaller builds
5. **Android demo:** `prism-android-demo` is the Android app module; `prism-demo-core` is the KMP library (uses `com.android.kotlin.multiplatform.library`)
6. **Android API 35+:** Previously, wgpu4k-native shim classes in `java.lang.foreign` package were shadowed by the boot classpath on Android 15+, causing `InstantiationError`. This is resolved in the current hyeons-lab/wgpu4k-native fork — tested working on API 36 (Pixel 10 Pro Fold).

## Development Logs (devlog/)

See [CONVENTIONS.md](devlog/CONVENTIONS.md) for devlog conventions. AI coding agents must maintain development logs proactively.

## Random Notes

- When clearing caches, do NOT delete `.gradle` folder - be specific about what's deleted
- Create all temporary/scratch files in `.scratch/` folder
- When asked to update docs, also consider updating external repos if applicable
- Always test on at least JVM platform before marking as complete
- Shader code (WGSL) should be validated with web tools: https://webgpufundamentals.org/webgpu/lessons/webgpu-wgsl-validation.html
- wgpu4k resources use `.bind()` within `AutoClosableContext` for automatic cleanup - always use this pattern
- For performance-critical code, prefer inline value classes for Vec2/Vec3/Vec4 (already done)
- Keep shader uniforms aligned to 16 bytes (WGSL std140 layout)
