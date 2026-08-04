import Foundation
import Observation

enum GuardianLoadState: Equatable {
    case idle
    case loaded
    case empty
    case failed
}

enum GuardianRoutineManagementError: Error, Equatable {
    case duplicateDailyStartTime
    case scheduledRoutineSetDeletion
    case assignedRoutineSetDeletion
    case lastRoutineSetDeletion
}

struct RoutineDraft: Equatable, Identifiable {
    let id: UUID?
    var title: String
    var icon: IconRef
    var colorToken: String
    var scheduledTime: LocalTime?

    init(
        id: UUID?,
        title: String,
        iconName: RoutineIconName,
        colorToken: String,
        scheduledTime: LocalTime?
    ) {
        self.id = id
        self.title = title
        self.icon = try! IconRef.builtin(name: iconName.rawValue)
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    init(
        id: UUID?,
        title: String,
        icon: IconRef,
        colorToken: String,
        scheduledTime: LocalTime?
    ) {
        self.id = id
        self.title = title
        self.icon = icon
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    var isNew: Bool { id == nil }

    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Routine.allowedColorTokens.contains(colorToken)
    }

    var iconName: RoutineIconName {
        get {
            guard icon.type == .builtin,
                  let name = icon.name.flatMap(RoutineIconName.init(rawValue:)) else {
                return .star
            }
            return name
        }
        set {
            icon = try! IconRef.builtin(name: newValue.rawValue)
        }
    }
}

struct RoutineSetNameDraft: Equatable, Identifiable {
    let id: UUID
    var name: String

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}

struct RoutineSetScheduleDraft: Equatable, Identifiable {
    let id: UUID
    var startTime: LocalTime
}

@MainActor
@Observable
final class GuardianRoutineManagementState {
    private let repository: any RoutineRepository
    private let photoStore: any RoutinePhotoStoring
    private let now: () -> Date
    private let calendar: Calendar

    private(set) var loadState: GuardianLoadState = .idle
    private(set) var routineSets: [RoutineSet] = []
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var todayRoutineAssignment: DailyRoutineAssignment?
    private(set) var todayAssignedRoutineSetID: UUID?
    private(set) var routines: [Routine] = []

    var selectedRoutineSetID: UUID?
    var selectedRoutineID: UUID?
    var draft: RoutineDraft?
    var isEditingRoutineSets = false
    var routineSetNameDraft: RoutineSetNameDraft?
    var routineSetScheduleDraft: RoutineSetScheduleDraft?
    var pendingDeleteRoutine: Routine?
    var pendingDeleteRoutineSet: RoutineSet?

    init(
        repository: any RoutineRepository,
        photoStore: any RoutinePhotoStoring,
        now: @escaping () -> Date,
        calendar: Calendar
    ) {
        self.repository = repository
        self.photoStore = photoStore
        self.now = now
        self.calendar = calendar
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

    var requiresTodayRoutineSelection: Bool {
        hasRoutineSets
            && !routineSets.contains(where: { $0.dailyStartTime != nil })
            && todayAssignedRoutineSetID == nil
    }

    var scheduledRoutineSets: [RoutineSet] {
        routineSets
            .filter { $0.dailyStartTime != nil }
            .sorted {
                guard let lhs = $0.dailyStartTime, let rhs = $1.dailyStartTime else {
                    return $0.dailyStartTime != nil
                }
                if lhs == rhs { return $0.createdAt < $1.createdAt }
                return (lhs.hour, lhs.minute) < (rhs.hour, rhs.minute)
            }
    }

    var hasUnsavedDraft: Bool {
        guard let draft else { return false }
        if draft.isNew { return draft.isValid }
        guard let original = selectedRoutine else { return true }
        return draft.title != localizedTitle(for: original)
            || draft.icon != original.icon
            || draft.colorToken != original.colorToken
            || draft.scheduledTime != original.scheduledTime
    }

    func load() throws {
        do {
            let fetchedRoutineSets = try repository.routineSets()
            routineSets = fetchedRoutineSets
            activeRoutineSet = fetchedRoutineSets.first(where: \.isActive)
            let today = DailyLog.localDateString(for: now(), calendar: calendar)
            todayRoutineAssignment = try repository.dailyRoutineAssignment(on: today)
            if let todayRoutineAssignment,
               fetchedRoutineSets.contains(where: { $0.id == todayRoutineAssignment.routineSetID }) {
                todayAssignedRoutineSetID = todayRoutineAssignment.routineSetID
            } else {
                todayAssignedRoutineSetID = nil
            }

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
            loadState = .failed
            throw error
        }
    }

    func markLoadFailed() {
        loadState = .failed
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
            icon: routine.icon,
            colorToken: routine.colorToken,
            scheduledTime: routine.scheduledTime
        )
    }

    func cancelDraft() {
        draft = nil
    }

    func saveDraft(localeIdentifier: String) throws -> Bool {
        guard let selectedRoutineSet, let draft, draft.isValid else { return false }
        let updatedAt = now()
        let title = try LocalizedText([localeIdentifier: draft.title])
        if let id = draft.id, let original = try repository.routine(id: id) {
            let updated = try Routine(
                id: original.id,
                routineSetID: original.routineSetID,
                titleKey: original.titleKey,
                title: title,
                icon: draft.icon,
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
                icon: draft.icon,
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
        return true
    }

    func updateDraftPhoto(data: Data) throws {
        guard draft != nil else { return }
        draft?.icon = try photoStore.savePhotoData(data)
    }

    func resetDraftIconToDefault() {
        draft?.icon = try! IconRef.builtin(name: RoutineIconName.star.rawValue)
    }

    func requestDelete(_ routine: Routine) {
        pendingDeleteRoutine = routine
    }

    func confirmDelete() throws -> Bool {
        guard let routine = pendingDeleteRoutine else { return false }
        try repository.deleteRoutine(id: routine.id, at: now())
        pendingDeleteRoutine = nil
        draft = nil
        return true
    }

    func toggleRoutineSetEditing() {
        isEditingRoutineSets.toggle()
        routineSetNameDraft = nil
        pendingDeleteRoutineSet = nil
    }

    func moveRoutines(from source: IndexSet, to destination: Int) throws -> Bool {
        guard let selectedRoutineSet else { return false }
        var moved = routines
        let moving = source.sorted().map { moved[$0] }
        for index in source.sorted(by: >) {
            moved.remove(at: index)
        }
        let adjustedDestination = destination - source.filter { $0 < destination }.count
        moved.insert(contentsOf: moving, at: adjustedDestination)
        try saveRoutineOrder(moved, in: selectedRoutineSet.id)
        return true
    }

    func moveRoutine(_ routine: Routine, direction: Int) throws -> Bool {
        guard let selectedRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }) else { return false }
        let destination = index + direction
        guard routines.indices.contains(destination) else { return false }
        var moved = routines
        moved.swapAt(index, destination)
        try saveRoutineOrder(moved, in: selectedRoutineSet.id)
        return true
    }

    func moveRoutine(_ routine: Routine, to destination: Int) throws -> Bool {
        guard let selectedRoutineSet,
              let index = routines.firstIndex(where: { $0.id == routine.id }),
              routines.indices.contains(destination),
              index != destination else { return false }
        var moved = routines
        let item = moved.remove(at: index)
        moved.insert(item, at: destination)
        try saveRoutineOrder(moved, in: selectedRoutineSet.id)
        return true
    }

    func saveRoutineOrder(orderedIDs: [UUID]) throws -> Bool {
        guard let selectedRoutineSet,
              orderedIDs.count == routines.count,
              Set(orderedIDs) == Set(routines.map(\.id)) else { return false }
        let routinesByID = Dictionary(uniqueKeysWithValues: routines.map { ($0.id, $0) })
        let moved = orderedIDs.compactMap { routinesByID[$0] }
        guard moved.count == routines.count else { return false }
        try saveRoutineOrder(moved, in: selectedRoutineSet.id)
        return true
    }

    func selectRoutineSet(_ routineSet: RoutineSet) -> Bool {
        guard routineSets.contains(where: { $0.id == routineSet.id }) else { return false }
        selectedRoutineSetID = routineSet.id
        selectedRoutineID = nil
        draft = nil
        return true
    }

    func selectTodayAssignedRoutineSet() -> Bool {
        guard let todayAssignedRoutineSetID,
              let routineSet = routineSets.first(where: { $0.id == todayAssignedRoutineSetID }) else {
            return false
        }
        return selectRoutineSet(routineSet)
    }

    func isRoutineSetAssignedToday(_ routineSet: RoutineSet) -> Bool {
        todayAssignedRoutineSetID == routineSet.id
    }

    func assignRoutineSetForToday(_ routineSet: RoutineSet) throws -> Bool {
        guard routineSets.contains(where: { $0.id == routineSet.id }) else { return false }
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        todayRoutineAssignment = try repository.assignRoutineSet(
            routineSet.id,
            on: today,
            at: now()
        )
        todayAssignedRoutineSetID = routineSet.id
        return true
    }

    func isRoutineSetScheduledDaily(_ routineSet: RoutineSet) -> Bool {
        routineSet.dailyStartTime != nil
    }

    func beginScheduleRoutineSet(_ routineSet: RoutineSet) {
        let proposedTime = routineSet.dailyStartTime ?? nextAvailableDailyStartTime()
        routineSetScheduleDraft = RoutineSetScheduleDraft(
            id: routineSet.id,
            startTime: proposedTime
        )
    }

    func cancelRoutineSetSchedule() {
        routineSetScheduleDraft = nil
    }

    func saveRoutineSetSchedule() throws -> Bool {
        guard let draft = routineSetScheduleDraft,
              let original = routineSets.first(where: { $0.id == draft.id }) else { return false }
        guard !routineSets.contains(where: {
            $0.id != draft.id && $0.dailyStartTime == draft.startTime
        }) else {
            throw GuardianRoutineManagementError.duplicateDailyStartTime
        }
        let updated = try RoutineSet(
            id: original.id,
            name: original.name,
            isActive: original.isActive,
            dailyStartTime: draft.startTime,
            createdAt: original.createdAt,
            updatedAt: now(),
            deletedAt: original.deletedAt
        )
        try repository.updateRoutineSet(updated)
        routineSetScheduleDraft = nil
        return true
    }

    func removeRoutineSetFromDailySchedule(_ routineSet: RoutineSet) throws -> Bool {
        guard routineSet.dailyStartTime != nil else { return false }
        let updated = try RoutineSet(
            id: routineSet.id,
            name: routineSet.name,
            isActive: routineSet.isActive,
            dailyStartTime: nil,
            createdAt: routineSet.createdAt,
            updatedAt: now(),
            deletedAt: routineSet.deletedAt
        )
        try repository.updateRoutineSet(updated)
        return true
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

    func saveRoutineSetName(localeIdentifier: String) throws -> Bool {
        guard let routineSetNameDraft,
              routineSetNameDraft.isValid,
              let original = try? repository.routineSet(id: routineSetNameDraft.id) else { return false }
        let updated = try RoutineSet(
            id: original.id,
            name: LocalizedText([localeIdentifier: routineSetNameDraft.name]),
            isActive: original.isActive,
            dailyStartTime: original.dailyStartTime,
            createdAt: original.createdAt,
            updatedAt: now(),
            deletedAt: original.deletedAt
        )
        try repository.updateRoutineSet(updated)
        self.routineSetNameDraft = nil
        return true
    }

    func requestDeleteRoutineSet(_ routineSet: RoutineSet) {
        pendingDeleteRoutineSet = routineSet
    }

    func confirmDeleteRoutineSet() throws -> Bool {
        guard let routineSet = pendingDeleteRoutineSet else { return false }
        if routineSet.dailyStartTime != nil {
            pendingDeleteRoutineSet = nil
            throw GuardianRoutineManagementError.scheduledRoutineSetDeletion
        }
        if todayAssignedRoutineSetID == routineSet.id {
            pendingDeleteRoutineSet = nil
            throw GuardianRoutineManagementError.assignedRoutineSetDeletion
        }
        let visibleSets = try repository.routineSets()
        if routineSet.isActive {
            guard let nextActiveSet = visibleSets.first(where: { $0.id != routineSet.id }) else {
                pendingDeleteRoutineSet = nil
                throw GuardianRoutineManagementError.lastRoutineSetDeletion
            }
            let activated = try RoutineSet(
                id: nextActiveSet.id,
                name: nextActiveSet.name,
                isActive: true,
                dailyStartTime: nextActiveSet.dailyStartTime,
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
        return true
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

    private func nextAvailableDailyStartTime() -> LocalTime {
        let used = Set(routineSets.compactMap(\.dailyStartTime))
        for hour in 0..<24 {
            if let candidate = try? LocalTime(hour: hour, minute: 0),
               !used.contains(candidate) {
                return candidate
            }
        }
        return try! LocalTime(hour: 8, minute: 0)
    }

    private func saveRoutineOrder(_ moved: [Routine], in routineSetID: UUID) throws {
        try repository.reorderRoutines(
            in: routineSetID,
            orderedIDs: moved.map(\.id),
            at: now()
        )
    }
}
