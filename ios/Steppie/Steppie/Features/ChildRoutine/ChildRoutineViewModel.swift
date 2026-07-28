import Foundation
import Observation

enum ChildRoutineLoadState: Equatable {
    case idle
    case loaded
    case empty
    case failed
}

enum ChildRoutinePage: Equatable {
    case focus
    case list
}

enum ChildRoutineLayout: Equatable {
    case singlePane
    case splitPane
}

enum ChildRoutineLayoutPolicy {
    static func layout(for availableWidth: CGFloat) -> ChildRoutineLayout {
        availableWidth >= SteppieLayout.splitMinimumWidth ? .splitPane : .singlePane
    }
}

@MainActor
@Observable
final class ChildRoutineViewModel {
    private let repository: any RoutineRepository
    private let speechGuide: any RoutineSpeechGuiding
    private let feedbackPerformer: any RoutineFeedbackPerforming
    private let notificationScheduler: any RoutineNotificationScheduling
    private let now: () -> Date
    private let calendar: Calendar
    private let locale: () -> Locale
    private let isNotificationSchedulingEnabled: Bool
    @ObservationIgnored private var didRequestNotificationAuthorization = false
    @ObservationIgnored private var pendingNotificationRoute: RoutineNotificationRoute?
    @ObservationIgnored private var isRoutineSpeechActive = true
    @ObservationIgnored private var scheduledTransitionTask: Task<Void, Never>?
    @ObservationIgnored private var feedbackAdvanceTask: Task<Void, Never>?

    private(set) var loadState: ChildRoutineLoadState = .idle
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var plannedRoutineSets: [RoutineSet] = []
    private(set) var routines: [Routine] = []
    private(set) var plannedRoutines: [Routine] = []
    private(set) var nextScheduledRoutineSet: RoutineSet?
    private(set) var nextScheduledStartDate: Date?
    private(set) var selectedRoutineID: UUID?
    private(set) var page: ChildRoutinePage = .focus
    private(set) var settings: AppSettings?
    private(set) var completedRoutineIDs: Set<UUID> = []
    private(set) var completionFeedbackRoutineID: UUID?
    private(set) var undoRoutineID: UUID?
    private(set) var today: String

    init(
        repository: any RoutineRepository,
        speechGuide: (any RoutineSpeechGuiding)? = nil,
        feedbackPerformer: (any RoutineFeedbackPerforming)? = nil,
        notificationScheduler: (any RoutineNotificationScheduling)? = nil,
        now: @escaping () -> Date = Date.init,
        calendar: Calendar = .current,
        locale: @escaping () -> Locale = { .autoupdatingCurrent },
        isNotificationSchedulingEnabled: Bool = true
    ) {
        self.repository = repository
        self.speechGuide = speechGuide ?? NoopRoutineSpeechGuide()
        self.feedbackPerformer = feedbackPerformer ?? NoopRoutineFeedbackPerformer()
        self.notificationScheduler = notificationScheduler ?? NoopRoutineNotificationScheduler()
        self.now = now
        self.calendar = calendar
        self.locale = locale
        self.isNotificationSchedulingEnabled = isNotificationSchedulingEnabled
        today = DailyLog.localDateString(for: now(), calendar: calendar)
    }

    static func preview(repository: any RoutineRepository) -> ChildRoutineViewModel {
        ChildRoutineViewModel(
            repository: repository,
            notificationScheduler: NoopRoutineNotificationScheduler(),
            isNotificationSchedulingEnabled: false
        )
    }

    var currentRoutine: Routine? {
        routines.first { !completedRoutineIDs.contains($0.id) }
    }

    var selectedRoutine: Routine? {
        if let completionFeedbackRoutineID {
            return routines.first { $0.id == completionFeedbackRoutineID }
        }
        guard let selectedRoutineID else { return currentRoutine }
        return routines.first { $0.id == selectedRoutineID } ?? currentRoutine
    }

    var completedCount: Int { routines.filter { completedRoutineIDs.contains($0.id) }.count }
    var totalCount: Int { routines.count }
    var isAllCompleted: Bool {
        !plannedRoutines.isEmpty
            && plannedRoutines.allSatisfy { completedRoutineIDs.contains($0.id) }
    }
    var isWaitingForNextRoutineSet: Bool {
        activeRoutineSet == nil && nextScheduledRoutineSet != nil
    }
    var isShowingCompletionFeedback: Bool { completionFeedbackRoutineID != nil }
    var canUndoCompletion: Bool { undoRoutineID != nil }
    var undoDurationSeconds: Int { settings?.undoDurationSeconds ?? 5 }

    var nextRoutineAfterFeedback: Routine? {
        guard let completionFeedbackRoutineID,
              let completedRoutine = routines.first(where: { $0.id == completionFeedbackRoutineID }) else {
            return nil
        }
        return routines.first {
            $0.order > completedRoutine.order && !completedRoutineIDs.contains($0.id)
        }
    }

    var nextRoutineSetAfterFeedback: (routineSet: RoutineSet, firstRoutine: Routine)? {
        guard let activeRoutineSet,
              routines.allSatisfy({ completedRoutineIDs.contains($0.id) }),
              let currentIndex = plannedRoutineSets.firstIndex(where: { $0.id == activeRoutineSet.id })
        else {
            return nil
        }
        for routineSet in plannedRoutineSets.dropFirst(currentIndex + 1) {
            if let firstRoutine = plannedRoutines.first(where: {
                $0.routineSetID == routineSet.id && !completedRoutineIDs.contains($0.id)
            }) {
                return (routineSet, firstRoutine)
            }
        }
        return nil
    }

    var canStartNextRoutineSetAfterFeedback: Bool {
        guard let next = nextRoutineSetAfterFeedback,
              let startTime = next.routineSet.dailyStartTime,
              let startDate = scheduledDate(for: startTime) else {
            return nextRoutineSetAfterFeedback != nil
        }
        return startDate <= now()
    }

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            scheduledTransitionTask?.cancel()
            today = DailyLog.localDateString(for: now(), calendar: calendar)
            settings = try repository.appSettings()
            let plan = try routineSetsForToday()
            guard !plan.isEmpty else {
                reset(to: .empty)
                return
            }

            var fetchedRoutines: [Routine] = []
            for routineSet in plan {
                fetchedRoutines.append(contentsOf: try repository.routines(in: routineSet.id))
            }
            guard !fetchedRoutines.isEmpty else {
                plannedRoutineSets = plan
                resetRoutines(to: .empty)
                return
            }

            let dailyLogs = try repository.dailyLogs(on: today, routineSetID: nil)
            plannedRoutineSets = plan
            plannedRoutines = fetchedRoutines
            completedRoutineIDs = Set(
                dailyLogs
                    .filter { $0.status == .completed }
                    .map(\.routineID)
            )
            completionFeedbackRoutineID = nil
            resolveCurrentRoutineSet(at: now())
            loadState = .loaded
            applyPendingNotificationRouteIfNeeded()
            speakSelectedRoutineIfNeeded()
            requestNotificationAuthorizationAndRescheduleIfNeeded()
            scheduleAutomaticTransitionIfNeeded()
        } catch {
            reset(to: .failed)
        }
    }

    private func routineSetsForToday() throws -> [RoutineSet] {
        let scheduledSets = try repository.routineSets()
            .filter { $0.dailyStartTime != nil }
            .sorted(by: routineSetScheduleSort)
        if !scheduledSets.isEmpty {
            return scheduledSets
        }

        guard let assignment = try repository.dailyRoutineAssignment(on: today),
              let assignedSet = try repository.routineSet(id: assignment.routineSetID),
              assignedSet.deletedAt == nil else {
            return []
        }
        return [assignedSet]
    }

    private func resolveCurrentRoutineSet(at date: Date) {
        activeRoutineSet = nil
        routines = []
        nextScheduledRoutineSet = nil
        nextScheduledStartDate = nil

        for routineSet in plannedRoutineSets {
            let setRoutines = plannedRoutines.filter { $0.routineSetID == routineSet.id }
            guard !setRoutines.isEmpty,
                  setRoutines.contains(where: { !completedRoutineIDs.contains($0.id) }) else {
                continue
            }

            if let startTime = routineSet.dailyStartTime,
               let startDate = scheduledDate(for: startTime),
               startDate > date {
                nextScheduledRoutineSet = routineSet
                nextScheduledStartDate = startDate
                selectedRoutineID = nil
                return
            }

            activeRoutineSet = routineSet
            routines = setRoutines
            if !setRoutines.contains(where: { $0.id == selectedRoutineID })
                || selectedRoutineID.map(completedRoutineIDs.contains) == true {
                selectedRoutineID = currentRoutine?.id
            }
            return
        }

        if let lastSet = plannedRoutineSets.last {
            activeRoutineSet = lastSet
            routines = plannedRoutines.filter { $0.routineSetID == lastSet.id }
        }
        selectedRoutineID = nil
    }

    func selectRoutine(_ routine: Routine, showFocus: Bool) {
        guard routines.contains(where: { $0.id == routine.id }) else { return }
        selectedRoutineID = routine.id
        if showFocus {
            page = .focus
            speakSelectedRoutineIfNeeded()
        }
    }

    func showList() {
        page = .list
        speechGuide.stop()
    }

    func showFocus() {
        if completionFeedbackRoutineID == nil {
            selectedRoutineID = currentRoutine?.id
        }
        page = .focus
        speakSelectedRoutineIfNeeded()
    }

    func setRoutineSpeechActive(_ isActive: Bool) {
        isRoutineSpeechActive = isActive
        if !isActive {
            speechGuide.stop()
        }
    }

    func cardState(for routine: Routine) -> RoutineCardState {
        if routine.id == completionFeedbackRoutineID || completedRoutineIDs.contains(routine.id) {
            return .completed
        }
        if isShowingCompletionFeedback {
            return .upcoming
        }
        return routine.id == currentRoutine?.id ? .current : .upcoming
    }

    func completeSelectedRoutine() {
        guard let routine = selectedRoutine,
              routine.id == currentRoutine?.id,
              !completedRoutineIDs.contains(routine.id),
              let activeRoutineSet else {
            return
        }

        do {
            let completedAt = now()
            _ = try repository.setRoutineCompleted(
                routineID: routine.id,
                routineSetID: activeRoutineSet.id,
                on: today,
                at: completedAt
            )
            settings = try repository.appSettings()
            completedRoutineIDs.insert(routine.id)
            selectedRoutineID = routine.id
            completionFeedbackRoutineID = routine.id
            undoRoutineID = routine.id
            page = .focus

            if let settings {
                feedbackPerformer.routineCompleted(settings: settings)
                speechGuide.speak(localizedCompletionSpeech(for: routine), settings: settings)
            }
            scheduleAutomaticFeedbackAdvance()
            rescheduleNotifications()
        } catch {
            reset(to: .failed)
        }
    }

    func undoLastCompletion() {
        guard let undoRoutineID else { return }
        feedbackAdvanceTask?.cancel()
        do {
            _ = try repository.undoRoutineCompletion(
                routineID: undoRoutineID,
                on: today,
                at: now()
            )
            completedRoutineIDs.remove(undoRoutineID)
            completionFeedbackRoutineID = nil
            selectedRoutineID = undoRoutineID
            self.undoRoutineID = nil
            page = .focus
            speakSelectedRoutineIfNeeded()
            rescheduleNotifications()
        } catch {
            reset(to: .failed)
        }
    }

    func proceedAfterCompletionFeedback() {
        guard completionFeedbackRoutineID != nil else { return }
        feedbackAdvanceTask?.cancel()
        completionFeedbackRoutineID = nil
        undoRoutineID = nil

        if isAllCompleted {
            selectedRoutineID = nil
            if let settings {
                feedbackPerformer.allRoutinesCompleted(settings: settings)
                speechGuide.speak(localizedAllDoneSpeech, settings: settings)
            }
        } else if routines.allSatisfy({ completedRoutineIDs.contains($0.id) }) {
            load()
        } else {
            selectedRoutineID = currentRoutine?.id
            speakSelectedRoutineIfNeeded()
        }
        rescheduleNotifications()
    }

    func appDidBecomeActive() {
        guard loadState == .loaded else { return }
        let currentDate = DailyLog.localDateString(for: now(), calendar: calendar)
        if currentDate == today, !isWaitingForNextRoutineSet {
            rescheduleNotifications()
        } else {
            load()
        }
    }

    func openNotificationRoute(_ route: RoutineNotificationRoute) {
        if loadState != .loaded {
            pendingNotificationRoute = route
            return
        }
        applyNotificationRoute(route)
    }

    private func reset(to state: ChildRoutineLoadState) {
        scheduledTransitionTask?.cancel()
        feedbackAdvanceTask?.cancel()
        activeRoutineSet = nil
        plannedRoutineSets = []
        plannedRoutines = []
        nextScheduledRoutineSet = nil
        nextScheduledStartDate = nil
        resetRoutines(to: state)
    }

    private func resetRoutines(to state: ChildRoutineLoadState) {
        routines = []
        selectedRoutineID = nil
        page = .focus
        completedRoutineIDs = []
        completionFeedbackRoutineID = nil
        undoRoutineID = nil
        loadState = state
    }

    private func speakSelectedRoutineIfNeeded() {
        guard isRoutineSpeechActive,
              page == .focus,
              !isAllCompleted,
              completionFeedbackRoutineID == nil,
              let selectedRoutine,
              cardState(for: selectedRoutine) == .current,
              let settings else {
            return
        }
        speechGuide.speak(localizedTitle(for: selectedRoutine), settings: settings)
    }

    private func applyPendingNotificationRouteIfNeeded() {
        guard let pendingNotificationRoute else { return }
        self.pendingNotificationRoute = nil
        applyNotificationRoute(pendingNotificationRoute)
    }

    private func applyNotificationRoute(_ route: RoutineNotificationRoute) {
        guard route.date == today,
              activeRoutineSet?.id == route.routineSetID,
              routines.contains(where: { $0.id == route.routineID }) else {
            return
        }
        completionFeedbackRoutineID = nil
        undoRoutineID = nil
        selectedRoutineID = route.routineID
        page = .focus
        speakSelectedRoutineIfNeeded()
    }

    private func requestNotificationAuthorizationAndRescheduleIfNeeded() {
        guard isNotificationSchedulingEnabled else { return }
        guard !didRequestNotificationAuthorization else {
            rescheduleNotifications()
            return
        }
        didRequestNotificationAuthorization = true
        Task { [weak self, notificationScheduler] in
            _ = await notificationScheduler.requestAuthorizationIfNeeded()
            await MainActor.run {
                self?.rescheduleNotifications()
            }
        }
    }

    private func rescheduleNotifications() {
        guard isNotificationSchedulingEnabled else { return }
        guard let settings else { return }
        let routines = plannedRoutines
        let completedRoutineIDs = completedRoutineIDs
        let date = today
        let now = now()
        let calendar = calendar
        let locale = locale()
        Task { [notificationScheduler] in
            await notificationScheduler.rescheduleTodayReminders(
                routines: routines,
                completedRoutineIDs: completedRoutineIDs,
                date: date,
                settings: settings,
                now: now,
                calendar: calendar,
                locale: locale
            )
        }
    }

    private func scheduleAutomaticTransitionIfNeeded() {
        scheduledTransitionTask?.cancel()
        guard let nextScheduledStartDate else { return }
        let delay = nextScheduledStartDate.timeIntervalSince(now())
        guard delay > 0 else {
            load()
            return
        }
        scheduledTransitionTask = Task { [weak self] in
            let nanoseconds = UInt64(min(delay, 86_400) * 1_000_000_000)
            try? await Task.sleep(nanoseconds: nanoseconds)
            guard !Task.isCancelled else { return }
            self?.load()
        }
    }

    private func scheduleAutomaticFeedbackAdvance() {
        feedbackAdvanceTask?.cancel()
        let delay = UInt64(max(undoDurationSeconds, 1)) * 1_000_000_000
        feedbackAdvanceTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: delay)
            guard !Task.isCancelled else { return }
            self?.proceedAfterCompletionFeedback()
        }
    }

    private func scheduledDate(for time: LocalTime) -> Date? {
        var components = calendar.dateComponents([.year, .month, .day], from: now())
        components.hour = time.hour
        components.minute = time.minute
        components.second = 0
        return calendar.date(from: components)
    }

    private func routineSetScheduleSort(_ lhs: RoutineSet, _ rhs: RoutineSet) -> Bool {
        guard let lhsTime = lhs.dailyStartTime, let rhsTime = rhs.dailyStartTime else {
            return lhs.dailyStartTime != nil
        }
        if lhsTime == rhsTime { return lhs.createdAt < rhs.createdAt }
        return (lhsTime.hour, lhsTime.minute) < (rhsTime.hour, rhsTime.minute)
    }

    private func localizedTitle(for routine: Routine) -> String {
        let localeIdentifier = locale().identifier
        return routine.title.resolved(
            appLocale: localeIdentifier,
            systemLanguages: [localeIdentifier]
        )
    }

    private var localizedAllDoneSpeech: String {
        locale().language.languageCode?.identifier == "en"
            ? "All routines are complete."
            : "오늘 할 일을 모두 끝냈어요."
    }

    private func localizedCompletionSpeech(for routine: Routine) -> String {
        let title = localizedTitle(for: routine)
        return locale().language.languageCode?.identifier == "en"
            ? "\(title) done. Great job!"
            : "\(title) 완료! 잘했어요!"
    }
}
