# 000006-feat-ios-native-support

**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `feat/ios-native-support`

## Intent

Implement iOS native support (M7): move DemoScene to commonMain, create iOS entry point with MTKViewDelegateProtocol + wgpu4k `iosContextRenderer`, build XCFramework, scaffold Xcode project, add Compose iOS demo with tab bar, and fix critical review findings.

## What Changed

- **2026-02-15T13:22-08:00** Created implementation plan -> [devlog/plans/000006-01-ios-native-support.md](plans/000006-01-ios-native-support.md)
- **2026-02-15T13:25-08:00** `prism-demo/build.gradle.kts` — Moved wgpu4k deps from jvmMain/wasmJsMain to commonMain; added XCFramework binary config for iosArm64/iosSimulatorArm64.
- **2026-02-15T13:25-08:00** `prism-demo/src/commonMain/.../DemoScene.kt` — Moved from jvmMain to commonMain. Later added `tick(deltaTime, elapsed, frameCount, rotationSpeed)` method encapsulating rotation logic shared by all platforms.
- **2026-02-15T13:27-08:00** `prism-demo/src/wasmJsMain/.../Main.kt` — Refactored to use shared `createDemoScene()`.
- **2026-02-15T13:35-08:00** `prism-demo/src/iosMain/.../IosDemoController.kt` — iOS entry point: `suspend fun configureDemo(view: MTKView)` with `DemoRenderDelegate` implementing `MTKViewDelegateProtocol`. Added `IosDemoHandle` wrapping IosContext + DemoScene with `shutdown()`. Fallback to 800x600 if drawableSize zero. Guard for non-positive dimensions in resize.
- **2026-02-15T13:38-08:00** `prism-renderer/src/iosMain/.../RenderSurface.ios.kt` — Replaced `TODO()` with Kermit logging. Surface managed by MTKView/wgpu4k externally.
- **2026-02-15T13:40-08:00** `ios-demo/project.yml` — xcodegen spec for iOS app (iOS 15.0). Later renamed to `prism-ios-demo/`.
- **2026-02-15T13:40-08:00** `ios-demo/Sources/{AppDelegate,ViewController}.swift` — Swift entry point. Later: `IosDemoHandle` stored as property with `shutdown()` in `deinit`, `showError()` for Metal init failures, moved wgpu init from `viewDidLoad` to `viewDidAppear` with MTKView starting paused.
- **2026-02-15T13:40-08:00** `ios-demo/Sources/Info.plist` — iOS plist. Fixed `armv7` -> `arm64`, added iPad orientations, added `UIApplicationSceneManifest`.
- **2026-02-15T15:10-08:00** Created plan -> [devlog/plans/000006-03-ios-remaining-fixes.md](plans/000006-03-ios-remaining-fixes.md)
- **2026-02-15T15:10-08:00** `prism-demo/src/iosMain/.../ComposeIosEntry.kt` — Compose iOS demo: `composeDemoViewController()` returns `ComposeUIViewController`, embeds MTKView via `UIKitView`, DemoStore MVI with Material3 controls. Null check for Metal device, error handling, dimension guards, `NSOperationQueue.mainQueue` for thread-safe FPS dispatch.
- **2026-02-15T15:10-08:00** `ios-demo/Sources/{SceneDelegate,ComposeViewController}.swift` — UITabBarController with "Native" and "Compose" tabs. SceneDelegate replaces old UIApplicationDelegate lifecycle.
- **2026-02-15T15:10-08:00** `prism-demo/src/commonMain/.../NativeDemoApp.kt`, `DemoApp.kt` — Moved dead code from commonMain to jvmMain.
- **2026-02-15T15:36-08:00** Deleted stale root directories: `commonMain/`, `commonNativeMain/`, `desktopNativeMain/`, `jvmMain/`, `webMain/`.
- **2026-02-15T15:50-08:00** `BUILD_STATUS.md`, `AGENTS.md`, `PLAN.md` — Updated for M7 iOS completion.
- **2026-02-15T15:54-08:00** `ios-demo/Sources/ViewController.swift` — Added `mtkView.isPaused = true` and `mtkView.delegate = nil` in `deinit` before shutdown.

## Decisions

- **2026-02-15T13:22-08:00** **Move wgpu4k deps to commonMain** — DemoScene uses WGPUContext; sharing deps is cleanest.
- **2026-02-15T13:35-08:00** **`@OptIn(BetaInteropApi::class)` for DemoRenderDelegate** — Standard for K/N classes implementing ObjC protocols.
- **2026-02-15T13:40-08:00** **Static XCFramework** — Avoids embedding dynamic framework in iOS app bundle.
- **2026-02-15T14:30-08:00** **`IosDemoHandle` as lifecycle manager** — Single handle with one `shutdown()` method. Mirrors JVM and WASM patterns.
- **2026-02-15T15:10-08:00** **Direct MTKView management in Compose iOS** — Rather than routing through PrismView stub.
- **2026-02-15T15:10-08:00** **`@Suppress("DEPRECATION")` for UIKitView** — Compose 1.10.0 deprecates old API but replacement not available yet.
- **2026-02-15T15:24-08:00** **MTKView.isPaused for lazy init** — Paused in `viewDidLoad`, unpaused after `configureDemo` completes.
- **2026-02-15T15:24-08:00** **NSOperationQueue.mainQueue for FPS dispatch** — `drawInMTKView` runs on Metal display-link thread; must dispatch to main for Compose state.
- **2026-02-15T15:54-08:00** **Pause + clear delegate before shutdown in deinit** — Prevents render callbacks touching freed GPU resources.

## Research & Discoveries

- `iosContextRenderer(view: MTKView, width: Int, height: Int)` extracts CAMetalLayer via `objcPtr()`, creates WGPU instance -> surface -> adapter -> device.
- `IosContext` wraps `WGPUContext` and `MTKView`, implements `AutoCloseable`.
- K/N `MTKViewDelegateProtocol.mtkView(drawableSizeWillChange:)` receives `CValue<CGSize>` — use `.useContents {}`.
- `CACurrentMediaTime()` from `platform.QuartzCore` gives high-resolution monotonic time.
- Compose Multiplatform 1.10.0 `UIKitView` deprecated signature — replacement `UIKitInteropProperties` not yet available.
- `ComposeUIViewController` from `androidx.compose.ui.window` creates a UIViewController hosting Compose content.

## Issues

- **XCFramework import** — `Unresolved reference 'XCFramework'`. Needs explicit import in build.gradle.kts.
- **IosContext and DemoScene leaked** — `configureDemo()` returned IosContext but Swift discarded it. Fixed with `IosDemoHandle`.
- **No error handling in Swift** — `fatalError` on Metal init failure. Fixed with `showError()` UILabel.
- **drawableSize zero at init** — Fallback to 800x600, corrected on first layout.
- **armv7 in UIRequiredDeviceCapabilities** — XCFramework only has arm64. Fixed.
- **UIKitView deprecation** — Suppressed; replacement API not available in 1.10.0.

## Lessons Learned

- wgpu4k iOS path is clean: `iosContextRenderer` handles Metal/WGPU plumbing.
- `applyDefaultHierarchyTemplate()` creates `iosMain` automatically when both iosArm64/iosSimulatorArm64 declared.
- Compose iOS interop migration sometimes ahead of stable release — deprecated APIs may lack replacements.
- Always verify resize propagation end-to-end (native surface + renderer depth texture + camera aspect ratio).

## Commits

- `1bd8607` — chore: add devlog for iOS native support (M7)
- `3b61b04` — refactor: move DemoScene to commonMain and consolidate wgpu4k deps
- `de51ecc` — feat: add iOS native rendering with MTKView delegate
- `890cc9f` — feat: add Xcode project for iOS demo via xcodegen
- `ae9ce40` — fix: address critical review findings for iOS native support
- `54ed262` — feat: add Compose iOS demo + fix outstanding issues
- `f53a473` — fix: address all review findings for iOS support
- `d1eb2de` — docs: update devlog with corrected commit hashes and cleanup session
- `4dd05f7` — docs: update BUILD_STATUS, AGENTS, and PLAN for M7 iOS completion
- `6382ec5` — fix: address PR review comments for iOS resize guards and teardown

> **Note:** All references to `ios-demo/` in this devlog refer to the directory renamed to `prism-ios-demo/` in `feat/sync-ios-tab-rotation`.
