# Plan: iOS Native Support (M7)

## Thinking

The goal is to get the existing rotating lit cube demo running natively on iOS. Let me work through what's needed.

**Starting point assessment:** All Prism modules already declare `iosArm64` and `iosSimulatorArm64` targets, but they're empty — no source files in any `iosMain/` directory under `prism-demo`. The renderer has a `RenderSurface.ios.kt` but it's a stub that throws `TODO()`. So the KMP target declarations are there but nothing is wired up.

**The key question: how does wgpu4k work on iOS?** I checked the wgpu4k source at `~/development/wgpu4k` and its Maven local artifacts. The toolkit has an `iosMain/` with two files: `context.ios.kt` and `Surface.ios.kt`. The entry point is `suspend fun iosContextRenderer(view: MTKView, width: Int, height: Int): IosContext`. It takes an MTKView, extracts the CAMetalLayer via `objcPtr()` pointer casting, and creates the full WGPU pipeline (instance → surface → adapter → device). Returns an `IosContext` wrapping `WGPUContext`. This is the iOS analog of `glfwContextRenderer()` (JVM) and `canvasContextRenderer()` (WASM).

**So the pattern is clear:** Swift creates an MTKView → passes it to Kotlin → Kotlin calls `iosContextRenderer()` → gets a `WGPUContext` → feeds it to the same `createDemoScene()` factory used by JVM and WASM.

**Problem: DemoScene.kt is JVM-only.** It lives in `prism-demo/src/jvmMain/`. But looking at its imports — `Engine`, `World`, `WgpuRenderer`, `Camera`, `Mesh`, `Color`, `WGPUContext` — these are all available in commonMain (wgpu4k has common API types). The only reason it's in jvmMain is historical: it was created for the JVM demo first. Moving it to commonMain should work if wgpu4k deps are available there.

**Dep consolidation needed:** `prism-demo/build.gradle.kts` has wgpu4k in `jvmMain.dependencies` and `wasmJsMain.dependencies` separately. But `prism-renderer/build.gradle.kts` already has wgpu4k in `commonMain.dependencies` — so the pattern of having it in common is established. I can consolidate.

**WASM refactoring opportunity:** `WasmMain.kt` duplicates the entire scene setup (Engine, World, Camera, Cube entity creation) — about 40 lines that are identical to `createDemoScene()`. Once DemoScene.kt is in commonMain, WASM can use `createDemoScene()` too. This is an opportunistic cleanup that reduces duplication.

**iOS render loop:** On JVM it's a `while(!shouldClose)` loop. On WASM it's `requestAnimationFrame`. On iOS, MetalKit provides `MTKViewDelegateProtocol` with `drawInMTKView()` — called by the display link at the view's `preferredFramesPerSecond`. This is the idiomatic Metal render loop. The delegate also gets `mtkView(_:drawableSizeWillChange:)` for resize events. I need a K/N class implementing this protocol.

**K/N ObjC interop considerations:** To implement `MTKViewDelegateProtocol`, the class needs to extend `NSObject()` and use `@OptIn(BetaInteropApi::class)`. The `drawableSizeWillChange` parameter comes as `CValue<CGSize>` which needs `.useContents {}` to unpack. Frame timing via `CACurrentMediaTime()` from `platform.QuartzCore`.

**No `runBlocking` on K/N** — `iosContextRenderer` is a suspend function. The Swift side calls it via the generated completion handler pattern (Kotlin suspend → Swift async/callback). So Swift calls `configureDemo(view:) { context, error in ... }`.

**XCFramework vs regular framework:** Need to produce a `.xcframework` that bundles both `iosArm64` (device) and `iosSimulatorArm64` (simulator) slices. Kotlin/Native supports this via `XCFramework("BaseName")` in the build script. Using `isStatic = true` avoids the need to embed a dynamic framework in the app bundle.

**Xcode project:** Rather than a manually maintained `.xcodeproj`, I'll use xcodegen with a `project.yml` spec. This keeps the source of truth in a small YAML file and the generated `.xcodeproj` can be gitignored. The project needs to link the XCFramework and MetalKit/Metal frameworks.

**RenderSurface.ios.kt:** The `TODO()` in `configure()` will crash at runtime. On iOS the wgpu4k path doesn't use `RenderSurface` (the surface is created by `iosContextRenderer` from the MTKView directly), but if any code path hits it, it shouldn't crash. Replace with logging.

**Implementation sequence:** I want to minimize risk of breaking existing platforms. So:
1. First move deps and DemoScene to commonMain — verify JVM and WASM still compile
2. Then add iOS-specific code — verify it compiles
3. Then scaffold the Xcode project — verify XCFramework builds
4. Quality checks last

---

## Plan

### Context

All Prism engine modules already declare iOS targets (`iosArm64`, `iosSimulatorArm64`) but have only stub implementations. wgpu4k ships full iOS `.klib` artifacts and provides `iosContextRenderer(MTKView, width, height)` in wgpu4k-toolkit that creates a `WGPUContext` from a Metal-backed `MTKView`. The goal is to wire this up so the existing `DemoScene` (rotating lit cube) runs natively on iOS via an Xcode project generated by xcodegen.

### Architecture

```
Xcode Project (ios-demo/, generated by xcodegen)
├── Swift AppDelegate + ViewController
│   └── MTKView (Metal-backed)
│       └── Calls Kotlin's configureDemo(view) via framework interop
└── Links PrismDemo.xcframework (static, built by Gradle)

Kotlin (prism-demo iosMain)
├── IosDemoController.kt
│   ├── suspend fun configureDemo(view: MTKView)
│   │   └── iosContextRenderer(view, w, h) → IosContext → createDemoScene()
│   └── DemoRenderDelegate : MTKViewDelegateProtocol
│       ├── drawInMTKView() → tick render loop (rotation + world.update)
│       └── mtkView(drawableSizeWillChange:) → scene.updateAspectRatio()
└── Uses shared DemoScene from commonMain
```

### Files to Modify/Create

#### 1. Move DemoScene.kt to commonMain

**`prism-demo/src/jvmMain/.../DemoScene.kt`** → **`prism-demo/src/commonMain/.../DemoScene.kt`**
- Already uses only common APIs (Engine, World, WgpuRenderer, Camera, Mesh, etc.)
- Requires wgpu4k in commonMain deps (for `WGPUContext` import)

#### 2. Update prism-demo/build.gradle.kts

- Move `wgpu4k` + `wgpu4k-toolkit` from jvmMain/wasmJsMain to commonMain deps
- Add iOS framework binary configuration:
  ```kotlin
  val xcf = XCFramework("PrismDemo")
  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
      target.binaries.framework {
          baseName = "PrismDemo"
          isStatic = true
          xcf.add(this)
      }
  }
  ```

#### 3. Create iOS entry point

**`prism-demo/src/iosMain/kotlin/com/hyeonslab/prism/demo/IosDemoController.kt`** — New
- `suspend fun configureDemo(view: MTKView)`: calls `iosContextRenderer()`, creates `DemoScene` via shared `createDemoScene()`
- `DemoRenderDelegate`: implements `MTKViewDelegateProtocol`
  - `drawInMTKView()`: updates cube rotation, calls `world.update(time)`
  - `mtkView(drawableSizeWillChange:)`: calls `scene.updateAspectRatio()`
- Uses `@OptIn(ExperimentalForeignApi::class)` for K/N cinterop types
- Frame timing via `platform.QuartzCore.CACurrentMediaTime()`

#### 4. Create Xcode project via xcodegen

**`ios-demo/project.yml`** — New (xcodegen spec)
- Target: `PrismDemo` iOS app, deployment target 15.0
- Links `PrismDemo.xcframework` from `../prism-demo/build/XCFrameworks/release/`
- Links `MetalKit.framework`
- Sources from `ios-demo/Sources/`

**`ios-demo/Sources/AppDelegate.swift`** — New
- Standard `UIApplicationDelegate` with `UIWindow` setup

**`ios-demo/Sources/ViewController.swift`** — New
- Creates `MTKView` with `MTLCreateSystemDefaultDevice()`
- Calls `IosDemoControllerKt.configureDemo(view:)` in `viewDidLoad()`

**`ios-demo/Sources/Info.plist`** — New
- Standard iOS app plist

#### 5. Update iOS stubs

**`prism-renderer/src/iosMain/.../RenderSurface.ios.kt`**
- Replace `TODO()` in `configure()` with Kermit logging

#### 6. Refactor WasmMain.kt (opportunistic cleanup)

**`prism-demo/src/wasmJsMain/.../Main.kt`**
- Replace inline scene setup with `createDemoScene()` (now available from commonMain)
- Keep WASM-specific render loop (`requestAnimationFrame`)

### Implementation Sequence

1. Add wgpu4k + wgpu4k-toolkit to prism-demo commonMain deps (remove from jvmMain/wasmJsMain)
2. Move `DemoScene.kt` from jvmMain to commonMain
3. Verify JVM demo still works: `./gradlew :prism-demo:compileKotlinJvm`
4. Refactor `WasmMain.kt` to use shared `createDemoScene()`
5. Verify WASM still builds: `./gradlew :prism-demo:compileKotlinWasmJs`
6. Add iOS framework binary config to build.gradle.kts
7. Create `IosDemoController.kt` in iosMain
8. Fix `RenderSurface.ios.kt` stub (remove TODO crash)
9. Build XCFramework: `./gradlew assemblePrismDemoReleaseXCFramework`
10. Create `ios-demo/project.yml` for xcodegen
11. Create Swift source files (AppDelegate, ViewController)
12. Add `ios-demo/` to `.gitignore` for generated `.xcodeproj` contents
13. Run quality checks: `./gradlew ktfmtFormat ktfmtCheck detektJvmMain jvmTest`

### Verification

1. **JVM regression**: `./gradlew :prism-demo:compileKotlinJvm` succeeds
2. **WASM regression**: `./gradlew :prism-demo:compileKotlinWasmJs` succeeds
3. **iOS framework**: `./gradlew assemblePrismDemoReleaseXCFramework` produces XCFramework
4. **iOS app**: `xcodegen generate` in ios-demo/ → build in Xcode → run on iOS Simulator
5. **Code quality**: `./gradlew ktfmtCheck detektJvmMain jvmTest` all pass
