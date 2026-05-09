#include "include/prism_flutter/prism_flutter_plugin_c_api.h"

#include <flutter/plugin_registrar_windows.h>

#include "prism_flutter_plugin.h"

void PrismFlutterPluginCApiRegisterWithRegistrar(
    FlutterDesktopPluginRegistrarRef registrar) {
  prism_flutter::PrismFlutterPlugin::RegisterWithRegistrar(
      flutter::PluginRegistrarManager::GetInstance()
          ->GetRegistrar<flutter::PluginRegistrarWindows>(registrar));
}
