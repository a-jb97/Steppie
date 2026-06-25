import Foundation

nonisolated enum LogStatus: String, Codable, Equatable, Sendable {
    case completed
    case undone
}

nonisolated struct DailyLog: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let date: String
    let routineID: UUID
    let routineSetID: UUID
    let status: LogStatus
    let completedAt: Date?
    let createdAt: Date
    let updatedAt: Date

    init(
        id: UUID = UUID(),
        date: String,
        routineID: UUID,
        routineSetID: UUID,
        status: LogStatus = .undone,
        completedAt: Date? = nil,
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) throws {
        self.id = id
        self.date = date
        self.routineID = routineID
        self.routineSetID = routineSetID
        self.status = status
        self.completedAt = completedAt
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        try validate()
    }

    func validate() throws {
        guard id.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "DailyLog.id")
        }
        guard routineID.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "DailyLog.routineId")
        }
        guard routineSetID.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "DailyLog.routineSetId")
        }
        guard Self.isValidLocalDate(date) else {
            throw RoutineDomainError.invalidLocalDate(date)
        }
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
        switch status {
        case .completed where completedAt == nil:
            throw RoutineDomainError.completedLogRequiresCompletedAt
        case .undone where completedAt != nil:
            throw RoutineDomainError.undoneLogRequiresNoCompletedAt
        default:
            break
        }
    }

    static func localDateString(for date: Date, calendar: Calendar = .current) -> String {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        let year = components.year ?? 0
        let month = components.month ?? 0
        let day = components.day ?? 0
        return String(format: "%04d-%02d-%02d", year, month, day)
    }

    static func isValidLocalDate(_ value: String) -> Bool {
        let pattern = #"^\d{4}-\d{2}-\d{2}$"#
        guard value.range(of: pattern, options: .regularExpression) != nil else { return false }

        let formatter = DateFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        guard let parsed = formatter.date(from: value) else { return false }
        return formatter.string(from: parsed) == value
    }
}
