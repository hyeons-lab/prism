import UIKit

class SceneDelegate: UIResponder, UIWindowSceneDelegate {

    var window: UIWindow?

    func scene(
        _ scene: UIScene,
        willConnectTo session: UISceneSession,
        options connectionOptions: UIScene.ConnectionOptions
    ) {
        guard let windowScene = scene as? UIWindowScene else { return }

        let window = UIWindow(windowScene: windowScene)

        // Tab 1: Native MTKView demo
        let nativeVC = ViewController()
        nativeVC.tabBarItem = UITabBarItem(title: "Native", image: UIImage(systemName: "cube"), tag: 0)

        // Tab 2: Compose Multiplatform demo
        let composeVC = ComposeViewController()
        composeVC.tabBarItem = UITabBarItem(title: "Compose", image: UIImage(systemName: "slider.horizontal.3"), tag: 1)

        let tabBar = UITabBarController()
        tabBar.viewControllers = [nativeVC, composeVC]

        window.rootViewController = tabBar
        window.makeKeyAndVisible()
        self.window = window
    }
}
