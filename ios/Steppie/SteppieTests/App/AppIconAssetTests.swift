import Foundation
import ImageIO
import Testing

struct AppIconAssetTests {
    @Test("App Store 아이콘은 1024px 정사각형이며 alpha channel이 없다")
    func appStoreIconsAreOpaque() throws {
        let iconDirectory = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("Steppie")
            .appendingPathComponent("Assets.xcassets")
            .appendingPathComponent("AppIcon.appiconset")
        let iconNames = [
            "Steppie-iOS-Default-1024x1024@1x.png",
            "Steppie-iOS-Dark-1024x1024@1x.png",
            "Steppie-iOS-ClearDark-1024x1024@1x.png"
        ]

        for iconName in iconNames {
            let iconURL = iconDirectory.appendingPathComponent(iconName)
            let source = try #require(CGImageSourceCreateWithURL(iconURL as CFURL, nil))
            let properties = try #require(
                CGImageSourceCopyPropertiesAtIndex(source, 0, nil)
                    as? [CFString: Any]
            )
            let image = try #require(CGImageSourceCreateImageAtIndex(source, 0, nil))

            #expect(properties[kCGImagePropertyPixelWidth] as? Int == 1_024)
            #expect(properties[kCGImagePropertyPixelHeight] as? Int == 1_024)
            #expect(
                [.none, .noneSkipFirst, .noneSkipLast].contains(image.alphaInfo)
            )
        }
    }
}
