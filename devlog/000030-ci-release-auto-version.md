# 000030 — ci/release-auto-version

**Agent:** Claude (claude-opus-4-8) @ prism branch ci/release-auto-version

## Intent

Stop requiring the maintainer to type a version when triggering the Release
workflow. Default to the project's published artifact version and allow an
optional override.

## What Changed

- 2026-06-07T22:11-07:00 `.github/workflows/release.yml` — make the `version`
  workflow_dispatch input optional (`required: false`, `default: ""`). Renamed
  "Validate version input" → "Resolve version" (`id: version`): when the input
  is blank it reads `VERSION_NAME` from `gradle.properties`; a non-blank input
  overrides it. The step validates semver + tag-uniqueness and exposes the
  resolved value as `steps.version.outputs.version`. Downstream steps (Update
  Package.swift, Commit/tag/push, Create GitHub Release) now reference that
  output instead of `inputs.version`.

## Decisions

- 2026-06-07T22:11-07:00 Read the version from `gradle.properties` (`VERSION_NAME`),
  not the toml. The user first said "the toml file", but the artifact version is
  not there — `gradle/libs.versions.toml` only holds tool/dependency versions
  (kotlin, agp, wgpu4k, …). `VERSION_NAME` in `gradle.properties` is the single
  source vanniktech publishes from, so reading it there keeps one source of
  truth; adding the version to the toml would create a second that can drift.
  Confirmed with the user.
- 2026-06-07T22:11-07:00 Expose the resolved version as a step output and thread
  it through downstream steps, rather than re-reading the file in each step —
  one resolution + validation point.
- 2026-06-07T22:25-07:00 (PR review) Three hardening fixes to the Resolve step:
  (1) extract `VERSION_NAME` with `awk` instead of a `grep | head | cut | tr`
  pipeline — Actions runs bash with `-e -o pipefail`, so a missing key would trip
  the pipeline before the explicit empty-VERSION error; `awk` is one process that
  exits 0 with no match and tolerates whitespace around `=`. (2) Check tag
  uniqueness with `git show-ref --verify --quiet refs/tags/v$VERSION` instead of
  `git rev-parse v$VERSION`, which resolves any ref (a branch named `vX.Y.Z` would
  false-positive). (3) Broaden the semver regex to accept hyphenated prerelease
  (`1.2.3-alpha-1`) and build metadata (`1.2.3+build.5`), matching the "semver"
  error wording.

## Issues

- None. YAML validates. Smoke-tested the resolution shell logic against the real
  `gradle.properties` (now `VERSION_NAME=0.1.0` after #58): empty input resolves
  to `0.1.0`; an override (`0.2.0-rc.1`) is used verbatim; both pass the semver
  check.

## Commits

- 852baf6 — ci: default release version from gradle.properties, allow override
- HEAD — ci: harden version resolution (awk, tag-ref check, semver regex) (PR review)
