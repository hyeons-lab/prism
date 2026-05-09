#include "prism_flutter_plugin.h"

#include <flutter/plugin_registrar_windows.h>

#include <memory>

// Phase 1 scaffolding: the plugin registers itself with the Flutter engine
// but does not yet expose a method channel or platform view. Dart talks to
// the engine directly via FFI (see prism_engine_ffi.dart). A real
// HWND-backed PrismRenderView is a Phase 2 follow-up.

namespace prism_flutter {

// static
void PrismFlutterPlugin::RegisterWithRegistrar(
    flutter::PluginRegistrarWindows* registrar) {
  auto plugin = std::make_unique<PrismFlutterPlugin>();
  registrar->AddPlugin(std::move(plugin));
}

PrismFlutterPlugin::PrismFlutterPlugin() = default;

PrismFlutterPlugin::~PrismFlutterPlugin() = default;

}  // namespace prism_flutter
