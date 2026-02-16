# prism-demo

Demo application showcasing Prism Engine across platforms. Renders a rotating lit cube driven by the ECS, with interactive Compose UI controls on supported platforms.

## Platforms

### JVM Desktop

Three entry points are available:

| Entry point | File | Description |
|---|---|---|
| **GLFW** | `jvmMain/.../GlfwMain.kt` | Default. Pure GLFW window, no UI controls. Run with `./gradlew :prism-demo:run`. |
| **Compose + AWT** | `jvmMain/.../ComposeMain.kt` | JFrame with `PrismPanel` (AWT Canvas + Metal surface) and a `ComposePanel` sidebar for Material3 controls. Run with `./gradlew :prism-demo:runCompose`. |
| **Compose Desktop** | `jvmMain/.../Main.kt` | Compose `Window` with `PrismOverlay` (3D + UI). Requires an IDE run configuration targeting `main()`. |

### Web (WASM/JS)

| Entry point | File | Description |
|---|---|---|
| **WebGPU Canvas** | `wasmJsMain/.../Main.kt` | HTML Canvas with WebGPU via `canvasContextRenderer()` and `requestAnimationFrame()`. No UI controls. |

Build: `./gradlew :prism-demo:wasmJsBrowserDistribution`

### iOS

The iOS demo is a Swift app (`prism-ios-demo/`) with a `UITabBarController` containing two tabs:

| Tab | Rendering | UI Controls | Entry point |
|---|---|---|---|
| **Native** | MTKView + `DemoRenderDelegate` | None | `iosMain/.../IosDemoController.kt` |
| **Compose** | MTKView embedded via `UIKitView` | Material3 sliders, pause, color picker | `iosMain/.../ComposeIosEntry.kt` |

Both tabs share a single `DemoStore` and `SharedDemoTime` so rotation angle, pause state, speed, and cube color are synchronized across tabs. Changing any setting on the Compose tab is immediately reflected on the Native tab.

**Build and run:**

```bash
# Build XCFramework
./gradlew :prism-demo:assemblePrismDemoDebugXCFramework

# Generate Xcode project (requires xcodegen)
cd prism-ios-demo && xcodegen generate

# Build and launch on simulator
xcodebuild -project prism-ios-demo/PrismDemo.xcodeproj \
  -scheme PrismDemoApp \
  -destination 'platform=iOS Simulator,name=iPhone 16 Pro' \
  -configuration Debug build

xcrun simctl install booted prism-ios-demo/build/Debug-iphonesimulator/PrismDemoApp.app
xcrun simctl launch booted com.hyeonslab.prism.demo
```

## Shared Code (commonMain)

| File | Purpose |
|---|---|
| `DemoScene.kt` | Factory (`createDemoScene()`) that bootstraps an ECS world with camera, cube, and `WgpuRenderer`. `tick()` advances rotation for non-interactive demos. |
| `DemoSceneState.kt` | MVI store: `DemoUiState` (speed, pause, color, fps), `DemoIntent` (actions), `DemoStore` (pure reducer). |
| `ComposeDemoControls.kt` | Material3 composable with rotation speed slider, pause/resume button, and color presets. Used by both JVM Compose and iOS Compose. |

## Interactive Controls (Compose tabs)

- **Rotation speed** — Slider (0-360 deg/s, default 45)
- **Pause / Resume** — Freezes cube rotation
- **Cube color** — Circular preset swatches (blue, red, green, gold, purple, white)
- **FPS** — Smoothed display updated each frame
