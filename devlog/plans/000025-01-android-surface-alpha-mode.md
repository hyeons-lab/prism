# 000025-01 — Android surface alpha mode

## Thinking

The Android/Vulkan surface on a Pixel 10 Pro Fold reports `supportedAlphaMode: [Inherit, Auto]`.
`WgpuRenderer.initialize` configures the surface with `CompositeAlphaMode.Opaque`, which is not in
that set. wgpu's `wgpuSurfaceConfigure` aborts the process on an unsupported alpha mode (it's a
fatal error, not a recoverable one), so there's no try/catch escape — we must pass a supported value.

`io.ygdrasil.webgpu.Surface` already exposes `supportedAlphaMode: Set<CompositeAlphaMode>`, so the
renderer can query and choose at runtime. We want to keep the previous Opaque behavior on platforms
that support it (desktop/Metal) to avoid any compositing/perf change there, and only fall back where
necessary.

## Plan

1. In `WgpuRenderer.initialize`, replace `val alphaMode = CompositeAlphaMode.Opaque` with
   `chooseAlphaMode(wgpuContext.surface.supportedAlphaMode)`.
2. Add `internal fun chooseAlphaMode(supported): CompositeAlphaMode` (top-level, same file):
   Opaque → Inherit → Auto → first supported → Opaque (defensive default).
3. Add `AlphaModeTest` in `prism-renderer/commonTest` covering the preference order and the observed
   `[Inherit, Auto]` Android case.
4. `./gradlew ktfmtFormat` then `./gradlew ktfmtCheck detektJvmMain jvmTest`.
5. Commit, push, open draft PR. Verify on-device by rebuilding a downstream Android app against this
   branch.
