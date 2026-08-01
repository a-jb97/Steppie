import Foundation

struct ChildRoutineNotificationSnapshot {
    let routines: [Routine]
    let completedRoutineIDs: Set<UUID>
    let date: String
    let settings: AppSettings
    let now: Date
    let calendar: Calendar
    let locale: Locale
}

@MainActor
final class ChildRoutineNotificationCoordinator {
    private let scheduler: any RoutineNotificationScheduling
    private let isSchedulingEnabled: Bool
    private var didRequestAuthorization = false

    init(
        scheduler: any RoutineNotificationScheduling,
        isSchedulingEnabled: Bool
    ) {
        self.scheduler = scheduler
        self.isSchedulingEnabled = isSchedulingEnabled
    }

    func requestAuthorizationAndRescheduleIfNeeded(
        currentSnapshot: @escaping @MainActor () -> ChildRoutineNotificationSnapshot?
    ) {
        guard isSchedulingEnabled else { return }
        guard !didRequestAuthorization else {
            reschedule(currentSnapshot())
            return
        }

        didRequestAuthorization = true
        Task { [weak self, scheduler] in
            _ = await scheduler.requestAuthorizationIfNeeded()
            self?.reschedule(currentSnapshot())
        }
    }

    func reschedule(_ snapshot: ChildRoutineNotificationSnapshot?) {
        guard isSchedulingEnabled, let snapshot else { return }
        Task { [scheduler] in
            await scheduler.rescheduleTodayReminders(
                routines: snapshot.routines,
                completedRoutineIDs: snapshot.completedRoutineIDs,
                date: snapshot.date,
                settings: snapshot.settings,
                now: snapshot.now,
                calendar: snapshot.calendar,
                locale: snapshot.locale
            )
        }
    }
}
