import Flutter
import UIKit
import QuartzCore

public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = PrismIOSPlatformViewFactory(messenger: registrar.messenger())
        registrar.register(factory, withId: "engine.prism.flutter/render_view")
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

class PrismIOSPlatformView: NSObject, FlutterPlatformView {
    private let _view: UIView
    private let engineHandle: Int64
    private var displayLink: CADisplayLink?

    init(frame: CGRect, arguments args: Any?) {
        let params = args as? [String: Any]
        self.engineHandle = params?["engineHandle"] as? Int64 ?? 0
        self._view = UIView(frame: frame)
        self._view.backgroundColor = .black

        super.init()

        if engineHandle != 0 {
            setupMetalLayer()
        }
    }

    func view() -> UIView {
        return _view
    }

    private func setupMetalLayer() {
        let metalLayer = CAMetalLayer()
        metalLayer.frame = _view.bounds
        metalLayer.contentsScale = UIScreen.main.scale
        _view.layer.addSublayer(metalLayer)

        let rawPtr = Unmanaged.passUnretained(metalLayer).toOpaque()
        let width = Int32(metalLayer.bounds.width * metalLayer.contentsScale)
        let height = Int32(metalLayer.bounds.height * metalLayer.contentsScale)

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
    }
}

// C API bindings (provided by PrismNative.xcframework)
@_silgen_name("prism_attach_metal_layer")
func prism_attach_metal_layer(_ handle: Int64, _ layer: UnsafeMutableRawPointer, _ width: Int32, _ height: Int32)

@_silgen_name("prism_render_frame")
func prism_render_frame(_ handle: Int64)

@_silgen_name("prism_detach_surface")
func prism_detach_surface(_ handle: Int64)
