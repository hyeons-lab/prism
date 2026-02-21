// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "prism_flutter",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "prism-flutter", targets: ["prism_flutter"])
    ],
    targets: [
        // PrismDemo XCFramework built via:
        //   ./gradlew :prism-demo-core:assemblePrismDemoReleaseXCFramework
        .binaryTarget(
            name: "PrismDemo",
            path: "Frameworks/PrismDemo.xcframework"
        ),
        .target(
            name: "prism_flutter",
            dependencies: ["PrismDemo"],
            path: "Sources/prism_flutter"
        ),
    ]
)
