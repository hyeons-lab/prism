import Flutter

/// Flutter plugin entry point for iOS.
///
/// Engine lifecycle and state queries are handled entirely by Dart FFI through
/// the PrismNative C API — no MethodChannel or platform view is needed here.
public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {}
}
