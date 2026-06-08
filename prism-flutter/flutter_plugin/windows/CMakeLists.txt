# Phase 1 scaffolding: minimal Flutter Windows plugin that registers with the
# engine and bundles `prism.dll` next to the host executable. Dart talks to
# prism-native directly via DynamicLibrary.open('prism.dll'). A real
# HWND-backed PrismRenderView is a Phase 2 follow-up.

cmake_minimum_required(VERSION 3.14)
set(PROJECT_NAME "prism_flutter")
project(${PROJECT_NAME} LANGUAGES CXX)

# This value is used when generating builds using this plugin, so it must
# not be changed.
set(PLUGIN_NAME "prism_flutter_plugin")

list(APPEND PLUGIN_SOURCES
  "prism_flutter_plugin.cpp"
  "prism_flutter_plugin.h"
  "prism_flutter_plugin_c_api.cpp"
  "include/prism_flutter/prism_flutter_plugin_c_api.h"
)

add_library(${PLUGIN_NAME} SHARED
  "include/prism_flutter/prism_flutter_plugin_c_api.h"
  ${PLUGIN_SOURCES}
)
apply_standard_settings(${PLUGIN_NAME})
set_target_properties(${PLUGIN_NAME} PROPERTIES
  CXX_VISIBILITY_PRESET hidden)
target_compile_definitions(${PLUGIN_NAME} PRIVATE FLUTTER_PLUGIN_IMPL)
target_include_directories(${PLUGIN_NAME} INTERFACE
  "${CMAKE_CURRENT_SOURCE_DIR}/include")
target_link_libraries(${PLUGIN_NAME} PRIVATE flutter flutter_wrapper_plugin)

# List of absolute paths to libraries that should be bundled with the plugin.
# `prism.dll` is dropped into `native/` by the Gradle task
# `:prism-flutter:bundleNativeWindows`. Flutter copies bundled libraries
# next to the host executable on build.
set(PRISM_NATIVE_LIB "${CMAKE_CURRENT_SOURCE_DIR}/native/prism.dll")
if(EXISTS "${PRISM_NATIVE_LIB}")
  set(prism_flutter_bundled_libraries
    "${PRISM_NATIVE_LIB}"
    PARENT_SCOPE
  )
else()
  message(WARNING
    "prism.dll not found at ${PRISM_NATIVE_LIB}. Run "
    "`./gradlew :prism-flutter:bundleNativeWindows` before building the host app.")
  set(prism_flutter_bundled_libraries "" PARENT_SCOPE)
endif()
