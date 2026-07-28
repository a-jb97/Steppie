import Foundation

nonisolated struct RoutineSet: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let name: LocalizedText
    let isActive: Bool
    let dailyStartTime: LocalTime?
    let createdAt: Date
    let updatedAt: Date
    let deletedAt: Date?

    init(
        id: UUID = UUID(),
        name: LocalizedText,
        isActive: Bool = false,
        dailyStartTime: LocalTime? = nil,
        createdAt: Date = .now,
        updatedAt: Date = .now,
        deletedAt: Date? = nil
    ) throws {
        self.id = id
        self.name = name
        self.isActive = isActive
        self.dailyStartTime = dailyStartTime
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        try validate()
    }

    func validate() throws {
        guard id.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "RoutineSet.id")
        }
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
        if isActive && deletedAt != nil {
            throw RoutineDomainError.activeRoutineSetCannotBeDeleted
        }
    }
}
