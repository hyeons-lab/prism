import MetalKit
import PrismDemo
import UIKit

class ViewController: UIViewController {

    private var mtkView: MTKView!
    private var demoHandle: IosDemoHandle?
    private var isInitialized = false

    deinit {
        mtkView?.isPaused = true
        mtkView?.delegate = nil
        demoHandle?.shutdown()
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        guard let device = MTLCreateSystemDefaultDevice() else {
            showError("Metal is not supported on this device")
            return
        }

        mtkView = MTKView(frame: view.bounds, device: device)
        mtkView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        mtkView.colorPixelFormat = .bgra8Unorm
        mtkView.depthStencilPixelFormat = .depth32Float
        mtkView.preferredFramesPerSecond = 60
        mtkView.isPaused = true  // Paused until wgpu init in viewDidAppear
        view.addSubview(mtkView)
    }

    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)

        // Lazy init: only create wgpu resources when the tab becomes visible for the first time.
        // This avoids doubling GPU memory when both tabs init simultaneously on launch.
        guard !isInitialized, mtkView != nil else { return }
        isInitialized = true

        IosDemoControllerKt.configureDemo(view: mtkView) { handle, error in
            if let error = error {
                self.showError("Failed to initialize: \(error.localizedDescription)")
                return
            }
            self.demoHandle = handle
            self.mtkView.isPaused = false
        }
    }

    private func showError(_ message: String) {
        print("Prism: \(message)")

        let label = UILabel()
        label.text = message
        label.textColor = .white
        label.backgroundColor = UIColor(red: 0.8, green: 0.2, blue: 0.2, alpha: 0.9)
        label.textAlignment = .center
        label.numberOfLines = 0
        label.font = .systemFont(ofSize: 16, weight: .medium)
        label.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(label)
        NSLayoutConstraint.activate([
            label.leadingAnchor.constraint(equalTo: view.leadingAnchor, constant: 20),
            label.trailingAnchor.constraint(equalTo: view.trailingAnchor, constant: -20),
            label.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }
}
