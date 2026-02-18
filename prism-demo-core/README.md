# prism-demo-core

Shared KMP library containing the demo application logic for all platforms. Renders a rotating lit cube driven by the ECS, with interactive Compose UI controls on supported platforms.

## Module relationship

```
prism-demo-core/            KMP library  — shared demo code (all platforms)
prism-ios-demo/        Swift app    — iOS entry point, consumes prism-demo-core XCFramework
prism-android-demo/    Android app  — Android entry point, consumes prism-demo-core AAR
```

`prism-demo-core` is a **library module**, not an application. For JVM, macOS native, and wasmJs it includes runnable entry points directly. For iOS and Android, it produces a framework/library consumed by a separate platform-specific app module:

- **`prism-ios-demo/`** is a native Swift Xcode project that embeds the `PrismDemo.xcframework` built by `prism-demo-core`. See [prism-ios-demo/README.md](../prism-ios-demo/README.md).
- **`prism-android-demo/`** is a pure Android application module that depends on `prism-demo-core` as a Gradle library dependency. It provides `PrismDemoActivity` — a minimal Activity with SurfaceView + Choreographer render loop.

This separation keeps the KMP library plugin (`com.android.kotlin.multiplatform.library`) in `prism-demo-core` and the Android application plugin (`com.android.application`) in `prism-android-demo`, which is required by AGP 8.x+ (combining both in one module is deprecated and will break in AGP 9).

## Platforms

### JVM Desktop

Three entry points are available:

| Entry point | File | Gradle task |
|---|---|---|
| **GLFW** (default) | `jvmMain/.../GlfwMain.kt` | `./gradlew :prism-demo-core:jvmRun` |
| **Compose + AWT** | `jvmMain/.../ComposeMain.kt` | `./gradlew :prism-demo-core:runCompose` |
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
| `./gradlew :prism-demo-core:wasmJsBrowserDevelopmentRun` | Dev server with hot reload |
| `./gradlew :prism-demo-core:wasmJsBrowserDistribution` | Production build to `build/dist/wasmJs/productionExecutable/` |

### iOS

The iOS demo is a Swift app (`prism-ios-demo/`) that showcases how to consume the KMP `prism-demo-core` XCFramework from native Swift. It uses a `UITabBarController` with two tabs:

| Tab | Rendering | UI Controls | Entry point |
|---|---|---|---|
| **Native** | MTKView + `DemoRenderDelegate` | None | `iosMain/.../IosDemoController.kt` |
| **Compose** | MTKView embedded via `UIKitView` | Material3 sliders, pause, color picker | `iosMain/.../ComposeIosEntry.kt` |

Both tabs share a single `DemoStore` and `SharedDemoTime` so rotation angle, pause state, speed, and cube color are synchronized across tabs. Changing any setting on the Compose tab is immediately reflected on the Native tab.

**Build and run:**

```bash
# Build XCFramework
./gradlew :prism-demo-core:assemblePrismDemoDebugXCFramework

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

> **Note:** `prism-demo-core` is the KMP multiplatform module — shared Kotlin code that runs on JVM, WASM, and iOS. It produces the `PrismDemo.xcframework` consumed by Swift. `prism-ios-demo` is the native Swift iOS app that embeds this framework. Swift code uses `import PrismDemo` to access KMP types.

### Android

The Android demo is a separate app module (`prism-android-demo/`) that depends on `prism-demo-core` as a library.

| Entry point | File | Description |
|---|---|---|
| **SurfaceView + Vulkan** | `prism-android-demo/.../PrismDemoActivity.kt` | Full-screen SurfaceView with Choreographer-driven render loop via wgpu4k `androidContextRenderer()` |

**Build and run:**

```bash
# Build debug APK
./gradlew :prism-android-demo:assembleDebug

# Install on connected device/emulator
adb install prism-android-demo/build/outputs/apk/debug/prism-android-demo-debug.apk
```

### macOS Native

| Entry point | File | Gradle task |
|---|---|---|
| **GLFW + AppKit** | `macosMain/.../MacosDemoMain.kt` | `./gradlew :prism-demo-core:runDebugExecutableMacosArm64` |

- **GLFW window** — Metal-backed rendering via `createPrismSurface()` + wgpu4k `glfwContextRenderer()`
- **AppKit controls** — Floating `NSPanel` with rotation speed slider (`NSSlider`) and pause/resume button (`NSButton`)
- Both GLFW and AppKit events are processed on the main thread via `glfwPollEvents()`

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
