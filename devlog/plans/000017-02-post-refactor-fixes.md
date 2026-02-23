# Plan: Fix Post-Refactor Issues in prism-flutter / prism-flutter-demo

## Thinking

The critical review of the Step 10 refactor (generalise `prism-flutter` + `prism-flutter-demo`)
identified 12 issues. They fall into four categories:

1. **Resource management** (Fixes 1, 7): `shutdown()` doesn't release GPU resources; `bridge`
   may be accessed before initialisation in the detach path.

2. **Rendering correctness** (Fixes 2, 3, 9, 10): surface resizes are silently ignored;
   FPS is never dispatched; a frame-timing spike occurs on resume; the pause guard is
   asymmetric between Android and macOS.

3. **API / type safety** (Fixes 6, 8, 12): the two-factory pattern in `PrismFlutterPlugin`
   forces an unchecked cast; GLB asset loading was removed from Android; `DemoBridge` is
   unnecessarily `open`.

4. **Compile / registration errors** (Fixes 4, 5, 11): `PrismFlutterPlugin.swift` calls the
   no-arg `PrismMacOSPlatformViewFactory()` init that no longer exists; the macOS example
   registers the platform view twice; a stale `@SuppressLint` annotation remains.

Issues are addressed in dependency order: SDK changes first, then demo bridges, then the
plugin entry points and Swift/macOS layer.

## Plan

### Fix 1 — `shutdown()` GPU resource leak
Add `override fun shutdown()` to `DemoBridge`, `DemoAndroidBridge`, `DemoMacosBridge`:
```kotlin
override fun shutdown() {
    scene?.shutdown()
    super.shutdown()
}
```
Files: `DemoBridge.kt`, `DemoAndroidBridge.kt`, `DemoMacosBridge.kt`.

---

### Fix 2 — Surface resize silently ignored (Android)
**A** Add `open fun onDimensionsChanged(width: Int, height: Int) {}` to `PrismAndroidBridge`.
**B** Override in `DemoAndroidBridge`: `scene?.updateAspectRatio(width, height)`.
**C** In `PrismPlatformView.surfaceChanged`, replace early `return` with:
```kotlin
if (bridge.isInitialized()) {
    bridge.onDimensionsChanged(width, height)
    return
}
```
Files: `PrismAndroidBridge.kt`, `DemoAndroidBridge.kt`, `PrismPlatformView.kt`.

---

### Fix 3 — FPS never dispatched
Dispatch FPS at the start of `tickScene` in both `DemoAndroidBridge` and `DemoMacosBridge`:
```kotlin
if (deltaTime > 0f) {
    val smoothedFps = store.state.value.fps * 0.9f + (1f / deltaTime) * 0.1f
    store.dispatch(DemoIntent.UpdateFps(smoothedFps))
}
```
Files: `DemoAndroidBridge.kt`, `DemoMacosBridge.kt`.

---

### Fix 4 — `PrismFlutterPlugin.swift` compile error (no-arg init gone)
Replace the direct `PrismMacOSPlatformViewFactory()` call with a `configure`/`register`
pattern that requires a `bridgeFactory` closure before the Flutter engine starts.
File: `PrismFlutterPlugin.swift`.

---

### Fix 5 — Double platform-view registration (macOS)
Remove manual `registrar.register(factory, withId:)` from `AppDelegate`. Call
`PrismFlutterPlugin.configure { DemoMacosBridge() }` before `super.applicationDidFinishLaunching`
so `GeneratedPluginRegistrant` performs the single registration.
File: `AppDelegate.swift`.

---

### Fix 6 — `handlerFactory` type mismatch / unchecked cast
Replace the two-factory `configure(bridgeFactory:handlerFactory:)` with a single
`configure((Context) -> PrismBridgeBundle)` where `PrismBridgeBundle` holds both
the bridge and handler. The host creates both in the same typed scope — no cast needed.
Context parameter also enables Fix 8.
File: `PrismFlutterPlugin.kt`.

---

### Fix 7 — `lateinit bridge` access before initialisation
Guard `bridge.shutdown()` in `onDetachedFromEngine`:
```kotlin
if (::bridge.isInitialized) bridge.shutdown()
```
File: `PrismFlutterPlugin.kt`.

---

### Fix 8 — GLB asset loading removed from Android
Make `createScene`/`onSurfaceReady` `suspend`. Add `glbLoader: (() -> ByteArray?)? = null`
to `DemoAndroidBridge`. Load GLB bytes inside `createScene`; fall back to procedural demo
scene if null.
Files: `PrismAndroidBridge.kt`, `DemoAndroidBridge.kt`.

---

### Fix 9 — Frame timing spike on resume
Add `fun resetFrameTiming()` to `PrismAndroidBridge` (sets `lastMark = now`). Call it
in `PrismPlatformView.resumeRendering()` before posting the frame callback.
Files: `PrismAndroidBridge.kt`, `PrismPlatformView.kt`.

---

### Fix 10 — Pause check asymmetry (Android vs macOS)
Add `protected open val isPaused: Boolean get() = false` to `PrismMetalBridge`.
Add `if (!isPaused)` guard to `renderFrame()`. Remove the inline `if (!isPaused)` from
`DemoMacosBridge.tickScene()`. Change `DemoMacosBridge.isPaused` to `override val`.
Files: `PrismMetalBridge.kt`, `DemoMacosBridge.kt`.

---

### Fix 11 — Stale `@SuppressLint("ClickableViewAccessibility")`
Remove the annotation and its import from `PrismPlatformView.kt`.

---

### Fix 12 — `DemoBridge` is `open` with no subclass
Change `open class DemoBridge` → `class DemoBridge`.
File: `DemoBridge.kt`.

---

## Files Changed

| File | Fixes |
|------|-------|
| `prism-flutter/src/androidMain/.../PrismAndroidBridge.kt` | #2, #8, #9 |
| `prism-flutter/src/macosMain/.../PrismMetalBridge.kt` | #10 |
| `prism-flutter-demo/src/commonMain/.../DemoBridge.kt` | #1, #12 |
| `prism-flutter-demo/src/androidMain/.../DemoAndroidBridge.kt` | #1, #2, #3, #8 |
| `prism-flutter-demo/src/macosMain/.../DemoMacosBridge.kt` | #1, #3, #10 |
| `prism-flutter/flutter_plugin/android/.../PrismFlutterPlugin.kt` | #6, #7 |
| `prism-flutter/flutter_plugin/android/.../PrismPlatformView.kt` | #2, #9, #11 |
| `prism-flutter/flutter_plugin/macos/.../PrismFlutterPlugin.swift` | #4 |
| `prism-flutter/flutter_plugin/example/macos/Runner/AppDelegate.swift` | #5 |
