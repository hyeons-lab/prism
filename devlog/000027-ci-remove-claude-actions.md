# 000027 — ci/remove-claude-actions

**Agent:** Claude (claude-opus-4-8) @ prism branch ci/remove-claude-actions

## Intent

Remove the Claude Code GitHub Action integration from CI. User: "remove claude
github action" → confirmed scope = both workflows.

## What Changed

- 2026-06-07T20:19-0700 `.github/workflows/claude.yml` — deleted. The `@claude`
  mention bot (responded to `@claude` in issue/PR comments and reviews via
  `anthropics/claude-code-action@v1`).
- 2026-06-07T20:19-0700 `.github/workflows/claude-code-review.yml` — deleted.
  The automatic Claude code review on `pull_request` opened/ready_for_review/
  reopened.

## Decisions

- 2026-06-07T20:19-0700 Remove both workflows rather than one. Reasoning: user
  confirmed "Both" — drop the Claude integration from CI entirely.
- 2026-06-07T20:19-0700 Left the `CLAUDE_CODE_OAUTH_TOKEN` repo secret in place.
  Reasoning: deleting a secret is out of scope for a workflow change and is
  trivially re-addable; removing the workflows already stops all usage of it.
  Can be deleted separately from repo settings if desired.

## Commits

- HEAD — ci: remove Claude Code GitHub Action workflows
