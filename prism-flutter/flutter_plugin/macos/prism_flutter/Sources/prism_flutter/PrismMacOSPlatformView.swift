import FlutterMacOS
import MetalKit
import QuartzCore

/// Factory that creates `PrismMacOSMetalView` instances.
///
/// The engine handle is read from the Dart creationParams dictionary
/// (`{'engineHandle': <int64>}`) passed via `AppKitView`. This matches the iOS
/// plugin pattern — no pre-configured bridge object is needed.
public class PrismMacOSPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    public override init() {
        super.init()
    }

    public func create(withViewIdentifier viewId: Int64, arguments args: Any?) -> NSView {
        let params = args as? [String: Any]
        let engineHandle = params?["engineHandle"] as? Int64 ?? 0
        return PrismMacOSMetalView(engineHandle: engineHandle)
    }

    public func createArgsCodec() -> (FlutterMessageCodec & NSObjectProtocol)? {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// MTKView-backed Flutter platform view for macOS.
///
/// On the first draw call, retrieves the `CAMetalLayer` from the view's layer tree
/// and passes its raw pointer to `prism_attach_metal_layer` so the Kotlin/Native
/// side can configure a wgpu surface on it. Subsequent draw calls invoke
/// `prism_render_frame` to render one frame.
class PrismMacOSMetalView: MTKView, MTKViewDelegate {

    private let engineHandle: Int64
    private var surfaceAttached = false

    init(engineHandle: Int64) {
        self.engineHandle = engineHandle
        let device = MTLCreateSystemDefaultDevice()
        super.init(frame: .zero, device: device)
        isPaused = false
        enableSetNeedsDisplay = false
        preferredFramesPerSecond = 60
        delegate = self
    }

    required init(coder: NSCoder) { fatalError("init(coder:) not supported") }

    deinit {
        let handle = engineHandle
        DispatchQueue.main.async { prism_detach_surface(handle) }
    }

    // MARK: MTKViewDelegate

    func draw(in view: MTKView) {
        let size = view.drawableSize
        guard size.width > 0 && size.height > 0 else { return }
        guard engineHandle != 0 else { return }

        if !surfaceAttached {
            guard let metalLayer = view.layer as? CAMetalLayer else { return }
            let rawPtr = Unmanaged.passUnretained(metalLayer).toOpaque()
            prism_attach_metal_layer(engineHandle, rawPtr, Int32(size.width), Int32(size.height))
            surfaceAttached = true
            // Skip rendering on the attachment frame; wait for the surface to be ready.
            return
        }

        prism_render_frame(engineHandle)
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        guard surfaceAttached && size.width > 0 && size.height > 0 else { return }
        prism_resize(engineHandle, Int32(size.width), Int32(size.height))
    }

    // MARK: Mouse / scroll input

    override var acceptsFirstResponder: Bool { true }

    override func acceptsFirstMouse(for event: NSEvent?) -> Bool { true }

    override func mouseDragged(with event: NSEvent) {
        // TODO: wire to prism_orbit C API when input functions are exposed.
    }

    override func scrollWheel(with event: NSEvent) {
        // TODO: wire to prism_zoom C API when input functions are exposed.
    }
}

// C API bindings — provided by PrismNative.xcframework.
@_silgen_name("prism_attach_metal_layer")
func prism_attach_metal_layer(_ handle: Int64, _ ptr: UnsafeMutableRawPointer, _ width: Int32, _ height: Int32)

@_silgen_name("prism_render_frame")
func prism_render_frame(_ handle: Int64)

@_silgen_name("prism_detach_surface")
func prism_detach_surface(_ handle: Int64)

@_silgen_name("prism_resize")
func prism_resize(_ handle: Int64, _ width: Int32, _ height: Int32)
