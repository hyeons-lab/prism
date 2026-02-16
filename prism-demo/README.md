# prism-demo

Demo application showcasing Prism Engine across platforms. Renders a rotating lit cube driven by the ECS, with interactive Compose UI controls on supported platforms.

## Platforms

### JVM Desktop

Three entry points are available:

| Entry point | File | Gradle task |
|---|---|---|
| **GLFW** (default) | `jvmMain/.../GlfwMain.kt` | `./gradlew :prism-demo:jvmRun` |
| **Compose + AWT** | `jvmMain/.../ComposeMain.kt` | `./gradlew :prism-demo:runCompose` |
| **Compose Desktop** | `jvmMain/.../Main.kt` | IDE run configuration targeting `main()` |

- **GLFW** — Pure GLFW window, no UI controls. Requires `-XstartOnFirstThread` on macOS (applied automatically by the build).
- **Compose + AWT** — JFrame with `PrismPanel` (AWT Canvas + Metal surface) and a `ComposePanel` sidebar for Material3 controls.
- **Compose Desktop** — Compose `Window` with `PrismOverlay` (3D + UI). No Gradle task registered; run from an IDE.

### Web (WASM/JS)

| Entry point | File | Description |
|---|---|---|
| **WebGPU Canvas** | `wasmJsMain/.../Main.kt` | HTML Canvas with WebGPU via `canvasContextRenderer()` and `requestAnimationFrame()`. No UI controls. |

| Task | Description |
|---|---|
| `./gradlew :prism-demo:wasmJsBrowserDevelopmentRun` | Dev server with hot reload |
| `./gradlew :prism-demo:wasmJsBrowserDistribution` | Production build to `build/dist/wasmJs/productionExecutable/` |

### iOS

The iOS demo is a Swift app (`prism-ios-demo/`) that showcases how to consume the KMP `prism-demo` XCFramework from native Swift. It uses a `UITabBarController` with two tabs:

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
xcodebuild -project prism-ios-demo/PrismiOSDemo.xcodeproj \
  -scheme PrismiOSDemo \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build

# Install and launch on a booted simulator
xcrun simctl install booted prism-ios-demo/build/Debug-iphonesimulator/PrismiOSDemo.app
xcrun simctl launch booted com.hyeonslab.prism.ios.demo
```

> **Note:** `prism-demo` is the KMP multiplatform module — shared Kotlin code that runs on JVM, WASM, and iOS. It produces the `PrismDemo.xcframework` consumed by Swift. `prism-ios-demo` is the native Swift iOS app that embeds this framework. Swift code uses `import PrismDemo` to access KMP types.

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
