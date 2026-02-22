// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "prism_flutter",
    platforms: [.macOS(.v10_15)],
    products: [
        .library(name: "prism-flutter", targets: ["prism_flutter"])
    ],
    targets: [
        // Pre-built libprism.dylib (C++ core) wrapped as XCFramework.
        // Build: ./gradlew :prism-flutter:bundleNativeMacOS
        .binaryTarget(
            name: "PrismNative",
            path: "Frameworks/PrismNative.xcframework"
        ),
        // Pre-built Kotlin/Native bridge (PrismMetalBridge, PrismBridge, etc.).
        // Build: ./gradlew :prism-flutter:bundleFlutterMacOS
        .binaryTarget(
            name: "PrismFlutter",
            path: "Frameworks/PrismFlutter.xcframework"
        ),
        .target(
            name: "prism_flutter",
            dependencies: ["PrismNative", "PrismFlutter"],
            path: "Sources/prism_flutter"
        ),
    ]
)
