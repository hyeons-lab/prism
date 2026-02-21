import Flutter
import UIKit
import PrismDemo

/// Flutter plugin entry point for iOS. Registers the method channel and platform view factory.
///
/// Method channel contract (must match FlutterMethodHandler.kt on Android):
///   togglePause() → true
///   setMetallic({metallic: Float}) → true
///   setRoughness({roughness: Float}) → true
///   setEnvIntensity({intensity: Float}) → true
///   isInitialized() → Bool
///   getState() → {metallic, roughness, envIntensity, isPaused, fps}
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
        case "togglePause":
            store.dispatch(event: DemoIntent.TogglePause.shared)
            result(true)

        case "setMetallic":
            let metallic = (args["metallic"] as? NSNumber)?.floatValue ?? 0.0
            store.dispatch(event: DemoIntent.SetMetallic(metallic: metallic))
            result(true)

        case "setRoughness":
            let roughness = (args["roughness"] as? NSNumber)?.floatValue ?? 0.5
            store.dispatch(event: DemoIntent.SetRoughness(roughness: roughness))
            result(true)

        case "setEnvIntensity":
            let intensity = (args["intensity"] as? NSNumber)?.floatValue ?? 1.0
            store.dispatch(event: DemoIntent.SetEnvIntensity(envIntensity: intensity))
            result(true)

        case "isInitialized":
            result(activePlatformView?.isReady ?? false)

        case "getState":
            let state = store.state.value
            result([
                "metallic": state.metallic,
                "roughness": state.roughness,
                "envIntensity": state.envIntensity,
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
