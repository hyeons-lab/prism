import MetalKit
import PrismDemo
import UIKit

class ViewController: UIViewController {

    private var mtkView: MTKView!

    override func viewDidLoad() {
        super.viewDidLoad()

        guard let device = MTLCreateSystemDefaultDevice() else {
            fatalError("Metal is not supported on this device")
        }

        mtkView = MTKView(frame: view.bounds, device: device)
        mtkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mtkView.colorPixelFormat = .bgra8Unorm
        mtkView.depthStencilPixelFormat = .depth32Float
        mtkView.preferredFramesPerSecond = 60
        view.addSubview(mtkView)

        IosDemoControllerKt.configureDemo(view: mtkView) { context, error in
            if let error = error {
                print("Prism: Failed to configure demo: \(error)")
                return
            }
            print("Prism: Demo configured successfully")
        }
    }
}
