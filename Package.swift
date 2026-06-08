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
            url: "https://github.com/hyeons-lab/prism/releases/download/v0.1.0/Prism.xcframework.zip",
            checksum: "a30f07a26adfdd3602903460ae1edb75dbbe6f172be3160dab89d75df344abb6"
        )
    ]
)
