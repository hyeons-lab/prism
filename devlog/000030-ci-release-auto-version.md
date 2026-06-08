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

## Issues

- None. YAML validates. Smoke-tested the resolution shell logic against the real
  `gradle.properties` (now `VERSION_NAME=0.1.0` after #58): empty input resolves
  to `0.1.0`; an override (`0.2.0-rc.1`) is used verbatim; both pass the semver
  check.

## Commits

- HEAD — ci: default release version from gradle.properties, allow override
