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

        let channel = FlutterMethodChannel(
            name: "engine.prism.flutter/engine",
            binaryMessenger: registrar.messenger)
        channel.setMethodCallHandler { call, result in
            switch call.method {
            case "togglePause":
                result(nil)
            case "isInitialized":
                result(false)
            case "getState":
                result(["initialized": false])
            case "shutdown":
                result(nil)
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }
}
