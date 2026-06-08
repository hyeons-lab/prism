# 000026 — ci/maven-publish-workflow

**Agent:** Claude (claude-opus-4-8) @ prism branch ci/maven-publish-workflow

## Intent

Add a CI workflow to publish Prism's reusable Kotlin Multiplatform libraries to
Maven Central, "like the triage publish workflow". The triage workflow targets
crates.io (Rust); Prism is KMP, so the analog is Maven Central via the
`com.vanniktech.maven.publish` plugin. The user explicitly chose vanniktech.

## What Changed

- 2026-06-07T19:59-0700 `.github/workflows/publish.yml` — new
  `workflow_dispatch` workflow with a `dry_run` boolean input (default true),
  mirroring triage's dry-run/real shape. Runs on `macos-26` (to include Apple
  klib targets), sets up JDK 25/21 + Gradle + konan cache + setup-wgpu4k (iOS
  rust targets), then either `publishToMavenLocal` (dry run, no secrets) or
  `publishToMavenCentral` (real, with `ORG_GRADLE_PROJECT_*` secrets).
- 2026-06-07T19:59-0700 `devlog/plans/000026-01-maven-publish-workflow.md` —
  design doc and audit of existing publishing config.

## Decisions

- 2026-06-07T19:59-0700 Workflow only — no per-module changes. Reasoning: 10
  reusable libraries (math, core, renderer, scene, ecs, input, assets, audio,
  native-widgets, compose) already carry a complete `mavenPublishing { }` block,
  and `gradle.properties` already has GROUP/VERSION_NAME/POM_*. The plugin and
  catalog entry exist. The only missing piece was CI.
- 2026-06-07T19:59-0700 Excluded modules left as-is: demos (apps), prism-ios
  (XCFramework via release.yml), prism-js (wasmJs executable / web SDK bundle),
  prism-native (C-ABI shared libs), prism-flutter (integration bridge,
  maintainer left unpublished). None are Maven-consumable Kotlin libraries.
- 2026-06-07T19:59-0700 macOS runner, mirroring release.yml. Apple klib targets
  are silently dropped from the publication on a Linux runner.
- 2026-06-07T19:59-0700 Dry run → `publishToMavenLocal` (Gradle analog of
  `cargo package`); real → `publishToMavenCentral`. No tagging/GH release here —
  release.yml owns that for the XCFramework, so the two workflows don't contend
  over tags.
- 2026-06-07T19:59-0700 `--no-configuration-cache` on publish/sign tasks
  (vanniktech publish + signing tasks are not config-cache compatible).

## Issues

- None encountered yet. Real publication requires repo secrets the user must add
  (`MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `SIGNING_KEY`,
  `SIGNING_KEY_PASSWORD`) — passed through as `ORG_GRADLE_PROJECT_*` env vars.

## Research & Discoveries

- vanniktech 0.36.0: `signAllPublications()` makes signing non-required for
  SNAPSHOT versions and for `publishToMavenLocal`, so the dry-run path needs no
  secrets. Signing is enforced only for non-SNAPSHOT remote publication.
- Known limitation (pre-existing, not introduced here): renderer-and-up modules'
  POMs depend on `io.ygdrasil:wgpu4k:0.2.0-SNAPSHOT`, a forked snapshot not on
  Maven Central, so those artifacts aren't externally resolvable until wgpu4k is
  published. prism-math / prism-core (no wgpu4k dep) are cleanly consumable.

## Commits

- HEAD — ci: add Maven Central publish workflow

## Next Steps

- User: add the four repo secrets, then run the workflow with `dry_run=true`
  first to verify packaging, then `dry_run=false` to publish.
