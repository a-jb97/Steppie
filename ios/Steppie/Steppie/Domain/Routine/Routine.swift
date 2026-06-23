import Foundation

nonisolated struct Routine: Codable, Equatable, Identifiable, Sendable {
    static let defaultColorToken = "color.card.sky"
    static let allowedColorTokens: Set<String> = [
        "color.card.sky",
        "color.card.mint",
        "color.card.lemon",
        "color.card.peach",
        "color.card.lavender",
        "color.card.rose",
    ]

    let id: UUID
    let routineSetID: UUID
    let titleKey: String?
    let title: LocalizedText
    let icon: IconRef
    let colorToken: String
    let order: Int
    let scheduledTime: LocalTime?
    let isActive: Bool
    let createdAt: Date
    let updatedAt: Date
    let deletedAt: Date?

    init(
        id: UUID = UUID(),
        routineSetID: UUID,
        titleKey: String? = nil,
        title: LocalizedText,
        icon: IconRef,
        colorToken: String = Routine.defaultColorToken,
        order: Int,
        scheduledTime: LocalTime? = nil,
        isActive: Bool = true,
        createdAt: Date = .now,
        updatedAt: Date = .now,
        deletedAt: Date? = nil
    ) throws {
        self.id = id
        self.routineSetID = routineSetID
        self.titleKey = titleKey
        self.title = title
        self.icon = icon
        self.colorToken = colorToken
        self.order = order
        self.scheduledTime = scheduledTime
        self.isActive = isActive
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
        try validate()
    }

    func validate() throws {
        guard id.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "Routine.id")
        }
        guard routineSetID.isVersion4 else {
            throw RoutineDomainError.uuidMustBeVersion4(field: "Routine.routineSetId")
        }
        guard Self.allowedColorTokens.contains(colorToken) else {
            throw RoutineDomainError.invalidColorToken(colorToken)
        }
        guard order >= 0 else {
            throw RoutineDomainError.invalidOrder(order)
        }
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
    }
}
