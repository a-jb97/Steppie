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

nonisolated enum GuardianPINPurpose: Equatable {
    case enter
    case setup
    case change
}

@MainActor
@Observable
final class GuardianModeViewModel {
    private let onDataChanged: () -> Void
    private let routineManagementState: GuardianRoutineManagementState
    private let recordsState: GuardianRecordsState
    private let backupState: GuardianBackupState
    private let settingsState: GuardianSettingsState
    private let routineSetCreationState: GuardianRoutineSetCreationState
    private let routineTemplateState: GuardianRoutineTemplateState

    private(set) var errorMessage: String?

    var selectedDestination: GuardianDestination?
    var templateReturnDestination: GuardianDestination?
    init(
        repository: any RoutineRepository,
        photoStore: (any RoutinePhotoStoring)? = nil,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current,
        onDataChanged: @escaping () -> Void
    ) {
        let resolvedPhotoStore = photoStore ?? FileRoutinePhotoStore()
        self.onDataChanged = onDataChanged
        self.routineManagementState = GuardianRoutineManagementState(
            repository: repository,
            photoStore: resolvedPhotoStore,
            now: now,
            calendar: calendar
        )
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

    var loadState: GuardianLoadState { routineManagementState.loadState }
    var routineSets: [RoutineSet] { routineManagementState.routineSets }
    var activeRoutineSet: RoutineSet? { routineManagementState.activeRoutineSet }
    var todayRoutineAssignment: DailyRoutineAssignment? { routineManagementState.todayRoutineAssignment }
    var todayAssignedRoutineSetID: UUID? { routineManagementState.todayAssignedRoutineSetID }
    var routines: [Routine] { routineManagementState.routines }
    var selectedRoutineSetID: UUID? {
        get { routineManagementState.selectedRoutineSetID }
        set { routineManagementState.selectedRoutineSetID = newValue }
    }
    var selectedRoutineID: UUID? {
        get { routineManagementState.selectedRoutineID }
        set { routineManagementState.selectedRoutineID = newValue }
    }
    var draft: RoutineDraft? {
        get { routineManagementState.draft }
        set { routineManagementState.draft = newValue }
    }
    var isEditingRoutineSets: Bool {
        get { routineManagementState.isEditingRoutineSets }
        set { routineManagementState.isEditingRoutineSets = newValue }
    }
    var routineSetNameDraft: RoutineSetNameDraft? {
        get { routineManagementState.routineSetNameDraft }
        set { routineManagementState.routineSetNameDraft = newValue }
    }
    var routineSetScheduleDraft: RoutineSetScheduleDraft? {
        get { routineManagementState.routineSetScheduleDraft }
        set { routineManagementState.routineSetScheduleDraft = newValue }
    }
    var pendingDeleteRoutine: Routine? {
        get { routineManagementState.pendingDeleteRoutine }
        set { routineManagementState.pendingDeleteRoutine = newValue }
    }
    var pendingDeleteRoutineSet: RoutineSet? {
        get { routineManagementState.pendingDeleteRoutineSet }
        set { routineManagementState.pendingDeleteRoutineSet = newValue }
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
        routineManagementState.selectedRoutine
    }

    var selectedRoutineSet: RoutineSet? {
        routineManagementState.selectedRoutineSet
    }

    var hasRoutineSets: Bool {
        routineManagementState.hasRoutineSets
    }

    var requiresTodayRoutineSelection: Bool {
        routineManagementState.requiresTodayRoutineSelection
    }

    var scheduledRoutineSets: [RoutineSet] {
        routineManagementState.scheduledRoutineSets
    }

    var hasUnsavedDraft: Bool {
        routineManagementState.hasUnsavedDraft
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
            try routineManagementState.load()
            recordsState.refresh()
        } catch {
            errorMessage = "정보를 불러오지 못했어요."
            routineManagementState.markLoadFailed()
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
        routineManagementState.beginAddRoutine()
    }

    func beginEditRoutine(_ routine: Routine) {
        routineManagementState.beginEditRoutine(routine)
    }

    func cancelDraft() {
        routineManagementState.cancelDraft()
    }

    func saveDraft(localeIdentifier: String) {
        do {
            guard try routineManagementState.saveDraft(localeIdentifier: localeIdentifier) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "저장하지 못했어요."
        }
    }

    func updateDraftPhoto(data: Data) {
        do {
            try routineManagementState.updateDraftPhoto(data: data)
        } catch {
            errorMessage = "사진을 저장하지 못했어요."
        }
    }

    func resetDraftIconToDefault() {
        routineManagementState.resetDraftIconToDefault()
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
        routineManagementState.requestDelete(routine)
    }

    func confirmDelete() {
        do {
            guard try routineManagementState.confirmDelete() else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "삭제하지 못했어요."
        }
    }

    func toggleRoutineSetEditing() {
        routineManagementState.toggleRoutineSetEditing()
    }

    func moveRoutines(from source: IndexSet, to destination: Int) {
        do {
            guard try routineManagementState.moveRoutines(from: source, to: destination) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "순서를 바꾸지 못했어요."
        }
    }

    func moveRoutine(_ routine: Routine, direction: Int) {
        do {
            guard try routineManagementState.moveRoutine(routine, direction: direction) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "순서를 바꾸지 못했어요."
        }
    }

    func moveRoutine(_ routine: Routine, to destination: Int) {
        do {
            guard try routineManagementState.moveRoutine(routine, to: destination) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "순서를 바꾸지 못했어요."
        }
    }

    func saveRoutineOrder(orderedIDs: [UUID]) {
        do {
            guard try routineManagementState.saveRoutineOrder(orderedIDs: orderedIDs) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "순서를 바꾸지 못했어요."
        }
    }

    func selectRoutineSet(_ routineSet: RoutineSet) {
        guard routineManagementState.selectRoutineSet(routineSet) else { return }
        load()
    }

    func selectTodayAssignedRoutineSet() {
        guard routineManagementState.selectTodayAssignedRoutineSet() else { return }
        load()
    }

    func isRoutineSetAssignedToday(_ routineSet: RoutineSet) -> Bool {
        routineManagementState.isRoutineSetAssignedToday(routineSet)
    }

    func assignRoutineSetForToday(_ routineSet: RoutineSet) {
        do {
            guard try routineManagementState.assignRoutineSetForToday(routineSet) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "오늘 루틴으로 설정하지 못했어요."
        }
    }

    func isRoutineSetScheduledDaily(_ routineSet: RoutineSet) -> Bool {
        routineManagementState.isRoutineSetScheduledDaily(routineSet)
    }

    func beginScheduleRoutineSet(_ routineSet: RoutineSet) {
        routineManagementState.beginScheduleRoutineSet(routineSet)
    }

    func cancelRoutineSetSchedule() {
        routineManagementState.cancelRoutineSetSchedule()
    }

    func saveRoutineSetSchedule() {
        do {
            guard try routineManagementState.saveRoutineSetSchedule() else { return }
            load()
            onDataChanged()
        } catch GuardianRoutineManagementError.duplicateDailyStartTime {
            errorMessage = "같은 시작 시각을 사용하는 루틴 세트가 있어요. 다른 시간을 선택해 주세요."
        } catch {
            errorMessage = "매일 루틴 시간을 저장하지 못했어요."
        }
    }

    func removeRoutineSetFromDailySchedule(_ routineSet: RoutineSet) {
        do {
            guard try routineManagementState.removeRoutineSetFromDailySchedule(routineSet) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "매일 루틴에서 제외하지 못했어요."
        }
    }

    func beginRenameRoutineSet(_ routineSet: RoutineSet) {
        routineManagementState.beginRenameRoutineSet(routineSet)
    }

    func cancelRoutineSetRename() {
        routineManagementState.cancelRoutineSetRename()
    }

    func saveRoutineSetName(localeIdentifier: String) {
        do {
            guard try routineManagementState.saveRoutineSetName(
                localeIdentifier: localeIdentifier
            ) else { return }
            load()
            onDataChanged()
        } catch {
            errorMessage = "루틴 세트 이름을 저장하지 못했어요."
        }
    }

    func requestDeleteRoutineSet(_ routineSet: RoutineSet) {
        routineManagementState.requestDeleteRoutineSet(routineSet)
    }

    func confirmDeleteRoutineSet() {
        do {
            guard try routineManagementState.confirmDeleteRoutineSet() else { return }
            load()
            onDataChanged()
        } catch GuardianRoutineManagementError.scheduledRoutineSetDeletion {
            errorMessage = "매일 사용하는 루틴 세트는 바로 삭제할 수 없어요. 먼저 매일 루틴에서 제외해 주세요."
        } catch GuardianRoutineManagementError.assignedRoutineSetDeletion {
            errorMessage = "현재 사용 중인 루틴 세트는 삭제할 수 없어요. 먼저 다른 루틴 세트를 오늘 루틴으로 설정해 주세요."
        } catch GuardianRoutineManagementError.lastRoutineSetDeletion {
            errorMessage = "마지막 루틴 세트는 삭제할 수 없어요."
        } catch {
            errorMessage = "루틴 세트를 삭제하지 못했어요."
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
        routineManagementState.localizedTitle(for: routine)
    }

    func localizedTitle(for routineSet: RoutineSet) -> String {
        routineManagementState.localizedTitle(for: routineSet)
    }

}
