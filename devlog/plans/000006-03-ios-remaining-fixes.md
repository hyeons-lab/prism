# Plan: Fix Outstanding iOS Issues + Add Compose iOS Demo

## Thinking

PR #15 (iOS native support, M7) was reviewed and 11 issues were found. Session 2 fixed the critical ones (P0/P1), but three items were deferred to GitHub issues:
- #16: UISceneDelegate modernization (P1)
- #17: Compose deps in iOS binary / dead code in commonMain (P2)
- #18: Rotation logic duplication across platforms (P2)

The user wants all three fixed AND a Compose Multiplatform iOS demo added, so the app has two entry points: native MTKView and Compose-based with UI controls.

For the Compose iOS demo, the key decision is whether to route through `PrismView` (the expect/actual composable) or directly manage MTKView + DemoScene. Since `PrismView` is a stub on Apple targets and implementing it properly is a separate task, the direct approach mirrors `ComposeMain.kt` (JVM): embed MTKView via `UIKitView`, initialize wgpu in a `LaunchedEffect`, install a render delegate that reads `DemoStore` state, and overlay `ComposeDemoControls`.

The `UIKitView` API in Compose Multiplatform 1.10.0 has a deprecated old signature and a new one using `UIKitInteropProperties`. However, testing revealed `UIKitInteropProperties` isn't available in 1.10.0 — need to use the old API with `@Suppress("DEPRECATION")`.

For the tab bar, UISceneDelegate is the modern pattern. AppDelegate becomes minimal (returns scene config), SceneDelegate creates UIWindow and sets UITabBarController as root.

---

## Plan

### Phase 1: Issue #18 — Extract DemoScene.tick()
- Add `tick(deltaTime, elapsed, frameCount)` to `DemoScene` class
- Simplify GlfwMain.kt, Main.kt (WASM), IosDemoController.kt to call `scene.tick()`
- ComposeMain.kt keeps its own user-controllable rotation loop

### Phase 2: Issue #17 — Clean up commonMain deps
- `git mv` NativeDemoApp.kt and DemoApp.kt from commonMain to jvmMain (dead code using JVM-only PrismSurface/Scene)
- Remove `prism-native-widgets` from commonMain deps (transitive via prism-compose)

### Phase 3: Compose iOS Demo
- New `ComposeIosEntry.kt` in iosMain with `composeDemoViewController()` returning ComposeUIViewController
- `IosComposeDemoContent` composable: UIKitView for MTKView + LaunchedEffect for wgpu init + ComposeDemoControls overlay
- `ComposeRenderDelegate`: reads DemoStore state for user-controllable rotation, color, pause
- New `ComposeViewController.swift`: hosts ComposeUIViewController as child

### Phase 4: Issue #16 + P2 #9
- Rewrite AppDelegate to minimal scene-based (configurationForConnecting)
- New SceneDelegate with UITabBarController (Native + Compose tabs)
- Add UIApplicationSceneManifest to Info.plist
- Remove redundant `embed: false` from project.yml

### Verification
- `./gradlew ktfmtFormat ktfmtCheck detektJvmMain jvmTest`
- `./gradlew compileKotlinIosArm64 compileKotlinIosSimulatorArm64 compileKotlinWasmJs`
- `./gradlew assemblePrismDemoReleaseXCFramework`
