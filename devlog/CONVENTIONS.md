# Devlog Conventions

**AI coding agents MUST maintain a development log in `devlog/` to track changes, decisions, and reasoning across sessions.** Update it proactively as you work — do not wait for the user to ask.

## Formats

**Timestamps:** ISO 8601 with UTC offset — e.g. `2026-02-14T10:30-08:00`. Be consistent within a file.

**Devlog file:** `devlog/NNNNNN-<branch-name>.md` — one file per branch.
- `NNNNNN`: zero-padded 6-digit sequence number (check highest in `devlog/` and increment)
- `<branch-name>`: Git branch name with `/` replaced by `-`
- One flat file per branch — combine all work into a single set of sections, not separate sessions
- On merge conflict: rebase onto main and renumber your file

**Plan file:** `devlog/plans/NNNNNN-NN-<description>.md`
- First `NNNNNN` matches the branch devlog prefix; `NN` is a required two-digit plan sequence (01, 02, ...)
- Structure: `## Thinking` (exploratory reasoning) then `## Plan` (actionable steps)
- Plans are append-only — if a plan changes during execution, note the deviation in the devlog

## Sections

Each devlog file should have:

- **Agent:** `Agent Name (model-id) @ repository branch branch-name` — identify the agent, model, repository, and branch. If the agent or model changes mid-branch, add a new Agent line with a timestamp noting the switch.
- **Intent:** User's goal or problem being solved
- **What Changed:** `timestamp path/to/file — what and why` (record final state, not iterations; group similar files)
- **Decisions:** `timestamp Decision — reasoning`
- **Issues:** Problems, failed attempts, and resolutions. Log what you tried, why it failed, and what you learned.
- **Commits:** `hash — message`

Optional sections (omit if empty):
- **Research & Discoveries:** Findings, links — so future agents don't re-discover them
- **Lessons Learned:** Reusable insights, pitfalls, API quirks
- **Next Steps:** What's left to do

## Rules

- **One flat file per branch** — append to existing sections as work progresses. Don't split into sessions.
- **Track "why" not just "what"** — capture reasoning, not file diffs
- **Append-only across conversations** — if continuing from a previous conversation, append new entries to existing sections rather than rewriting them
- **Never log secrets** — no API keys, tokens, credentials, PII, or private URLs. Use placeholders like `<API_KEY>` instead.
