import Flutter
import UIKit
import MetalKit
import PrismDemo

/// Factory for creating PrismPlatformView instances from Flutter's platform view registry.
class PrismPlatformViewFactory: NSObject, FlutterPlatformViewFactory {

    private let store: DemoStore

    init(store: DemoStore) {
        self.store = store
        super.init()
    }

    func create(
        withFrame frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: Any?
    ) -> FlutterPlatformView {
        return PrismPlatformView(frame: frame, store: store)
    }

    func createArgsCodec() -> FlutterMessageCodec & NSObjectProtocol {
        return FlutterStandardMessageCodec.sharedInstance()
    }
}

/// iOS platform view that hosts an MTKView with wgpu4k rendering via the PrismDemo framework.
/// Mirrors the pattern from ViewController.swift: MTKView + configureDemo + IosDemoHandle.
class PrismPlatformView: NSObject, FlutterPlatformView {

    private let mtkView: MTKView
    private var demoHandle: IosDemoHandle?
    private var isInitialized = false
    private let store: DemoStore

    init(frame: CGRect, store: DemoStore) {
        self.store = store

        guard let device = MTLCreateSystemDefaultDevice() else {
            fatalError("Metal is not supported on this device")
        }

        mtkView = MTKView(frame: frame, device: device)
        mtkView.colorPixelFormat = .bgra8Unorm
        mtkView.depthStencilPixelFormat = .depth32Float
        mtkView.preferredFramesPerSecond = 60
        mtkView.isPaused = true

        super.init()

        // Defer wgpu init to next layout pass so the view has real dimensions
        DispatchQueue.main.async { [weak self] in
            self?.initializeIfNeeded()
        }
    }

    func view() -> UIView {
        return mtkView
    }

    private func initializeIfNeeded() {
        guard !isInitialized else { return }
        isInitialized = true

        IosDemoControllerKt.configureDemo(view: mtkView) { [weak self] handle, error in
            guard let self = self, let handle = handle else { return }
            self.demoHandle = handle
            self.mtkView.isPaused = false
        }
    }

    deinit {
        mtkView.isPaused = true
        mtkView.delegate = nil
        demoHandle?.shutdown()
        demoHandle = nil
    }
}
