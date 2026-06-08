# 000025 — feat/flutter-desktop-scaffold

**Agent:** Claude (claude-opus-4-7) @ prism branch feat/flutter-desktop-scaffold

## Intent

Begin the Flutter Desktop milestone (per AGENTS.md "What's next") for
Linux + Windows. This branch lands **Phase 1**: plugin platform
scaffolding so `prism_flutter` compiles and loads `libprism.so` /
`prism.dll` on those targets. **Phase 2** (real wgpu-backed platform
views + example app + CI) is a follow-up.

The Dart `PrismEngine` (FFI) is already cross-platform — see
`prism_engine_ffi.dart`'s `_loadBindings()` which already handles
Linux/Windows via `DynamicLibrary.open(...)`. The missing pieces on the
desktop targets are purely on the platform-plugin side: a CMake build
file + a no-op plugin class so Flutter's tooling accepts the plugin
and bundles the native lib. This unblocks anyone using `prism_flutter`
in headless contexts on Linux/Windows even before the platform-view
work lands.

## What Changed

- 2026-05-09T09:49-0700 `devlog/plans/000025-01-flutter-desktop-scaffold.md`
  — design doc; phased approach, what Phase 1 includes vs omits.
- 2026-05-09T09:55-0700 `prism-flutter/flutter_plugin/linux/` — Flutter
  Linux plugin scaffold: `CMakeLists.txt` (declares
  `prism_flutter_plugin` SHARED, exports
  `prism_flutter_bundled_libraries` pointing at `native/libprism.so`),
  `include/prism_flutter/prism_flutter_plugin.h`,
  `prism_flutter_plugin.cc` (no-op `FlPlugin` registration), and
  `.gitignore` for the Gradle-dropped `.so`.
- 2026-05-09T09:58-0700 `prism-flutter/flutter_plugin/windows/` —
  Flutter Windows plugin scaffold: `CMakeLists.txt`,
  `include/prism_flutter/prism_flutter_plugin_c_api.h`,
  `prism_flutter_plugin_c_api.cpp` (C-API entry into the C++ plugin),
  `prism_flutter_plugin.h` + `prism_flutter_plugin.cpp` (minimal
  `flutter::Plugin` subclass), and `.gitignore` for the
  Gradle-dropped `.dll`.
- 2026-05-09T10:00-0700 `prism-flutter/flutter_plugin/pubspec.yaml` —
  added `linux:` and `windows:` entries under
  `flutter.plugin.platforms`. Linux uses `pluginClass:
  PrismFlutterPlugin`; Windows uses `pluginClass:
  PrismFlutterPluginCApi` (Flutter Windows convention — registrar is
  the C-API symbol, not the C++ class).
- 2026-05-09T10:02-0700 `prism-flutter/build.gradle.kts` — registered
  `bundleNativeLinux` (depends on
  `:prism-native:linkReleaseSharedLinuxX64`, copies into
  `flutter_plugin/linux/native/`) and `bundleNativeWindows` (depends
  on `:prism-native:linkReleaseSharedMingwX64`, copies into
  `flutter_plugin/windows/native/`).

## Decisions

- 2026-05-09T09:49-0700 Phased rollout (scaffold first, platform views
  later) over a single mega-PR. Reasoning: a real platform view on
  each desktop target is a non-trivial piece of GPU-surface
  integration (Linux: GTK widget hosting via FlView/external texture;
  Windows: HWND-parenting via FlutterPlatformView). Bundling that with
  scaffolding into one PR makes review intractable. The scaffold by
  itself is small but standalone-useful: it lets headless FFI use
  cases work today.
- 2026-05-09T09:49-0700 Place the bundled native lib at
  `flutter_plugin/<linux|windows>/native/` and have CMake export the
  absolute path through `prism_flutter_bundled_libraries` (the
  variable Flutter's Linux/Windows build tooling reads to copy
  runtime files into the host app). No CMake `IMPORTED` target is
  involved — the `.so`/`.dll` is *runtime-loaded* by Dart's
  `DynamicLibrary.open(...)`, not link-time-linked to the plugin
  shared library. This mirrors the Android approach
  (`flutter_plugin/android/src/main/jniLibs/<arch>/`) where Gradle
  drops the `.so` for AGP to bundle automatically.
- 2026-05-09T09:49-0700 Skip generating `prism-flutter-demo/example/`
  scaffolding for `linux/`/`windows/` in this PR. `flutter create`
  produces ~30 files of host-app boilerplate that aren't useful
  without rendering; defer to Phase 2 when there's something to show.

## Issues

- 2026-05-09T10:30-0700 First commit (`0709dd6`) had a broken
  `target_link_libraries(${PLUGIN_NAME} PRIVATE PkgConfig::GTK)` line
  in `linux/CMakeLists.txt` — the `PkgConfig::GTK` target was never
  defined (no `find_package(PkgConfig)` / `pkg_check_modules(GTK ...)`
  call), so a real Linux build would have failed at CMake-configure
  time. Removed in follow-up commit. The `flutter` link target
  already pulls in GTK headers transitively (matches the minimal
  pattern used by `url_launcher_linux` and other Flutter Linux
  plugins). Also dropped the unused `<gtk/gtk.h>` include from
  `prism_flutter_plugin.cc`.
- 2026-05-09T10:32-0700 Devlog and plan referred to a CMake
  `IMPORTED` target for the bundled native library (Copilot review
  flagged this). No `IMPORTED` target is created — the `.so`/`.dll`
  is dropped via `prism_flutter_bundled_libraries` and runtime-loaded
  by Dart's `DynamicLibrary.open(...)`. Plan and devlog updated to
  match. Convention says plans are append-only, but in this case the
  text described the implementation incorrectly (not a forward
  intent), so the simplest fix was to correct the wording in place;
  noting the edit here for traceability. Plan's "static lib" wording
  for Windows was likewise corrected to "SHARED" to match the actual
  `add_library(... SHARED ...)`.

## Commits

- 0709dd6 — feat: scaffold Flutter Linux + Windows plugin platforms
- HEAD — fix: drop broken GTK link, correct doc drift
