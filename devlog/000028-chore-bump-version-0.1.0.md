# 000028 — chore/bump-version-0.1.0

**Agent:** Claude (claude-opus-4-8) @ prism branch chore/bump-version-0.1.0

## Intent

Bump the published artifact version `0.1.0-SNAPSHOT` → `0.1.0`, then fix the
Maven coordinate mismatches in the Flutter plugin's Android build that the bump
exposed.

## What Changed

- 2026-06-07T21:13-0700 `gradle.properties` — `VERSION_NAME` `0.1.0-SNAPSHOT` →
  `0.1.0` (canonical version for all com.hyeons-lab publications).
- 2026-06-07T21:13-0700 `AGENTS.md` — Version doc line → `0.1.0`.
- 2026-06-07T21:13-0700 `prism-flutter/flutter_plugin/android/build.gradle` — the
  two prism deps changed from `engine.prism:…:0.1.0-SNAPSHOT` to
  `com.hyeons-lab:prism-flutter-android:0.1.0` and
  `com.hyeons-lab:prism-native-widgets-android:0.1.0`; removed the now-dead
  `includeGroupByRegex("engine\\.prism.*")` mavenLocal content filter.
- 2026-06-07T21:13-0700 `prism-flutter/build.gradle.kts` — apply
  `libs.plugins.maven.publish` and add a `mavenPublishing { }` block (mirrors
  prism-native-widgets) so `com.hyeons-lab:prism-flutter-android` is actually
  published; the flutter plugin depended on an artifact that was published
  nowhere.

## Decisions

- 2026-06-07T21:13-0700 `engine.prism` is a dead group — nothing publishes under
  it (only stale package paths in PLAN.md). Real group is `com.hyeons-lab`, real
  packages are `com.hyeonslab.prism.*`. So the flutter plugin deps were simply
  wrong; corrected to the real coordinates rather than making the build resolve
  a nonexistent group.
- 2026-06-07T21:13-0700 Publish `prism-flutter` rather than drop the dep. The
  flutter plugin's Android library genuinely needs the prism-flutter Android AAR;
  it `api`-exposes prism-core + prism-native-widgets (both already published), so
  publishing it is the consistent fix and yields a clean POM.
- 2026-06-07T21:13-0700 Left the historical devlog (000013) reference to the old
  coordinate untouched — devlogs are append-only records of what was true then.
- 2026-06-07T21:44-07:00 (PR review) Reference `${project.version}` in the two
  prism dep coordinates instead of repeating the `0.1.0` literal. The plugin is
  versioned in lockstep with the SDK (its own `version = "0.1.0"` is the single
  source), so this removes the duplication that caused the original drift.

## Issues

- None. `:prism-flutter:publishAndroidPublicationToMavenLocal` produced
  `com.hyeons-lab:prism-flutter-android:0.1.0` whose POM depends on
  `com.hyeons-lab:prism-core-android:0.1.0` and `…:prism-native-widgets-android:0.1.0`
  — matching the flutter plugin deps. `ktfmtCheck detektJvmMain jvmTest` and
  `:prism-flutter:compileAndroidMain` pass.

## Research & Discoveries

- vanniktech reports `group: prism / version: unspecified` for the Gradle project
  even when publishing correctly — it sets publication groupId/version from
  `GROUP`/`VERSION_NAME` in gradle.properties, not from `project.group/version`.
  prism-native-widgets (known-good) shows the same, so this is expected.
- 0.1.0 is a release version: a real `publishToMavenCentral` now requires GPG
  signing (the publish workflow already wires `SIGNING_KEY*`); the
  `publishToMavenLocal` dry-run path does not require it.

## Commits

- HEAD — chore: bump artifact version to 0.1.0 and fix Flutter plugin coordinates
