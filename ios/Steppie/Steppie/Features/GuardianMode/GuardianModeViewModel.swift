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

struct GuardianRecordSummary: Equatable, Identifiable {
    let id: String
    let date: String
    let weekdaySymbol: String
    let completedCount: Int
    let totalCount: Int

    var remainingCount: Int {
        max(totalCount - completedCount, 0)
    }

    var completionRatio: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount)
    }

    var percentage: Int {
        Int((completionRatio * 100).rounded())
    }

    var statusText: String {
        guard totalCount > 0 else { return "기록 없음" }
        return remainingCount == 0 ? "완료" : "\(remainingCount)개 남음"
    }
}

struct GuardianRecordRoutineRow: Equatable, Identifiable {
    let id: UUID
    let title: String
    let status: LogStatus
    let completedAt: Date?
    let isDeleted: Bool
    let isInactive: Bool

    var isCompleted: Bool {
        status == .completed
    }

    var statusText: String {
        isCompleted ? "완료" : "미완료"
    }

    var availabilityText: String? {
        if isDeleted { return "삭제된 활동" }
        if isInactive { return "비활성 활동" }
        return nil
    }
}

struct GuardianRecordDetail: Equatable {
    let date: String
    let completedCount: Int
    let totalCount: Int
    let rows: [GuardianRecordRoutineRow]

    var isEmpty: Bool {
        totalCount == 0 && rows.isEmpty
    }

    var completionRatio: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount)
    }

    var percentage: Int {
        Int((completionRatio * 100).rounded())
    }
}

@MainActor
@Observable
final class GuardianModeViewModel {
    private let repository: any RoutineRepository
    private let photoStore: any RoutinePhotoStoring
    private let now: () -> Date
    private let calendar: Calendar
    private let onDataChanged: () -> Void

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
    var pendingDeleteRoutine: Routine?
    var pendingDeleteRoutineSet: RoutineSet?
    var selectedTemplateID: String?
    var templateReturnDestination: GuardianDestination?
    private(set) var recordSummaries: [GuardianRecordSummary] = []
    private(set) var recordCalendarDates: Set<String> = []
    private(set) var selectedRecordDate: String?
    private(set) var selectedRecordDetail: GuardianRecordDetail?
    private(set) var selectedCalendarRecordDate: String?
    private(set) var selectedCalendarRecordDetail: GuardianRecordDetail?
    private(set) var backupPackage: BackupPackage?
    private(set) var validatedRestorePayload: BackupRestorePayload?
    private(set) var backupStatusMessage: String?
    private(set) var oneTimeRecoveryCode: String?
    private(set) var recoveryCodeStatusMessage: String?
    private(set) var recoveryCodeErrorMessage: String?
    var restorePIN = ""

    var validatedRestoreSnapshot: RoutineRepositorySnapshot? {
        validatedRestorePayload?.snapshot
    }

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
        hasRoutineSets && todayAssignedRoutineSetID == nil
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
            refreshRecords()

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
            refreshRecords()
        } catch {
            errorMessage = "정보를 불러오지 못했어요."
            loadState = .failed
        }
    }

    func selectRecordDate(_ date: String) {
        guard recordSummaries.contains(where: { $0.date == date }) else { return }
        selectedRecordDate = date
        refreshSelectedRecordDetail()
    }

    func prepareRecordCalendar() {
        refreshRecordCalendarDates()
        selectedCalendarRecordDate = DailyLog.localDateString(for: now(), calendar: calendar)
        refreshSelectedCalendarRecordDetail()
    }

    func selectCalendarRecordDate(_ date: String) {
        guard recordCalendarDates.contains(date) else { return }
        selectedCalendarRecordDate = date
        refreshSelectedCalendarRecordDetail()
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

    private func refreshRecords() {
        refreshRecordCalendarDates()
        let dates = recentRecordDates()
        if selectedRecordDate.map({ !dates.contains($0) }) != false {
            selectedRecordDate = dates.first
        }
        recordSummaries = dates.map { date in
            (try? makeRecordSummary(for: date)) ?? GuardianRecordSummary(
                id: date,
                date: date,
                weekdaySymbol: weekdaySymbol(for: date),
                completedCount: 0,
                totalCount: 0
            )
        }
        refreshSelectedRecordDetail()
        if selectedCalendarRecordDate.map({ !recordCalendarDates.contains($0) }) == true {
            selectedCalendarRecordDate = recordCalendarDates.sorted(by: >).first
        }
        refreshSelectedCalendarRecordDetail()
    }

    private func refreshSelectedRecordDetail() {
        guard let selectedRecordDate else {
            selectedRecordDetail = nil
            return
        }
        selectedRecordDetail = try? makeRecordDetail(for: selectedRecordDate)
    }

    private func refreshRecordCalendarDates() {
        var dates = Set((try? repository.dailyLogDates()) ?? [])
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        if (try? routineSetRepresented(on: today)) != nil {
            dates.insert(today)
        }
        recordCalendarDates = dates
    }

    private func refreshSelectedCalendarRecordDetail() {
        guard let selectedCalendarRecordDate else {
            selectedCalendarRecordDetail = nil
            return
        }
        guard recordCalendarDates.contains(selectedCalendarRecordDate) else {
            selectedCalendarRecordDetail = nil
            return
        }
        selectedCalendarRecordDetail = try? makeRecordDetail(for: selectedCalendarRecordDate)
    }

    private func recentRecordDates() -> [String] {
        let start = calendar.startOfDay(for: now())
        return (0..<7).compactMap { offset in
            guard let date = calendar.date(byAdding: .day, value: -offset, to: start) else {
                return nil
            }
            return DailyLog.localDateString(for: date, calendar: calendar)
        }
    }

    private func makeRecordSummary(for date: String) throws -> GuardianRecordSummary {
        let detail = try makeRecordDetail(for: date)
        return GuardianRecordSummary(
            id: date,
            date: date,
            weekdaySymbol: weekdaySymbol(for: date),
            completedCount: detail.completedCount,
            totalCount: detail.totalCount
        )
    }

    private func makeRecordDetail(for date: String) throws -> GuardianRecordDetail {
        let logs = try repository.dailyLogs(on: date, routineSetID: nil)
        let routinesForDate = try routinesRepresented(on: date, logs: logs)
        let logsByRoutineID = Dictionary(uniqueKeysWithValues: logs.map { ($0.routineID, $0) })
        let rows = routinesForDate.map { routine in
            let log = logsByRoutineID[routine.id]
            return GuardianRecordRoutineRow(
                id: routine.id,
                title: localizedTitle(for: routine),
                status: log?.status ?? .undone,
                completedAt: log?.completedAt,
                isDeleted: routine.deletedAt != nil,
                isInactive: !routine.isActive
            )
        }
        let completedCount = rows.filter(\.isCompleted).count
        return GuardianRecordDetail(
            date: date,
            completedCount: completedCount,
            totalCount: rows.count,
            rows: rows
        )
    }

    private func routinesRepresented(on date: String, logs: [DailyLog]) throws -> [Routine] {
        if logs.isEmpty {
            guard let routineSet = try routineSetRepresented(on: date) else { return [] }
            return try repository.routines(in: routineSet.id)
                .filter { routineExisted($0, on: date) }
                .sorted(by: routineRecordSort)
        }

        let routineSetIDs = Set(logs.map(\.routineSetID))
        let representedRoutines = try routineSetIDs.flatMap { routineSetID in
            try repository.routines(
                in: routineSetID,
                includeInactive: true,
                includeDeleted: true
            )
        }

        var routinesByID = Dictionary(uniqueKeysWithValues: representedRoutines.map { ($0.id, $0) })
        for log in logs where routinesByID[log.routineID] == nil {
            if let routine = try repository.routine(id: log.routineID) {
                routinesByID[routine.id] = routine
            }
        }

        let logRoutineIDs = Set(logs.map(\.routineID))
        return routinesByID.values
            .filter { routine in
                if logRoutineIDs.contains(routine.id) { return true }
                guard routineExisted(routine, on: date) else { return false }
                return routine.isActive || routine.deletedAt != nil
            }
            .sorted(by: routineRecordSort)
    }

    private func routineSetRepresented(on date: String) throws -> RoutineSet? {
        if let assignment = try repository.dailyRoutineAssignment(on: date),
           let assignedSet = try repository.routineSet(id: assignment.routineSetID),
           assignedSet.deletedAt == nil {
            return assignedSet
        }

        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        guard date == today else { return nil }
        if let activeRoutineSet {
            return activeRoutineSet
        }
        return try repository.routineSets().first(where: \.isActive)
    }

    private func routineRecordSort(_ lhs: Routine, _ rhs: Routine) -> Bool {
        if lhs.routineSetID == rhs.routineSetID {
            if lhs.order == rhs.order { return lhs.createdAt < rhs.createdAt }
            return lhs.order < rhs.order
        }
        return lhs.createdAt < rhs.createdAt
    }

    private func routineExisted(_ routine: Routine, on localDate: String) -> Bool {
        guard let dayRange = dayRange(for: localDate) else { return false }
        guard routine.createdAt < dayRange.end else { return false }
        if let deletedAt = routine.deletedAt {
            return deletedAt >= dayRange.start
        }
        return true
    }

    private func dayRange(for localDate: String) -> (start: Date, end: Date)? {
        guard let parsedDate = Self.localDateFormatter.date(from: localDate) else {
            return nil
        }
        let components = Self.localDateFormatter.calendar.dateComponents([.year, .month, .day], from: parsedDate)
        guard let start = calendar.date(from: components),
              let end = calendar.date(byAdding: .day, value: 1, to: start) else {
            return nil
        }
        return (start, end)
    }

    private func weekdaySymbol(for localDate: String) -> String {
        guard let date = Self.localDateFormatter.date(from: localDate) else {
            return localDate
        }
        let weekday = calendar.component(.weekday, from: date)
        let symbols = ["일", "월", "화", "수", "목", "금", "토"]
        return symbols[max(0, min(symbols.count - 1, weekday - 1))]
    }

    private static let localDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()

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
