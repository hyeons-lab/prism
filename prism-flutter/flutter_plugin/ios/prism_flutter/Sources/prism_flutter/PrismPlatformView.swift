import Flutter
import UIKit
import MetalKit
import PrismDemo

/// Factory for creating PrismPlatformView instances from Flutter's platform view registry.
class PrismPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    private let store: DemoStore
    private weak var plugin: PrismFlutterPlugin?

    init(store: DemoStore, plugin: PrismFlutterPlugin) {
        self.store = store
        self.plugin = plugin
        super.init()
    }

    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        let view = PrismPlatformView(frame: frame, store: store)
        plugin?.trackPlatformView(view)
        return view
    }

    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// iOS platform view that hosts an MTKView with wgpu4k rendering via the PrismDemo framework.
/// Loads the DamagedHelmet.glb glTF model. Drag to rotate the model.
class PrismPlatformView: NSObject, FlutterPlatformView {

    private let containerView: UIView
    private var mtkView: MTKView?
    private var demoHandle: IosDemoHandle?
    private var isInitialized = false
    private let store: DemoStore

    /// True once `configureDemoWithGltf` has completed successfully and a render handle exists.
    var isReady: Bool { demoHandle != nil }

    init(frame: CGRect, store: DemoStore) {
        self.store = store
        self.containerView = UIView(frame: frame)

        guard let device = MTLCreateSystemDefaultDevice() else {
            // Metal not available — show error label instead of crashing
            let label = UILabel(frame: frame)
            label.text = "Metal is not supported on this device"
            label.textAlignment = .center
            label.textColor = .white
            label.backgroundColor = .darkGray
            containerView.addSubview(label)
            super.init()
            return
        }

        let view = MTKView(frame: frame, device: device)
        view.colorPixelFormat = .bgra8Unorm
        view.depthStencilPixelFormat = .depth32Float
        view.preferredFramesPerSecond = 60
        view.isPaused = true
        view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        containerView.addSubview(view)
        self.mtkView = view

        super.init()

        // Drag-to-rotate via pan gesture on the container view.
        let panGesture = UIPanGestureRecognizer(target: self, action: #selector(handlePan(_:)))
        containerView.addGestureRecognizer(panGesture)

        // Defer wgpu init to next layout pass so the view has real dimensions
        DispatchQueue.main.async { [weak self] in
            self?.initializeIfNeeded()
        }
    }

    func view() -> UIView {
        return containerView
    }

    func shutdown() {
        mtkView?.isPaused = true
        mtkView?.delegate = nil
        demoHandle?.shutdown()
        demoHandle = nil
    }

    @objc private func handlePan(_ recognizer: UIPanGestureRecognizer) {
        let translation = recognizer.translation(in: containerView)
        recognizer.setTranslation(.zero, in: containerView)
        demoHandle?.orbitBy(dx: -Float(translation.x) * 0.005, dy: Float(translation.y) * 0.005)
    }

    private func initializeIfNeeded() {
        guard !isInitialized, let mtkView = mtkView else { return }
        isInitialized = true // guard against re-entry while async init is in flight

        IosDemoControllerKt.configureDemoWithGltf(view: mtkView, store: store) { [weak self] handle, error in
            guard let self = self else { return }

            if let error = error {
                NSLog("Prism Flutter: configureDemoWithGltf failed: \(error.localizedDescription)")
                self.isInitialized = false // allow retry on next layout pass
                self.showErrorLabel(message: "Rendering failed: \(error.localizedDescription)")
                return
            }

            guard let handle = handle else {
                NSLog("Prism Flutter: configureDemoWithGltf returned nil handle")
                self.isInitialized = false
                self.showErrorLabel(message: "Rendering initialization failed.")
                return
            }

            self.demoHandle = handle
            self.mtkView?.isPaused = false
        }
    }

    private func showErrorLabel(message: String) {
        // Remove any previous error label but keep the mtkView intact so Metal resources are not
        // unnecessarily torn down. Error is displayed as an overlay on top of the metal view.
        containerView.viewWithTag(999)?.removeFromSuperview()
        let label = UILabel(frame: containerView.bounds)
        label.tag = 999
        label.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        label.text = message
        label.textAlignment = .center
        label.textColor = .white
        label.numberOfLines = 0
        label.backgroundColor = UIColor.darkGray.withAlphaComponent(0.85)
        containerView.addSubview(label)
    }

    deinit {
        shutdown()
    }
}
