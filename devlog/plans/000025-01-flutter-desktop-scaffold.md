# Plan: Flutter Desktop scaffolding for Linux + Windows

## Thinking

The Flutter plugin (`prism-flutter/flutter_plugin/`) currently advertises
support for `android`, `ios`, and `macos` only. The `prism-native`
module already builds `libprism.so` (linuxX64) and `prism.dll`
(mingwX64) in the docker CI job, and the Dart side
(`prism_engine_ffi.dart`, `prism_sdk_ffi.dart`) already loads them via
`DynamicLibrary.open(...)` for both platforms — but the plugin has no
platform code that registers with the Flutter engine on Linux/Windows
and no CMake glue that bundles the native `.so`/`.dll` into the host
app.

The macOS plugin works because:

1. The platform directory `flutter_plugin/macos/` exists with a
   `Package.swift`, a `PrismFlutterPlugin` class, and an
   `MTKView`-backed `PrismMacOSPlatformView`.
2. `Frameworks/PrismNative.xcframework` is bundled — the dylib is
   pre-loaded by dyld so Dart's `DynamicLibrary.process()` can resolve
   `prism_*` symbols.
3. `pubspec.yaml`'s `flutter.plugin.platforms.macos.pluginClass`
   registers the platform view factory.
4. Dart's `prism_render_view_mobile.dart` returns an `AppKitView` that
   embeds the platform view and passes the engine handle.

For Linux + Windows, doing the **full** equivalent means wiring a
real wgpu surface to a GTK widget (Linux) or HWND (Windows) and
turning that into a Flutter platform view. Both have constrained
platform-view APIs vs UIKit/AppKit — Linux Flutter uses GTK with an
external-texture-first model; Windows desktop platform views work but
require careful HWND parenting.

Doing all of that here would mean a multi-PR effort with substantial
C++/CMake code on each platform. To keep this PR reviewable, I'm
splitting the work into phases:

**Phase 1 (this PR):** scaffold the platform directories so the plugin
*compiles* and *loads* on both desktop targets, and so the native
library is bundled. The Dart `PrismRenderView` will continue showing
its existing fallback ("Prism: render view not yet available on this
platform") on these platforms — but `PrismEngine` (FFI) will be
fully usable from Dart since the native lib is now bundled. This is
what someone embedding `prism_flutter` in headless contexts would
actually use.

**Phase 2 (follow-up):** implement the actual `PrismLinuxPlatformView`
(GTK widget hosting a wgpu surface via `RawWindowHandle`) and the
analogous `PrismWindowsPlatformView`. Add CI jobs to verify the
example app builds.

### What Phase 1 includes

- `prism-flutter/flutter_plugin/linux/`
  - `CMakeLists.txt` — Flutter Linux plugin convention; declares
    `prism_flutter_plugin` SHARED library and exports
    `prism_flutter_bundled_libraries` (a parent-scope variable read by
    Flutter's Linux build tool) pointing at `native/libprism.so`.
    No CMake `IMPORTED` target — the `.so` is loaded at runtime by
    Dart's `DynamicLibrary.open(...)`, not link-time-linked to the
    plugin.
  - `include/prism_flutter/prism_flutter_plugin.h` — public header for
    `prism_flutter_plugin_register_with_registrar()`.
  - `prism_flutter_plugin.cc` — minimal `FlPlugin` class that just
    registers itself; no method channel, no platform view yet.
  - `.gitignore` for build outputs.
- `prism-flutter/flutter_plugin/windows/`
  - `CMakeLists.txt` — Flutter Windows plugin convention; declares
    `prism_flutter_plugin` SHARED library and exports
    `prism_flutter_bundled_libraries` pointing at `native/prism.dll`.
  - `include/prism_flutter/prism_flutter_plugin_c_api.h` — C API
    entry point for Flutter's plugin registrar.
  - `prism_flutter_plugin_c_api.cpp` — registers the Windows plugin.
  - `prism_flutter_plugin.h` + `prism_flutter_plugin.cpp` — minimal
    `flutter::Plugin` subclass.
  - `.gitignore` for build outputs.
- `prism-flutter/flutter_plugin/pubspec.yaml` — add `linux:` and
  `windows:` entries under `flutter.plugin.platforms`.
- `prism-flutter/build.gradle.kts` — add `bundleNativeLinux` and
  `bundleNativeWindows` Gradle tasks following the
  `bundleNativeAndroid*` pattern: each depends on the corresponding
  `:prism-native:linkReleaseShared*` task and copies the produced
  `.so`/`.dll` into the plugin's platform directory.

### What Phase 1 deliberately omits

- A working `PrismRenderView` on Linux/Windows. The existing Dart
  fallback for unknown platforms still applies. Users running the
  Flutter demo on Linux/Windows will see the placeholder text — the
  3D scene won't render. This is intentional and called out in the PR
  description.
- CI for `flutter build linux`/`flutter build windows`. Adding those
  before there's a render view to verify is dubious value; defer until
  Phase 2 lands so CI exercises actual functionality.
- `prism-flutter-demo/example/linux/` and `windows/` directories.
  `flutter create --platforms=linux,windows .` would generate ~30
  files of host-app boilerplate; doing it before there's anything to
  show is noise. Defer to Phase 2.

### Risks / open questions

- The Flutter Linux plugin convention for shipping a third-party `.so`
  is to add it to `prism_flutter_bundled_libraries` (a CMake variable
  the Flutter tooling reads) so it gets copied into
  `<app>/lib/`. Need to verify this works with our prism-native output
  path (Gradle copies it into `flutter_plugin/linux/native/`).
- On Windows, the same convention exists with
  `prism_flutter_bundled_libraries` listing the `.dll`. The DLL gets
  placed next to the executable.
- Symbol resolution order: on Linux/Windows, Dart's
  `DynamicLibrary.open('libprism.so' / 'prism.dll')` requires the lib
  to be on the runtime search path. Flutter's bundled-libraries
  mechanism handles this on both platforms by placing the lib in the
  app's lib/exec dir. Verifying empirically is Phase 2.

## Plan

1. Devlog + plan files.
2. Create Linux plugin scaffold:
   - `linux/CMakeLists.txt`, `include/...h`, `prism_flutter_plugin.cc`,
     `.gitignore`.
3. Create Windows plugin scaffold:
   - `windows/CMakeLists.txt`, `include/...h`,
     `prism_flutter_plugin_c_api.h/.cpp`,
     `prism_flutter_plugin.h/.cpp`, `.gitignore`.
4. Update `pubspec.yaml`: add `linux:` and `windows:` plugin
   platforms. Set `pluginClass: PrismFlutterPlugin` on each. Keep
   `ffiPlugin: true`.
5. Update `prism-flutter/build.gradle.kts`: add `bundleNativeLinux`
   (depends on `:prism-native:linkReleaseSharedLinuxX64`, copies
   `libprism.so` into `flutter_plugin/linux/native/`) and
   `bundleNativeWindows` (depends on
   `:prism-native:linkReleaseSharedMingwX64`, copies `prism.dll` into
   `flutter_plugin/windows/native/`).
6. Open draft PR with explicit scope statement: Phase 1 scaffolding
   only. Phase 2 (platform views + CI) is a follow-up.
