import FlutterMacOS
import PrismNative  // ensures libprism.dylib is linked and embedded

public class PrismFlutterPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {}
}
