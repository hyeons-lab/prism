## Thinking

The Flutter macOS demo uses a `Timer.periodic(500ms)` for three things:

1. **Init check** — polls `isInitialized()` until true. Unnecessary: `initialize()` is synchronous
   under the hood (`_initialized = true` is set before the `Future` resolves).
2. **Scene-ready retry** — retries `loadGltfFromPath` and polls `isRendererReady` because
   `prism_load_gltf_from_path` silently returns if the Metal surface hasn't been attached yet. The
   surface is attached asynchronously on the first `MTKView.draw(in:)` call, which fires after
   Flutter creates the platform view — always after Dart's `initState()` runs.
3. **FPS display** — polls `getState()` every 500 ms to refresh the FPS counter.

Root fix for #2: queue the pending GLB path in native code and auto-load it as soon as
`prism_attach_metal_layer` succeeds. This eliminates the retry loop entirely.

For #1 and #3, replace the wall-clock timer with a Flutter `Ticker` (vsync-aligned, ~60 fps):
- Check `isRendererReady` each frame until true
- Sample `fps` (new synchronous getter, no async overhead) each frame, trigger `setState` only when
  value changes by ≥1 fps

The synchronous `fps` getter is added to ffi/dispatch/channel layers. The dispatch layer
short-circuits to the ffi impl's getter (the channel/Android stub returns 0.0).

## Plan

1. **MacosBridge.kt**: add `pendingGlbPaths: AtomicRef<Map<Long, String>>`. In
   `prismLoadGltfFromPath`, queue path if surface absent; in `prismAttachMetalLayer`, consume queue
   after surface is stored; in `prismDetachSurface`, clear queue entry.

2. **IosBridge.kt**: identical changes — `iosSurfaces`/`iosScenes` → same pattern with `IosBridge`
   log tag.

3. **prism_engine_ffi.dart**: add `double get fps => _bindings.prism_get_fps(_engineHandle);`

4. **prism_engine_dispatch.dart**: add `double get fps` that forwards to ffi impl or returns 0.0.

5. **prism_engine_channel.dart**: add `double get fps => 0.0;` stub.

6. **main.dart**: replace `Timer.periodic` + `_isInitialized` with `SingleTickerProviderStateMixin`
   + `Ticker`. Extract `_setup()` for async init+load. `_onFrame` checks readiness and FPS diff.

7. Format: `./gradlew ktfmtFormat`

8. Check: `./gradlew ktfmtCheck detektJvmMain jvmTest`

9. Dart analysis: `cd prism-flutter-demo/example && flutter analyze`

10. Commit + push + update devlog
