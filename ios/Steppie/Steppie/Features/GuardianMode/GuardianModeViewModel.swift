import Foundation
import Observation

enum GuardianDestination: Hashable {
    case routineSetCreator
    case routineEditor
    case feedbackSettings
    case records
    case security
    case backupRestore
}

enum GuardianLoadState: Equatable {
    case idle
    case loaded
    case empty
    case failed
}

enum GuardianPINPurpose: Equatable {
    case enter
    case setup
    case change
}

struct RoutineDraft: Equatable, Identifiable {
    let id: UUID?
    var title: String
    var iconName: RoutineIconName
    var colorToken: String
    var scheduledTime: LocalTime?

    var isNew: Bool { id == nil }
    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Routine.allowedColorTokens.contains(colorToken)
    }
}

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

struct RoutineSetStepDraft: Equatable, Identifiable {
    let id: UUID
    var title: String
    var iconName: RoutineIconName
    var colorToken: String
    var scheduledTime: LocalTime?

    init(
        id: UUID = UUID(),
        title: String = "",
        iconName: RoutineIconName = .star,
        colorToken: String = Routine.defaultColorToken,
        scheduledTime: LocalTime? = nil
    ) {
        self.id = id
        self.title = title
        self.iconName = iconName
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Routine.allowedColorTokens.contains(colorToken)
    }
}

struct RoutineSetDraft: Equatable {
    var name: String
    var steps: [RoutineSetStepDraft]

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !steps.isEmpty
            && steps.allSatisfy(\.isValid)
    }
}

struct RoutineSetNameDraft: Equatable, Identifiable {
    let id: UUID
    var name: String

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

@MainActor
@Observable
final class GuardianModeViewModel {
    private let repository: any RoutineRepository
    private let now: () -> Date
    private let onDataChanged: () -> Void

    private(set) var loadState: GuardianLoadState = .idle
    private(set) var routineSets: [RoutineSet] = []
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var routines: [Routine] = []
    private(set) var settings: AppSettings?
    private(set) var errorMessage: String?

    var selectedDestination: GuardianDestination?
    var selectedRoutineSetID: UUID?
    var selectedRoutineID: UUID?
    var draft: RoutineDraft?
    var routineSetDraft: RoutineSetDraft?
    var selectedRoutineSetStepID: UUID?
    var routineSetStepDraft: RoutineSetStepDraft?
    var isEditingRoutineSets = false
    var routineSetNameDraft: RoutineSetNameDraft?
    var pendingDeleteRoutine: Routine?
    var pendingDeleteRoutineSet: RoutineSet?
    private(set) var backupPackage: BackupPackage?
    private(set) var validatedRestorePayload: BackupRestorePayload?
    private(set) var backupStatusMessage: String?
    var restorePIN = ""

    var validatedRestoreSnapshot: RoutineRepositorySnapshot? {
        validatedRestorePayload?.snapshot
    }

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date = Date.init,
        onDataChanged: @escaping () -> Void
    ) {
        self.repository = repository
        self.now = now
        self.onDataChanged = onDataChanged
    }

    var selectedRoutine: Routine? {
        guard let selectedRoutineID else { return routines.first }
        return routines.first { $0.id == selectedRoutineID } ?? routines.first
    }

    var selectedRoutineSet: RoutineSet? {
        guard let selectedRoutineSetID else { return routineSets.first }
        return routineSets.first { $0.id == selectedRoutineSetID } ?? routineSets.first
    }

    var hasRoutineSets: Bool {
        !routineSets.isEmpty
    }

    var hasUnsavedDraft: Bool {
        guard let draft else { return false }
        if draft.isNew { return draft.isValid }
        guard let original = selectedRoutine else { return true }
        return draft.title != localizedTitle(for: original)
            || draft.iconName.rawValue != original.icon.name
            || draft.colorToken != original.colorToken
            || draft.scheduledTime != original.scheduledTime
    }

    var canSaveRoutineSetDraft: Bool {
        routineSetDraft?.isValid == true
    }

    var selectedRoutineSetStep: RoutineSetStepDraft? {
        guard let routineSetDraft else { return nil }
        guard let selectedRoutineSetStepID else { return routineSetDraft.steps.first }
        return routineSetDraft.steps.first { $0.id == selectedRoutineSetStepID } ?? routineSetDraft.steps.first
    }

    var hasUnsavedRoutineSetDraft: Bool {
        guard let routineSetDraft else { return false }
        return !routineSetDraft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || !routineSetDraft.steps.isEmpty
            || routineSetStepDraft?.isValid == true
    }

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            settings = try repository.appSettings()
            let fetchedRoutineSets = try repository.routineSets()
            routineSets = fetchedRoutineSets
            activeRoutineSet = fetchedRoutineSets.first(where: \.isActive)

            guard !fetchedRoutineSets.isEmpty else {
                activeRoutineSet = nil
                routines = []
                selectedRoutineSetID = nil
                selectedRoutineID = nil
                loadState = .empty
                return
            }

            if selectedRoutineSetID.map({ id in !fetchedRoutineSets.contains { $0.id == id } }) != false {
                selectedRoutineSetID = activeRoutineSet?.id ?? fetchedRoutineSets.first?.id
            }

            guard let selectedRoutineSetID else {
                routines = []
                selectedRoutineID = nil
                loadState = .empty
                return
            }

            routines = try repository.routines(in: selectedRoutineSetID)
            if selectedRoutineID.map({ id in !routines.contains { $0.id == id } }) != false {
                selectedRoutineID = routines.first?.id
            }
            loadState = .loaded
        } catch {
            errorMessage = "정보를 불러오지 못했어요."
            loadState = .failed
        }
    }

    func beginCreateRoutineSet() {
        routineSetDraft = RoutineSetDraft(name: "", steps: [])
        selectedRoutineSetStepID = nil
        routineSetStepDraft = nil
        isEditingRoutineSets = false
        selectedDestination = .routineSetCreator
    }

    func cancelRoutineSetDraft() {
        routineSetDraft = nil
        selectedRoutineSetStepID = nil
        routineSetStepDraft = nil
    }

    func beginAddRoutineSetStep() {
        routineSetStepDraft = RoutineSetStepDraft()
        selectedRoutineSetStepID = nil
    }

    func beginEditRoutineSetStep(_ step: RoutineSetStepDraft) {
        selectedRoutineSetStepID = step.id
        routineSetStepDraft = step
    }

    func cancelRoutineSetStepDraft() {
        routineSetStepDraft = nil
    }

    func saveRoutineSetStepDraft() {
        guard let routineSetStepDraft, routineSetStepDraft.isValid else { return }
        if let index = routineSetDraft?.steps.firstIndex(where: { $0.id == routineSetStepDraft.id }) {
            routineSetDraft?.steps[index] = routineSetStepDraft
        } else {
            routineSetDraft?.steps.append(routineSetStepDraft)
        }
        selectedRoutineSetStepID = routineSetStepDraft.id
        self.routineSetStepDraft = nil
    }

    func deleteRoutineSetStep(_ step: RoutineSetStepDraft) {
        routineSetDraft?.steps.removeAll { $0.id == step.id }
        if selectedRoutineSetStepID == step.id {
            selectedRoutineSetStepID = routineSetDraft?.steps.first?.id
        }
        if routineSetStepDraft?.id == step.id {
            routineSetStepDraft = nil
        }
    }

    func moveRoutineSetStep(_ step: RoutineSetStepDraft, direction: Int) {
        guard var steps = routineSetDraft?.steps,
              let index = steps.firstIndex(where: { $0.id == step.id }) else { return }
        let destination = index + direction
        guard steps.indices.contains(destination) else { return }
        steps.swapAt(index, destination)
        routineSetDraft?.steps = steps
    }

    func saveRoutineSetDraft(localeIdentifier: String) {
        guard let routineSetDraft, routineSetDraft.isValid else { return }
        do {
            let createdAt = now()
            let routineSet = try RoutineSet(
                name: LocalizedText([localeIdentifier: routineSetDraft.name]),
                isActive: true,
                createdAt: createdAt,
                updatedAt: createdAt
            )
            try repository.createRoutineSet(routineSet)

            for (order, step) in routineSetDraft.steps.enumerated() {
                let routine = try Routine(
                    routineSetID: routineSet.id,
                    title: LocalizedText([localeIdentifier: step.title]),
                    icon: IconRef.builtin(name: step.iconName.rawValue),
                    colorToken: step.colorToken,
                    order: order,
                    scheduledTime: step.scheduledTime,
                    createdAt: createdAt,
                    updatedAt: createdAt
                )
                try repository.createRoutine(routine)
            }

            self.routineSetDraft = nil
            selectedRoutineSetStepID = nil
            routineSetStepDraft = nil
            selectedRoutineSetID = routineSet.id
            selectedDestination = .routineEditor
            load()
            onDataChanged()
        } catch {
            errorMessage = "루틴 세트를 저장하지 못했어요."
        }
    }

    func beginAddRoutine() {
        guard selectedRoutineSet != nil else { return }
        selectedRoutineID = nil
        draft = RoutineDraft(
            id: nil,
            title: "",
            iconName: .star,
            colorToken: Routine.defaultColorToken,
            scheduledTime: nil
        )
    }

    func beginEditRoutine(_ routine: Routine) {
        selectedRoutineID = routine.id
        draft = RoutineDraft(
            id: routine.id,
            title: localizedTitle(for: routine),
            iconName: RoutineIconName(rawValue: routine.icon.name ?? "") ?? .star,
            colorToken: routine.colorToken,
            scheduledTime: routine.scheduledTime
        )
    }

    func cancelDraft() {
        draft = nil
    }

    func saveDraft(localeIdentifier: String) {
        guard let selectedRoutineSet, let draft, draft.isValid else { return }
        do {
            let updatedAt = now()
            let title = try LocalizedText([localeIdentifier: draft.title])
            let icon = try IconRef.builtin(name: draft.iconName.rawValue)
            if let id = draft.id, let original = try repository.routine(id: id) {
                let updated = try Routine(
                    id: original.id,
                    routineSetID: original.routineSetID,
                    titleKey: original.titleKey,
                    title: title,
                    icon: icon,
                    colorToken: draft.colorToken,
                    order: original.order,
                    scheduledTime: draft.scheduledTime,
                    isActive: original.isActive,
                    createdAt: original.createdAt,
                    updatedAt: updatedAt,
                    deletedAt: original.deletedAt
                )
                try repository.updateRoutine(updated)
            } else {
                let routine = try Routine(
                    routineSetID: selectedRoutineSet.id,
                    title: title,
                    icon: icon,
                    colorToken: draft.colorToken,
                    order: routines.count,
                    scheduledTime: draft.scheduledTime,
                    createdAt: updatedAt,
                    updatedAt: updatedAt
                )
                try repository.createRoutine(routine)
                selectedRoutineID = routine.id
            }
            self.draft = nil
            load()
            onDataChanged()
        } catch {
            errorMessage = "저장하지 못했어요."
        }
    }

    func requestDelete(_ routine: Routine) {
        pendingDeleteRoutine = routine
    }

    func confirmDelete() {
        guard let routine = pendingDeleteRoutine else { return }
        do {
            try repository.deleteRoutine(id: routine.id, at: now())
            pendingDeleteRoutine = nil
            draft = nil
            load()
            onDataChanged()
        } catch {
            errorMessage = "삭제하지 못했어요."
        }
    }

    func toggleRoutineSetEditing() {
        isEditingRoutineSets.toggle()
        routineSetNameDraft = nil
        pendingDeleteRoutineSet = nil
    }

    func moveRoutines(from source: IndexSet, to destination: Int) {
        guard let selectedRoutineSet else { return }
        var moved = routines
        let moving = source.sorted().map { moved[$0] }
        for index in source.sorted(by: >) {
            moved.remove(at: index)
        }
        let adjustedDestination = destination - source.filter { $0 < destination }.count
        moved.insert(contentsOf: moving, at: adjustedDestination)
        saveRoutineOrder(moved, in: selectedRoutineSet.id)
    }

    func moveRoutine(_ routine: Routine, direction: Int) {
        guard let selectedRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }) else { return }
        let destination = index + direction
        guard routines.indices.contains(destination) else { return }
        var moved = routines
        moved.swapAt(index, destination)
        saveRoutineOrder(moved, in: selectedRoutineSet.id)
    }

    func moveRoutine(_ routine: Routine, to destination: Int) {
        guard let selectedRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }),
              routines.indices.contains(destination),
              index != destination else { return }
        var moved = routines
        let item = moved.remove(at: index)
        moved.insert(item, at: destination)
        saveRoutineOrder(moved, in: selectedRoutineSet.id)
    }

    func selectRoutineSet(_ routineSet: RoutineSet) {
        guard routineSets.contains(where: { $0.id == routineSet.id }) else { return }
        selectedRoutineSetID = routineSet.id
        selectedRoutineID = nil
        draft = nil
        load()
    }

    func beginRenameRoutineSet(_ routineSet: RoutineSet) {
        routineSetNameDraft = RoutineSetNameDraft(
            id: routineSet.id,
            name: localizedTitle(for: routineSet)
        )
    }

    func cancelRoutineSetRename() {
        routineSetNameDraft = nil
    }

    func saveRoutineSetName(localeIdentifier: String) {
        guard let routineSetNameDraft,
              routineSetNameDraft.isValid,
              let original = try? repository.routineSet(id: routineSetNameDraft.id) else { return }
        do {
            let updated = try RoutineSet(
                id: original.id,
                name: LocalizedText([localeIdentifier: routineSetNameDraft.name]),
                isActive: original.isActive,
                createdAt: original.createdAt,
                updatedAt: now(),
                deletedAt: original.deletedAt
            )
            try repository.updateRoutineSet(updated)
            self.routineSetNameDraft = nil
            load()
            onDataChanged()
        } catch {
            errorMessage = "루틴 세트 이름을 저장하지 못했어요."
        }
    }

    func requestDeleteRoutineSet(_ routineSet: RoutineSet) {
        pendingDeleteRoutineSet = routineSet
    }

    func confirmDeleteRoutineSet() {
        guard let routineSet = pendingDeleteRoutineSet else { return }
        do {
            let visibleSets = try repository.routineSets()
            if routineSet.isActive {
                guard let nextActiveSet = visibleSets.first(where: { $0.id != routineSet.id }) else {
                    errorMessage = "마지막 루틴 세트는 삭제할 수 없어요."
                    pendingDeleteRoutineSet = nil
                    return
                }
                let activated = try RoutineSet(
                    id: nextActiveSet.id,
                    name: nextActiveSet.name,
                    isActive: true,
                    createdAt: nextActiveSet.createdAt,
                    updatedAt: now(),
                    deletedAt: nextActiveSet.deletedAt
                )
                try repository.updateRoutineSet(activated)
            }

            try repository.deleteRoutineSet(id: routineSet.id, at: now())
            if selectedRoutineSetID == routineSet.id {
                selectedRoutineSetID = nil
            }
            pendingDeleteRoutineSet = nil
            draft = nil
            load()
            onDataChanged()
        } catch {
            errorMessage = "루틴 세트를 삭제하지 못했어요."
        }
    }

    private func saveRoutineOrder(_ moved: [Routine], in routineSetID: UUID) {
        do {
            try repository.reorderRoutines(
                in: routineSetID,
                orderedIDs: moved.map(\.id),
                at: now()
            )
            load()
            onDataChanged()
        } catch {
            errorMessage = "순서를 바꾸지 못했어요."
        }
    }

    func setPIN(_ pin: String) -> Bool {
        do {
            let current = try repository.appSettings()
            let updated = try copySettings(current, guardianPinHash: GuardianPinService.makeHash(for: pin))
            try repository.updateAppSettings(updated)
            settings = updated
            return true
        } catch {
            return false
        }
    }

    func hasGuardianPIN() -> Bool {
        guard let settings = try? repository.appSettings() else { return false }
        return settings.guardianPinHash != nil
    }

    func verifyPIN(_ pin: String) -> Bool {
        (try? repository.appSettings()).map { GuardianPinService.verify(pin, against: $0.guardianPinHash) } ?? false
    }

    func updatePIN(oldPIN: String, newPIN: String) -> Bool {
        guard verifyPIN(oldPIN) else { return false }
        return setPIN(newPIN)
    }

    func updateFeedbackSettings(_ transform: (AppSettings) throws -> AppSettings) {
        do {
            let current = try repository.appSettings()
            let updated = try transform(current)
            try repository.updateAppSettings(updated)
            settings = updated
            onDataChanged()
        } catch {
            errorMessage = "설정을 저장하지 못했어요."
        }
    }

    func createBackupPackage() {
        do {
            backupPackage = try BackupService(repository: repository, now: now).exportPackage()
            backupStatusMessage = "백업 파일을 만들었어요."
            errorMessage = nil
        } catch {
            backupPackage = nil
            errorMessage = "백업 파일을 만들지 못했어요."
        }
    }

    func validateRestorePackage(_ data: Data) {
        do {
            validatedRestorePayload = try BackupService(repository: repository, now: now).validatePackagePayload(data)
            restorePIN = ""
            backupStatusMessage = "백업 파일을 확인했어요. 복원하려면 보호자 PIN을 입력해 주세요."
            errorMessage = nil
        } catch {
            validatedRestorePayload = nil
            restorePIN = ""
            errorMessage = backupErrorMessage(for: error)
        }
    }

    var canConfirmRestore: Bool {
        validatedRestorePayload != nil && verifyPIN(restorePIN)
    }

    func cancelRestore() {
        validatedRestorePayload = nil
        restorePIN = ""
    }

    func confirmRestore() {
        guard let validatedRestorePayload, verifyPIN(restorePIN) else {
            errorMessage = "PIN을 확인해 주세요."
            return
        }
        do {
            try BackupService(repository: repository, now: now).restorePayload(validatedRestorePayload)
            self.validatedRestorePayload = nil
            restorePIN = ""
            backupStatusMessage = "백업을 복원했어요."
            load()
            onDataChanged()
        } catch {
            errorMessage = "복원하지 못했어요. 기존 데이터는 유지됩니다."
        }
    }

    func reportBackupFileError(_ message: String) {
        errorMessage = message
    }

    func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: Locale.autoupdatingCurrent.identifier,
            systemLanguages: Locale.preferredLanguages
        )
    }

    func localizedTitle(for routineSet: RoutineSet) -> String {
        routineSet.name.resolved(
            appLocale: Locale.autoupdatingCurrent.identifier,
            systemLanguages: Locale.preferredLanguages
        )
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

    private func backupErrorMessage(for error: Error) -> String {
        guard let backupError = error as? BackupError else {
            return "백업 파일을 읽지 못했어요."
        }
        switch backupError {
        case .invalidChecksum:
            return "백업 파일이 손상되었어요."
        case .unsupportedSchemaVersion:
            return "지원하지 않는 백업 버전이에요."
        case .unsupportedPlatform:
            return "이 iOS 버전에서 복원할 수 없는 백업이에요."
        case .missingFile, .invalidPackage, .invalidManifest, .invalidData:
            return "올바른 Steppie 백업 파일이 아니에요."
        case .duplicateID, .invalidReference:
            return "백업 데이터 관계가 올바르지 않아요."
        }
    }
}
