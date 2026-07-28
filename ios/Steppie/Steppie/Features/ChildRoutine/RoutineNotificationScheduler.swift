import Foundation
import UserNotifications

enum RoutineNotificationAuthorizationStatus: Equatable, Sendable {
    case notDetermined
    case authorized
    case denied
    case provisional
    case ephemeral
    case unknown
}

struct RoutineNotificationRequest: Equatable, Sendable {
    let id: String
    let routineID: UUID
    let routineSetID: UUID
    let date: String
    let fireDate: Date
    let title: String
    let body: String
}

@MainActor
protocol RoutineNotificationScheduling {
    func requestAuthorizationIfNeeded() async -> RoutineNotificationAuthorizationStatus
    func rescheduleTodayReminders(
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        date: String,
        settings: AppSettings,
        now: Date,
        calendar: Calendar,
        locale: Locale
    ) async
}

struct NoopRoutineNotificationScheduler: RoutineNotificationScheduling {
    init() {}

    func requestAuthorizationIfNeeded() async -> RoutineNotificationAuthorizationStatus {
        .authorized
    }

    func rescheduleTodayReminders(
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        date: String,
        settings: AppSettings,
        now: Date,
        calendar: Calendar,
        locale: Locale
    ) async {}
}

@MainActor
final class IOSRoutineNotificationScheduler: RoutineNotificationScheduling {
    private let center: UNUserNotificationCenter
    private let identifierPrefix = "steppie.routineReminder"

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func requestAuthorizationIfNeeded() async -> RoutineNotificationAuthorizationStatus {
        let settings = await center.notificationSettings()
        let status = RoutineNotificationAuthorizationStatus(settings.authorizationStatus)
        guard status == .notDetermined else { return status }

        do {
            let granted = try await center.requestAuthorization(options: [.alert, .sound, .badge])
            return granted ? .authorized : .denied
        } catch {
            return .denied
        }
    }

    func rescheduleTodayReminders(
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        date: String,
        settings: AppSettings,
        now: Date,
        calendar: Calendar,
        locale: Locale
    ) async {
        await cancelSteppieReminders()
        let authorizationStatus = RoutineNotificationAuthorizationStatus(
            (await center.notificationSettings()).authorizationStatus
        )
        guard authorizationStatus.allowsScheduling else { return }

        for request in Self.notificationRequests(
            routines: routines,
            completedRoutineIDs: completedRoutineIDs,
            date: date,
            settings: settings,
            now: now,
            calendar: calendar,
            locale: locale,
            identifierPrefix: identifierPrefix
        ) {
            let content = UNMutableNotificationContent()
            content.title = request.title
            content.body = request.body
            content.sound = settings.soundEnabled ? .default : nil
            content.userInfo = [
                "routineID": request.routineID.uuidString,
                "routineSetID": request.routineSetID.uuidString,
                "date": request.date,
            ]

            let components = calendar.dateComponents(
                [.year, .month, .day, .hour, .minute],
                from: request.fireDate
            )
            let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
            let notificationRequest = UNNotificationRequest(
                identifier: request.id,
                content: content,
                trigger: trigger
            )
            try? await center.add(notificationRequest)
        }
    }

    static func notificationRequests(
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        date: String,
        settings: AppSettings,
        now: Date,
        calendar: Calendar,
        locale: Locale,
        identifierPrefix: String = "steppie.routineReminder"
    ) -> [RoutineNotificationRequest] {
        guard !settings.notificationLeadTimes.isEmpty else { return [] }

        return routines.flatMap { routine in
            guard !completedRoutineIDs.contains(routine.id),
                  let scheduledTime = routine.scheduledTime,
                  let scheduledDate = dateForRoutine(
                    date: date,
                    time: scheduledTime,
                    calendar: calendar
                  ) else {
                return [RoutineNotificationRequest]()
            }

            return settings.notificationLeadTimes.compactMap { leadTime in
                guard let fireDate = calendar.date(
                    byAdding: .minute,
                    value: -leadTime,
                    to: scheduledDate
                ),
                      fireDate > now,
                      !settings.isQuietTime(fireDate, calendar: calendar) else {
                    return nil
                }

                let localizedTitle = routine.title.resolved(
                    appLocale: locale.identifier,
                    systemLanguages: Locale.preferredLanguages
                )
                return RoutineNotificationRequest(
                    id: "\(identifierPrefix).\(date).\(routine.id.uuidString).\(leadTime)",
                    routineID: routine.id,
                    routineSetID: routine.routineSetID,
                    date: date,
                    fireDate: fireDate,
                    title: notificationTitle(locale: locale),
                    body: notificationBody(
                        routineTitle: localizedTitle,
                        leadTime: leadTime,
                        locale: locale
                    )
                )
            }
        }
    }

    private func cancelSteppieReminders() async {
        let pending = await center.pendingNotificationRequests()
        let identifiers = pending
            .map(\.identifier)
            .filter { $0.hasPrefix(identifierPrefix) }
        center.removePendingNotificationRequests(withIdentifiers: identifiers)
    }

    private static func dateForRoutine(
        date: String,
        time: LocalTime,
        calendar: Calendar
    ) -> Date? {
        let parts = date.split(separator: "-").compactMap { Int(String($0)) }
        guard parts.count == 3 else { return nil }
        var components = DateComponents()
        components.calendar = calendar
        components.year = parts[0]
        components.month = parts[1]
        components.day = parts[2]
        components.hour = time.hour
        components.minute = time.minute
        return calendar.date(from: components)
    }

    private static func notificationTitle(locale: Locale) -> String {
        locale.language.languageCode?.identifier == "en"
            ? "Steppie reminder"
            : "차례차례 알림"
    }

    private static func notificationBody(
        routineTitle: String,
        leadTime: Int,
        locale: Locale
    ) -> String {
        locale.language.languageCode?.identifier == "en"
            ? "\(routineTitle) starts in \(leadTime) minutes."
            : "\(leadTime)분 뒤 \(routineTitle)을 시작해요."
    }
}

private extension RoutineNotificationAuthorizationStatus {
    init(_ status: UNAuthorizationStatus) {
        switch status {
        case .notDetermined:
            self = .notDetermined
        case .denied:
            self = .denied
        case .authorized:
            self = .authorized
        case .provisional:
            self = .provisional
        case .ephemeral:
            self = .ephemeral
        @unknown default:
            self = .unknown
        }
    }

    var allowsScheduling: Bool {
        switch self {
        case .authorized, .provisional, .ephemeral:
            true
        case .notDetermined, .denied, .unknown:
            false
        }
    }
}

private extension AppSettings {
    func isQuietTime(_ date: Date, calendar: Calendar) -> Bool {
        guard let quietHoursStart, let quietHoursEnd else { return false }
        let components = calendar.dateComponents([.hour, .minute], from: date)
        guard let hour = components.hour, let minute = components.minute else { return false }
        let currentMinutes = hour * 60 + minute
        let startMinutes = quietHoursStart.hour * 60 + quietHoursStart.minute
        let endMinutes = quietHoursEnd.hour * 60 + quietHoursEnd.minute

        if startMinutes < endMinutes {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes
        }
        if startMinutes > endMinutes {
            return currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
        return true
    }
}
