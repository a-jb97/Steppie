import Foundation
import Observation
import UserNotifications

nonisolated struct RoutineNotificationRoute: Equatable, Sendable {
    let routineID: UUID
    let routineSetID: UUID
    let date: String

    init?(userInfo: [AnyHashable: Any]) {
        guard let routineIDValue = userInfo["routineID"] as? String,
              let routineSetIDValue = userInfo["routineSetID"] as? String,
              let routineID = UUID(uuidString: routineIDValue),
              let routineSetID = UUID(uuidString: routineSetIDValue),
              let date = userInfo["date"] as? String,
              DailyLog.isValidLocalDate(date) else {
            return nil
        }

        self.routineID = routineID
        self.routineSetID = routineSetID
        self.date = date
    }

    init(routineID: UUID, routineSetID: UUID, date: String) {
        self.routineID = routineID
        self.routineSetID = routineSetID
        self.date = date
    }
}

@MainActor
@Observable
final class RoutineNotificationRouter {
    private(set) var pendingRoute: RoutineNotificationRoute?

    func open(_ route: RoutineNotificationRoute) {
        pendingRoute = route
    }

    func consumePendingRoute() -> RoutineNotificationRoute? {
        defer { pendingRoute = nil }
        return pendingRoute
    }
}

@MainActor
final class RoutineNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    private let router: RoutineNotificationRouter

    init(router: RoutineNotificationRouter) {
        self.router = router
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        guard let route = RoutineNotificationRoute(
            userInfo: response.notification.request.content.userInfo
        ) else { return }

        await router.open(route)
    }
}
