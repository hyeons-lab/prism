import Cocoa
import FlutterMacOS
import PrismFlutterDemo
import prism_flutter

// DemoMacosBridge satisfies both SDK protocols.
// PrismMetalBridgeProtocol — surface lifecycle (attachMetalLayer, renderFrame, …)
// PrismInputDelegate       — camera input (orbitBy, zoom), forwarded from mouse/scroll events
// Note: SKIE may adjust ObjC method labels — verify selectors after first build.
extension DemoMacosBridge: @retroactive PrismMetalBridgeProtocol {}
extension DemoMacosBridge: @retroactive PrismInputDelegate {}

@main
class AppDelegate: FlutterAppDelegate {
  // init() is called during NIB unarchiving, before awakeFromNib on any other NIB object,
  // so this is the earliest safe point to configure PrismFlutterPlugin before
  // MainFlutterWindow.awakeFromNib calls RegisterGeneratedPlugins.
  override init() {
    super.init()
    PrismFlutterPlugin.configure(bridge: DemoMacosBridge())
  }

  override func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
    return true
  }

  override func applicationSupportsSecureRestorableState(_ app: NSApplication) -> Bool {
    return true
  }

  override func applicationDidFinishLaunching(_ notification: Notification) {
    super.applicationDidFinishLaunching(notification)
  }
}
