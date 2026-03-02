import Flutter
import MetalKit
import UIKit

public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = PrismIOSPlatformViewFactory(messenger: registrar.messenger())
        registrar.register(factory, withId: "engine.prism.flutter/render_view")
        // Asset path resolution is handled entirely in Dart via rootBundle + temp files.
        // No method channel registration needed on iOS.
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

/// A UIView subclass that holds the embedded MTKView and drives the initial attach + resize.
///
/// `prism_attach_metal_layer` is deferred to the first `layoutSubviews` call so that wgpu
/// sees a valid Metal drawable size. Subsequent layout changes call `prism_resize`.
private class PrismMetalView: UIView {
    var engineHandle: Int64 = 0
    /// Strong reference to the embedded MTKView. MTKView is a subview of this container so ARC
    /// keeps it alive while the Kotlin/Native side holds a raw pointer to it.
    var mtkView: MTKView?
    private var isAttached = false

    override func layoutSubviews() {
        super.layoutSubviews()
        guard let mtkView = mtkView, engineHandle != 0 else { return }
        mtkView.frame = bounds
        let scale = (window?.screen ?? UIScreen.main).scale
        let width = Int32(bounds.width * scale)
        let height = Int32(bounds.height * scale)
        // Skip zero-size layouts (fired before Flutter sets the real frame).
        // Without this guard, prism_attach_metal_layer is called with 0×0 which
        // causes renderer init to fail and isAttached is set, blocking any retry.
        guard width > 0 && height > 0 else { return }
        if !isAttached {
            isAttached = true
            let rawPtr = Unmanaged.passUnretained(mtkView).toOpaque()
            let handle = engineHandle
            // Call synchronously. prism_attach_metal_layer calls
            // runBlocking(Dispatchers.Default){iosContextRenderer()} which dispatches to a
            // worker-thread pool — it does NOT resume on the main RunLoop, so blocking here
            // is safe and eliminates the attach/detach race that an async dispatch would create.
            prism_attach_metal_layer(handle, rawPtr, width, height)
        } else {
            prism_resize(engineHandle, width, height)
        }
    }
}

/// Weak-reference proxy used to break the CADisplayLink → PrismIOSPlatformView retain cycle.
///
/// `CADisplayLink` strongly retains its target. Without indirection, the cycle
/// `PrismIOSPlatformView → displayLink → PrismIOSPlatformView` would prevent `deinit` from
/// ever firing, leaking the engine, surface, and GPU resources permanently.
private class PrismDisplayLinkProxy: NSObject {
    weak var target: PrismIOSPlatformView?
    init(_ target: PrismIOSPlatformView) { self.target = target }
    @objc func renderFrame() { target?.renderFrame() }
}

class PrismIOSPlatformView: NSObject, FlutterPlatformView {
    private let _view: PrismMetalView
    private let engineHandle: Int64
    private var displayLink: CADisplayLink?

    init(frame: CGRect, arguments args: Any?) {
        let params = args as? [String: Any]
        self.engineHandle = (params?["engineHandle"] as? NSNumber)?.int64Value ?? 0
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
        // prism_attach_metal_layer is deferred to PrismMetalView.layoutSubviews so that wgpu
        // sees a valid drawable size (the view is in the window hierarchy at that point).
        _view.mtkView = mtkView

        // Use a weak proxy so CADisplayLink does not strongly retain self, breaking the
        // retain cycle that would otherwise prevent deinit from firing.
        let proxy = PrismDisplayLinkProxy(self)
        displayLink = CADisplayLink(target: proxy, selector: #selector(PrismDisplayLinkProxy.renderFrame))
        displayLink?.add(to: .main, forMode: .common)

        let pan = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        _view.addGestureRecognizer(pan)
        let pinch = UIPinchGestureRecognizer(target: self, action: #selector(handlePinch(_:)))
        _view.addGestureRecognizer(pinch)
    }

    @objc private func handlePan(_ recognizer: UIPanGestureRecognizer) {
        let translation = recognizer.translation(in: recognizer.view)
        prism_orbit_by(engineHandle, -Double(translation.x) * 0.01, Double(translation.y) * 0.01)
        recognizer.setTranslation(.zero, in: recognizer.view)
    }

    @objc private func handlePinch(_ recognizer: UIPinchGestureRecognizer) {
        prism_zoom(engineHandle, Double(recognizer.velocity) * 0.05)
    }

    @objc func renderFrame() {
        prism_render_frame(engineHandle)
    }

    deinit {
        displayLink?.invalidate()
        prism_detach_surface(engineHandle)
        _view.mtkView = nil
    }
}

// C API bindings (provided by PrismNative.xcframework)
// NOTE: on iOS `ptr` must be a `MTKView *`.
//       On macOS the same symbol expects a `CAMetalLayer *`. Both are void* at the C level.
@_silgen_name("prism_attach_metal_layer")
func prism_attach_metal_layer(_ handle: Int64, _ ptr: UnsafeMutableRawPointer, _ width: Int32, _ height: Int32)

@_silgen_name("prism_render_frame")
func prism_render_frame(_ handle: Int64)

@_silgen_name("prism_detach_surface")
func prism_detach_surface(_ handle: Int64)

@_silgen_name("prism_resize")
func prism_resize(_ handle: Int64, _ width: Int32, _ height: Int32)

@_silgen_name("prism_orbit_by")
func prism_orbit_by(_ handle: Int64, _ dx: Double, _ dy: Double)

@_silgen_name("prism_zoom")
func prism_zoom(_ handle: Int64, _ delta: Double)
