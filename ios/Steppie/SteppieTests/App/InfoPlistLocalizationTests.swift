import Foundation
import Testing

@Suite("InfoPlistLocalizationTests")
struct InfoPlistLocalizationTests {
    @Test("앱 표시 이름은 한국어와 영어 localization을 제공한다")
    func providesLocalizedAppDisplayNames() throws {
        let koreanStrings = try localizedInfoPlistStrings(for: "ko")
        let englishStrings = try localizedInfoPlistStrings(for: "en")

        #expect(koreanStrings["CFBundleDisplayName"] == "차례차례")
        #expect(englishStrings["CFBundleDisplayName"] == "Steppie")
    }

    @Test("카메라 권한 안내는 한국어와 영어 localization을 제공한다")
    func providesLocalizedCameraUsageDescriptions() throws {
        let koreanStrings = try localizedInfoPlistStrings(for: "ko")
        let englishStrings = try localizedInfoPlistStrings(for: "en")

        #expect(
            koreanStrings["NSCameraUsageDescription"]
                == "활동 카드 사진 촬영에 카메라를 사용합니다."
        )
        #expect(
            englishStrings["NSCameraUsageDescription"]
                == "Steppie uses the camera to take photos for activity cards."
        )
    }

    private func localizedInfoPlistStrings(for localization: String) throws -> [String: String] {
        let stringsURL = try #require(
            Bundle.main.url(
                forResource: "InfoPlist",
                withExtension: "strings",
                subdirectory: nil,
                localization: localization
            )
        )
        let data = try Data(contentsOf: stringsURL)

        return try #require(
            PropertyListSerialization.propertyList(from: data, format: nil)
                as? [String: String]
        )
    }
}
