# 000029 — ci/release-commit-author

**Agent:** Claude (claude-opus-4-8) @ prism branch ci/release-commit-author

## Intent

Attribute the release workflow's automated commit (the Package.swift update +
tag) to the maintainer instead of the generic `github-actions[bot]`, and
SSH-sign the commit + tag so GitHub marks them "Verified". Workflows stay
separate — no consolidation of release.yml and publish.yml.

## What Changed

- 2026-06-07T21:29-07:00 `.github/workflows/release.yml` — in the "Commit, tag,
  and push" step, set the git author to `David Berrios` /
  `<maintainer-email>` instead of `github-actions[bot]` /
  `github-actions[bot]@users.noreply.github.com`.
- 2026-06-07T21:34-07:00 `.github/workflows/release.yml` — SSH-sign the release
  commit and tag (`gpg.format=ssh`, `commit.gpgsign`/`tag.gpgsign`, `commit -S`,
  `tag -s`) using a `GIT_SSH_SIGNING_KEY` secret, so GitHub marks them "Verified".
- 2026-06-07T21:47-07:00 `.github/workflows/release.yml` — source the commit
  author email from a `RELEASE_GIT_EMAIL` secret (guarded — the step fails if it
  is unset) instead of hardcoding it; the name stays `David Berrios`.

## Decisions

- 2026-06-07T21:29-07:00 Keep `release.yml` and `publish.yml` separate; only the
  commit authorship changes. (An earlier exploration toward merging Maven publish
  into release.yml was discarded at the user's direction — "keep them separate".)
- 2026-06-07T21:29-07:00 Scope limited to the release commit identity. POM
  developer info and repo/SCM URLs in gradle.properties were intentionally left
  unchanged.
- 2026-06-07T21:34-07:00 SSH signing over GPG. Reasoning: no gpg-agent /
  loopback-pinentry / passphrase wrangling in CI, no third-party action (prism
  uses only `actions/*` + `gradle/actions`), and the dedicated CI key is
  unrelated to the Maven `SIGNING_KEY` (whose UID is the Central identity, not
  `<maintainer-email>`). Verified the exact step logic locally: signed commit
  and tag both report a good signature (`%G?` = `G`).

## Issues

- None. YAML validates; local smoke test of the step (ssh-keygen derive pub +
  git config ssh signing + `commit -S` + `tag -s`) yields `git verify-commit`
  and `git verify-tag` "Good signature".
- 2026-06-07T21:45-07:00 (PR review) Redacted the maintainer email to
  `<maintainer-email>` (CONVENTIONS.md forbids PII in devlogs; the real value
  lives in release.yml) and fixed timestamp offsets to the `-07:00` colon form.

## Required setup (one-time, by maintainer)

- Generate a no-passphrase signing key:
  `ssh-keygen -t ed25519 -C "prism release signing" -f prism_release_signing -N ""`
- Add `prism_release_signing.pub` to GitHub → Settings → SSH and GPG keys →
  New SSH key → **Key type: Signing Key**.
- Add the private key `prism_release_signing` as repo secret
  `GIT_SSH_SIGNING_KEY`.
- Ensure the maintainer email is a verified email on the GitHub account, and set
  it as the `RELEASE_GIT_EMAIL` secret (used as the commit author email).

## Commits

- HEAD — ci: attribute and SSH-sign the release commit and tag
