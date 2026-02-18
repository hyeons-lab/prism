# 000013 — feat/android-compose-integration

## Agent
Claude Code (claude-opus-4-6) @ repository:prism branch:feat/android-compose-integration

## Intent
Add Android Compose integration so the demo app can render a rotating lit cube inside a Compose UI with Material3 controls (speed slider, pause, color presets) — matching the existing JVM Desktop and iOS Compose demos. Then refactor all Compose demos to use PrismView/PrismOverlay with onSurfaceReady/onSurfaceResized callbacks instead of bypassing them.

## What Changed
- 2026-02-17 gradle/libs.versions.toml — added activity-compose library
- 2026-02-17 prism-compose/build.gradle.kts — added androidMain deps (wgpu4k, wgpu4k-toolkit, coroutines-android)
- 2026-02-17 prism-compose/src/androidMain/.../PrismView.android.kt — replaced stub with AndroidView + SurfaceView + SurfaceHolder.Callback + withFrameNanos render loop
- 2026-02-17 prism-demo-core/src/androidMain/.../ComposeAndroidEntry.kt — new bypass-pattern composable with DemoStore MVI, wgpu init, render loop, ComposeDemoControls overlay
- 2026-02-17 prism-android-demo/build.gradle.kts — added kotlin-compose plugin, activity-compose, compose deps, prism-compose project dep
- 2026-02-17 prism-android-demo/src/main/.../ComposeDemoActivity.kt — new ComponentActivity with setContent
- 2026-02-17 prism-android-demo/src/main/AndroidManifest.xml — registered ComposeDemoActivity as second launcher
- 2026-02-18 prism-renderer/build.gradle.kts — promoted wgpu4k from implementation to api scope
- 2026-02-18 prism-native-widgets/src/commonMain/.../PrismSurface.kt — added wgpuContext to expect class
- 2026-02-18 prism-native-widgets/src/*/PrismSurface.*.kt — added actual keyword to wgpuContext on all 7 platform actuals
- 2026-02-18 prism-native-widgets/src/androidMain/.../PrismSurface.android.kt — removed unused AndroidSurfaceInfo data class
- 2026-02-18 prism-native-widgets/build.gradle.kts — removed redundant wgpu4k deps (now transitive via prism-renderer api)
- 2026-02-18 prism-compose/src/commonMain/.../PrismView.kt — added onSurfaceReady and onSurfaceResized callback params
- 2026-02-18 prism-compose/src/commonMain/.../PrismOverlay.kt — forward onSurfaceReady/onSurfaceResized to PrismView
- 2026-02-18 prism-compose/src/jvmMain/.../PrismView.jvm.kt — invoke callbacks from PrismPanel onReady/onResized
- 2026-02-18 prism-compose/src/androidMain/.../PrismView.android.kt — invoke callbacks from surface lifecycle
- 2026-02-18 prism-compose/src/appleMain/.../PrismView.apple.kt — added callback params to stub signature
- 2026-02-18 prism-compose/src/wasmJsMain/.../PrismView.wasmJs.kt — added callback params to stub signature
- 2026-02-18 prism-compose/build.gradle.kts — removed redundant wgpu4k deps (now transitive)
- 2026-02-18 prism-demo-core/src/commonMain/.../DemoScene.kt — added createDemoScene overload with external Engine
- 2026-02-18 prism-demo-core/src/commonMain/.../ComposeDemoApp.kt — deleted (superseded by refactored demos)
- 2026-02-18 prism-demo-core/src/jvmMain/.../Main.kt — deleted (referenced deleted ComposeDemoApp)
- 2026-02-18 prism-demo-core/src/jvmMain/.../ComposeMain.kt — refactored to use PrismOverlay with callbacks
- 2026-02-18 prism-demo-core/src/androidMain/.../ComposeAndroidEntry.kt — refactored to use PrismOverlay with callbacks

- 2026-02-18 prism-core/src/commonMain/.../Engine.kt — added removeSubsystem() for clean scene disposal without engine shutdown
- 2026-02-18 prism-demo-core/src/commonMain/.../DemoScene.kt — added ownsEngine flag, dispose() method, updated shutdown() to respect ownership
- 2026-02-18 prism-compose/src/jvmMain/.../PrismView.jvm.kt — added rememberUpdatedState for onSurfaceReady/onSurfaceResized callbacks
- 2026-02-18 prism-compose/src/androidMain/.../PrismView.android.kt — added rememberUpdatedState for callbacks, added renderingActive guard
- 2026-02-18 prism-demo-core/src/androidMain/.../ComposeAndroidEntry.kt — added dispose cleanup, DisposableEffect, try/catch, removed FPS double-dispatch, removed redundant updateAspectRatio
- 2026-02-18 prism-demo-core/src/jvmMain/.../ComposeMain.kt — same fixes as Android + windowClosing handler with DO_NOTHING_ON_CLOSE + WindowAdapter

## Decisions
- 2026-02-17 Follow JVM ComposeMain bypass pattern (DemoStore, not EngineStore) — consistent with iOS and JVM demos
- 2026-02-17 Use AndroidView + SurfaceView + SurfaceHolder.Callback for PrismView.android.kt — matches Android native rendering pipeline
- 2026-02-17 Use withFrameNanos render loop (like JVM ComposeMain) — simpler than Choreographer for Compose context
- 2026-02-18 Promote wgpu4k to api in prism-renderer — needed for WGPUContext type visibility in downstream modules (PrismView callbacks)
- 2026-02-18 Use gameLoop.onRender for per-frame scene logic — GameLoop.tick() already invokes onFixedUpdate/onUpdate/onRender, so wrapping onRender adds demo logic (rotation, material, FPS) before the base ECS update. Zero changes to PrismView's render loop needed.
- 2026-02-18 Delete ComposeDemoApp — prototype superseded by refactored JVM/Android demos using PrismOverlay
- 2026-02-18 Remove AndroidSurfaceInfo — no longer used by any consumer after refactor

## Issues
- 2026-02-17 API 35+ InstantiationError — previously documented as a blocker, but verified working on API 36 (Pixel 10 Pro Fold). The hyeons-lab/wgpu4k-native fork resolved this.
- 2026-02-18 PrismSurface actuals missing `actual` keyword on wgpuContext — all 7 platform actuals already had the property but without the actual keyword since it wasn't in the expect. Adding it to the expect required adding `actual` to all actuals.
- 2026-02-18 Critical review identified 10 issues. Fixed all:
  - #1 Stale callback capture in JVM SwingPanel factory — added `rememberUpdatedState`
  - #2 Android surface re-creation leaks old scene — added `scene?.dispose()` before creating new scene in onSurfaceReady
  - #3 DemoScene.shutdown() shut down shared Engine — added `ownsEngine` flag, `dispose()` method, `Engine.removeSubsystem()`
  - #4 No cleanup on composition exit — added `DisposableEffect(Unit)` with `scene?.dispose()` on both JVM and Android
  - #5 FPS double-dispatch — removed redundant `DemoIntent.UpdateFps` from onRender (PrismView already dispatches FrameTick to EngineStore)
  - #6 Thread-safety docs — added comments on rotationAngle being EDT/main-thread only
  - #7 JVM window close leak — changed to `DO_NOTHING_ON_CLOSE` + `WindowAdapter.windowClosing` that disposes scene before frame
  - #8 Redundant updateAspectRatio — removed from onSurfaceResized (renderer.resize already handles this)
  - #9 Missing try/catch — wrapped onSurfaceReady body in try/catch on both platforms
  - #10 PrismView.android.kt stale callbacks — added `rememberUpdatedState` for both callbacks
- 2026-02-18 ktfmtFormat hitting Gradle Worker Daemon timeout — machine under heavy load, worker crashed. Need to retry with clean daemon.

## Commits
- a01e9c9 — feat: implement Android Compose integration with Material3 controls
- eab93df — docs: update Android API 35+ status and add Compose demo to project status
- 35e9e57 — fix: address review issues in Android Compose integration
- caf12b8 — fix: resolve remaining review issues (theme conflict, duplicated class)
- e101cd7 — fix: guard against init/destroy race and remove redundant deps

## Progress
- [x] Create devlog and plan files
- [x] Add activity-compose to version catalog
- [x] Update prism-compose/build.gradle.kts with Android deps
- [x] Implement PrismView.android.kt (replace stub)
- [x] Create ComposeAndroidEntry.kt in prism-demo-core androidMain
- [x] Update prism-android-demo (build.gradle.kts, ComposeDemoActivity, manifest)
- [x] Format, validate, build
- [x] Create draft PR — https://github.com/hyeons-lab/prism/pull/31
- [x] Refactor: Promote wgpu4k to api in prism-renderer
- [x] Refactor: Add wgpuContext to PrismSurface expect + fix all actuals
- [x] Refactor: Add onSurfaceReady/onSurfaceResized to PrismView/PrismOverlay
- [x] Refactor: Add createDemoScene overload with external Engine
- [x] Refactor: Rewrite ComposeAndroidEntry to use PrismOverlay
- [x] Refactor: Rewrite ComposeMain to use PrismOverlay (overlay layout)
- [x] Refactor: Delete ComposeDemoApp and unused Main.kt
- [x] Refactor: Clean up redundant wgpu4k deps
- [x] Refactor: Delete unused AndroidSurfaceInfo
- [x] Review: Fix stale callback capture (rememberUpdatedState on JVM + Android PrismView)
- [x] Review: Fix DemoScene shared-engine shutdown (ownsEngine + dispose + Engine.removeSubsystem)
- [x] Review: Fix Android surface re-creation leak (dispose old scene in onSurfaceReady)
- [x] Review: Add DisposableEffect cleanup on both JVM and Android
- [x] Review: Add try/catch error handling in onSurfaceReady on both platforms
- [x] Review: Remove FPS double-dispatch and redundant updateAspectRatio
- [x] Review: Add JVM windowClosing handler (DO_NOTHING_ON_CLOSE + WindowAdapter)
- [x] Review: Document thread-safety assumption on rotationAngle
- [ ] Format, validate (ktfmtFormat + ktfmtCheck + detektJvmMain + jvmTest + assembleDebug)
- [ ] Commit and push review fixes
