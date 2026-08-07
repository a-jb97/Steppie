import Foundation
import Observation

@MainActor
@Observable
final class GuardianBackupState {
    private let service: BackupService

    private(set) var package: BackupPackage?
    private(set) var validatedRestorePayload: BackupRestorePayload?
    private(set) var statusMessage: String?
    var restorePIN = ""

    var validatedRestoreSnapshot: RoutineRepositorySnapshot? {
        validatedRestorePayload?.snapshot
    }

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date
    ) {
        self.service = BackupService(repository: repository, now: now)
    }

    func createPackage() throws {
        do {
            package = try service.exportPackage()
            statusMessage = String(localized: "백업 파일을 만들었어요.")
        } catch {
            package = nil
            throw error
        }
    }

    func validateRestorePackage(_ data: Data) throws {
        do {
            validatedRestorePayload = try service.validatePackagePayload(data)
            restorePIN = ""
            statusMessage = String(localized: "백업 파일을 확인했어요. 복원하려면 보호자 PIN을 입력해 주세요.")
        } catch {
            validatedRestorePayload = nil
            restorePIN = ""
            throw error
        }
    }

    func canConfirmRestore(verifyPIN: (String) -> Bool) -> Bool {
        validatedRestorePayload != nil && verifyPIN(restorePIN)
    }

    func cancelRestore() {
        validatedRestorePayload = nil
        restorePIN = ""
    }

    func restore() throws {
        guard let validatedRestorePayload else { return }
        try service.restorePayload(validatedRestorePayload)
        self.validatedRestorePayload = nil
        restorePIN = ""
        statusMessage = String(localized: "백업을 복원했어요.")
    }

    func validationErrorMessage(for error: Error) -> String {
        guard let backupError = error as? BackupError else {
            return String(localized: "백업 파일을 읽지 못했어요.")
        }
        switch backupError {
        case .invalidChecksum:
            return String(localized: "백업 파일이 손상되었어요.")
        case .unsupportedSchemaVersion:
            return String(localized: "지원하지 않는 백업 버전이에요.")
        case .unsupportedPlatform:
            return String(localized: "이 iOS 버전에서 복원할 수 없는 백업이에요.")
        case .missingFile, .invalidPackage, .invalidManifest, .invalidData:
            return String(localized: "올바른 Steppie 백업 파일이 아니에요.")
        case .duplicateID, .invalidReference:
            return String(localized: "백업 데이터 관계가 올바르지 않아요.")
        }
    }
}
