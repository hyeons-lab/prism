import FlutterMacOS
import MetalKit
import QuartzCore

/// Factory that creates PrismMetalView instances for AppKitView in Flutter.
class PrismMacOSPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    func create(withViewIdentifier viewId: Int64, arguments args: Any?) -> NSView {
        let engineHandle = (args as? [String: Any])?["engineHandle"] as? Int64 ?? 0
        return PrismMetalView(engineHandle: engineHandle)
    }

    func createArgsCodec() -> (FlutterMessageCodec & NSObjectProtocol)? {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// MTKView-backed platform view that drives wgpu rendering via the prism-native C API.
///
/// Lifecycle:
///   1. Flutter passes the engine handle via creationParams["engineHandle"].
///   2. On the first draw call the Metal layer is attached to the engine via
///      prism_attach_metal_layer, which initialises the wgpu surface.
///   3. MTKView's display-link calls draw(in:) at 60 fps; each call forwards to
///      prism_render_frame which ticks the engine and submits a wgpu render pass.
///   4. On deinit, prism_detach_surface releases the wgpu surface resources.
class PrismMetalView: MTKView, MTKViewDelegate {

    private let engineHandle: Int64
    private var surfaceAttached = false

    init(engineHandle: Int64) {
        self.engineHandle = engineHandle
        // Provide a Metal device so the view's CAMetalLayer is properly configured.
        // wgpu will attach its own context to the layer via prism_attach_metal_layer.
        let device = MTLCreateSystemDefaultDevice()
        super.init(frame: .zero, device: device)
        isPaused = false
        enableSetNeedsDisplay = false
        preferredFramesPerSecond = 60
        delegate = self
    }

    required init(coder: NSCoder) { fatalError("init(coder:) not supported") }

    deinit {
        if surfaceAttached {
            prism_detach_surface(engineHandle)
        }
    }

    // MARK: MTKViewDelegate

    func draw(in view: MTKView) {
        guard engineHandle != 0 else { return }

        // Lazily attach the wgpu surface on the first draw when the layer is ready.
        if !surfaceAttached {
            guard let metalLayer = view.layer as? CAMetalLayer else { return }
            let rawPtr = Unmanaged.passUnretained(metalLayer).toOpaque()
            let w = Int32(view.drawableSize.width)
            let h = Int32(view.drawableSize.height)
            prism_attach_metal_layer(engineHandle, rawPtr, w, h)
            surfaceAttached = true
        }

        // Forward rendering to the prism-native wgpu render pass.
        prism_render_frame(engineHandle)
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {}
}
