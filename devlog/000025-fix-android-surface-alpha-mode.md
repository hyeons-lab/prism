# 000025 — fix/android-surface-alpha-mode

**Agent:** Claude (claude-opus-4-8) @ prism branch fix/android-surface-alpha-mode

## Intent

`WgpuRenderer.initialize` hardcodes `CompositeAlphaMode.Opaque` when configuring the surface. On
Android/Vulkan many surfaces advertise only `[Inherit, Auto]` and never `Opaque`. wgpu treats
configuring a surface with an unsupported alpha mode as a **fatal (aborting) error**, so the app
crashes natively in `wgpuSurfaceConfigure`. Pick an alpha mode the surface actually supports.

## Research & Discoveries

- Reproduced on a Pixel 10 Pro Fold (Android 16, arm64, Vulkan via wgpu4k) running both the stock
  `prism-android-demo` and a downstream app. Native crash backtrace:
  `wgpuSurfaceConfigure` → `wgpu_native::handle_error_fatal` → `std::process::abort`, called from
  `WgpuRenderer.initialize` → `Engine.initialize` → `createGltfDemoScene`.
- wgpu4k logs the cause at surface creation:
  `io.ygdrasil.webgpu.Surface_native - supportedAlphaMode: [Inherit, Auto]`
  (and `supportedTextureFormats: [RGBA8UnormSrgb, RGBA8Unorm, RGBA16Float, RGB10A2Unorm]`).
- `io.ygdrasil.webgpu.Surface` exposes `supportedAlphaMode: Set<CompositeAlphaMode>` — exactly what's
  needed to choose a valid mode at runtime.

## What Changed

- 2026-06-07T13:05-07:00 prism-renderer/.../WgpuRenderer.kt — replace the hardcoded
  `CompositeAlphaMode.Opaque` with `chooseAlphaMode(wgpuContext.surface.supportedAlphaMode)`. Added
  `internal fun chooseAlphaMode(...)`: prefers Opaque (desktop/Metal), then Inherit, then Auto, then
  any supported mode. Keeps existing desktop behavior; fixes Android.
- 2026-06-07T13:05-07:00 (no unit test added) — verified on-device instead; see Issues.

## Decisions

- 2026-06-07T13:05-07:00 Prefer Opaque-then-Inherit-then-Auto rather than always Auto — preserves the
  prior, intentional Opaque behavior wherever the platform supports it (desktop/Metal) and only
  diverges where Opaque is unavailable. Reasoning: minimize behavioral change on already-working
  platforms while unblocking Android.
- 2026-06-07T13:05-07:00 Helper is a pure `internal` top-level function so it's unit-testable in
  commonTest without a GPU/device.

## Issues

- Root-caused by reproducing on the stock demo first, ruling out downstream app misconfiguration.
- Tried adding `AlphaModeTest` in commonTest but it failed with `UnsupportedClassVersionError`:
  `io.ygdrasil.webgpu.CompositeAlphaMode` is compiled to class version 69 (Java 25), while the
  `jvmTest` runner resolves to an older JDK. No existing prism commonTest references `io.ygdrasil`
  runtime types — that's the de-facto convention. Removed the test; the fix is verified on-device
  (Pixel 10 Pro Fold: pre-fix native crash → post-fix renders) and the logic is a 4-branch selector.

## Commits

- HEAD — fix(renderer): select a supported surface alpha mode (fixes Android Vulkan crash)
