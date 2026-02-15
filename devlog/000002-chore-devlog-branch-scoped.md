# 000002-chore-devlog-branch-scoped

## Session 1 — Restructure devlogs for PR workflows (2026-02-14 23:00 PST, opus-4-6)

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/devlog-branch-scoped`

### Intent
Restructure devlog naming from date-based (`YYYY-MM-DD.md`) to branch-scoped (`NNNNNN-branch-name.md`) with 6-digit sequence number prefixes. The date-based scheme caused merge conflicts when multiple branches worked on the same day. Branch-scoped files eliminate collisions entirely.

### What Changed
- **[2026-02-14 23:00 PST]** `devlog/2026-02-14.md` → `devlog/000001-initial-scaffolding.md` — Renamed to new convention. Updated H1 heading to match filename.
- **[2026-02-14 23:00 PST]** `devlog/README.md` — Rewritten with branch-scoped naming, 6-digit prefix, and sequence number assignment instructions.
- **[2026-02-14 23:00 PST]** `AGENTS.md` — Updated Session Logs section: branch-scoped files, sequence number workflow (assign at branch start, renumber on conflict during rebase), main is protected, simplified append-only guideline, Next Steps optional for single-session branches, fixed inconsistent example headers.
- **[2026-02-14 23:11 PST]** `devlog/000002-chore-devlog-branch-scoped.md` — Created devlog for this branch.

### Decisions
- **[2026-02-14 23:00 PST]** **Assign sequence number at branch start, not before merge** — Simpler workflow. If it conflicts at merge time, the branch needs rebasing anyway (branch protection requires up-to-date), at which point the number gets updated.
- **[2026-02-14 23:00 PST]** **6-digit prefix** — Sufficient for 999,999 entries. Sorts lexicographically.
- **[2026-02-14 23:00 PST]** **No devlogs directly on main** — Main is protected; all work goes through PRs. Devlog files arrive on main via merge.

### Issues
- **[RESOLVED] Sequence number workflow gap** — Initial version said to create the file at session start but assign the number before merge. Fixed: assign at start, renumber on conflict.
- **[RESOLVED] Example structure inconsistency** — Session 2 header was missing the date. Fixed.
- **[RESOLVED] Stale H1 heading** — `000001-initial-scaffolding.md` still had `# 2026-02-14`. Fixed to match filename.

### Commits
- `5bc712b` — chore: restructure devlogs to branch-scoped files with sequence numbers
