# 000013-01 — Android Compose Integration

## Context

Android rendering via wgpu4k/Vulkan works (M8), but only through a plain Activity + SurfaceView. The PrismView.android.kt in prism-compose is a stub. Both JVM Desktop and iOS have working Compose demos with Material3 controls. This adds full Android Compose support following the same bypass pattern used by JVM ComposeMain and iOS ComposeIosEntry.

## Plan

1. Add `androidx.activity:activity-compose` to version catalog
2. Update `prism-compose/build.gradle.kts` — add androidMain dependencies (wgpu4k, coroutines-android)
3. Implement `PrismView.android.kt` — replace stub with AndroidView + SurfaceView + SurfaceHolder.Callback
4. Create `ComposeAndroidEntry.kt` in prism-demo-core androidMain — bypass pattern with DemoStore, withFrameNanos render loop
5. Update `prism-android-demo/build.gradle.kts` — add Compose plugins and deps
6. Create `ComposeDemoActivity.kt` — ComponentActivity.setContent wrapper
7. Update `AndroidManifest.xml` — register ComposeDemoActivity as second launcher
8. Format and validate
