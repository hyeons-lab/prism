import FlutterMacOS

/// Flutter plugin entry point for macOS.
///
/// Registers the Metal platform view factory and stub method channel so that Dart
/// can embed a `PrismRenderView` and optionally query engine state.
///
/// No pre-configuration is required. The engine handle is passed from Dart via
/// `AppKitView(creationParams: {'engineHandle': handle})`, matching the iOS pattern.
public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        registrar.register(
            PrismMacOSPlatformViewFactory(),
            withId: "engine.prism.flutter/render_view")
        // Asset path resolution is handled entirely in Dart via rootBundle + temp files.
        // No method channel registration needed on macOS.
    }
}
