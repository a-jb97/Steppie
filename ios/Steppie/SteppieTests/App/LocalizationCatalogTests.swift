import Foundation
import Testing

struct LocalizationCatalogTests {
    @Test("출시 문자열 catalog는 모든 key에 영어 번역을 제공한다")
    func allCatalogEntriesProvideEnglishLocalization() throws {
        let catalog = try loadCatalog()
        let missingEnglishKeys = catalog.compactMap { key, value -> String? in
            guard
                let entry = value as? [String: Any],
                let localizations = entry["localizations"] as? [String: Any],
                let english = localizations["en"] as? [String: Any],
                let stringUnit = english["stringUnit"] as? [String: Any],
                stringUnit["value"] is String
            else {
                return key
            }

            return nil
        }
        .sorted()

        #expect(missingEnglishKeys.isEmpty, "영어 번역 누락: \(missingEnglishKeys)")
    }

    @Test("보호자 홈 메뉴 문자열은 영어 번역을 제공한다")
    func guardianHomeMenuKeysProvideExpectedEnglishLocalization() throws {
        let catalog = try loadCatalog()
        let expectedTranslations = [
            "루틴 세트 생성": "Create routine set",
            "새 루틴 제목과 단계 목록 만들기": "Create a routine title and step list",
            "템플릿에서 시작하기": "Start from a template",
            "아침, 학교, 취침 루틴으로 빠르게 만들기": "Quickly create a morning, school, or bedtime routine",
            "루틴 관리": "Manage routines",
            "루틴 세트 목록, 활동 추가, 순서 변경": "Routine sets, adding activities, and reordering",
            "먼저 루틴 세트를 만들어 주세요": "Create a routine set first",
            "음성, 효과음, 햅틱, 알림": "Voice guidance, sound effects, haptics, and notifications",
            "진행 기록": "Progress history",
            "날짜별 완료 현황": "Completion progress by date",
            "PIN, 복구 코드, 백업/복원": "PIN, recovery code, and backup/restore"
        ]

        for (key, expectedTranslation) in expectedTranslations {
            let entry = try #require(catalog[key] as? [String: Any])
            let localizations = try #require(entry["localizations"] as? [String: Any])
            let english = try #require(localizations["en"] as? [String: Any])
            let stringUnit = try #require(english["stringUnit"] as? [String: Any])
            let translation = try #require(stringUnit["value"] as? String)

            #expect(translation == expectedTranslation)
        }
    }

    private func loadCatalog() throws -> [String: Any] {
        let projectDirectory = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let catalogURL = projectDirectory
            .appendingPathComponent("Steppie")
            .appendingPathComponent("Localizable.xcstrings")
        let data = try Data(contentsOf: catalogURL)
        let root = try #require(
            JSONSerialization.jsonObject(with: data) as? [String: Any]
        )

        return try #require(root["strings"] as? [String: Any])
    }
}
