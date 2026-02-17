# 000012: Android Rendering Support (M8)

**Agent:** Claude Code (claude-opus-4-6) @ prism branch android-support
**Intent:** Implement Android wgpu4k rendering support (issue #9), migrate to `com.android.kotlin.multiplatform.library`, add Android targets to all modules, create Android demo activity.

## What Changed

2026-02-16T14:00-08:00 AGENTS.md — Consolidated duplicate worktree workflow sections; added frozen-snapshot instruction; updated project status for M8 completion; updated Android platform description and tech stack.

2026-02-16T14:05-08:00 gradle/libs.versions.toml — Added `android-kotlin-multiplatform-library` plugin alias.

2026-02-16T14:05-08:00 build.gradle.kts — Added `android.kotlin.multiplatform.library` plugin declaration with `apply false`.

2026-02-16T14:10-08:00 prism-math, prism-core, prism-renderer, prism-native-widgets build.gradle.kts — Migrated from `com.android.library` + `androidTarget()` + top-level `android {}` to `com.android.kotlin.multiplatform.library` + `kotlin { android { } }`.

2026-02-16T14:15-08:00 prism-ecs, prism-scene, prism-input, prism-assets, prism-audio, prism-compose build.gradle.kts — Added Android targets using `com.android.kotlin.multiplatform.library` plugin with `kotlin { android { } }` configuration.

2026-02-16T14:15-08:00 prism-assets/src/androidMain/.../FileReader.android.kt — New actual for FileReader (same as JVM impl). Required because Android is now a separate target from JVM.

2026-02-16T14:15-08:00 prism-compose/src/androidMain/.../PrismView.android.kt — New actual for PrismView (placeholder Box). Required because Android is now a separate target from JVM.

2026-02-16T14:20-08:00 prism-native-widgets/src/androidMain/.../PrismSurface.android.kt — Replaced stub with real wgpu4k implementation. Added `createPrismSurface(surfaceHolder, width, height)` factory using `androidContextRenderer()`. Added wgpu4k + coroutines-android deps to androidMain sourceSet.

2026-02-16T14:25-08:00 prism-demo/build.gradle.kts — Added `com.android.application` plugin, `androidTarget()`, android application block (namespace, compileSdk, minSdk, targetSdk, applicationId). Added coroutines-android dep for androidMain.

2026-02-16T14:25-08:00 prism-demo/src/androidMain/AndroidManifest.xml — New manifest with PrismDemoActivity as launcher.

2026-02-16T14:25-08:00 prism-demo/src/androidMain/.../PrismDemoActivity.kt — New Activity with SurfaceView + Choreographer render loop + DemoScene integration.

## Decisions

2026-02-16T14:05-08:00 Used `android {}` (not `androidLibrary {}`) inside `kotlin {}` — `androidLibrary {}` is deprecated since AGP 8.12. The `android {}` block is the current recommended DSL.

2026-02-16T14:05-08:00 Kept `com.android.application` + `androidTarget()` for prism-demo — the new `com.android.kotlin.multiplatform.library` plugin only works for library modules, not application modules. There's no KMP replacement for `com.android.application` yet.

2026-02-16T14:20-08:00 Used wgpu4k-toolkit-android AAR directly instead of PanamaPort — wgpu4k already provides `androidContextRenderer()` that creates Vulkan surfaces from SurfaceHolder. No need for PanamaPort intermediary.

2026-02-16T14:25-08:00 Used Choreographer.FrameCallback for render loop — vsync-aligned like requestAnimationFrame on web and CADisplayLink on iOS. Consistent with other platform render loops.

## Issues

2026-02-16T14:15-08:00 Adding Android target to prism-assets and prism-compose broke build — these modules have expect/actual declarations (FileReader, PrismView) that previously only needed jvmMain actuals. With the new KMP Android plugin, Android is a separate target from JVM and needs its own actuals. Fixed by creating androidMain actual files.

## Commits

ca843cf — chore: add devlog and plan for Android rendering (M8)
9eeda31 — feat: implement Android rendering support (M8)
