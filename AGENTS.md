# AGENTS.md

This file provides guidance to ai agents when working with code in this repository.

## Project Overview

Prism is a modular, cross-platform 3D game engine built with Kotlin Multiplatform (KMP). It targets JVM Desktop, Web (WASM), iOS, Android, and macOS/Linux/Windows native platforms with a single Kotlin codebase. The rendering backend uses **wgpu4k** for cross-platform GPU access (Vulkan, Metal, DX12, WebGPU).

- **Group ID**: com.hyeons-lab
- **Version**: 0.1.0-SNAPSHOT
- **Docs**: PLAN.md (tech spec), BUILD_STATUS.md (milestone status)

## Build Commands

```bash
# Build
./gradlew build                                   # Full build (all modules, all targets)
./gradlew :prism-renderer:build                   # Single module

# Format + Lint
./gradlew ktfmtFormat                             # Auto-format (Google style, 100-char width)
./gradlew ktfmtCheck detektJvmMain                # Check formatting + static analysis

# Tests
./gradlew jvmTest                                 # All JVM tests
./gradlew :prism-math:jvmTest                     # Single module JVM tests
./gradlew :prism-core:macosArm64Test              # Native platform tests
./gradlew macosArm64Test iosSimulatorArm64Test    # All Apple native tests

# Full CI quality check (what CI runs)
./gradlew ktfmtCheck detektJvmMain jvmTest

# Run demos (JVM Desktop)
./gradlew :prism-demo-core:jvmRun                 # GLFW window (Metal/Vulkan)
./gradlew :prism-demo-core:runCompose              # Compose Desktop with embedded 3D
./gradlew :prism-demo-core:runDebugExecutableMacosArm64  # macOS native demo

# Build for platforms
./gradlew :prism-demo-core:wasmJsBrowserDistribution     # WASM for web
./gradlew :prism-demo-core:assemblePrismDemoDebugXCFramework  # iOS XCFramework
./gradlew :prism-ios:assemblePrismDebugXCFramework       # Prism.xcframework (SPM)
```

### Environment Setup

- **JDK 21+** required (JDK 25 used for demo/native-widgets/compose modules via `jvmToolchain(25)`)
- **Gradle 9.2.0** (wrapper included)
- **wgpu4k 0.2.0-SNAPSHOT** must be built from source (see below)
- macOS/iOS: Xcode 14+ with Command Line Tools
- Android: Android SDK with NDK, API 28+

### wgpu4k Dependency (Critical)

Prism depends on wgpu4k 0.2.0-SNAPSHOT from **forked repos** (hyeons-lab/wgpu4k, hyeons-lab/wgpu4k-native, hyeons-lab/webgpu-ktypes). These must be built and published to Maven local before Prism compiles. The specific commits are tracked in `gradle/libs.versions.toml` under `wgpu4kCommit`, `wgpu4kNativeCommit`, and `webgpuKtypesCommit`.

CI handles this via `.github/actions/setup-wgpu4k`. For local development:
```bash
git clone https://github.com/hyeons-lab/wgpu4k.git
cd wgpu4k && ./gradlew publishToMavenLocal
```
Verify: `ls ~/.m2/repository/io/ygdrasil/wgpu4k/0.2.0-SNAPSHOT/`

## Architecture

### Module Dependency Graph

```
prism-math                  # Vec2/3/4, Mat3/4, Quaternion, Transform
  └─► prism-core            # Engine, GameLoop, Time, Subsystem, Store<State,Event>
       ├─► prism-renderer    # Renderer interface + WgpuRenderer (wgpu4k)
       │    ├─► prism-scene       # Scene graph (Node, MeshNode, CameraNode, LightNode)
       │    ├─► prism-ecs         # Entity-Component-System (World, Entity, Component, System)
       │    └─► prism-assets      # Asset management (loaders, glTF, FileReader)
       ├─► prism-input       # Cross-platform input handling
       └─► prism-audio       # Audio interface (stub)

prism-renderer
  └─► prism-native-widgets   # Platform-specific GPU surfaces (PrismSurface)
       ├─► prism-compose     # Compose Multiplatform integration
       └─► prism-flutter     # Flutter bridge (MethodChannel + WASM)

prism-demo-core              # Shared demo code (all platforms)
prism-android-demo           # Android app
prism-ios-demo               # iOS Swift app (consumes XCFramework)
prism-ios                    # XCFramework aggregator for SPM
```

### KMP Source Structure

Each module follows the standard KMP layout: `commonMain/` (~90% of code), plus platform-specific source sets (`jvmMain/`, `wasmJsMain/`, `androidMain/`, `iosMain/`, `macosArm64Main/`, `linuxX64Main/`, `mingwX64Main/`). Tests go in corresponding `*Test/` directories.

### Key Architectural Patterns

- **MVI (Model-View-Intent)**: `Store<State, Event>` interface in prism-core drives UI state management. `EngineStore`/`DemoStore` in prism-compose implement pure reducers. All Compose and Flutter demos use this pattern.
- **Suspend factory for surfaces**: `createPrismSurface()` is an `expect`/`actual` suspend function with 7 platform implementations. All demo consumers use this entry point.
- **AutoClosableContext**: All wgpu4k GPU resources use `.bind()` within `AutoClosableContext` for automatic cleanup. Always follow this pattern.
- **ECS-driven rendering**: `RenderSystem` queries entities with `TransformComponent` + `MeshComponent` + `MaterialComponent` and drives `WgpuRenderer`. Lights come from `LightComponent` queries.
- **PBR pipeline**: 4 bind groups (scene, object, material, environment). Shader uniforms must be 16-byte aligned (WGSL std140 layout). HDR render target (RGBA16Float) with Khronos PBR Neutral tone mapping.

### Build System

- **Convention plugin**: `build-logic/` contains `prism-quality.gradle.kts` (KtFmt Google style + Detekt). All modules apply `id("prism-quality")`.
- **Version catalog**: `gradle/libs.versions.toml` centralizes all dependency versions.
- **Android plugin**: Modules use `com.android.kotlin.multiplatform.library` (not `com.android.library`).
- **wgpu4k resolution**: `settings.gradle.kts` configures `mavenLocal` scoped to `io.ygdrasil` and `com.hyeons-lab` groups.

## Code Quality

**All warnings are errors.** Every module sets `allWarningsAsErrors.set(true)`.

- **No star imports** — always use explicit imports
- **`expect`/`actual`** for platform code, with `-Xexpect-actual-classes` compiler flag
- **Inline functions** in commonMain must be `public` or `internal` (not `private`)
- **Logging**: Use Kermit (`Logger.i`, `Logger.w`, `Logger.e`)
- **Testing**: `kotlin.test` + Kotest 6.0.7 assertions (`shouldBe`, `shouldContain`, `plusOrMinus`)
- **Immutable data classes** for components and value types

## Development Workflow

### Git Worktree Workflow (Required)

All feature work uses git worktrees. Never switch branches in the main checkout.

```bash
# Start a feature (from ~/development/prism)
git fetch origin
git worktree add worktrees/<branch-name> -b <type>/<branch-name> origin/main
cd worktrees/<branch-name>
git branch --unset-upstream

# Create draft PR immediately
gh pr create --draft --title "WIP: <description>" --body ""

# After merge, clean up
cd ~/development/prism
git worktree remove worktrees/<branch-name>
git branch -d <type>/<branch-name>
```

Rules:
- Never commit directly to `main`
- Create the worktree *before* planning or exploring
- `worktrees/` is gitignored

### Devlog Workflow

Before writing code, create plan and devlog files in `devlog/`:

1. **Branch devlog**: `devlog/NNNNNN-<branch-name>.md` (increment from highest existing number)
2. **Plan file**: `devlog/plans/NNNNNN-NN-<description>.md` with `## Thinking` then `## Plan` sections
3. Plans are append-only; note deviations in the devlog

See `devlog/CONVENTIONS.md` for full spec. Key rules:
- Timestamps: ISO 8601 with UTC offset (e.g. `2026-02-14T10:30-08:00`)
- Required sections: Agent, Intent, What Changed, Decisions, Issues, Commits
- Track reasoning, not file diffs
- Never log secrets

### Commit Messages

Conventional commits format: `<type>: <description>` with optional body.
Types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`

**PR descriptions MUST match the commit message body** (for squash consistency).

### Feature Development Steps

1. Create worktree → 2. Create devlog + plan → 3. Draft PR → 4. Define `expect` interfaces in `commonMain` → 5. Add `actual` implementations → 6. Tests in `commonTest` → 7. `./gradlew ktfmtFormat` → 8. `./gradlew ktfmtCheck detektJvmMain jvmTest` → 9. Commit → 10. Update PR description

### PR Submission Checklist

Before marking a PR ready for review:

- [ ] `./gradlew ktfmtFormat` run — no formatting violations
- [ ] `./gradlew ktfmtCheck detektJvmMain jvmTest` passes locally
- [ ] PR title concise (under 72 chars), imperative mood
- [ ] PR description matches final squash commit body (summary + bullets)
- [ ] Devlog updated: `What Changed`, `Decisions`, `Commits` filled in
- [ ] No secrets, credentials, or private URLs committed

CI runs `ktfmtCheck + detekt + jvmTest` on every push. PRs must pass CI before merging.

## Current Project Status

**Phase:** glTF 2.0 asset loading complete (M10), all platforms operational

**Completed milestones:** Math (M1) · Engine core + ECS (M2–M3) · JVM rendering (M4) · Compose Desktop (M5) · WASM/Web (M6) · iOS native + Compose (M7) · Android (M8) · PBR materials (M9) · glTF 2.0 loading (M10) · Flutter integration (M11)

**Key capabilities:** 268 tests · PBR Cook-Torrance BRDF · IBL · HDR tone mapping · glTF 2.0 with PBR textures · DamagedHelmet demo · mobile orbit drag · Flutter (Android/iOS/Web) · all 7 platform `createPrismSurface()` actuals

**What's next:** Flutter Desktop (macOS, Windows, Linux)

See BUILD_STATUS.md and PLAN.md for detailed milestone status.

## Known Issues & Workarounds

1. **Inline function visibility**: Inline functions in commonMain must be public/internal, not private
2. **expect/actual warnings**: Modules need `-Xexpect-actual-classes` in `compilerOptions`
3. **wgpu4k suspend functions**: Bridge with `runBlocking` or use coroutine-based engine loop
4. **WASM build size**: Use `-Xwasm-enable-array-range-checks=false`
5. **macOS GLFW**: Requires `-XstartOnFirstThread` JVM arg (conflicts with Swing/Compose EDT — handled in `prism-demo-core/build.gradle.kts`)
6. **Shader validation**: Use https://webgpufundamentals.org/webgpu/lessons/webgpu-wgsl-validation.html

## Debugging Tips

**JVM Desktop:**
- Run with `-Dwgpu.backend=vulkan` (Linux/Windows) or `-Dwgpu.backend=metal` (macOS)
- Enable wgpu validation layers: `-Dwgpu.validation=true`

**WASM:**
- Use browser DevTools console for errors
- Check WebGPU support: `navigator.gpu !== undefined`

**Native (iOS/macOS):**
- Use Xcode Instruments for Metal debugging
- Use `Logger.d` for cross-platform logging (Kermit)

## Miscellaneous

- When clearing caches, do NOT delete the `.gradle` folder — be specific about what's deleted
- Create temporary/scratch files in `.scratch/`
- Always test on at least JVM platform before marking as complete
- Shader uniforms must be 16-byte aligned (WGSL std140 layout)
- wgpu4k resources use `.bind()` within `AutoClosableContext` — always use this pattern
- WGSL shaders should be validated with the web tool linked in Known Issues
