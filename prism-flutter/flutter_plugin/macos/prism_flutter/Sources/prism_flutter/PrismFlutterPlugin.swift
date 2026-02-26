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
            case "resolveFlutterAssetPath":
                if let assetKey = call.arguments as? String {
                    // On macOS, Flutter assets live inside App.framework's Resources directory.
                    let appFrameworkPath = Bundle.main.bundlePath
                        + "/Contents/Frameworks/App.framework"
                    if let appBundle = Bundle(path: appFrameworkPath),
                       let resourcePath = appBundle.resourcePath {
                        result(resourcePath + "/flutter_assets/" + assetKey)
                    } else {
                        result(nil)
                    }
                } else {
                    result(nil)
                }
            // The remaining methods are handled via Dart FFI on macOS (not the channel).
            // Return FlutterMethodNotImplemented so callers get a clear signal rather
            // than silently wrong values (e.g. isInitialized: false).
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }
}
