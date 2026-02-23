import Flutter
import UIKit
import QuartzCore

public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = PrismIOSPlatformViewFactory(messenger: registrar.messenger())
        registrar.register(factory, withId: "engine.prism.flutter/render_view")

        // Register a method channel for engine control. Handlers below are stubs
        // that will be wired to the native C API once the engine model stabilises.
        let channel = FlutterMethodChannel(
            name: "engine.prism.flutter/engine",
            binaryMessenger: registrar.messenger()
        )
        channel.setMethodCallHandler { call, result in
            switch call.method {
            case "togglePause":
                // TODO: wire to prism_native C API when a pause/resume function is exposed.
                result(nil)
            case "isInitialized":
                // Return true if the platform view has been created (engine handle non-zero).
                result(false)
            case "getState":
                result(["initialized": false])
            case "shutdown":
                // TODO: call prism_detach_surface for the associated engine handle.
                result(nil)
            default:
                result(FlutterMethodNotImplemented)
            }
        }
    }
}

class PrismIOSPlatformViewFactory: NSObject, FlutterPlatformViewFactory {
    private let messenger: FlutterBinaryMessenger

    init(messenger: FlutterBinaryMessenger) {
        self.messenger = messenger
        super.init()
    }

    func create(withFrame frame: CGRect, viewIdentifier viewId: Int64, arguments args: Any?) -> FlutterPlatformView {
        return PrismIOSPlatformView(frame: frame, arguments: args)
    }

    public func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// A UIView subclass that forwards layout changes to the Kotlin/Native prism_resize C API.
private class PrismMetalView: UIView {
    var engineHandle: Int64 = 0
    var metalLayer: CAMetalLayer?

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let layer = metalLayer, engineHandle != 0 else { return }
        layer.frame = bounds
        let width = Int32(bounds.width * layer.contentsScale)
        let height = Int32(bounds.height * layer.contentsScale)
        prism_resize(engineHandle, width, height)
    }
}

class PrismIOSPlatformView: NSObject, FlutterPlatformView {
    private let _view: PrismMetalView
    private let engineHandle: Int64
    private var displayLink: CADisplayLink?

    init(frame: CGRect, arguments args: Any?) {
        let params = args as? [String: Any]
        self.engineHandle = params?["engineHandle"] as? Int64 ?? 0
        self._view = PrismMetalView(frame: frame)
        self._view.backgroundColor = .black
        self._view.engineHandle = self.engineHandle

        super.init()

        if engineHandle != 0 {
            setupMetalLayer()
        }
    }

    func view() -> UIView {
        return _view
    }

    private func setupMetalLayer() {
        let layer = CAMetalLayer()
        layer.frame = _view.bounds
        layer.contentsScale = UIScreen.main.scale
        _view.layer.addSublayer(layer)
        // Retain the layer via the strong property on the view so ARC keeps it alive while the
        // Kotlin/Native side holds a raw pointer to it.
        _view.metalLayer = layer

        let rawPtr = Unmanaged.passUnretained(layer).toOpaque()
        let width = Int32(layer.bounds.width * layer.contentsScale)
        let height = Int32(layer.bounds.height * layer.contentsScale)

        prism_attach_metal_layer(engineHandle, rawPtr, width, height)

        displayLink = CADisplayLink(target: self, selector: #selector(renderFrame))
        displayLink?.add(to: .main, forMode: .common)
    }

    @objc private func renderFrame() {
        prism_render_frame(engineHandle)
    }

    deinit {
        displayLink?.invalidate()
        prism_detach_surface(engineHandle)
        _view.metalLayer = nil
    }
}

// C API bindings (provided by PrismNative.xcframework)
@_silgen_name("prism_attach_metal_layer")
func prism_attach_metal_layer(_ handle: Int64, _ layer: UnsafeMutableRawPointer, _ width: Int32, _ height: Int32)

@_silgen_name("prism_render_frame")
func prism_render_frame(_ handle: Int64)

@_silgen_name("prism_detach_surface")
func prism_detach_surface(_ handle: Int64)

@_silgen_name("prism_resize")
func prism_resize(_ handle: Int64, _ width: Int32, _ height: Int32)
