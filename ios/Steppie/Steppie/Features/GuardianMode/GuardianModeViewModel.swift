import Foundation
import Observation

enum GuardianDestination: Hashable {
    case routineEditor
    case feedbackSettings
    case records
    case security
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
    var isValid: Bool { !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
}

@MainActor
@Observable
final class GuardianModeViewModel {
    private let repository: any RoutineRepository
    private let now: () -> Date
    private let onDataChanged: () -> Void

    private(set) var loadState: GuardianLoadState = .idle
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var routines: [Routine] = []
    private(set) var settings: AppSettings?
    private(set) var errorMessage: String?

    var selectedDestination: GuardianDestination?
    var selectedRoutineID: UUID?
    var draft: RoutineDraft?
    var pendingDeleteRoutine: Routine?

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

    var hasUnsavedDraft: Bool {
        guard let draft else { return false }
        if draft.isNew { return draft.isValid }
        guard let original = selectedRoutine else { return true }
        return draft.title != localizedTitle(for: original)
            || draft.iconName.rawValue != original.icon.name
            || draft.colorToken != original.colorToken
            || draft.scheduledTime != original.scheduledTime
    }

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            settings = try repository.appSettings()
            guard let activeSet = try repository.routineSets().first(where: \.isActive) else {
                activeRoutineSet = nil
                routines = []
                loadState = .empty
                return
            }
            activeRoutineSet = activeSet
            routines = try repository.routines(in: activeSet.id)
            if selectedRoutineID.map({ id in !routines.contains { $0.id == id } }) != false {
                selectedRoutineID = routines.first?.id
            }
            loadState = .loaded
        } catch {
            errorMessage = "정보를 불러오지 못했어요."
            loadState = .failed
        }
    }

    func beginAddRoutine() {
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
        guard let activeRoutineSet, let draft, draft.isValid else { return }
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
                    routineSetID: activeRoutineSet.id,
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

    func moveRoutines(from source: IndexSet, to destination: Int) {
        guard let activeRoutineSet else { return }
        var moved = routines
        let moving = source.sorted().map { moved[$0] }
        for index in source.sorted(by: >) {
            moved.remove(at: index)
        }
        let adjustedDestination = destination - source.filter { $0 < destination }.count
        moved.insert(contentsOf: moving, at: adjustedDestination)
        saveRoutineOrder(moved, in: activeRoutineSet.id)
    }

    func moveRoutine(_ routine: Routine, direction: Int) {
        guard let activeRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }) else { return }
        let destination = index + direction
        guard routines.indices.contains(destination) else { return }
        var moved = routines
        moved.swapAt(index, destination)
        saveRoutineOrder(moved, in: activeRoutineSet.id)
    }

    func moveRoutine(_ routine: Routine, to destination: Int) {
        guard let activeRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }),
              routines.indices.contains(destination),
              index != destination else { return }
        var moved = routines
        let item = moved.remove(at: index)
        moved.insert(item, at: destination)
        saveRoutineOrder(moved, in: activeRoutineSet.id)
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

    func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
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
            createdAt: settings.createdAt,
            updatedAt: now()
        )
    }
}
