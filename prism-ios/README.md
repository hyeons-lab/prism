# prism-ios

iOS XCFramework distribution module. Aggregates all Prism Engine libraries into a single `Prism.xcframework` for consumption via Swift Package Manager.

This module has no code of its own — it re-exports every engine module so that iOS consumers get the full Prism API through a single `import Prism`.

## How it differs from similar modules

| Module | What it is | Produces | Consumed by |
|---|---|---|---|
| **`prism-ios`** (this) | Distribution aggregator — iOS only | `Prism.xcframework` | Any iOS app via SPM |
| `prism-demo` | KMP demo app (JVM, WASM, iOS) | `PrismDemo.xcframework` | `prism-ios-demo` |
| `prism-ios-demo` | Native Swift iOS app | `.app` bundle | End users / simulators |

- **`prism-ios`** bundles the engine for library consumers. Use this if you're building your own iOS app with Prism.
- **`prism-demo`** bundles demo-specific code (rotating cube, MVI store, Compose controls) on top of the engine. It targets all platforms, not just iOS.
- **`prism-ios-demo`** is a Swift app that imports `PrismDemo` to showcase the engine. It is not a library.

## Exported modules

The framework re-exports all public APIs from:

- `prism-math` — Vec2/3/4, Mat3/4, Quaternion, Transform
- `prism-core` — Engine, GameLoop, Subsystem, Store
- `prism-renderer` — Renderer, Mesh, Material, Shader, Camera
- `prism-scene` — Node, Scene, MeshNode, CameraNode, LightNode
- `prism-ecs` — World, Entity, Component, System
- `prism-input` — InputManager, InputEvent
- `prism-assets` — AssetManager, loaders
- `prism-audio` — Audio interfaces (stub)
- `prism-native-widgets` — Platform-specific rendering surfaces (PrismSurface)
- `prism-compose` — PrismView, EngineStore, PrismOverlay

## Build

```bash
# Debug (used in CI)
./gradlew :prism-ios:assemblePrismDebugXCFramework

# Release (used for distribution)
./gradlew :prism-ios:assemblePrismReleaseXCFramework
```

Output: `prism-ios/build/XCFrameworks/{debug|release}/Prism.xcframework`

## Distribution

Release builds are packaged and published automatically by the `release.yml` GitHub Actions workflow when a `v*` tag is pushed. The workflow zips the XCFramework, creates a GitHub Release, and updates `Package.swift` on `main` with the download URL and checksum so SPM resolves it automatically.
