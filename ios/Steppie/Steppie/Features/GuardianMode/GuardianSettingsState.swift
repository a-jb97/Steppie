import Foundation
import Observation

extension AppSettings {
    func replacing(
        feedbackIntensity: FeedbackIntensity? = nil,
        soundEnabled: Bool? = nil,
        ttsEnabled: Bool? = nil,
        ttsRate: Double? = nil,
        ttsVolume: Double? = nil,
        hapticEnabled: Bool? = nil,
        notificationLeadTimes: [Int]? = nil,
        quietHours: (LocalTime?, LocalTime?)? = nil,
        updatedAt: Date = .now
    ) throws -> AppSettings {
        let resolvedQuietHoursStart: LocalTime?
        let resolvedQuietHoursEnd: LocalTime?
        if let quietHours {
            resolvedQuietHoursStart = quietHours.0
            resolvedQuietHoursEnd = quietHours.1
        } else {
            resolvedQuietHoursStart = quietHoursStart
            resolvedQuietHoursEnd = quietHoursEnd
        }

        return try AppSettings(
            id: id,
            guardianPinHash: guardianPinHash,
            recoveryCodeHash: recoveryCodeHash,
            feedbackIntensity: feedbackIntensity ?? self.feedbackIntensity,
            soundEnabled: soundEnabled ?? self.soundEnabled,
            ttsEnabled: ttsEnabled ?? self.ttsEnabled,
            ttsRate: ttsRate ?? self.ttsRate,
            ttsVolume: ttsVolume ?? self.ttsVolume,
            hapticEnabled: hapticEnabled ?? self.hapticEnabled,
            undoDurationSeconds: undoDurationSeconds,
            notificationLeadTimes: notificationLeadTimes ?? self.notificationLeadTimes,
            quietHoursStart: resolvedQuietHoursStart,
            quietHoursEnd: resolvedQuietHoursEnd,
            locale: locale,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }
}

@MainActor
@Observable
final class GuardianSettingsState {
    private let repository: any RoutineRepository
    private let now: () -> Date
    private let onDataChanged: () -> Void

    private(set) var settings: AppSettings?
    private(set) var oneTimeRecoveryCode: String?
    private(set) var recoveryCodeStatusMessage: String?
    private(set) var recoveryCodeErrorMessage: String?

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date,
        onDataChanged: @escaping () -> Void
    ) {
        self.repository = repository
        self.now = now
        self.onDataChanged = onDataChanged
    }

    func load() throws {
        settings = try repository.appSettings()
    }

    func setPIN(_ pin: String) -> Bool {
        do {
            let current = try repository.appSettings()
            let updated = try copySettings(
                current,
                guardianPinHash: GuardianPinService.makeHash(for: pin)
            )
            try repository.updateAppSettings(updated)
            settings = updated
            onDataChanged()
            return true
        } catch {
            return false
        }
    }

    func setPINAndGenerateRecoveryCode(_ pin: String) -> Bool {
        do {
            let current = try repository.appSettings()
            let recoveryCode = GuardianPinService.generateRecoveryCode()
            let updated = try copySettings(
                current,
                guardianPinHash: GuardianPinService.makeHash(for: pin),
                recoveryCodeHash: GuardianPinService.makeRecoveryCodeHash(for: recoveryCode)
            )
            try repository.updateAppSettings(updated)
            settings = updated
            oneTimeRecoveryCode = recoveryCode
            recoveryCodeStatusMessage = String(localized: "복구 코드를 만들었어요. 이 코드는 한 번만 표시됩니다.")
            recoveryCodeErrorMessage = nil
            onDataChanged()
            return true
        } catch {
            recoveryCodeErrorMessage = String(localized: "복구 코드를 만들지 못했어요.")
            return false
        }
    }

    func hasGuardianPIN() -> Bool {
        guard let settings = try? repository.appSettings() else { return false }
        return settings.guardianPinHash != nil
    }

    func verifyPIN(_ pin: String) -> Bool {
        guard let current = try? repository.appSettings(),
              GuardianPinService.verify(pin, against: current.guardianPinHash) else {
            return false
        }
        upgradePINHashIfNeeded(pin, current: current)
        return true
    }

    func updatePIN(oldPIN: String, newPIN: String) -> Bool {
        guard verifyPIN(oldPIN) else { return false }
        return setPIN(newPIN)
    }

    func verifyRecoveryCode(_ code: String) -> Bool {
        let sanitizedCode = String(code.filter(\.isNumber).prefix(6))
        let isValid = (try? repository.appSettings())
            .map {
                let isValid = GuardianPinService.verifyRecoveryCode(
                    sanitizedCode,
                    against: $0.recoveryCodeHash
                )
                if isValid {
                    upgradeRecoveryCodeHashIfNeeded(sanitizedCode, current: $0)
                }
                return isValid
            } ?? false
        if isValid {
            recoveryCodeErrorMessage = nil
        } else {
            recoveryCodeErrorMessage = String(localized: "복구 코드가 맞지 않아요. 6자리 숫자를 확인해 주세요.")
        }
        return isValid
    }

    func regenerateRecoveryCode() -> Bool {
        do {
            let current = try repository.appSettings()
            let recoveryCode = makeNewRecoveryCode(excluding: current.recoveryCodeHash)
            let updated = try copySettings(
                current,
                recoveryCodeHash: GuardianPinService.makeRecoveryCodeHash(for: recoveryCode)
            )
            try repository.updateAppSettings(updated)
            settings = updated
            oneTimeRecoveryCode = recoveryCode
            recoveryCodeStatusMessage = String(localized: "새 복구 코드를 만들었어요. 이전 복구 코드는 사용할 수 없습니다.")
            recoveryCodeErrorMessage = nil
            onDataChanged()
            return true
        } catch {
            recoveryCodeErrorMessage = String(localized: "복구 코드를 다시 만들지 못했어요.")
            return false
        }
    }

    func clearOneTimeRecoveryCode() {
        oneTimeRecoveryCode = nil
    }

    func clearRecoveryCodeError() {
        recoveryCodeErrorMessage = nil
    }

    func update(_ transform: (AppSettings) throws -> AppSettings) throws {
        let current = try repository.appSettings()
        let updated = try transform(current)
        try repository.updateAppSettings(updated)
        settings = updated
        onDataChanged()
    }

    private func makeNewRecoveryCode(excluding storedHash: String?) -> String {
        for _ in 0..<10 {
            let candidate = GuardianPinService.generateRecoveryCode()
            if !GuardianPinService.verifyRecoveryCode(candidate, against: storedHash) {
                return candidate
            }
        }
        return GuardianPinService.generateRecoveryCode()
    }

    private func upgradePINHashIfNeeded(_ pin: String, current: AppSettings) {
        guard GuardianPinService.needsHashUpgrade(current.guardianPinHash),
              let upgradedHash = try? GuardianPinService.makeHash(for: pin) else {
            return
        }
        updateSecurityHashesIfPossible(current, guardianPinHash: upgradedHash)
    }

    private func upgradeRecoveryCodeHashIfNeeded(_ code: String, current: AppSettings) {
        guard GuardianPinService.needsHashUpgrade(current.recoveryCodeHash),
              let upgradedHash = try? GuardianPinService.makeRecoveryCodeHash(for: code) else {
            return
        }
        updateSecurityHashesIfPossible(current, recoveryCodeHash: upgradedHash)
    }

    private func updateSecurityHashesIfPossible(
        _ current: AppSettings,
        guardianPinHash: String? = nil,
        recoveryCodeHash: String? = nil
    ) {
        do {
            let updated = try copySettings(
                current,
                guardianPinHash: guardianPinHash,
                recoveryCodeHash: recoveryCodeHash
            )
            try repository.updateAppSettings(updated)
            settings = updated
            onDataChanged()
        } catch {
            // Authentication remains valid even if a legacy hash cannot be upgraded yet.
        }
    }

    private func copySettings(
        _ settings: AppSettings,
        guardianPinHash: String? = nil,
        recoveryCodeHash: String? = nil
    ) throws -> AppSettings {
        try AppSettings(
            id: settings.id,
            guardianPinHash: guardianPinHash ?? settings.guardianPinHash,
            recoveryCodeHash: recoveryCodeHash ?? settings.recoveryCodeHash,
            feedbackIntensity: settings.feedbackIntensity,
            soundEnabled: settings.soundEnabled,
            ttsEnabled: settings.ttsEnabled,
            ttsRate: settings.ttsRate,
            ttsVolume: settings.ttsVolume,
            hapticEnabled: settings.hapticEnabled,
            undoDurationSeconds: settings.undoDurationSeconds,
            notificationLeadTimes: settings.notificationLeadTimes,
            quietHoursStart: settings.quietHoursStart,
            quietHoursEnd: settings.quietHoursEnd,
            locale: settings.locale,
            createdAt: settings.createdAt,
            updatedAt: now()
        )
    }
}
