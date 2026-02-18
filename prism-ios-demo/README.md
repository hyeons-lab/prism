# prism-ios-demo

Native Swift iOS app that demonstrates how to consume the Prism Engine KMP framework. Uses a `UITabBarController` with two tabs:

| Tab | Rendering | UI Controls |
|---|---|---|
| **Native** | MTKView + `DemoRenderDelegate` | None |
| **Compose** | MTKView embedded via `UIKitView` | Material3 sliders, pause, color picker |

Both tabs share a single `DemoStore` and `SharedDemoTime` so rotation angle, pause state, speed, and cube color are synchronized across tabs.

## Relationship to `prism-demo-core`

- **`prism-demo-core`** is the KMP multiplatform module containing shared Kotlin code that runs on JVM (GLFW/Compose), WASM, and iOS. It produces the `PrismDemo.xcframework` consumed by this app.
- **`prism-ios-demo`** (this project) is the native Swift iOS app that embeds the KMP framework and showcases it from a Swift perspective.

Swift code uses `import PrismDemo` to access KMP types — this imports the XCFramework module, not this app.

## Build and Run

```bash
# 1. Build the KMP XCFramework
./gradlew :prism-demo-core:assemblePrismDemoDebugXCFramework

# 2. Generate Xcode project (requires xcodegen: brew install xcodegen)
cd prism-ios-demo && xcodegen generate

# 3. Build and launch on simulator
xcodebuild -project PrismiOSDemo.xcodeproj \
  -scheme PrismiOSDemo \
  -destination 'generic/platform=iOS Simulator' \
  -configuration Debug build

# Install and launch on a booted simulator
xcrun simctl install booted build/Debug-iphonesimulator/PrismiOSDemo.app
xcrun simctl launch booted com.hyeonslab.prism.ios.demo
```

## Project Structure

```
prism-ios-demo/
├── project.yml              # xcodegen spec
├── Sources/
│   ├── AppDelegate.swift
│   ├── SceneDelegate.swift        # UITabBarController setup
│   ├── ViewController.swift       # Native tab (MTKView)
│   ├── ComposeViewController.swift # Compose tab (UIKitView)
│   └── Info.plist
└── README.md
```

## Prerequisites

- Xcode 14+ with Command Line Tools
- [xcodegen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`)
- `PrismDemo.xcframework` built via `./gradlew :prism-demo-core:assemblePrismDemoDebugXCFramework`
