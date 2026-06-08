# 000031 — fix/compose-linux-publish

**Agent:** Claude (claude-opus-4-8) @ prism branch fix/compose-linux-publish

## Intent

Fix the failing `publishToMavenCentral` run
(actions/runs/27117220819) — the first real Maven Central publish.

## What Changed

- 2026-06-07T22:20-07:00 `prism-compose/build.gradle.kts` — removed the
  `linuxX64()` and `mingwX64()` targets (replaced with an explanatory comment).
  Compose Multiplatform has no linux/windows Kotlin/Native target, so this module
  had no source for them.

## Decisions

- 2026-06-07T22:20-07:00 Remove the targets rather than add stub source. Compose
  Multiplatform does not publish artifacts for `linuxX64`/`mingwX64`; Linux and
  Windows are covered by the JVM Desktop target (which `prism-compose` already
  has via `jvm()`). The native targets were non-functional and only existed to
  break publishing.

## Issues

- **Root cause:** `prism-compose` is the only published module with **no
  `commonMain` sources** (its code lives in `nonNativeMain` = jvm/android/wasm and
  `appleMain` = ios/macos). The declared `linuxX64()`/`mingwX64()` targets
  therefore compiled nothing (`compileKotlinLinuxX64` = NO-SOURCE → no klib), but
  the publication still ran `generateMetadataFileForLinuxX64Publication`, which
  failed with `FileNotFoundException: prism-compose-linuxX64Main.klib`. This was
  latent until the first full multiplatform `publishToMavenCentral`.
- Audited all 11 published modules: only `prism-compose` has native targets
  without `commonMain` source; every other module (incl. the newly-published
  `prism-flutter`) has `commonMain` sources, so their klibs build and publish.
- Verified: `:prism-compose:publishToMavenLocal` now BUILD SUCCESSFUL (generates
  Android/IosArm64/IosSimulatorArm64/Jvm/Macos/WasmJs/root publications, no
  Linux/Mingw); `publish(LinuxX64|MingwX64)Publication` tasks no longer exist;
  `:prism-compose:ktfmtCheck` passes.

## Commits

- HEAD — fix(compose): drop unsupported linux/mingw native targets
