# Session Logs (devlog/)

**Claude Code MUST maintain a development log in `devlog/` to track changes, decisions, and reasoning across sessions.** Update it proactively as you work — do not wait for the user to ask.

## Formats

**Timestamps:** `[YYYY-MM-DD HH:MM TZ]` — e.g. `[2026-02-14 10:30 PST]`. Be consistent within a session.

**Devlog file:** `devlog/NNNNNN-<branch-name>.md` — one file per branch.
- `NNNNNN`: zero-padded 6-digit sequence number (check highest in `devlog/` and increment)
- `<branch-name>`: Git branch name with `/` replaced by `-`
- Multiple sessions use `## Session N — Topic` headers within the same file
- On merge conflict: rebase onto main and renumber your file

**Plan file:** `devlog/plans/NNNNNN-NN-<description>.md`
- First `NNNNNN` matches the branch devlog prefix; `NN` is a required two-digit plan sequence (01, 02, ...)
- Structure: `## Thinking` (exploratory reasoning) then `## Plan` (actionable steps)
- Plans are append-only — if a plan changes during execution, note the deviation in the devlog

## Per-Session Sections

Under each `## Session N — Topic (timestamp, model-name)` header, include:

- **Intent:** User's goal or problem being solved
- **What Changed:** `[timestamp] path/to/file — what and why` (record final state, not iterations; group similar files)
- **Decisions:** `[timestamp] Decision — reasoning`
- **Issues:** Problems, failed attempts, and resolutions. Log what you tried, why it failed, and what you learned.
- **Commits:** `hash — message`

Optional sections (omit if empty):
- **Research & Discoveries:** Findings, links — so future sessions don't re-discover them
- **Lessons Learned:** Reusable insights, pitfalls, API quirks
- **Next Steps:** (end of file) What's left to do across all sessions

## Rules

- **One file per branch** — new session = new `## Session N` header, not a new file
- **Track "why" not just "what"** — capture reasoning, not file diffs
- **Append-only across sessions** — update your own session freely; never edit prior sessions (add corrections instead)
- **Never log secrets** — no API keys, tokens, credentials, PII, or private URLs. Use placeholders like `<API_KEY>` instead.
