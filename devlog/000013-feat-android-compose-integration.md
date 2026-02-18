# 000013 — feat/android-compose-integration

## Agent
Claude Code (claude-opus-4-6) @ repository:prism branch:feat/android-compose-integration

## Intent
Add Android Compose integration so the demo app can render a rotating lit cube inside a Compose UI with Material3 controls (speed slider, pause, color presets) — matching the existing JVM Desktop and iOS Compose demos.

## What Changed
- 2026-02-17 gradle/libs.versions.toml — added activity-compose library
- 2026-02-17 prism-compose/build.gradle.kts — added androidMain deps (wgpu4k, wgpu4k-toolkit, coroutines-android)
- 2026-02-17 prism-compose/src/androidMain/.../PrismView.android.kt — replaced stub with AndroidView + SurfaceView + SurfaceHolder.Callback + withFrameNanos render loop
- 2026-02-17 prism-demo-core/src/androidMain/.../ComposeAndroidEntry.kt — new bypass-pattern composable with DemoStore MVI, wgpu init, render loop, ComposeDemoControls overlay
- 2026-02-17 prism-android-demo/build.gradle.kts — added kotlin-compose plugin, activity-compose, compose deps, prism-compose project dep
- 2026-02-17 prism-android-demo/src/main/.../ComposeDemoActivity.kt — new ComponentActivity with setContent
- 2026-02-17 prism-android-demo/src/main/AndroidManifest.xml — registered ComposeDemoActivity as second launcher

## Decisions
- 2026-02-17 Follow JVM ComposeMain bypass pattern (DemoStore, not EngineStore) — consistent with iOS and JVM demos
- 2026-02-17 Use AndroidView + SurfaceView + SurfaceHolder.Callback for PrismView.android.kt — matches Android native rendering pipeline
- 2026-02-17 Use withFrameNanos render loop (like JVM ComposeMain) — simpler than Choreographer for Compose context

## Issues
- 2026-02-17 API 35+ InstantiationError — previously documented as a blocker, but verified working on API 36 (Pixel 10 Pro Fold). The hyeons-lab/wgpu4k-native fork resolved this.

## Commits
- a01e9c9 — feat: implement Android Compose integration with Material3 controls

## Progress
- [x] Create devlog and plan files
- [x] Add activity-compose to version catalog
- [x] Update prism-compose/build.gradle.kts with Android deps
- [x] Implement PrismView.android.kt (replace stub)
- [x] Create ComposeAndroidEntry.kt in prism-demo-core androidMain
- [x] Update prism-android-demo (build.gradle.kts, ComposeDemoActivity, manifest)
- [x] Format, validate, build
- [x] Create draft PR — https://github.com/hyeons-lab/prism/pull/31
