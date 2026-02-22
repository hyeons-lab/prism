import Foundation

/// Protocol that any macOS wgpu bridge must satisfy.
/// Implemented by Kotlin's PrismMetalBridge subclasses (e.g. DemoMacosBridge).
@objc public protocol PrismMetalBridgeProtocol: AnyObject {
    // SKIE translates Kotlin `fun attachMetalLayer(layerPtr: ...)` to the ObjC selector
    // `attachMetalLayerLayerPtr:width:height:` (no "With" infix). The explicit @objc
    // annotation keeps the Swift protocol's required selector in sync with SKIE's output.
    @objc(attachMetalLayerLayerPtr:width:height:)
    func attachMetalLayer(layerPtr: UnsafeMutableRawPointer?, width: Int32, height: Int32)
    func renderFrame()
    @objc(resizeWidth:height:)
    func resize(width: Int32, height: Int32)
    func detachSurface()
    func isInitialized() -> Bool

    // Engine control — used by the method channel handler in PrismFlutterPlugin.
    func togglePause()
    func getPauseState() -> Bool
    func getCurrentFps() -> Double
}

/// Optional protocol for bridges that support pointer/scroll camera input.
///
/// `PrismMetalView` soft-casts its bridge to this protocol on each mouse/scroll event.
/// SDK consumers that do not implement it simply receive no input forwarding.
/// Demo bridges (e.g. DemoMacosBridge) conform via a retroactive extension in the app.
///
/// SKIE selector notes:
///   `fun orbitBy(dx, dy)` → `orbitByDx:dy:`  (no rename needed)
///   `fun zoom(delta)`     → `zoomDelta:`      (@objc rename required)
@objc public protocol PrismInputDelegate: AnyObject {
    func orbitBy(dx: Double, dy: Double)
    @objc(zoomDelta:)
    func zoom(delta: Double)
}
