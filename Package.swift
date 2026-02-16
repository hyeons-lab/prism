// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "Prism",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "Prism", targets: ["Prism"])
    ],
    targets: [
        .binaryTarget(
            name: "Prism",
            url: "https://github.com/hyeons-lab/prism/releases/download/VERSION_PLACEHOLDER/Prism.xcframework.zip",
            checksum: "0000000000000000000000000000000000000000000000000000000000000000"
        )
    ]
)
