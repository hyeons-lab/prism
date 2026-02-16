# Plan: iOS Native Support — Critical Review Fixes

## Thinking

After completing the initial iOS native support implementation (Session 1, commits `1bd8607`–`890cc9f`), I did a critical review of PR #15 to catch issues before merging. The review surfaced 11 findings across 3 severity levels.

**P0 — Critical (must fix before merge):**

The most serious issue was resource leaks. `configureDemo()` returned an `IosContext`, but the Swift ViewController discarded the return value entirely — meaning the WGPU instance, adapter, device, and surface would never be released. Worse, `DemoScene` was captured inside `DemoRenderDelegate` but had no shutdown path at all. On JVM we call `scene.shutdown()` in `windowClosing`, on WASM in `onBeforeUnload`, but iOS had nothing.

The fix pattern was clear: wrap both `IosContext` and `DemoScene` in a single `IosDemoHandle` class with a `shutdown()` method, return it from `configureDemo()`, and have Swift store it and call `shutdown()` in `deinit`. This mirrors how the other platforms handle cleanup.

The third P0 was error handling. If `MTLCreateSystemDefaultDevice()` returned nil (simulator without Metal, old device), the app would `fatalError()`. If wgpu initialization failed, it would silently show a black screen. Both needed graceful error display.

**P1 — Should fix:**

`MTKView.drawableSize` can be zero in `viewDidLoad` because Auto Layout hasn't computed the view's bounds yet. The `iosContextRenderer()` call would get width=0, height=0, which could cause a wgpu surface configuration failure or division-by-zero in aspect ratio calculation. Fix: check for zero and fall back to sensible defaults (800x600), then let `mtkView(drawableSizeWillChange:)` correct the aspect ratio once layout completes.

The Info.plist had `armv7` in `UIRequiredDeviceCapabilities` — but the XCFramework only contains arm64 slices. This would prevent the app from installing on arm64-only devices that don't report armv7 compatibility (all modern iOS devices). Fixed to `arm64`.

`ROTATION_SPEED` was declared as `const val` with `.toFloat()` — but `.toFloat()` is not a compile-time constant expression in Kotlin, so this wouldn't compile. Changed to `val`.

The UISceneDelegate modernization (P1 #7) was a larger structural change that I deferred to issue #16, since the UIApplicationDelegate pattern still works on iOS 15+ (just with a deprecation warning).

**P2 — Quality improvements:**

Several P2 items were identified but deferred: Compose deps leaking into iOS binary (#17), rotation logic duplication across platforms (#18), and Swift/Kotlin interop being untested without a real device. These are real issues but not blockers for the PR — they're pre-existing or require separate focused work.

---

## Plan

### Context

Critical review of PR #15 (iOS native support) found 3 P0, 3 P1, and 5 P2 issues. P0s are resource leaks and missing error handling. P1s are zero-size edge case, wrong device capability, and deprecated lifecycle. P2s are deferred to follow-up issues.

### Changes

#### 1. Fix resource leaks (P0 #1, #2)

**`prism-demo/src/iosMain/.../IosDemoController.kt`**
- Add `IosDemoHandle` class wrapping `IosContext` + `DemoScene` with `shutdown()` method
- `configureDemo()` returns `IosDemoHandle` instead of bare `IosContext`
- `shutdown()` calls `scene.shutdown()` then `iosContext.close()`

**`ios-demo/Sources/ViewController.swift`**
- Store `IosDemoHandle` as property (`private var demoHandle: IosDemoHandle?`)
- Call `demoHandle?.shutdown()` in `deinit`

#### 2. Add error handling (P0 #3)

**`ios-demo/Sources/ViewController.swift`**
- Add `showError(_ message: String)` method displaying a red UILabel centered on screen
- Guard `MTLCreateSystemDefaultDevice()` with `showError` instead of `fatalError`
- Handle wgpu init failure in completion handler with `showError`

#### 3. Fix zero drawableSize (P1 #4)

**`prism-demo/src/iosMain/.../IosDemoController.kt`**
- Check `drawableSize` width/height for <= 0 before calling `iosContextRenderer()`
- Fall back to `DEFAULT_WIDTH = 800`, `DEFAULT_HEIGHT = 600`
- Log warning when using defaults
- `mtkView(drawableSizeWillChange:)` already corrects aspect ratio on first layout

#### 4. Fix const val compile error (P1 #5)

**`prism-demo/src/iosMain/.../IosDemoController.kt`**
- Change `ROTATION_SPEED` from `const val` to `val` (`.toFloat()` is not compile-time constant)

#### 5. Fix device capabilities (P1 #6)

**`ios-demo/Sources/Info.plist`**
- Change `armv7` to `arm64` in `UIRequiredDeviceCapabilities`
- Add `UISupportedInterfaceOrientations~ipad` with all 4 orientations

#### 6. Defer remaining items to GitHub issues

- **#16:** Modernize to UISceneDelegate lifecycle (P1 #7)
- **#17:** Move Compose deps out of commonMain (P2 #8)
- **#18:** Extract shared rotation logic to `DemoScene.tick()` (P2 #11)

### Files Summary

| File | Action |
|------|--------|
| `prism-demo/src/iosMain/.../IosDemoController.kt` | Update — IosDemoHandle, defaults, val fix |
| `ios-demo/Sources/ViewController.swift` | Update — store handle, deinit, showError |
| `ios-demo/Sources/Info.plist` | Update — arm64, iPad orientations |

### Verification

1. `./gradlew compileKotlinIosArm64` — iOS code compiles
2. `./gradlew ktfmtCheck detektJvmMain jvmTest` — quality checks pass
3. GitHub issues #16, #17, #18 created for deferred items
