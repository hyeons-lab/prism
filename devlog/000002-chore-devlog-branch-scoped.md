# 000002-chore-devlog-branch-scoped

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/devlog-branch-scoped`

## Intent

Restructure devlog naming from date-based (`YYYY-MM-DD.md`) to branch-scoped (`NNNNNN-branch-name.md`) with 6-digit sequence number prefixes. The date-based scheme caused merge conflicts when multiple branches worked on the same day.

## What Changed

- **2026-02-14T23:00-08:00** `devlog/2026-02-14.md` -> `devlog/000001-initial-scaffolding.md` — Renamed to new convention.
- **2026-02-14T23:00-08:00** `devlog/README.md` — Rewritten with branch-scoped naming, 6-digit prefix, and sequence number assignment instructions.
- **2026-02-14T23:00-08:00** `AGENTS.md` — Updated Session Logs section: branch-scoped files, sequence number workflow, main is protected.
- **2026-02-14T23:11-08:00** `devlog/000002-chore-devlog-branch-scoped.md` — Created devlog for this branch.

## Decisions

- **2026-02-14T23:00-08:00** **Assign sequence number at branch start, not before merge** — Simpler workflow. If it conflicts at merge time, rebase.
- **2026-02-14T23:00-08:00** **6-digit prefix** — Sufficient for 999,999 entries. Sorts lexicographically.
- **2026-02-14T23:00-08:00** **No devlogs directly on main** — Main is protected; devlogs arrive via merge.

## Issues

- **Sequence number workflow gap** — Initial version said to create the file at session start but assign the number before merge. Fixed: assign at start, renumber on conflict.

## Commits

- `5bc712b` — chore: restructure devlogs to branch-scoped files with sequence numbers
