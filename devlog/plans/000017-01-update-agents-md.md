# 000017-01 — Update AGENTS.md

## Thinking

AGENTS.md is the primary project reference for AI coding agents. It needs to reflect the actual current state of the project — what's been built, what's working, what's next.

Key areas to review:
1. **Current Project Status** — check if glTF 2.0 (M10) and mobile orbit drag fix are reflected
2. **Architecture** — ensure module descriptions, platform implementations, and abstractions are accurate
3. **Build commands** — verify commands still work and are complete
4. **Known Issues** — update resolved issues, add new ones

Looking at recent commits:
- `b8b184a fix: mobile orbit drag, iOS build, and docs cleanup (#41)` — orbit drag fix
- `8df9afc feat: implement glTF 2.0 asset loading with DamagedHelmet demo (M10) (#38)` — glTF M10
- `61eff60 ci: add 30-minute timeout to Claude Code Review job`
- `aefa404 feat: implement PBR materials pipeline (M9)`
- `75450d8 feat: implement Flutter integration (M11)`

The "What's next" section mentions glTF 2.0 as upcoming, but M10 (glTF) is already done. The status section likely needs updating.

## Plan

1. Read the current AGENTS.md carefully
2. Read BUILD_STATUS.md and PLAN.md if they exist to get authoritative current state
3. Check prism-demo-core for DamagedHelmet demo to confirm glTF status
4. Update **Current Project Status**:
   - Move glTF 2.0 asset loading from "What's next" to "What works"
   - Add mobile orbit drag fix
   - Add any other completed milestones
5. Review Architecture section for accuracy (new modules like prism-assets loader implementations)
6. Update Known Issues if any have been resolved
7. Format, commit, push, draft PR
