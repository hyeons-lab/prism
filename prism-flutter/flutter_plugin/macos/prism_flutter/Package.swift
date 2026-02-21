// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "prism_flutter",
    platforms: [.macOS(.v10_15)],
    products: [
        .library(name: "prism-flutter", targets: ["prism_flutter"])
    ],
    targets: [
        // Pre-built libprism.dylib wrapped as XCFramework.
        // Build: ./gradlew :prism-flutter:bundleNativeMacOS
        .binaryTarget(
            name: "PrismNative",
            path: "Frameworks/PrismNative.xcframework"
        ),
        .target(
            name: "prism_flutter",
            dependencies: ["PrismNative"],
            path: "Sources/prism_flutter"
        ),
    ]
)
