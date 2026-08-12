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
                let translation = stringUnit["value"] as? String,
                key.isEmpty || !translation.isEmpty
            else {
                return key
            }

            return nil
        }
        .sorted()

        #expect(missingEnglishKeys.isEmpty, "영어 번역 누락: \(missingEnglishKeys)")
    }

    @Test("한국어 사용자 문자열의 영어 번역은 한국어 원문을 그대로 사용하지 않는다")
    func koreanSourceKeysProvideTranslatedEnglishValues() throws {
        let catalog = try loadCatalog()
        let untranslatedKeys = catalog.compactMap { key, value -> String? in
            guard key.range(of: "[가-힣]", options: .regularExpression) != nil,
                  let entry = value as? [String: Any],
                  let localizations = entry["localizations"] as? [String: Any],
                  let english = localizations["en"] as? [String: Any],
                  let stringUnit = english["stringUnit"] as? [String: Any],
                  let translation = stringUnit["value"] as? String else {
                return nil
            }
            return translation == key ? key : nil
        }
        .sorted()

        #expect(untranslatedKeys.isEmpty, "한국어 원문이 남은 영어 번역: \(untranslatedKeys)")
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

    @Test("보호자 홈의 비활성 루틴 관리는 미구현이 아닌 선행 조건을 안내한다")
    func disabledRoutineManagementCopyDescribesPrerequisite() throws {
        let catalog = try loadCatalog()

        #expect(catalog["먼저 루틴 세트를 만들어 주세요"] != nil)
        #expect(catalog["준비 중"] == nil)
        #expect(catalog["아직 구현되지 않았습니다"] == nil)
    }

    @Test("보호자 PIN과 상태 메시지는 영어 번역을 제공한다")
    func guardianPINAndStatusKeysProvideExpectedEnglishLocalization() throws {
        let catalog = try loadCatalog()
        let expectedTranslations = [
            "보호자 확인": "Guardian verification",
            "4자리 PIN을 입력해 주세요": "Enter your 4-digit PIN",
            "PIN이 맞지 않아요. 다시 입력해 주세요.": "The PIN is incorrect. Try again.",
            "PIN 설정": "Set PIN",
            "새 보호자 PIN 4자리를 입력해 주세요": "Enter a new 4-digit guardian PIN",
            "PIN 변경": "Change PIN",
            "현재 PIN을 입력해 주세요": "Enter your current PIN",
            "복구 코드가 맞지 않아요. 6자리 숫자를 확인해 주세요.": "Recovery code does not match. Check the 6-digit number.",
            "백업 파일을 만들었어요.": "Backup file created.",
            "백업을 복원했어요.": "Backup restored.",
            "정보를 불러오지 못했어요.": "Could not load the information.",
            "시간 없음": "No time",
            "미완료": "Not completed"
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

    @Test("보호자 백업과 복원 화면은 영어 번역을 제공한다")
    func backupRestoreKeysProvideExpectedEnglishLocalization() throws {
        let catalog = try loadCatalog()
        let expectedTranslations = [
            "백업/복원": "Backup/Restore",
            "로컬 파일로 데이터를 내보내고 Replace 복원을 실행합니다": "Export data to a local file or run a replace restore",
            "백업 파일 만들기": "Create backup file",
            "백업 내보내기": "Export backup",
            "백업 파일 복원": "Restore backup file",
            "백업 파일 선택": "Choose backup file",
            "Replace 복원 확인": "Confirm Replace Restore",
            "복원 실행": "Restore",
            "백업 파일을 선택하지 못했어요.": "Could not select the backup file.",
            "백업 파일을 저장하지 못했어요.": "Could not save the backup file."
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
