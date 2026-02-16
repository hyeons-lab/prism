import PrismDemo
import UIKit

/// Hosts the Compose Multiplatform demo as a child UIViewController.
class ComposeViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()

        let composeVC = ComposeIosEntryKt.composeDemoViewController()
        addChild(composeVC)
        composeVC.view.frame = view.bounds
        composeVC.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(composeVC.view)
        composeVC.didMove(toParent: self)
    }
}
