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
./gradlew ktfmtCheck detekt

# Run tests
./gradlew test                          # All tests
./gradlew :prism-math:test              # Module-specific tests
./gradlew :prism-core:jvmTest           # Platform-specific tests
./gradlew :prism-core:macosArm64Test    # Native tests (macOS)

# Clean build
./gradlew clean build

# Run demo app (JVM Desktop)
./gradlew :prism-demo:run

# Build demo for specific platform
./gradlew :prism-demo:jvmJar            # JVM executable JAR
./gradlew :prism-demo:wasmJsBrowserDistribution  # WASM for web

# Full CI quality check
./gradlew ktfmtCheck detekt test

# Check Gradle dependencies
./gradlew dependencies --configuration commonMainCompileClasspath
```

### Environment Setup

**Required:**
- JDK 21 or later (JDK 22+ recommended for desktop FFI)
- Gradle 9.1+ (wrapper included)

**Platform-specific:**
- **macOS/iOS:** Xcode 14+ with Command Line Tools
- **Android:** Android SDK with NDK, API level 31+
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
├── prism-flutter       # Flutter bridge (minimal, future work)
└── prism-demo          # Demo application (rotating cube with lighting)
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

### Platform Implementations

- **JVM Desktop**: GLFW windowing (via wgpu4k's glfw-native) with Metal (macOS), Vulkan/DX12 (Windows/Linux)
- **Web/WASM**: HTML Canvas with WebGPU API, requestAnimationFrame game loop
- **iOS/macOS Native**: CAMetalLayer with Metal backend (wgpu4k C-interop)
- **Android**: SurfaceView with Vulkan backend, PanamaPort for FFI (planned)
- **Linux/Windows Native**: Native windowing with Vulkan/DX12 (planned)

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
       └─► prism-compose (Compose integration)

prism-demo
  └─► all engine modules
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
- **UI Framework:** Jetpack Compose Multiplatform 1.10.0
- **Windowing (JVM):** GLFW via wgpu4k's glfw-native
- **Android FFI:** PanamaPort (for Foreign Function & Memory API on Android 8.0+) - planned
- **Async:** kotlinx-coroutines 1.10.2
- **Serialization:** kotlinx-serialization 1.9.0
- **I/O:** kotlinx-io 0.8.2
- **Logging:** Kermit 2.0.8
- **Testing:** Kotest 6.0.7

### SDK Versions (Android)
- minSdk: 31 (Android 8.0+)
- compileSdk: 36
- targetSdk: 36
- AGP: 8.12.3

### wgpu4k Setup

Prism depends on **wgpu4k 0.2.0-SNAPSHOT**, which must be built from source and published to Maven local.

**License:** wgpu4k is licensed under Apache 2.0. See https://github.com/AskiaAI/wgpu4k

**Steps to build:**
```bash
# Clone wgpu4k
git clone https://github.com/AskiaAI/wgpu4k.git
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

## Current Project Status

**Phase:** Build system setup (Phase 1, mostly complete)

**What works:**
- ✅ All math operations (Vec2/3/4, Mat3/4, Quaternion, Transform)
- ✅ Engine core with subsystem architecture
- ✅ ECS with query system and built-in components
- ✅ Scene graph with node hierarchy
- ✅ Input event system (interfaces defined)
- ✅ Asset management (interfaces defined)
- ✅ wgpu4k integration in prism-renderer module

**What's in progress:**
- 🚧 WgpuRenderer implementation (Phase 2)
- 🚧 WGSL shader library
- 🚧 Platform-specific RenderSurface implementations
- 🚧 Demo app with rotating cube

**What's next:**
- ⏭️ Complete WgpuRenderer core (beginFrame, endFrame, draw commands)
- ⏭️ GLFW window integration for JVM Desktop
- ⏭️ WASM/Canvas integration for web
- ⏭️ Compose Multiplatform integration
- ⏭️ Mobile platforms (iOS/Android)

See BUILD_STATUS.md and PLAN.md for detailed implementation plan.

## Development Workflow

### Adding a New Feature

1. **Plan:** Determine which module(s) need changes
2. **Interface:** Define abstractions in `commonMain` first
3. **Implement:** Add platform-specific `actual` implementations
4. **Test:** Write tests in `commonTest` and platform-specific test sources
5. **Format:** Run `./gradlew ktfmtFormat`
6. **Validate:** Run `./gradlew ktfmtCheck detekt test`
7. **Commit:** Use conventional commit format
8. **PR:** Match PR description to commit body

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
5. **Android PanamaPort:** Not yet integrated; planned for Phase 8

## Session Logs (devlog/)

**Claude Code MUST maintain a development log in `devlog/` to track changes, decisions, and reasoning across sessions.**

### When to Create/Update Session Logs

- **Start of each working session:** Create a new devlog entry if one doesn't exist for today, or continue the existing one if still working on the same task
- **During work:** Update the current session log as you make changes, add decisions, or discover new information
- **New session trigger:** Start a new file when beginning a fresh conversation or switching to a significantly different task

### File Naming

Format: `devlog/YYYY-MM-DD_<sequence>.md`
- Example: `2026-02-14_01.md`
- Increment sequence (`01`, `02`, ...) for multiple sessions on the same day

### What to Log

**Required sections:**
- **Agents:** Table of all AI agents that made changes (model ID, repo, branch, role)
- **Intent:** The "why" — user's goal, problem being solved, or feature being built
- **What Changed:** File-by-file list with timestamps `[YYYY-MM-DD HH:MM TZ]` and reasoning (group by repo if multi-repo)
- **Decisions:** Key architectural/implementation choices with timestamps `[YYYY-MM-DD HH:MM TZ]` and reasoning
- **Research & Discoveries:** Findings that informed the work — include links to docs, APIs, source repos, issues, or examples that were useful. Future sessions can reference these instead of re-discovering them.
- **Issues:** Problems encountered and resolutions (or open items). **CRITICAL:** Log failed attempts, reverted changes, and lessons learned. If you try an approach that doesn't work, document WHY it failed and what you learned. This prevents repeating the same mistakes in future sessions.
- **Next Steps:** What's left or what to pick up next

**Optional sections:**
- **Model Switches:** Log if you switch models mid-session (timestamp, reason, task context)

### Guidelines

- **Group related work** — One session = one file. Don't fragment minor edits into separate files.
- **Keep it narrative** — Write for a human reading the timeline, not just a machine-parsable log
- **Target 30-100 lines per file** — If a single session exceeds ~150 lines, consider splitting into `_01`, `_02`
- **Track "why" not just "what"** — Capture reasoning behind decisions, not just file diffs
- **Simplify for single-agent sessions** — If only one agent and one repo, skip the full table, just note agent/model/branch at top
- **Update as you go — CRITICAL** — The devlog is a living document. Update it automatically after each significant change:
  - After creating/modifying files
  - After making decisions
  - Before committing changes
  - When encountering issues

  **DO NOT wait for the user to ask "update devlog"** — this should happen proactively as part of your workflow. The devlog is a real-time journal, not a post-hoc summary.
- **Record final state, not iterations** — If you change a file multiple times (e.g., iterating on a format), collapse into one entry describing the final result. Don't log each intermediate edit.
- **Group similar files** — If the same change applies to multiple files (e.g., creating native stubs for 3 platforms), combine into one entry: `RenderSurface.{macos,linux,mingw}.kt`
- **Add a topic to the title** — Use `# Session — YYYY-MM-DD (Topic)` to make sessions scannable (e.g., "Build System + WgpuRenderer", "Project Scaffolding")
- **Log failures and lessons learned** — If you try an approach that doesn't work and have to revert or change direction, document it in the Issues section with:
  - What you tried
  - Why it didn't work (error message, conceptual problem, API limitation)
  - What you learned
  - What approach you used instead

  This prevents wasting time repeating failed experiments in future sessions. Failed attempts are valuable knowledge.

### Example Structure

```markdown
# Session — YYYY-MM-DD (Topic)

## Agent
**Claude Code** (model-id) @ `repository` branch `branch-name`

## Intent
<Why this session exists...>

## What Changed
- **[YYYY-MM-DD HH:MM TZ]** `path/to/file` — <what changed and why>
- **[YYYY-MM-DD HH:MM TZ]** `path/to/another/file` — <what changed and why>

## Decisions
- **[YYYY-MM-DD HH:MM TZ]** **<Decision>** — <reasoning>
- **[YYYY-MM-DD HH:MM TZ]** **<Another decision>** — <reasoning>

## Research & Discoveries
- <findings>

## Issues
- <problems and resolutions>

## Next Steps
- <what's next>
```

**Timestamp format:** Use `[YYYY-MM-DD HH:MM TZ]` where TZ is your local timezone (PST, EST, UTC, etc.)
- Example: `[2026-02-14 10:30 PST]`
- Use standard timezone abbreviations (PST/PDT, EST/EDT, UTC, CET, JST, etc.)
- Be consistent within a session — don't mix timezones

See `devlog/README.md` for full conventions and multi-agent format.

## Random Notes

- When clearing caches, do NOT delete `.gradle` folder - be specific about what's deleted
- Create all temporary/scratch files in `.scratch/` folder
- When asked to update docs, also consider updating external repos if applicable
- Always test on at least JVM platform before marking as complete
- Shader code (WGSL) should be validated with web tools: https://webgpufundamentals.org/webgpu/lessons/webgpu-wgsl-validation.html
- wgpu4k resources use `.bind()` within `AutoClosableContext` for automatic cleanup - always use this pattern
- For performance-critical code, prefer inline value classes for Vec2/Vec3/Vec4 (already done)
- Keep shader uniforms aligned to 16 bytes (WGSL std140 layout)