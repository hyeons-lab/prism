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
                    // Use Flutter's registrar to resolve the canonical lookup key
                    // (e.g. "flutter_assets/assets/DamagedHelmet.glb"), then search
                    // App.framework (where flutter_assets lives in all build modes).
                    let key = registrar.lookupKey(forAsset: assetKey)
                    let appFrameworkPath = Bundle.main.bundlePath
                        + "/Contents/Frameworks/App.framework"
                    if let appBundle = Bundle(path: appFrameworkPath),
                       let resourcePath = appBundle.resourcePath {
                        let path = resourcePath + "/" + key
                        if FileManager.default.fileExists(atPath: path) {
                            result(path)
                            return
                        }
                    }
                    // Fallback: check the main bundle resource path directly.
                    if let resourcePath = Bundle.main.resourcePath {
                        let path = resourcePath + "/" + key
                        result(FileManager.default.fileExists(atPath: path) ? path : nil)
                    } else {
                        result(nil)
                    }
                } else {
                    result(nil)
                }
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
