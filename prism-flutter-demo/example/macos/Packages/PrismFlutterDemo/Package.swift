// swift-tools-version: 5.9
import PackageDescription

// Local package that exposes PrismFlutterDemo.xcframework to the Runner target.
// The XCFramework is built by: ./gradlew :prism-flutter-demo:bundleFlutterDemoMacOS
// It is intentionally kept here (in the example app) rather than in the generic
// prism-flutter plugin, which has no dependency on demo code.
let package = Package(
    name: "PrismFlutterDemo",
    platforms: [.macOS(.v10_15)],
    products: [
        .library(name: "PrismFlutterDemo", targets: ["PrismFlutterDemo"])
    ],
    targets: [
        .binaryTarget(
            name: "PrismFlutterDemo",
            path: "../../Frameworks/PrismFlutterDemo.xcframework"
        )
    ]
)
