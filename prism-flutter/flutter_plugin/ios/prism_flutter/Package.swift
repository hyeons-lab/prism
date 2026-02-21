// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "prism_flutter",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "prism-flutter", targets: ["prism_flutter"])
    ],
    targets: [
        // PrismNative XCFramework built via:
        //   ./gradlew :prism-flutter:bundleNativeiOS
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
