# Prism Engine - Build Status

## Phase 1: Build System Setup ✅ Complete

### All Modules Compiling (JVM)
- ✅ prism-math
- ✅ prism-core
- ✅ prism-renderer
- ✅ prism-ecs
- ✅ prism-scene
- ✅ prism-input
- ✅ prism-audio
- ✅ prism-assets
- ✅ prism-native-widgets
- ✅ prism-compose
- ✅ prism-demo

### Configuration Applied
- [x] Gradle 9.2.0
- [x] wgpu4k 0.2.0-SNAPSHOT (Maven local)
- [x] KMP properties (enableCInteropCommonization, ignoreDisabledTargets)
- [x] `-Xexpect-actual-classes` flag on modules with expect/actual declarations
- [x] `@PublishedApi internal` for inline function field access (prism-core, prism-ecs)
- [x] Native platform implementations (Linux, macOS, Windows) for Platform
- [x] JVM toolchain 25 for prism-demo, prism-native-widgets, prism-compose (FFI support)

## Phase 2: WgpuRenderer Implementation ✅ Complete

### Completed
- [x] WgpuRenderer class implementing Renderer interface
- [x] WGSL shaders for lit/unlit materials (Shaders.kt)
- [x] GlfwMain.kt demo entry point with GLFW windowing
- [x] Cube mesh with vertex/index buffers and depth testing
- [x] Uniform buffers (VP + model matrices) and bind groups
- [x] Material uniform buffer (baseColor)
- [x] Surface configuration and present lifecycle

### Pending
- [x] ~~RenderSurface~~ — Deleted; superseded by PrismSurface (prism-native-widgets) and PrismPanel (prism-compose)
- [x] WASM/Canvas integration for web (M6 complete)

## Milestones

### M1: Triangle on Screen (JVM) ✅
- wgpu4k compiles and links
- GLFW window opens
- Render pipeline with WGSL shaders
- **Status:** Complete

### M2: Rotating Cube (JVM) ✅
- Mesh.cube() uploaded to GPU with vertex/index buffers
- Camera uniform buffer with view-projection matrix
- Model matrix uniform updated each frame (45 deg/sec rotation)
- Depth testing enabled
- **Status:** Complete — `./gradlew :prism-demo:jvmRun` renders a rotating cube

### M3: Lit Cube with Materials ✅
- Directional light in fragment shader
- Material baseColor applied (blue cube)
- Normal-based diffuse (0.85) + ambient (0.15) lighting
- Cornflower blue background
- **Status:** Complete — lighting and materials working in demo

### M4: ECS-Driven Rendering ✅
- WgpuRenderer resource lifecycle fixed (per-frame AutoClosableContext, surface.configure())
- CameraComponent added to ECS
- RenderSystem implemented (queries entities, drives WgpuRenderer)
- GlfwMain.kt rewritten to use Engine + World + ECS entities
- **Status:** Complete — same rotating cube, now driven through Engine + ECS + WgpuRenderer pipeline

### M5: Compose Integration (JVM) ✅
- PrismPanel: AWT Canvas subclass with native handle extraction (macOS/Windows/Linux)
- AwtRenderingContext: custom RenderingContext bypassing GLFW glfwGetWindowSize()
- WgpuRenderer: surfacePreConfigured flag + onResize callback for AWT integration
- MVI architecture: `Store<State, Event>` interface in prism-core, `EngineStore`/`EngineState`/`EngineStateEvent` in prism-compose
- PrismView: stateless composable taking `EngineStore`, dispatches events through reducer (no callbacks)
- PrismOverlay/PrismTheme: composable wrappers accepting `EngineStore`
- Render loop: Compose `withFrameNanos` for vsync-synchronized frame timing
- Compose demo: Material3 UI controls driving 3D scene (rotation, color, pause) via `DemoStore` MVI
- Platform stubs (WASM, Apple) updated with Kermit logging; iosMain → appleMain for iOS + macOS coverage
- **Status:** Complete — `./gradlew :prism-demo:runCompose` launches Compose window with embedded 3D rendering

### M6: Web/WASM Support ✅
- WASM demo entry point using canvasContextRenderer()
- HTML Canvas with WebGPU feature detection and fallback
- requestAnimationFrame render loop with actual deltaTime
- Resource cleanup on error and page unload
- **Status:** Complete — `./gradlew :prism-demo:wasmJsBrowserDevelopmentRun` renders rotating cube in browser

### M7: iOS Native Support ✅
- DemoScene.kt moved to commonMain (shared across JVM, WASM, iOS) with `tick()` method deduplicating rotation logic
- wgpu4k deps consolidated from jvmMain/wasmJsMain to commonMain
- All platform render loops (GLFW, WASM, iOS) simplified to use `DemoScene.tick()`
- IosDemoController.kt: `configureDemo(MTKView)` + `DemoRenderDelegate` (MTKViewDelegateProtocol) with `IosDemoHandle` lifecycle wrapper
- Compose iOS demo: `ComposeIosEntry.kt` with `UIKitView` embedding MTKView, `DemoStore` MVI state, Material3 controls overlay
- UISceneDelegate modernization: `SceneDelegate.swift` with `UITabBarController` (Native + Compose tabs)
- XCFramework binary config (iosArm64 + iosSimulatorArm64, static framework)
- Xcode project scaffolding via xcodegen (prism-ios-demo/)
- Error handling: null Metal device guard, try-catch for wgpu init, error overlay UI
- Thread safety: `NSOperationQueue.mainQueue` for Compose state dispatch from Metal render thread
- Lazy wgpu init via `viewDidAppear` with `isPaused` guard to avoid double GPU memory
- `renderer.resize()` called on drawable size change for correct depth texture recreation
- prism-assets FileReader: nativeMain actual using kotlinx.io SystemFileSystem (covers all native targets); iosMain TODO stub removed
- **Status:** Complete — `./gradlew assemblePrismDemoDebugXCFramework` builds; Xcode project ready with two demos
### M7.5: PrismSurface + Native Demos ✅
- PrismSurface refactored to suspend factory pattern (`createPrismSurface()`) across all 7 platforms
- All demo consumers (JVM GLFW, iOS native, iOS Compose, WASM) wired through PrismSurface
- macOS native demo: GLFW/Metal window + AppKit floating NSPanel controls (speed slider, pause button)
- Android build targets added to prism-math, prism-core, prism-renderer, prism-native-widgets
- Android actuals created: Platform, PrismSurface (stubs, wgpu4k rendering deferred to M8)
- AGP bumped to 8.13.0 for maven-publish + android-library compatibility
- Double-close vulnerability fixed: mutable backing fields with null-on-close in detach()
- **Status:** Complete — `./gradlew :prism-demo:runDebugExecutableMacosArm64` runs macOS native demo

### M8: Android Support ✅
- [x] Android build targets added to all modules (migrated to `com.android.kotlin.multiplatform.library`)
- [x] Android actuals: Platform, PrismSurface, FileReader, PrismView
- [x] wgpu4k rendering via Vulkan (using wgpu4k-toolkit-android AAR, not PanamaPort)
- [x] Android demo: PrismDemoActivity with SurfaceView + Choreographer render loop
- [x] sRGB double-gamma fix: `srgbToLinear()` conversion for sRGB surface formats
- [x] ByteBuffer byte-order fix: forked webgpu-ktypes with `.order(ByteOrder.nativeOrder())`
- [x] Three upstream forks (wgpu4k-native, wgpu4k, webgpu-ktypes) for Android API 35+ compatibility
- **Status:** Complete — rotating lit cube on Android (Galaxy Fold, API 36, Vulkan)

### M9: PBR Materials ✅
- [x] Cook-Torrance BRDF: GGX NDF, Smith-GGX geometry, Fresnel-Schlick
- [x] Explicit multi-bind-group pipeline layout (4 groups: scene, object, material, environment)
- [x] Scene uniforms: VP matrix, camera position, ambient, light count (96 bytes)
- [x] Object uniforms: model matrix + padded normalMatrix mat3x3f (112 bytes)
- [x] Material uniforms: baseColor, metallic, roughness, emissive, occlusion, texture flags (48 bytes)
- [x] Light storage buffer: max 16 lights, 64 bytes/light, directional/point/spot types
- [x] UV sphere mesh with tangents (positionNormalUvTangent vertex layout)
- [x] Tangent-space normal mapping via TBN matrix + flag-based shader branching
- [x] Five PBR texture slots: albedo, normal, metallicRoughness, occlusion, emissive
- [x] ECS PBR RenderSystem: queries LightComponent entities, builds LightData list each frame
- [x] HDR render target (RGBA16Float intermediate) + Khronos PBR Neutral tone mapping
- [x] CPU-side IBL: BRDF split-sum LUT (256×256, Hammersley + GGX importance sampling)
- [x] CPU-side IBL: irradiance cubemap (hemisphere convolution of procedural sky)
- [x] CPU-side IBL: prefiltered specular env cubemap (5 mip levels, GGX importance sampling)
- [x] Procedural sky gradient: zenith blue → horizon white → nadir brown
- [x] PBR sphere grid demo: 7×7 spheres (metallic 0→1 on X, roughness 0→1 on Y)
- [x] Compose PBR controls: env intensity, metallic, roughness sliders
- **Status:** Complete — `./gradlew :prism-demo-core:jvmRun` renders PBR sphere grid with IBL + HDR

### M10: glTF Asset Loading ⏳
### M11: Flutter Integration ✅

- [x] Build system: prism-flutter module with Android, iOS, wasmJs targets, prism-demo-core dependency
- [x] PrismBridge + FlutterMethodHandler: DemoScene/DemoStore MVI bridge for native platforms
- [x] Android Flutter plugin: PrismFlutterPlugin.kt + PrismPlatformView.kt (SurfaceView + Choreographer)
- [x] iOS Flutter plugin: PrismFlutterPlugin.swift + PrismPlatformView.swift (MTKView + configureDemo)
- [x] Flutter Web: Kotlin/WASM @JsExport entry point + Dart JS interop via conditional imports
- [x] Dart plugin: PrismEngine (MethodChannel on mobile, JS interop on web), PrismRenderView (AndroidView/UiKitView/HtmlElementView)
- [x] Flutter example app: Material3 demo with speed slider, color buttons, pause/resume
- **Status:** Complete — Flutter plugin with Android, iOS, and Web support

## Test Coverage

| Module | Tests | Classes Tested |
|---|---|---|
| prism-math | 80 | Vec3 (26), Mat4 (29), Quaternion (25) |
| prism-renderer | 146 | Color (24), Mesh (35), VertexLayout (22), Camera (19), Shader (29), IblGenerator (8), LightData (6), Material (3) |
| prism-demo-core | 10 | DemoStore (MVI reducer, 10) |
| **Total** | **236** | |

Run tests: `./gradlew jvmTest` (JVM) or `./gradlew macosArm64Test iosSimulatorArm64Test` (Apple native)

## Code Quality

- [x] KtFmt (Google style, 100-char max width) via `./gradlew ktfmtCheck`
- [x] Detekt (maxIssues=0) via `./gradlew detektJvmMain`
- [x] `allWarningsAsErrors=true` on all modules
- [x] GitHub Actions CI (two jobs on every push/PR):
  - `ci` (ubuntu-latest): ktfmtCheck + detekt + jvmTest
  - `apple` (macos-15): macosArm64Test + iosSimulatorArm64Test + XCFramework build/verify + artifact upload + xcodebuild
  - wgpu4k build deduplicated into composite action (`.github/actions/setup-wgpu4k`)

## Build Commands

```bash
# Build all modules (JVM)
./gradlew compileKotlinJvm

# Full build with tests
./gradlew build

# Run demo app (JVM Desktop)
./gradlew :prism-demo:jvmRun

# Run Compose demo (JVM Desktop, embedded 3D in Compose)
./gradlew :prism-demo:runCompose

# Run demo app (WASM/Browser)
./gradlew :prism-demo:wasmJsBrowserDevelopmentRun

# Run macOS native demo (GLFW + AppKit controls)
./gradlew :prism-demo:runDebugExecutableMacosArm64

# Build Android APK
./gradlew :prism-demo:assembleDebug

# Install Android APK on connected device
adb install -r prism-demo/build/outputs/apk/debug/prism-demo-debug.apk

# Build iOS XCFramework (debug)
./gradlew assemblePrismDemoDebugXCFramework

# Generate Xcode project (requires xcodegen: brew install xcodegen)
cd prism-ios-demo && xcodegen generate
```

## Prerequisites

- JDK 25 (for FFI in prism-demo) or JDK 21+ (for other modules)
- Android SDK with API 28+ (for Android targets)
- wgpu4k 0.2.0-SNAPSHOT in Maven local (build from ~/development/wgpu4k)
- webgpu-ktypes 0.0.9-SNAPSHOT in Maven local (build from ~/development/webgpu-ktypes)
