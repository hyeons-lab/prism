# The Flutter tooling expects this CMake project to define `<plugin>_plugin`
# (the plugin shared library) and `<plugin>_bundled_libraries` (extra runtime
# files copied into the host app's `lib/` directory).
#
# Phase 1 scaffolding: the plugin shared library is essentially a no-op — it
# only registers with the Flutter engine. The Dart side uses
# DynamicLibrary.open('libprism.so') directly, so all we need to do here is
# arrange for `libprism.so` to land next to the binary at runtime via the
# bundled-libraries mechanism. A real platform view (GTK widget hosting a
# wgpu surface) is a Phase 2 follow-up.

cmake_minimum_required(VERSION 3.10)
set(PROJECT_NAME "prism_flutter")
project(${PROJECT_NAME} LANGUAGES CXX)

# This value is used when generating builds using this plugin, so it must
# not be changed.
set(PLUGIN_NAME "prism_flutter_plugin")

list(APPEND PLUGIN_SOURCES
  "prism_flutter_plugin.cc"
)

add_library(${PLUGIN_NAME} SHARED
  ${PLUGIN_SOURCES}
)
apply_standard_settings(${PLUGIN_NAME})
set_target_properties(${PLUGIN_NAME} PROPERTIES
  CXX_VISIBILITY_PRESET hidden)
target_compile_definitions(${PLUGIN_NAME} PRIVATE FLUTTER_PLUGIN_IMPL)
target_include_directories(${PLUGIN_NAME} INTERFACE
  "${CMAKE_CURRENT_SOURCE_DIR}/include")
target_link_libraries(${PLUGIN_NAME} PRIVATE flutter)

# List of absolute paths to libraries that should be bundled with the plugin.
# `libprism.so` is dropped into `native/` by the Gradle task
# `:prism-flutter:bundleNativeLinux`. The Flutter Linux build tool reads
# `<plugin>_bundled_libraries` from the parent scope and copies each path
# into the host app's `lib/` directory at install time. No CMake IMPORTED
# target is involved — the .so is loaded at runtime by Dart's
# `DynamicLibrary.open('libprism.so')`, not linked to the plugin at build
# time. If it's missing, the build still succeeds but the Dart runtime will
# get an ArgumentError and PrismEngine becomes a no-op.
set(PRISM_NATIVE_LIB "${CMAKE_CURRENT_SOURCE_DIR}/native/libprism.so")
if(EXISTS "${PRISM_NATIVE_LIB}")
  set(prism_flutter_bundled_libraries
    "${PRISM_NATIVE_LIB}"
    PARENT_SCOPE
  )
else()
  message(WARNING
    "libprism.so not found at ${PRISM_NATIVE_LIB}. Run "
    "`./gradlew :prism-flutter:bundleNativeLinux` before building the host app.")
  set(prism_flutter_bundled_libraries "" PARENT_SCOPE)
endif()
