# 000028-01 — Bump artifact version to 0.1.0 + fix coordinate mismatches

## Thinking

User asked to bump the published artifact version from `0.1.0-SNAPSHOT` to
`0.1.0`, then to fix the Maven coordinate mismatches surfaced while doing it.

The canonical version lives in `gradle.properties` (`VERSION_NAME`), read by the
vanniktech maven-publish plugin. `GROUP=com.hyeons-lab`.

Coordinate audit of `prism-flutter/flutter_plugin/android/build.gradle` (a plain
AGP library that consumes the prism KMP android artifacts from mavenLocal):

- Its two prism deps used group `engine.prism` — but every prism module
  publishes under `com.hyeons-lab`. Wrong group → never resolves.
- They pinned `0.1.0-SNAPSHOT` — stale once the version bumps to `0.1.0`.
- `prism-flutter-android` was referenced but `prism-flutter` had **no**
  `mavenPublishing` block, so that artifact is published nowhere (confirmed: no
  publish step anywhere references it). So even after fixing the group the dep
  could not resolve.

`engine.prism` is a dead group: nothing publishes under it; the only other hits
are stale package paths in PLAN.md (the real packages are `com.hyeonslab.prism.*`).

Fix is therefore three-fold and internally consistent:
1. `VERSION_NAME` → `0.1.0` (+ the AGENTS.md doc line).
2. Flutter plugin deps → `com.hyeons-lab:…:0.1.0`; drop the now-dead
   `includeGroupByRegex("engine\\.prism.*")` mavenLocal filter.
3. Publish `prism-flutter` (add `mavenPublishing { }` mirroring
   `prism-native-widgets`) so `com.hyeons-lab:prism-flutter-android:0.1.0`
   actually exists. It `api`-depends on the already-published prism-core and
   prism-native-widgets, so its POM is consistent.

Verified by publishing the android variant to mavenLocal: produced
`com.hyeons-lab:prism-flutter-android:0.1.0` whose POM depends on
`com.hyeons-lab:prism-core-android:0.1.0` and `…:prism-native-widgets-android:0.1.0`
— matching the flutter plugin deps exactly.

Note: 0.1.0 is a release version, so a real `publishToMavenCentral` now requires
GPG signing (the publish workflow already passes `SIGNING_KEY*`); `publishToMavenLocal`
(the dry-run path) does not.

## Plan

1. `gradle.properties`: `VERSION_NAME=0.1.0`.
2. `AGENTS.md`: Version line → 0.1.0.
3. `prism-flutter/flutter_plugin/android/build.gradle`: deps →
   `com.hyeons-lab:…:0.1.0`; remove `engine.prism` repo filter.
4. `prism-flutter/build.gradle.kts`: apply `maven.publish` plugin + add
   `mavenPublishing { publishToMavenCentral(); signAllPublications(); pom { } }`.
5. Validate: ktfmtCheck/detekt/jvmTest, `:prism-flutter:compileAndroidMain`,
   and a mavenLocal publish of the android variant to confirm the coordinate.
6. Devlog 000028, commit, PR.
