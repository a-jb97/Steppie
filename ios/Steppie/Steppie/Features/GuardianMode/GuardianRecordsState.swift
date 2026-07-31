import Foundation
import Observation

struct GuardianRecordSummary: Equatable, Identifiable {
    let id: String
    let date: String
    let weekdaySymbol: String
    let completedCount: Int
    let totalCount: Int

    var remainingCount: Int { max(totalCount - completedCount, 0) }

    var completionRatio: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount)
    }

    var percentage: Int { Int((completionRatio * 100).rounded()) }

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

    var isCompleted: Bool { status == .completed }
    var statusText: String { isCompleted ? "완료" : "미완료" }

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

    var isEmpty: Bool { totalCount == 0 && rows.isEmpty }

    var completionRatio: Double {
        guard totalCount > 0 else { return 0 }
        return Double(completedCount) / Double(totalCount)
    }

    var percentage: Int { Int((completionRatio * 100).rounded()) }
}

@MainActor
@Observable
final class GuardianRecordsState {
    private let repository: any RoutineRepository
    private let now: () -> Date
    private let calendar: Calendar

    private(set) var summaries: [GuardianRecordSummary] = []
    private(set) var calendarDates: Set<String> = []
    private(set) var selectedDate: String?
    private(set) var selectedDetail: GuardianRecordDetail?
    private(set) var selectedCalendarDate: String?
    private(set) var selectedCalendarDetail: GuardianRecordDetail?

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date,
        calendar: Calendar
    ) {
        self.repository = repository
        self.now = now
        self.calendar = calendar
    }

    func refresh() {
        refreshCalendarDates()
        let dates = recentRecordDates()
        if selectedDate.map({ !dates.contains($0) }) != false {
            selectedDate = dates.first
        }
        summaries = dates.map { date in
            (try? makeRecordSummary(for: date)) ?? GuardianRecordSummary(
                id: date,
                date: date,
                weekdaySymbol: weekdaySymbol(for: date),
                completedCount: 0,
                totalCount: 0
            )
        }
        refreshSelectedDetail()
        if selectedCalendarDate.map({ !calendarDates.contains($0) }) == true {
            selectedCalendarDate = calendarDates.sorted(by: >).first
        }
        refreshSelectedCalendarDetail()
    }

    func selectDate(_ date: String) {
        guard summaries.contains(where: { $0.date == date }) else { return }
        selectedDate = date
        refreshSelectedDetail()
    }

    func prepareCalendar() {
        refreshCalendarDates()
        selectedCalendarDate = DailyLog.localDateString(for: now(), calendar: calendar)
        refreshSelectedCalendarDetail()
    }

    func selectCalendarDate(_ date: String) {
        guard calendarDates.contains(date) else { return }
        selectedCalendarDate = date
        refreshSelectedCalendarDetail()
    }

    private func refreshSelectedDetail() {
        guard let selectedDate else {
            selectedDetail = nil
            return
        }
        selectedDetail = try? makeRecordDetail(for: selectedDate)
    }

    private func refreshCalendarDates() {
        var dates = Set((try? repository.dailyLogDates()) ?? [])
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        if (try? routineSetsRepresented(on: today).isEmpty) == false {
            dates.insert(today)
        }
        calendarDates = dates
    }

    private func refreshSelectedCalendarDetail() {
        guard let selectedCalendarDate else {
            selectedCalendarDetail = nil
            return
        }
        guard calendarDates.contains(selectedCalendarDate) else {
            selectedCalendarDetail = nil
            return
        }
        selectedCalendarDetail = try? makeRecordDetail(for: selectedCalendarDate)
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
            let routineSets = try routineSetsRepresented(on: date)
            return try routineSets.flatMap { try repository.routines(in: $0.id) }
                .filter { routineExisted($0, on: date) }
                .sorted(by: routineRecordSort)
        }

        var routineSetIDs = Set(logs.map(\.routineSetID))
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        if date == today {
            routineSetIDs.formUnion(try routineSetsRepresented(on: date).map(\.id))
        }
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

    private func routineSetsRepresented(on date: String) throws -> [RoutineSet] {
        let today = DailyLog.localDateString(for: now(), calendar: calendar)
        if date == today {
            let scheduledSets = try repository.routineSets()
                .filter { $0.dailyStartTime != nil }
                .sorted {
                    guard let lhs = $0.dailyStartTime, let rhs = $1.dailyStartTime else {
                        return $0.dailyStartTime != nil
                    }
                    if lhs == rhs { return $0.createdAt < $1.createdAt }
                    return (lhs.hour, lhs.minute) < (rhs.hour, rhs.minute)
                }
            if !scheduledSets.isEmpty {
                return scheduledSets
            }
        }

        if let assignment = try repository.dailyRoutineAssignment(on: date),
           let assignedSet = try repository.routineSet(id: assignment.routineSetID),
           assignedSet.deletedAt == nil {
            return [assignedSet]
        }

        return []
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
        let components = Self.localDateFormatter.calendar.dateComponents(
            [.year, .month, .day],
            from: parsedDate
        )
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

    private func localizedTitle(for routine: Routine) -> String {
        routine.title.resolved(
            appLocale: Locale.autoupdatingCurrent.identifier,
            systemLanguages: Locale.preferredLanguages
        )
    }

    private static let localDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }()
}
