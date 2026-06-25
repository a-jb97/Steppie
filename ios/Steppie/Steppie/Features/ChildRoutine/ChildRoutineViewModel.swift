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

    private(set) var loadState: ChildRoutineLoadState = .idle
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var routines: [Routine] = []
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
    var isAllCompleted: Bool { totalCount > 0 && completedCount == totalCount }
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

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            today = DailyLog.localDateString(for: now(), calendar: calendar)
            settings = try repository.appSettings()
            guard let activeSet = try repository.routineSets().first(where: \.isActive) else {
                reset(to: .empty)
                return
            }

            let fetchedRoutines = try repository.routines(in: activeSet.id)
            let dailyLogs = try repository.dailyLogs(on: today, routineSetID: activeSet.id)
            guard !fetchedRoutines.isEmpty else {
                activeRoutineSet = activeSet
                resetRoutines(to: .empty)
                return
            }

            activeRoutineSet = activeSet
            routines = fetchedRoutines
            completedRoutineIDs = Set(
                dailyLogs
                    .filter { $0.status == .completed }
                    .map(\.routineID)
            )
            completionFeedbackRoutineID = nil
            if !fetchedRoutines.contains(where: { $0.id == selectedRoutineID })
                || selectedRoutineID.map(completedRoutineIDs.contains) == true {
                selectedRoutineID = currentRoutine?.id
            }
            loadState = .loaded
            applyPendingNotificationRouteIfNeeded()
            speakSelectedRoutineIfNeeded()
            requestNotificationAuthorizationAndRescheduleIfNeeded()
        } catch {
            reset(to: .failed)
        }
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
    }

    func showFocus() {
        page = .focus
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
            }
            rescheduleNotifications()
        } catch {
            reset(to: .failed)
        }
    }

    func undoLastCompletion() {
        guard let undoRoutineID else { return }
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
        completionFeedbackRoutineID = nil
        undoRoutineID = nil

        if isAllCompleted {
            selectedRoutineID = nil
            if let settings {
                feedbackPerformer.allRoutinesCompleted(settings: settings)
                speechGuide.speak(localizedAllDoneSpeech, settings: settings)
            }
        } else {
            selectedRoutineID = currentRoutine?.id
            speakSelectedRoutineIfNeeded()
        }
        rescheduleNotifications()
    }

    func appDidBecomeActive() {
        guard loadState == .loaded else { return }
        let currentDate = DailyLog.localDateString(for: now(), calendar: calendar)
        if currentDate == today {
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
        activeRoutineSet = nil
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
        guard !isAllCompleted,
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
        guard let activeRoutineSet, let settings else { return }
        let routines = routines
        let completedRoutineIDs = completedRoutineIDs
        let routineSetID = activeRoutineSet.id
        let date = today
        let now = now()
        let calendar = calendar
        let locale = locale()
        Task { [notificationScheduler] in
            await notificationScheduler.rescheduleTodayReminders(
                routines: routines,
                completedRoutineIDs: completedRoutineIDs,
                routineSetID: routineSetID,
                date: date,
                settings: settings,
                now: now,
                calendar: calendar,
                locale: locale
            )
        }
    }

    private func localizedTitle(for routine: Routine) -> String {
        let localeIdentifier = Locale.autoupdatingCurrent.identifier
        return routine.title.resolved(
            appLocale: localeIdentifier,
            systemLanguages: Locale.preferredLanguages
        )
    }

    private var localizedAllDoneSpeech: String {
        Locale.autoupdatingCurrent.language.languageCode?.identifier == "en"
            ? "All routines are complete."
            : "오늘 할 일을 모두 끝냈어요."
    }
}
