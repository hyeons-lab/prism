import FlutterMacOS

/// Flutter plugin entry point for macOS.
///
/// Registers the Metal platform view and the engine method channel so that Dart can
/// query state (fps, isPaused) and control the render loop (togglePause).
///
/// **Configuration required:** call `configure(bridge:)` before the Flutter engine starts
/// (i.e. before `GeneratedPluginRegistrant` runs in `awakeFromNib`). The best place is
/// `AppDelegate.init()`.
public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    private static var bridge: PrismMetalBridgeProtocol?

    /// Supply the bridge before the Flutter engine starts.
    /// Demo apps call this with `DemoMacosBridge()` in `AppDelegate.init()`.
    public static func configure(bridge: PrismMetalBridgeProtocol) {
        Self.bridge = bridge
    }

    public static func register(with registrar: FlutterPluginRegistrar) {
        guard let bridge = bridge else {
            assertionFailure("Call PrismFlutterPlugin.configure(bridge:) before the Flutter engine starts")
            return
        }

        // Engine method channel — matches the Dart PrismEngine channel name.
        let channel = FlutterMethodChannel(
            name: "engine.prism.flutter/engine",
            binaryMessenger: registrar.messenger)
        channel.setMethodCallHandler { call, result in
            switch call.method {
            case "togglePause":
                bridge.togglePause()
                result(true)
            case "isInitialized":
                result(bridge.isInitialized)
            case "getState":
                result(bridge.getState() as NSDictionary)
            case "shutdown":
                bridge.detachSurface()
                result(true)
            default:
                result(FlutterMethodNotImplemented)
            }
        }

        // Platform view factory.
        registrar.register(
            PrismMacOSPlatformViewFactory(bridge: bridge),
            withId: "engine.prism.flutter/render_view")
    }
}
