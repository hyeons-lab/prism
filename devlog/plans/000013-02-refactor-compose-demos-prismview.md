## Context

All Compose demos (JVM, Android) bypassed PrismView/EngineStore by directly embedding GPU views, creating DemoScene (which creates its own Engine), and driving hand-rolled render loops. This existed because PrismView lacked two capabilities: (1) no WGPUContext delivery to callers, and (2) no per-frame scene hook.

## Plan

1. Promote wgpu4k to `api` in prism-renderer so WGPUContext is importable downstream
2. Add `wgpuContext` to PrismSurface expect (all 7 actuals already had it)
3. Add `onSurfaceReady(WGPUContext, Int, Int)` and `onSurfaceResized(Int, Int)` callbacks to PrismView expect and all actuals
4. Forward callbacks through PrismOverlay
5. Add `createDemoScene(engine, wgpuContext, ...)` overload that uses an existing Engine and wires `gameLoop.onRender` to drive ECS
6. Refactor Android ComposeAndroidEntry to use PrismOverlay with callbacks
7. Refactor JVM ComposeMain to use PrismOverlay with callbacks (split layout -> overlay layout)
8. Delete ComposeDemoApp (superseded) and unused Main.kt
9. Clean up redundant wgpu4k deps from prism-native-widgets and prism-compose
10. Delete unused AndroidSurfaceInfo data class
11. Validate: ktfmtCheck, detektJvmMain, jvmTest, assembleDebug
