import Foundation
import Observation

enum GuardianDestination: Hashable {
    case routineSetCreator
    case routineTemplates
    case routineEditor
    case feedbackSettings
    case records
    case recordCalendar
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
final class GuardianModeViewModel {
    private let repository: any RoutineRepository
    private let photoStore: any RoutinePhotoStoring
    private let now: () -> Date
    private let calendar: Calendar
    private let onDataChanged: () -> Void
    private let recordsState: GuardianRecordsState
    private let backupState: GuardianBackupState
    private let settingsState: GuardianSettingsState
    private let routineSetCreationState: GuardianRoutineSetCreationState
    private let routineTemplateState: GuardianRoutineTemplateState

    private(set) var loadState: GuardianLoadState = .idle
    private(set) var routineSets: [RoutineSet] = []
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var todayRoutineAssignment: DailyRoutineAssignment?
    private(set) var todayAssignedRoutineSetID: UUID?
    private(set) var routines: [Routine] = []
    private(set) var errorMessage: String?

    var selectedDestination: GuardianDestination?
    var selectedRoutineSetID: UUID?
    var selectedRoutineID: UUID?
    var draft: RoutineDraft?
    var isEditingRoutineSets = false
    var routineSetNameDraft: RoutineSetNameDraft?
    var routineSetScheduleDraft: RoutineSetScheduleDraft?
    var pendingDeleteRoutine: Routine?
    var pendingDeleteRoutineSet: RoutineSet?
    var templateReturnDestination: GuardianDestination?
    init(
        repository: any RoutineRepository,
        photoStore: (any RoutinePhotoStoring)? = nil,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current,
        onDataChanged: @escaping () -> Void
    ) {
        let resolvedPhotoStore = photoStore ?? FileRoutinePhotoStore()
        self.repository = repository
        self.photoStore = resolvedPhotoStore
        self.now = now
        self.calendar = calendar
        self.onDataChanged = onDataChanged
        self.recordsState = GuardianRecordsState(
            repository: repository,
            now: now,
            calendar: calendar
        )
        self.backupState = GuardianBackupState(
            repository: repository,
            now: now
        )
        self.settingsState = GuardianSettingsState(
            repository: repository,
            now: now,
            onDataChanged: onDataChanged
        )
        self.routineSetCreationState = GuardianRoutineSetCreationState(
            repository: repository,
            photoStore: resolvedPhotoStore,
            now: now
        )
        self.routineTemplateState = GuardianRoutineTemplateState(
            repository: repository,
            now: now
        )
    }

    var recordSummaries: [GuardianRecordSummary] { recordsState.summaries }
    var recordCalendarDates: Set<String> { recordsState.calendarDates }
    var selectedRecordDate: String? { recordsState.selectedDate }
    var selectedRecordDetail: GuardianRecordDetail? { recordsState.selectedDetail }
    var selectedCalendarRecordDate: String? { recordsState.selectedCalendarDate }
    var selectedCalendarRecordDetail: GuardianRecordDetail? { recordsState.selectedCalendarDetail }
    var backupPackage: BackupPackage? { backupState.package }
    var validatedRestoreSnapshot: RoutineRepositorySnapshot? { backupState.validatedRestoreSnapshot }
    var backupStatusMessage: String? { backupState.statusMessage }
    var restorePIN: String {
        get { backupState.restorePIN }
        set { backupState.restorePIN = newValue }
    }
    var settings: AppSettings? { settingsState.settings }
    var oneTimeRecoveryCode: String? { settingsState.oneTimeRecoveryCode }
    var recoveryCodeStatusMessage: String? { settingsState.recoveryCodeStatusMessage }
    var recoveryCodeErrorMessage: String? { settingsState.recoveryCodeErrorMessage }
    var routineSetDraft: RoutineSetDraft? {
        get { routineSetCreationState.draft }
        set { routineSetCreationState.draft = newValue }
    }
    var selectedRoutineSetStepID: UUID? {
        get { routineSetCreationState.selectedStepID }
        set { routineSetCreationState.selectedStepID = newValue }
    }
    var routineSetStepDraft: RoutineSetStepDraft? {
        get { routineSetCreationState.stepDraft }
        set { routineSetCreationState.stepDraft = newValue }
    }
    var selectedTemplateID: String? {
        get { routineTemplateState.selectedTemplateID }
        set { routineTemplateState.selectedTemplateID = newValue }
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

    var canSaveRoutineSetDraft: Bool {
        routineSetCreationState.canSave
    }

    var selectedRoutineSetStep: RoutineSetStepDraft? {
        routineSetCreationState.selectedStep
    }

    var routineTemplates: [BuiltInRoutineTemplate] {
        routineTemplateState.templates
    }

    var selectedTemplate: BuiltInRoutineTemplate? {
        routineTemplateState.selectedTemplate
    }

    var hasUnsavedRoutineSetDraft: Bool {
        routineSetCreationState.hasUnsavedDraft
    }

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            try settingsState.load()
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
            recordsState.refresh()

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
            recordsState.refresh()
        } catch {
            errorMessage = "정보를 불러오지 못했어요."
            loadState = .failed
        }
    }

    func selectRecordDate(_ date: String) {
        recordsState.selectDate(date)
    }

    func prepareRecordCalendar() {
        recordsState.prepareCalendar()
    }

    func selectCalendarRecordDate(_ date: String) {
        recordsState.selectCalendarDate(date)
    }

    func beginCreateRoutineSet() {
        routineSetCreationState.begin()
        routineTemplateState.clearSelection()
        isEditingRoutineSets = false
        selectedDestination = .routineSetCreator
    }

    func beginTemplateSelection(returnDestination: GuardianDestination? = nil) {
        routineSetCreationState.cancel()
        draft = nil
        templateReturnDestination = returnDestination
        routineTemplateState.beginSelection()
        selectedDestination = .routineTemplates
    }

    func closeTemplateSelection() {
        selectedDestination = templateReturnDestination
        templateReturnDestination = nil
    }

    func selectTemplate(_ template: BuiltInRoutineTemplate) {
        routineTemplateState.select(template)
    }

    func saveSelectedTemplate() {
        do {
            guard let routineSetID = try routineTemplateState.saveSelectedTemplate() else { return }
            templateReturnDestination = nil
            selectedRoutineSetID = routineSetID
            selectedDestination = .routineEditor
            load()
            onDataChanged()
        } catch {
            errorMessage = "템플릿을 저장하지 못했어요."
        }
    }

    func cancelRoutineSetDraft() {
        routineSetCreationState.cancel()
    }

    func beginAddRoutineSetStep() {
        routineSetCreationState.beginAddStep()
    }

    func beginEditRoutineSetStep(_ step: RoutineSetStepDraft) {
        routineSetCreationState.beginEditStep(step)
    }

    func cancelRoutineSetStepDraft() {
        routineSetCreationState.cancelStepDraft()
    }

    func saveRoutineSetStepDraft() {
        routineSetCreationState.saveStepDraft()
    }

    func deleteRoutineSetStep(_ step: RoutineSetStepDraft) {
        routineSetCreationState.deleteStep(step)
    }

    func moveRoutineSetStep(_ step: RoutineSetStepDraft, direction: Int) {
        routineSetCreationState.moveStep(step, direction: direction)
    }

    func saveRoutineSetDraft(localeIdentifier: String) {
        do {
            guard let routineSetID = try routineSetCreationState.save(
                localeIdentifier: localeIdentifier
            ) else { return }
            selectedRoutineSetID = routineSetID
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
            icon: routine.icon,
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
            load()
            onDataChanged()
        } catch {
            errorMessage = "저장하지 못했어요."
        }
    }

    func updateDraftPhoto(data: Data) {
        guard draft != nil else { return }
        do {
            draft?.icon = try photoStore.savePhotoData(data)
        } catch {
            errorMessage = "사진을 저장하지 못했어요."
        }
    }

    func resetDraftIconToDefault() {
        draft?.icon = try! IconRef.builtin(name: RoutineIconName.star.rawValue)
    }

    func updateRoutineSetStepDraftPhoto(data: Data) {
        do {
            try routineSetCreationState.updateStepPhoto(data: data)
        } catch {
            errorMessage = "사진을 저장하지 못했어요."
        }
    }

    func resetRoutineSetStepDraftIconToDefault() {
        routineSetCreationState.resetStepIconToDefault()
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

    func saveRoutineOrder(orderedIDs: [UUID]) {
        guard let selectedRoutineSet,
              orderedIDs.count == routines.count,
              Set(orderedIDs) == Set(routines.map(\.id)) else { return }
        let routinesByID = Dictionary(uniqueKeysWithValues: routines.map { ($0.id, $0) })
        let moved = orderedIDs.compactMap { routinesByID[$0] }
        guard moved.count == routines.count else { return }
        saveRoutineOrder(moved, in: selectedRoutineSet.id)
    }

    func selectRoutineSet(_ routineSet: RoutineSet) {
        guard routineSets.contains(where: { $0.id == routineSet.id }) else { return }
        selectedRoutineSetID = routineSet.id
        selectedRoutineID = nil
        draft = nil
        load()
    }

    func selectTodayAssignedRoutineSet() {
        guard let todayAssignedRoutineSetID,
              let routineSet = routineSets.first(where: { $0.id == todayAssignedRoutineSetID }) else { return }
        selectRoutineSet(routineSet)
    }

    func isRoutineSetAssignedToday(_ routineSet: RoutineSet) -> Bool {
        todayAssignedRoutineSetID == routineSet.id
    }

    func assignRoutineSetForToday(_ routineSet: RoutineSet) {
        guard routineSets.contains(where: { $0.id == routineSet.id }) else { return }
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        do {
            todayRoutineAssignment = try repository.assignRoutineSet(
                routineSet.id,
                on: today,
                at: now()
            )
            todayAssignedRoutineSetID = routineSet.id
            load()
            onDataChanged()
        } catch {
            errorMessage = "오늘 루틴으로 설정하지 못했어요."
        }
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

    func saveRoutineSetSchedule() {
        guard let draft = routineSetScheduleDraft,
              let original = routineSets.first(where: { $0.id == draft.id }) else { return }
        guard !routineSets.contains(where: {
            $0.id != draft.id && $0.dailyStartTime == draft.startTime
        }) else {
            errorMessage = "같은 시작 시각을 사용하는 루틴 세트가 있어요. 다른 시간을 선택해 주세요."
            return
        }
        do {
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
            load()
            onDataChanged()
        } catch {
            errorMessage = "매일 루틴 시간을 저장하지 못했어요."
        }
    }

    func removeRoutineSetFromDailySchedule(_ routineSet: RoutineSet) {
        guard routineSet.dailyStartTime != nil else { return }
        do {
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
            load()
            onDataChanged()
        } catch {
            errorMessage = "매일 루틴에서 제외하지 못했어요."
        }
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
                dailyStartTime: original.dailyStartTime,
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
            if routineSet.dailyStartTime != nil {
                errorMessage = "매일 사용하는 루틴 세트는 바로 삭제할 수 없어요. 먼저 매일 루틴에서 제외해 주세요."
                pendingDeleteRoutineSet = nil
                return
            }
            if todayAssignedRoutineSetID == routineSet.id {
                errorMessage = "현재 사용 중인 루틴 세트는 삭제할 수 없어요. 먼저 다른 루틴 세트를 오늘 루틴으로 설정해 주세요."
                pendingDeleteRoutineSet = nil
                return
            }
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
        settingsState.setPIN(pin)
    }

    func setPINAndGenerateRecoveryCode(_ pin: String) -> Bool {
        settingsState.setPINAndGenerateRecoveryCode(pin)
    }

    func hasGuardianPIN() -> Bool {
        settingsState.hasGuardianPIN()
    }

    func verifyPIN(_ pin: String) -> Bool {
        settingsState.verifyPIN(pin)
    }

    func updatePIN(oldPIN: String, newPIN: String) -> Bool {
        settingsState.updatePIN(oldPIN: oldPIN, newPIN: newPIN)
    }

    func verifyRecoveryCode(_ code: String) -> Bool {
        settingsState.verifyRecoveryCode(code)
    }

    func regenerateRecoveryCode() -> Bool {
        settingsState.regenerateRecoveryCode()
    }

    func clearOneTimeRecoveryCode() {
        settingsState.clearOneTimeRecoveryCode()
    }

    func clearRecoveryCodeError() {
        settingsState.clearRecoveryCodeError()
    }

    func updateFeedbackSettings(_ transform: (AppSettings) throws -> AppSettings) {
        do {
            try settingsState.update(transform)
        } catch {
            errorMessage = "설정을 저장하지 못했어요."
        }
    }

    func createBackupPackage() {
        do {
            try backupState.createPackage()
            errorMessage = nil
        } catch {
            errorMessage = "백업 파일을 만들지 못했어요."
        }
    }

    func validateRestorePackage(_ data: Data) {
        do {
            try backupState.validateRestorePackage(data)
            errorMessage = nil
        } catch {
            errorMessage = backupState.validationErrorMessage(for: error)
        }
    }

    var canConfirmRestore: Bool {
        backupState.canConfirmRestore(verifyPIN: verifyPIN)
    }

    func cancelRestore() {
        backupState.cancelRestore()
    }

    func confirmRestore() {
        guard canConfirmRestore else {
            errorMessage = "PIN을 확인해 주세요."
            return
        }
        do {
            try backupState.restore()
            load()
            onDataChanged()
        } catch {
            errorMessage = "복원하지 못했어요. 기존 데이터는 유지됩니다."
        }
    }

    func reportBackupFileError(_ message: String) {
        errorMessage = message
    }

    func clearErrorMessage() {
        errorMessage = nil
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

}
