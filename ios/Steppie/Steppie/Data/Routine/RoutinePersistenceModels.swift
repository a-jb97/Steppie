import Foundation
import SwiftData

@Model
final class RoutineSetRecord {
    @Attribute(.unique) var id: UUID
    var nameData: Data
    var isActive: Bool
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?

    init(
        id: UUID,
        nameData: Data,
        isActive: Bool,
        createdAt: Date,
        updatedAt: Date,
        deletedAt: Date?
    ) {
        self.id = id
        self.nameData = nameData
        self.isActive = isActive
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
    }
}

@Model
final class RoutineRecord {
    @Attribute(.unique) var id: UUID
    var routineSetID: UUID
    var titleKey: String?
    var titleData: Data
    var iconData: Data
    var colorToken: String
    var order: Int
    var scheduledTime: String?
    var isActive: Bool
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?

    init(
        id: UUID,
        routineSetID: UUID,
        titleKey: String?,
        titleData: Data,
        iconData: Data,
        colorToken: String,
        order: Int,
        scheduledTime: String?,
        isActive: Bool,
        createdAt: Date,
        updatedAt: Date,
        deletedAt: Date?
    ) {
        self.id = id
        self.routineSetID = routineSetID
        self.titleKey = titleKey
        self.titleData = titleData
        self.iconData = iconData
        self.colorToken = colorToken
        self.order = order
        self.scheduledTime = scheduledTime
        self.isActive = isActive
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        self.deletedAt = deletedAt
    }
}

enum RoutinePersistenceSchema {
    static let models: [any PersistentModel.Type] = [
        RoutineSetRecord.self,
        RoutineRecord.self,
    ]

    static var schema: Schema {
        Schema(models)
    }
}
