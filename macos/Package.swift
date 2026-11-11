// swift-tools-version:6.0
import PackageDescription

let package = Package(
    name: "IdearBubble",
    platforms: [.macOS(.v13)],
    targets: [
        .executableTarget(name: "IdearBubble", path: "Sources/IdearBubble")
    ]
)
