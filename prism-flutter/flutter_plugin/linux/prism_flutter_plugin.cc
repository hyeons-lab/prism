#include "include/prism_flutter/prism_flutter_plugin.h"

#include <flutter_linux/flutter_linux.h>

// Phase 1 scaffolding: the plugin registers itself with the Flutter engine
// but does not yet expose a method channel or platform view. Dart talks to
// the engine directly via FFI (see prism_engine_ffi.dart). A real GTK-backed
// PrismRenderView is a Phase 2 follow-up.

#define PRISM_FLUTTER_PLUGIN(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), prism_flutter_plugin_get_type(), \
                              PrismFlutterPlugin))

struct _PrismFlutterPlugin {
  GObject parent_instance;
};

G_DEFINE_TYPE(PrismFlutterPlugin, prism_flutter_plugin, g_object_get_type())

static void prism_flutter_plugin_dispose(GObject* object) {
  G_OBJECT_CLASS(prism_flutter_plugin_parent_class)->dispose(object);
}

static void prism_flutter_plugin_class_init(PrismFlutterPluginClass* klass) {
  G_OBJECT_CLASS(klass)->dispose = prism_flutter_plugin_dispose;
}

static void prism_flutter_plugin_init(PrismFlutterPlugin* self) {}

void prism_flutter_plugin_register_with_registrar(FlPluginRegistrar* registrar) {
  PrismFlutterPlugin* plugin = PRISM_FLUTTER_PLUGIN(
      g_object_new(prism_flutter_plugin_get_type(), nullptr));

  // No method channels or platform views registered in Phase 1.
  // Suppress unused-parameter warning on `registrar` while keeping the
  // signature stable for Phase 2.
  (void)registrar;

  g_object_unref(plugin);
}
