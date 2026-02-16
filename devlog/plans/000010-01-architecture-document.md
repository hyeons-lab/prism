# Plan: Architecture Document + AGENTS.md Workflow Update

## Thinking

The Prism engine has grown to ~4,200 lines across 12 modules with extensive cross-platform support (JVM, WASM, iOS, macOS native, Android stubs). The codebase has a project plan (PLAN.md) and build status (BUILD_STATUS.md) but lacks a standalone architecture document that captures the design decisions, patterns, data flow, performance characteristics, and future improvement areas in a format useful for contributors and AI agents. Additionally, the AGENTS.md lacks guidance on branch/worktree workflow and plan tracking for new bodies of work.

Key questions resolved during exploration:
- **Mat4 storage**: Column-major FloatArray, matches GPU convention — no transposition needed on upload
- **Perspective matrix**: Targets WebGPU [0,1] depth range, not OpenGL [-1,1]
- **ECS storage**: Map-of-maps (entity-centric sparse), keyed by class simpleName — simple but O(N) queries
- **Scene graph vs ECS**: Parallel abstractions, not integrated. Demo uses ECS for rendering.
- **PrismSurface pattern**: Suspend factory + expect/actual per platform, async GPU init
- **GameLoop modes**: Self-driven (blocking while loop) vs externally-driven (tick per frame)

---

## Plan

### Task 1: Create `ARCHITECTURE.md`

Create a comprehensive architecture document at the project root covering 12 sections bottom-up:

1. Build System Architecture — Gradle, KMP targets, wgpu4k, quality tools, CI, iOS dist, Android
2. Module Architecture & Dependency Graph — layered deps, responsibilities, source sets
3. Core Engine Layer — Engine, GameLoop (fixed timestep), Time, Subsystem, Platform, Store
4. Math Layer — Vec2/3/4, Mat3/4 (column-major), Quaternion (optimized rotateVec3), Transform
5. ECS Layer — World (map-of-maps), Entity, Component, System, queries, built-in components/systems
6. Rendering Layer — Renderer interface, WgpuRenderer, frame flow, WGSL shaders, vertex layout, pipeline
7. Scene Graph — Node hierarchy, typed nodes, relationship to ECS
8. Platform Surface Layer — PrismSurface expect/actual, PrismPanel AWT integration
9. Compose Integration — MVI architecture, EngineStore, PrismView, PrismOverlay
10. Demo Architecture — DemoScene, platform entry points, ECS rendering pipeline
11. Platform-Specific Integration Patterns — macOS AWT+Metal, iOS, WASM, macOS native
12. Performance Characteristics & Future Improvements

### Task 2: Update `AGENTS.md`

Add "## Branching & Plan Workflow" section after "## Contribution Guidelines" covering:
- Git worktree requirement for every new body of work
- Plan file creation before coding
- Plan versioning (append-only, new file for major pivots)
- Progress tracking in branch devlog

### Task 3: Create devlog files

- `devlog/000010-docs-architecture-agents-workflow.md` — branch devlog
- `devlog/plans/000010-01-architecture-document.md` — this plan file
