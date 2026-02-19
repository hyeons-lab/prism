import Flutter
import UIKit
import PrismDemo

/// Flutter plugin entry point for iOS. Registers the method channel and platform view factory.
///
/// Method channel contract (must match FlutterMethodHandler.kt on Android):
///   setRotationSpeed({speed: Float}) → true
///   togglePause() → true
///   setCubeColor({r: Float, g: Float, b: Float}) → true
///   isInitialized() → true
///   getState() → {rotationSpeed, isPaused, fps}
///   shutdown() → true
public class PrismFlutterPlugin: NSObject, FlutterPlugin {

    private let store = DemoStore()
    private weak var activePlatformView: PrismPlatformView?

    public static func register(with registrar: FlutterPluginRegistrar) {
        let plugin = PrismFlutterPlugin()

        let channel = FlutterMethodChannel(
            name: "engine.prism.flutter/engine",
            binaryMessenger: registrar.messenger()
        )
        registrar.addMethodCallDelegate(plugin, channel: channel)

        let factory = PrismPlatformViewFactory(store: plugin.store, plugin: plugin)
        registrar.register(factory, withId: "engine.prism.flutter/render_view")
    }

    func trackPlatformView(_ view: PrismPlatformView) {
        activePlatformView = view
    }

    public func handle(
        _ call: FlutterMethodCall,
        result: @escaping FlutterResult
    ) {
        let args = call.arguments as? [String: Any] ?? [:]

        switch call.method {
        case "setRotationSpeed":
            let speed = (args["speed"] as? NSNumber)?.floatValue ?? 45.0
            store.dispatch(event: DemoIntent.SetRotationSpeed(speed: speed))
            result(true)

        case "togglePause":
            store.dispatch(event: DemoIntent.TogglePause.shared)
            result(true)

        case "setCubeColor":
            let r = (args["r"] as? NSNumber)?.floatValue ?? 0.3
            let g = (args["g"] as? NSNumber)?.floatValue ?? 0.5
            let b = (args["b"] as? NSNumber)?.floatValue ?? 0.9
            store.dispatch(event: DemoIntent.SetCubeColor(color: Color(r: r, g: g, b: b)))
            result(true)

        case "isInitialized":
            result(activePlatformView?.isReady ?? false)

        case "getState":
            let state = store.state.value
            result([
                "rotationSpeed": state.rotationSpeed,
                "isPaused": state.isPaused,
                "fps": state.fps,
            ])

        case "shutdown":
            activePlatformView?.shutdown()
            result(true)

        default:
            result(FlutterMethodNotImplemented)
        }
    }
}
