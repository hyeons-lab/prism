# 000020 — feat/ios-ffi-bridge-prismview

**Agent:** Claude Sonnet 4.6 (claude-sonnet-4-6) @ prism feat/ios-ffi-bridge-prismview
**Intent:** Implement iOS FFI bridge in `prism-native`, add `onSurfaceReady` callback to `PrismView`, implement the apple actual (iOS) with UIKitView + MTKView, update Flutter iOS plugin to pass MTKView pointer, and refactor `ComposeIosEntry.kt` to use `PrismView`.

---

## Progress

- [ ] Part 1: iOS FFI bridge (`prism-native/src/iosMain/IosBridge.kt` + build.gradle.kts)
- [ ] Part 2a: `PrismView` expect + all actuals — add `onSurfaceReady` parameter
- [ ] Part 2b: `PrismView.ios.kt` — full UIKitView + MTKViewDelegateProtocol implementation
- [ ] Part 2c: `PrismView.macos.kt` — macOS stub with updated signature
- [ ] Part 3a: Refactor `ComposeIosEntry.kt` to use `PrismView(onSurfaceReady = ...)`
- [ ] Part 3b: Update `PrismFlutterPlugin.swift` to pass MTKView pointer

---

## What Changed

_To be filled as work progresses._

---

## Decisions

- `2026-02-23T...` Split `appleMain/PrismView.apple.kt` into `iosMain/PrismView.ios.kt` (full UIKitView implementation) and `macosMain/PrismView.macos.kt` (stub). Having both an `appleMain` actual AND an `iosMain` actual for the same `expect` would cause duplicate-actual compile errors; splitting ensures iOS gets the full impl while macOS keeps the stub.
- `2026-02-23T...` iOS FFI bridge (`IosBridge.kt`) uses `interpretObjCPointerOrNull<MTKView>` to reconstruct the MTKView from the opaque C pointer, matching the macOS bridge pattern (which uses `NativeAddress(ptr)` for a CAMetalLayer). This is necessary because `iosContextRenderer` takes an `MTKView`, not a raw layer pointer.
- `2026-02-23T...` Flutter iOS plugin changes to pass MTKView pointer rather than CAMetalLayer pointer so the Kotlin IosBridge can reconstruct the full MTKView object.

---

## Issues

_To be filled if problems arise._

---

## Commits

_To be filled._

---

## Next Steps

_After implementation: run `./gradlew :prism-native:linkDebugSharedIosSimulatorArm64` and `nm -gU ... | grep prism_` to verify FFI symbols._
