# 000012-01: Android Rendering Support (M8)

## Thinking

GitHub issue #9 requests Android rendering. Four modules already have `androidTarget()` stubs. The wgpu4k library provides `androidContextRenderer(SurfaceHolder, width, height)` in its `-toolkit-android` AAR (Maven local). The `com.android.library` plugin is deprecated for KMP in AGP 8.12+; need to migrate to `com.android.kotlin.multiplatform.library`.

Key findings from exploration:
- wgpu4k-toolkit-android AAR exists in Maven local with `androidContextRenderer()` that takes `SurfaceHolder`
- API: `androidContextRenderer(surfaceHolder, width, height) -> AndroidContext` with `.wgpuContext: WGPUContext`
- AGP 8.13.0 + KGP 2.3.0 support `kotlin { android { } }` block (not `androidLibrary {}` which is deprecated)
- `androidTarget()` is not needed with the new plugin — `android {}` inside `kotlin {}` declares the target

## Plan

### Task 0: AGENTS.md Worktree Workflow Cleanup
- Consolidate duplicate worktree sections (lines 329-348 and 414-446)
- Remove "Worktrees (Required)" subsection, fold into "Git Worktree Workflow"
- Add instruction: create worktree before exploring/planning

### Task 1: Migrate to `com.android.kotlin.multiplatform.library`
- Add plugin alias to `gradle/libs.versions.toml`
- Add `apply false` to root `build.gradle.kts`
- Migrate 4 existing modules: prism-math, prism-core, prism-renderer, prism-native-widgets
  - Replace `android.library` plugin with `android.kotlin.multiplatform.library`
  - Remove `androidTarget()` call
  - Remove top-level `android {}` block
  - Add `android { namespace; compileSdk; minSdk }` inside `kotlin {}`
- Add Android targets to 6 new modules: prism-ecs, prism-scene, prism-input, prism-assets, prism-audio, prism-compose

### Task 2: Implement Android PrismSurface
- Add wgpu4k deps to `androidMain` sourceSet in prism-native-widgets
- Replace stub `PrismSurface.android.kt` with real implementation
- Add `createPrismSurface(surfaceHolder, width, height)` factory

### Task 3: Create Android Demo Activity
- Add Android application config to prism-demo
- Create `PrismDemoActivity` with `SurfaceView` + `Choreographer` render loop
- Add `AndroidManifest.xml`

### Task 4: Devlog + PR
- Create devlog files
- Push draft PR with task checklist
- Update as work progresses
