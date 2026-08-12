import Foundation
import Testing

@Suite("PrivacyManifestTests")
struct PrivacyManifestTests {
    @Test("앱 privacy manifest는 UserDefaults required reason을 선언한다")
    func declaresUserDefaultsRequiredReason() throws {
        let manifestURL = try #require(
            Bundle.main.url(forResource: "PrivacyInfo", withExtension: "xcprivacy")
        )
        let manifestData = try Data(contentsOf: manifestURL)
        let manifest = try #require(
            PropertyListSerialization.propertyList(from: manifestData, format: nil)
                as? [String: Any]
        )
        let accessedAPITypes = try #require(
            manifest["NSPrivacyAccessedAPITypes"] as? [[String: Any]]
        )
        let userDefaultsDeclaration = try #require(
            accessedAPITypes.first {
                $0["NSPrivacyAccessedAPIType"] as? String
                    == "NSPrivacyAccessedAPICategoryUserDefaults"
            }
        )
        let reasons = try #require(
            userDefaultsDeclaration["NSPrivacyAccessedAPITypeReasons"] as? [String]
        )

        #expect(reasons.contains("CA92.1"))
        #expect(manifest["NSPrivacyTracking"] as? Bool == false)
    }
}
