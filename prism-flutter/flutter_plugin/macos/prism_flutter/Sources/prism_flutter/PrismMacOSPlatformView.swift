import FlutterMacOS
import MetalKit
import QuartzCore

/// Factory that creates PrismMetalView instances backed by a shared bridge.
/// The same bridge instance is reused across view re-creations so the method
/// channel always has a reference to the active rendering state.
public class PrismMacOSPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    private let bridge: PrismMetalBridgeProtocol

    public init(bridge: PrismMetalBridgeProtocol) {
        self.bridge = bridge
    }

    public func create(withViewIdentifier viewId: Int64, arguments args: Any?) -> NSView {
        return PrismMetalView(bridge: bridge)
    }

    public func createArgsCodec() -> (FlutterMessageCodec & NSObjectProtocol)? {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// Generic MTKView-backed Flutter platform view.
/// Drives any [PrismMetalBridgeProtocol] through the standard Metal layer lifecycle.
class PrismMetalView: MTKView, MTKViewDelegate {

    private let bridge: PrismMetalBridgeProtocol

    init(bridge: PrismMetalBridgeProtocol) {
        self.bridge = bridge
        let device = MTLCreateSystemDefaultDevice()
        super.init(frame: .zero, device: device)
        isPaused = false
        enableSetNeedsDisplay = false
        preferredFramesPerSecond = 60
        delegate = self
    }

    required init(coder: NSCoder) { fatalError("init(coder:) not supported") }

    deinit { bridge.detachSurface() }

    // MARK: MTKViewDelegate

    func draw(in view: MTKView) {
        let size = view.drawableSize
        guard size.width > 0 && size.height > 0 else { return }
        if !bridge.isInitialized() {
            guard let metalLayer = view.layer as? CAMetalLayer else { return }
            let rawPtr = Unmanaged.passUnretained(metalLayer).toOpaque()
            bridge.attachMetalLayer(
                layerPtr: rawPtr,
                width: Int32(size.width),
                height: Int32(size.height))
            // Don't render on the initialization frame; wait for the next draw call.
            // This ensures the surface reconfiguration in attachMetalLayer takes effect.
            return
        }
        bridge.renderFrame()
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {
        guard size.width > 0 && size.height > 0 else { return }
        bridge.resize(width: Int32(size.width), height: Int32(size.height))
    }

    // MARK: Mouse / scroll input

    override var acceptsFirstResponder: Bool { true }

    override func acceptsFirstMouse(for event: NSEvent?) -> Bool { true }

    /// Orbit the camera: horizontal drag → azimuth, vertical drag → elevation.
    /// Sensitivity of 0.01 rad/pt gives roughly 314 px per full revolution.
    /// Both axes are negated so dragging right/up feels like grabbing the scene.
    /// No-op when the bridge does not conform to PrismInputDelegate.
    override func mouseDragged(with event: NSEvent) {
        guard let input = bridge as? PrismInputDelegate else { return }
        let sensitivity = 0.01
        input.orbitBy(dx: -event.deltaX * sensitivity, dy: event.deltaY * sensitivity)
    }

    /// Zoom by adjusting orbit radius. Scroll up (positive scrollingDeltaY) zooms in.
    /// No-op when the bridge does not conform to PrismInputDelegate.
    override func scrollWheel(with event: NSEvent) {
        guard let input = bridge as? PrismInputDelegate else { return }
        input.zoom(delta: Double(event.scrollingDeltaY) * 0.1)
    }
}
