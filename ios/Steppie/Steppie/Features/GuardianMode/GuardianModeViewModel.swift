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
    var icon: IconRef
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
        self.icon = try! IconRef.builtin(name: iconName.rawValue)
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    init(
        id: UUID = UUID(),
        title: String = "",
        icon: IconRef,
        colorToken: String = Routine.defaultColorToken,
        scheduledTime: LocalTime? = nil
    ) {
        self.id = id
        self.title = title
        self.icon = icon
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

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

struct RoutineSetDraft: Equatable {
    var name: String
    var steps: [RoutineSetStepDraft]

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !steps.isEmpty
            && steps.allSatisfy(\.isValid)
    }
}

struct BuiltInRoutineTemplateStep: Equatable, Identifiable {
    let id: String
    let titleKey: String
    let title: LocalizedText
    let iconName: RoutineIconName
    let colorToken: String
    let scheduledTime: LocalTime?

    var icon: IconRef {
        try! IconRef.builtin(name: iconName.rawValue)
    }
}

struct BuiltInRoutineTemplate: Equatable, Identifiable {
    let id: String
    let name: LocalizedText
    let steps: [BuiltInRoutineTemplateStep]

    var routineCountText: String {
        "\(steps.count)개 활동"
    }
}

enum BuiltInRoutineTemplates {
    static let all: [BuiltInRoutineTemplate] = [
        template(
            id: "morning",
            name: ["ko": "아침 루틴", "en": "Morning routine"],
            steps: [
                ("routine.wakeUp", ["ko": "일어나기", "en": "Wake up"], .wakeUp, "color.card.sky"),
                ("routine.washFace", ["ko": "세수하기", "en": "Wash face"], .washFace, "color.card.mint"),
                ("routine.brushTeeth", ["ko": "양치하기", "en": "Brush teeth"], .brushTeeth, "color.card.lemon"),
                ("routine.getDressed", ["ko": "옷 입기", "en": "Get dressed"], .getDressed, "color.card.peach"),
                ("routine.breakfast", ["ko": "아침 먹기", "en": "Eat breakfast"], .breakfast, "color.card.lavender"),
                ("routine.packBag", ["ko": "가방 챙기기", "en": "Pack bag"], .packBag, "color.card.rose"),
            ]
        ),
        template(
            id: "school",
            name: ["ko": "학교 루틴", "en": "School routine"],
            steps: [
                ("routine.goSchool", ["ko": "학교 가기", "en": "Go to school"], .bus, "color.card.sky"),
                ("routine.readBook", ["ko": "책 읽기", "en": "Read book"], .book, "color.card.mint"),
                ("routine.lunch", ["ko": "점심 먹기", "en": "Eat lunch"], .lunch, "color.card.lemon"),
                ("routine.play", ["ko": "놀이하기", "en": "Play"], .playground, "color.card.peach"),
            ]
        ),
        template(
            id: "bedtime",
            name: ["ko": "취침 루틴", "en": "Bedtime routine"],
            steps: [
                ("routine.bath", ["ko": "목욕하기", "en": "Take a bath"], .bath, "color.card.sky"),
                ("routine.pajamas", ["ko": "잠옷 입기", "en": "Put on pajamas"], .pajamas, "color.card.mint"),
                ("routine.sleep", ["ko": "잠자기", "en": "Sleep"], .sleep, "color.card.lavender"),
            ]
        ),
    ]

    private static func template(
        id: String,
        name: [String: String],
        steps: [(String, [String: String], RoutineIconName, String)]
    ) -> BuiltInRoutineTemplate {
        BuiltInRoutineTemplate(
            id: id,
            name: try! LocalizedText(name),
            steps: steps.map { titleKey, title, iconName, colorToken in
                BuiltInRoutineTemplateStep(
                    id: titleKey,
                    titleKey: titleKey,
                    title: try! LocalizedText(title),
                    iconName: iconName,
                    colorToken: colorToken,
                    scheduledTime: nil
                )
            }
        )
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

    private(set) var loadState: GuardianLoadState = .idle
    private(set) var routineSets: [RoutineSet] = []
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var todayRoutineAssignment: DailyRoutineAssignment?
    private(set) var todayAssignedRoutineSetID: UUID?
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
    var routineSetScheduleDraft: RoutineSetScheduleDraft?
    var pendingDeleteRoutine: Routine?
    var pendingDeleteRoutineSet: RoutineSet?
    var selectedTemplateID: String?
    var templateReturnDestination: GuardianDestination?
    private(set) var oneTimeRecoveryCode: String?
    private(set) var recoveryCodeStatusMessage: String?
    private(set) var recoveryCodeErrorMessage: String?
    init(
        repository: any RoutineRepository,
        photoStore: (any RoutinePhotoStoring)? = nil,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current,
        onDataChanged: @escaping () -> Void
    ) {
        self.repository = repository
        self.photoStore = photoStore ?? FileRoutinePhotoStore()
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
        routineSetDraft?.isValid == true
    }

    var selectedRoutineSetStep: RoutineSetStepDraft? {
        guard let routineSetDraft else { return nil }
        guard let selectedRoutineSetStepID else { return routineSetDraft.steps.first }
        return routineSetDraft.steps.first { $0.id == selectedRoutineSetStepID } ?? routineSetDraft.steps.first
    }

    var routineTemplates: [BuiltInRoutineTemplate] {
        BuiltInRoutineTemplates.all
    }

    var selectedTemplate: BuiltInRoutineTemplate? {
        guard let selectedTemplateID else { return routineTemplates.first }
        return routineTemplates.first { $0.id == selectedTemplateID } ?? routineTemplates.first
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
        routineSetDraft = RoutineSetDraft(name: "", steps: [])
        selectedRoutineSetStepID = nil
        routineSetStepDraft = nil
        selectedTemplateID = nil
        isEditingRoutineSets = false
        selectedDestination = .routineSetCreator
    }

    func beginTemplateSelection(returnDestination: GuardianDestination? = nil) {
        routineSetDraft = nil
        selectedRoutineSetStepID = nil
        routineSetStepDraft = nil
        draft = nil
        templateReturnDestination = returnDestination
        selectedTemplateID = selectedTemplateID ?? routineTemplates.first?.id
        selectedDestination = .routineTemplates
    }

    func closeTemplateSelection() {
        selectedDestination = templateReturnDestination
        templateReturnDestination = nil
    }

    func selectTemplate(_ template: BuiltInRoutineTemplate) {
        selectedTemplateID = template.id
    }

    func saveSelectedTemplate() {
        guard let selectedTemplate else { return }
        do {
            let createdAt = now()
            let routineSet = try RoutineSet(
                name: selectedTemplate.name,
                isActive: false,
                createdAt: createdAt,
                updatedAt: createdAt
            )
            try repository.createRoutineSet(routineSet)

            for (order, step) in selectedTemplate.steps.enumerated() {
                let routine = try Routine(
                    routineSetID: routineSet.id,
                    titleKey: step.titleKey,
                    title: step.title,
                    icon: step.icon,
                    colorToken: step.colorToken,
                    order: order,
                    scheduledTime: step.scheduledTime,
                    createdAt: createdAt,
                    updatedAt: createdAt
                )
                try repository.createRoutine(routine)
            }

            selectedTemplateID = selectedTemplate.id
            templateReturnDestination = nil
            selectedRoutineSetID = routineSet.id
            selectedDestination = .routineEditor
            load()
            onDataChanged()
        } catch {
            errorMessage = "템플릿을 저장하지 못했어요."
        }
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
                isActive: false,
                createdAt: createdAt,
                updatedAt: createdAt
            )
            try repository.createRoutineSet(routineSet)

            for (order, step) in routineSetDraft.steps.enumerated() {
                let routine = try Routine(
                    routineSetID: routineSet.id,
                    title: LocalizedText([localeIdentifier: step.title]),
                    icon: step.icon,
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
        guard routineSetStepDraft != nil else { return }
        do {
            routineSetStepDraft?.icon = try photoStore.savePhotoData(data)
        } catch {
            errorMessage = "사진을 저장하지 못했어요."
        }
    }

    func resetRoutineSetStepDraftIconToDefault() {
        routineSetStepDraft?.icon = try! IconRef.builtin(name: RoutineIconName.star.rawValue)
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
        do {
            let current = try repository.appSettings()
            let updated = try copySettings(current, guardianPinHash: GuardianPinService.makeHash(for: pin))
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
            recoveryCodeStatusMessage = "복구 코드를 만들었어요. 이 코드는 한 번만 표시됩니다."
            recoveryCodeErrorMessage = nil
            onDataChanged()
            return true
        } catch {
            recoveryCodeErrorMessage = "복구 코드를 만들지 못했어요."
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

    func verifyRecoveryCode(_ code: String) -> Bool {
        let sanitizedCode = String(code.filter(\.isNumber).prefix(6))
        let isValid = (try? repository.appSettings())
            .map { GuardianPinService.verifyRecoveryCode(sanitizedCode, against: $0.recoveryCodeHash) } ?? false
        if isValid {
            recoveryCodeErrorMessage = nil
        } else {
            recoveryCodeErrorMessage = "복구 코드가 맞지 않아요. 6자리 숫자를 확인해 주세요."
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
            recoveryCodeStatusMessage = "새 복구 코드를 만들었어요. 이전 복구 코드는 사용할 수 없습니다."
            recoveryCodeErrorMessage = nil
            onDataChanged()
            return true
        } catch {
            recoveryCodeErrorMessage = "복구 코드를 다시 만들지 못했어요."
            return false
        }
    }

    func clearOneTimeRecoveryCode() {
        oneTimeRecoveryCode = nil
    }

    func clearRecoveryCodeError() {
        recoveryCodeErrorMessage = nil
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
