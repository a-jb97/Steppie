import Foundation

nonisolated struct DailyRoutineAssignment: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let date: String
    let routineSetID: UUID
    let createdAt: Date
    let updatedAt: Date

    init(
        id: UUID = UUID(),
        date: String,
        routineSetID: UUID,
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) throws {
        self.id = id
        self.date = date
        self.routineSetID = routineSetID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        try validate()
    }

    func validate() throws {
        guard id.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "DailyRoutineAssignment.id")
        }
        guard DailyLog.isValidLocalDate(date) else {
            throw RoutineDomainError.invalidLocalDate(date)
        }
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
    }
}
