import FlutterMacOS
import MetalKit

/// Factory that creates PrismMetalView instances for AppKitView in Flutter.
class PrismMacOSPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    func create(withViewIdentifier viewId: Int64, arguments args: Any?) -> NSView {
        return PrismMetalView()
    }

    func createArgsCodec() -> (FlutterMessageCodec & NSObjectProtocol)? {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// MTKView-backed Metal render surface embedded in Flutter via AppKitView.
///
/// Renders a dark-blue cleared viewport each frame. This proves the platform
/// view pipeline is wired correctly; wgpu-based scene rendering is added once
/// the prism-native C API exposes surface attachment.
class PrismMetalView: MTKView, MTKViewDelegate {

    private var commandQueue: MTLCommandQueue?

    init() {
        let device = MTLCreateSystemDefaultDevice()
        super.init(frame: .zero, device: device)
        commandQueue = device?.makeCommandQueue()
        clearColor = MTLClearColorMake(0.05, 0.05, 0.1, 1.0)
        isPaused = false
        enableSetNeedsDisplay = false
        preferredFramesPerSecond = 60
        delegate = self
    }

    required init(coder: NSCoder) { fatalError("init(coder:) not supported") }

    // MARK: MTKViewDelegate

    func draw(in view: MTKView) {
        guard
            let commandBuffer = commandQueue?.makeCommandBuffer(),
            let descriptor = view.currentRenderPassDescriptor,
            let encoder = commandBuffer.makeRenderCommandEncoder(descriptor: descriptor)
        else { return }
        encoder.endEncoding()
        if let drawable = view.currentDrawable {
            commandBuffer.present(drawable)
        }
        commandBuffer.commit()
    }

    func mtkView(_ view: MTKView, drawableSizeWillChange size: CGSize) {}
}
