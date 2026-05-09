#ifndef FLUTTER_PLUGIN_PRISM_FLUTTER_PLUGIN_H_
#define FLUTTER_PLUGIN_PRISM_FLUTTER_PLUGIN_H_

#include <flutter/plugin_registrar_windows.h>

#include <memory>

namespace prism_flutter {

class PrismFlutterPlugin : public flutter::Plugin {
 public:
  static void RegisterWithRegistrar(flutter::PluginRegistrarWindows* registrar);

  PrismFlutterPlugin();

  virtual ~PrismFlutterPlugin();

  // Disallow copy and assign.
  PrismFlutterPlugin(const PrismFlutterPlugin&) = delete;
  PrismFlutterPlugin& operator=(const PrismFlutterPlugin&) = delete;
};

}  // namespace prism_flutter

#endif  // FLUTTER_PLUGIN_PRISM_FLUTTER_PLUGIN_H_
