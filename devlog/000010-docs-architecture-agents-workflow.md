# 000010-docs-architecture-agents-workflow

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `docs-architecture-agents-workflow`

## Intent

Create a comprehensive ARCHITECTURE.md documenting the engine's design decisions, data flow, performance characteristics, and future improvements. Update AGENTS.md with branching/worktree workflow guidance for new bodies of work.

## What Changed

- **2026-02-16T14:30-08:00** `ARCHITECTURE.md` — **NEW**: Comprehensive architecture document covering 12 sections: build system, module dependency graph, core engine (Engine/GameLoop/Time/Subsystem/Platform/Store), math layer (Vec/Mat/Quaternion/Transform), ECS (World/Entity/Component/System with storage model analysis), rendering (WgpuRenderer frame flow, WGSL shaders, GPU resource model), scene graph (Node hierarchy, typed nodes), platform surfaces (PrismSurface expect/actual, PrismPanel AWT), Compose integration (MVI architecture), demo architecture (entry points, ECS pipeline), platform-specific patterns (macOS AWT+Metal, iOS, WASM), and performance characteristics with future improvements.
- **2026-02-16T14:30-08:00** `AGENTS.md` — Added "## Branching & Plan Workflow" section after "## Contribution Guidelines". Documents git worktree requirement, plan file creation, plan versioning (append-only), and progress tracking.
- **2026-02-16T14:30-08:00** `devlog/plans/000010-01-architecture-document.md` — **NEW**: Plan file with thinking section and three-task plan.
- **2026-02-16T14:30-08:00** `devlog/000010-docs-architecture-agents-workflow.md` — **NEW**: This branch devlog.

## Decisions

- **2026-02-16T14:25-08:00** **Bottom-up section ordering** — ARCHITECTURE.md starts with build system and low-level modules (math, core), progresses through mid-level (ECS, renderer, scene), to high-level (compose, demo, platform patterns). This matches the dependency graph and lets readers build understanding incrementally.
- **2026-02-16T14:25-08:00** **Document current state honestly** — Performance section explicitly calls out known inefficiencies (O(N) ECS queries, no batching, no caching) rather than aspirational descriptions. Useful for contributors prioritizing optimization work.
- **2026-02-16T14:25-08:00** **Keep AGENTS.md workflow section concise** — Detailed conventions already live in `devlog/CONVENTIONS.md`. The new section references it rather than duplicating content.
