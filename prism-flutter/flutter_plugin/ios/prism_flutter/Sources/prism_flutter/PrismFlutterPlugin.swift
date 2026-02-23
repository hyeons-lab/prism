import Flutter
import MetalKit
import UIKit

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

/// A UIView subclass that holds the embedded MTKView and forwards layout changes to
/// the Kotlin/Native prism_resize C API.
private class PrismMetalView: UIView {
    var engineHandle: Int64 = 0
    /// Strong reference to the embedded MTKView. MTKView is a subview of this container so ARC
    /// keeps it alive while the Kotlin/Native side holds a raw pointer to it.
    var mtkView: MTKView?

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let mtkView = mtkView, engineHandle != 0 else { return }
        mtkView.frame = bounds
        let scale = UIScreen.main.scale
        let width = Int32(bounds.width * scale)
        let height = Int32(bounds.height * scale)
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
            setupMtkView()
        }
    }

    func view() -> UIView {
        return _view
    }

    private func setupMtkView() {
        let mtkView = MTKView(frame: _view.bounds)
        mtkView.device = MTLCreateSystemDefaultDevice()
        _view.addSubview(mtkView)
        // Retain via the strong property on the container view so ARC keeps the MTKView alive
        // while the Kotlin/Native side holds a raw pointer to it.
        _view.mtkView = mtkView

        let rawPtr = Unmanaged.passUnretained(mtkView).toOpaque()
        let scale = UIScreen.main.scale
        let width = Int32(_view.bounds.width * scale)
        let height = Int32(_view.bounds.height * scale)

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
        _view.mtkView = nil
    }
}

// C API bindings (provided by PrismNative.xcframework)
@_silgen_name("prism_attach_metal_layer")
func prism_attach_metal_layer(_ handle: Int64, _ ptr: UnsafeMutableRawPointer, _ width: Int32, _ height: Int32)

@_silgen_name("prism_render_frame")
func prism_render_frame(_ handle: Int64)

@_silgen_name("prism_detach_surface")
func prism_detach_surface(_ handle: Int64)

@_silgen_name("prism_resize")
func prism_resize(_ handle: Int64, _ width: Int32, _ height: Int32)
