# 000026-01 — Maven Central publish workflow

## Thinking

The user asked for a "publish to crates.io" workflow "like triage", then
corrected: Prism is Kotlin Multiplatform, not Rust, so the analogous target is
**Maven Central** via the vanniktech `com.vanniktech.maven.publish` plugin.

Audit of the current state:

- The vanniktech plugin (`com.vanniktech.maven.publish` 0.36.0) is already in
  the version catalog and applied (`apply false`) at the root.
- `gradle.properties` already carries the full publishing coordinates: `GROUP`
  (`com.hyeons-lab`), `VERSION_NAME` (`0.1.0-SNAPSHOT`), POM URL/SCM/license/
  developer metadata.
- 10 reusable Kotlin libraries already declare a complete `mavenPublishing {
  publishToMavenCentral(); signAllPublications(); pom { ... } }` block:
  prism-math, prism-core, prism-renderer, prism-scene, prism-ecs, prism-input,
  prism-assets, prism-audio, prism-native-widgets, prism-compose.
- Correctly *not* configured (and should stay that way):
  - demos (prism-android-demo, prism-demo-core, prism-flutter-demo) — apps,
    not libraries.
  - prism-ios — XCFramework aggregator, already released via `release.yml`.
  - prism-js — produces a wasmJs *executable* / web SDK bundle, not a Maven lib.
  - prism-native — produces native shared libs (.so/.dll/.dylib) for C-ABI FFI,
    not a Gradle-consumable artifact.
  - prism-flutter — integration bridge (XCFramework + Flutter plugin glue);
    left unpublished by the maintainer, kept that way for now.

So per-module configuration is already done and intentional. The only missing
piece is the **CI workflow** — which is exactly the ask.

Design choices, mirroring the triage `publish.yml` shape (workflow_dispatch +
dry-run boolean) but adapted to Gradle/KMP:

- **Runner = macOS (`macos-26`).** To include the Apple `klib` targets
  (iosArm64, iosSimulatorArm64, macosArm64) in the publication, the publish
  must run on macOS — Apple targets are silently dropped on Linux. Mirrors
  `release.yml`.
- **Dry-run path → `publishToMavenLocal`.** The Gradle analog of `cargo
  package`: assembles + writes every publication into `~/.m2` without uploading.
  Needs no secrets (vanniktech makes signing non-required for local + SNAPSHOT).
- **Real path → `publishToMavenCentral`.** Uploads to the Central Portal.
  For SNAPSHOT versions it routes to the snapshot repo; for release versions it
  creates a manually-releasable deployment (no auto-release, conservative).
- **No tag / GitHub Release here.** `release.yml` already owns tagging and the
  GH release for the XCFramework. Keeping the Maven workflow publish-only avoids
  two workflows fighting over tags.
- **`--no-configuration-cache`** on the publish/sign tasks — vanniktech's
  publish + signing tasks are not configuration-cache compatible.
- Reuse the existing env scaffolding: JDK 25 (toolchain) + JDK 21 (default),
  `gradle/actions/setup-gradle`, konan cache, and `./.github/actions/setup-wgpu4k`
  with the iOS rust targets (renderer-and-up modules need wgpu4k in mavenLocal
  to compile).

Secrets (added to repo settings by the user, standard vanniktech names passed
as `ORG_GRADLE_PROJECT_*`):

- `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` — Central Portal user token.
- `SIGNING_KEY` (ASCII-armored GPG secret key) / `SIGNING_KEY_PASSWORD` —
  required only for non-SNAPSHOT releases.

Known limitation to surface (not introduced by this workflow): renderer-and-up
modules' POMs depend on `io.ygdrasil:wgpu4k:0.2.0-SNAPSHOT`, a forked snapshot
not on Maven Central, so those artifacts aren't externally resolvable yet. The
no-wgpu4k libraries (prism-math, prism-core) are cleanly consumable.

## Plan

1. Add `.github/workflows/publish.yml`:
   - `name: Publish to Maven Central`
   - `on: workflow_dispatch` with a required `dry_run` boolean input (default
     `true`).
   - `permissions: contents: read`.
   - Single `publish` job on `macos-26`:
     - checkout, JDK 25 + 21, setup-gradle, konan cache, setup-wgpu4k (iOS
       rust targets).
     - dry-run → `./gradlew publishToMavenLocal --no-configuration-cache`.
     - real → `./gradlew publishToMavenCentral --no-configuration-cache` with
       the four `ORG_GRADLE_PROJECT_*` secrets in `env`.
2. Devlog 000026.
3. ktfmt is irrelevant (YAML only); validate YAML parses. Optionally smoke-test
   `publishToMavenLocal` for prism-math locally (no secrets needed).
4. Draft PR, then mark ready.
