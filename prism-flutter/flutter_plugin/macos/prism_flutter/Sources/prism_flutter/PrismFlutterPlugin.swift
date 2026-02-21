import FlutterMacOS

/// Flutter plugin entry point for macOS.
///
/// Engine lifecycle and state queries are handled by Dart FFI through the
/// PrismNative C API. This plugin registers the platform view factory so that
/// PrismRenderView (AppKitView) gets a real Metal render surface.
public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = PrismMacOSPlatformViewFactory()
        registrar.register(factory, withId: "engine.prism.flutter/render_view")
    }
}
