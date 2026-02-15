# Development Log (devlog)

This folder contains a record of development sessions with Claude Code. Each file documents changes, decisions, research, and reasoning from a branch's work.

## Purpose

- **Recoverability:** Track what changed, why, and when
- **Timeline:** Create a narrative of decisions and discoveries
- **Multi-agent coordination:** Record which agents worked on what, with which models
- **Context preservation:** Maintain "why" reasoning, not just "what" changes
- **Avoid repeating mistakes:** Failed attempts and lessons learned are logged

## File Naming

Format: `NNNNNN-<branch-name>.md` — **one file per branch**.

- `NNNNNN` is a zero-padded 6-digit sequence number for chronological ordering
- `<branch-name>` is the Git branch name with `/` replaced by `-`
- Examples: `000001-initial-scaffolding.md`, `000012-feat-pbr-materials.md`
- Multiple sessions on the same branch use `## Session N — Topic` headers within the file

### Assigning the sequence number

1. Before merging your PR, rebase onto main (required by branch protection)
2. Check the highest existing number in `devlog/`
3. Use the next number for your file
4. This is safe because branches must be up-to-date with main before merging

## Reading the Log

- Files sort chronologically by sequence number
- Each file contains one or more sessions, separated by `---` dividers
- Session headers include the topic, start time, and model used
- Check the Agent line to see who (and which model) made changes
- Intent section explains the "why" behind the work
- Issues section contains lessons learned and failed approaches

These files are version-controlled and part of the repository history.