import Foundation
import SwiftData

@Model
final class RoutineSetRecord {
    @Attribute(.unique) var id: UUID
    var nameData: Data
    var isActive: Bool
    var dailyStartTime: String?
    var createdAt: Date
    var updatedAt: Date
    var deletedAt: Date?

    init(
        id: UUID,
        nameData: Data,
        isActive: Bool,
        dailyStartTime: String? = nil,
        createdAt: Date,
        updatedAt: Date,
        deletedAt: Date?
    ) {
        self.id = id
        self.nameData = nameData
        self.isActive = isActive
        self.dailyStartTime = dailyStartTime
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

@Model
final class DailyLogRecord {
    @Attribute(.unique) var id: UUID
    var date: String
    var routineID: UUID
    var routineSetID: UUID
    var status: String
    var completedAt: Date?
    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID,
        date: String,
        routineID: UUID,
        routineSetID: UUID,
        status: String,
        completedAt: Date?,
        createdAt: Date,
        updatedAt: Date
    ) {
        self.id = id
        self.date = date
        self.routineID = routineID
        self.routineSetID = routineSetID
        self.status = status
        self.completedAt = completedAt
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

@Model
final class AppSettingsRecord {
    @Attribute(.unique) var id: String
    var guardianPinHash: String?
    var recoveryCodeHash: String?
    var feedbackIntensity: String
    var soundEnabled: Bool
    var ttsEnabled: Bool
    var ttsRate: Double
    var ttsVolume: Double
    var hapticEnabled: Bool
    var undoDurationSeconds: Int
    var notificationLeadTimes: String?
    var quietHoursStart: String?
    var quietHoursEnd: String?
    var locale: String?
    var createdAt: Date
    var updatedAt: Date

    init(
        id: String,
        guardianPinHash: String? = nil,
        recoveryCodeHash: String? = nil,
        feedbackIntensity: String,
        soundEnabled: Bool,
        ttsEnabled: Bool,
        ttsRate: Double,
        ttsVolume: Double,
        hapticEnabled: Bool,
        undoDurationSeconds: Int,
        notificationLeadTimes: String? = nil,
        quietHoursStart: String? = nil,
        quietHoursEnd: String? = nil,
        locale: String? = nil,
        createdAt: Date,
        updatedAt: Date
    ) {
        self.id = id
        self.guardianPinHash = guardianPinHash
        self.recoveryCodeHash = recoveryCodeHash
        self.feedbackIntensity = feedbackIntensity
        self.soundEnabled = soundEnabled
        self.ttsEnabled = ttsEnabled
        self.ttsRate = ttsRate
        self.ttsVolume = ttsVolume
        self.hapticEnabled = hapticEnabled
        self.undoDurationSeconds = undoDurationSeconds
        self.notificationLeadTimes = notificationLeadTimes
        self.quietHoursStart = quietHoursStart
        self.quietHoursEnd = quietHoursEnd
        self.locale = locale
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

@Model
final class DailyRoutineAssignmentRecord {
    @Attribute(.unique) var id: UUID
    var date: String
    var routineSetID: UUID
    var createdAt: Date
    var updatedAt: Date

    init(
        id: UUID,
        date: String,
        routineSetID: UUID,
        createdAt: Date,
        updatedAt: Date
    ) {
        self.id = id
        self.date = date
        self.routineSetID = routineSetID
        self.createdAt = createdAt
        self.updatedAt = updatedAt
    }
}

enum RoutinePersistenceSchema {
    static let models: [any PersistentModel.Type] = [
        RoutineSetRecord.self,
        RoutineRecord.self,
        DailyLogRecord.self,
        AppSettingsRecord.self,
        DailyRoutineAssignmentRecord.self,
    ]

    static var schema: Schema {
        Schema(models)
    }
}
