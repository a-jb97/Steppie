import Foundation

enum RoutinePersistenceMappingError: Error {
    case invalidStoredLocalTime(String)
    case invalidStoredLogStatus(String)
    case invalidStoredFeedbackIntensity(String)
}

extension RoutineSetRecord {
    convenience init(domain: RoutineSet) throws {
        self.init(
            id: domain.id,
            nameData: try RoutineDataCoder.encode(domain.name),
            isActive: domain.isActive,
            createdAt: domain.createdAt,
            updatedAt: domain.updatedAt,
            deletedAt: domain.deletedAt
        )
    }

    func domainModel() throws -> RoutineSet {
        try RoutineSet(
            id: id,
            name: RoutineDataCoder.decode(LocalizedText.self, from: nameData),
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            deletedAt: deletedAt
        )
    }

    func apply(_ domain: RoutineSet) throws {
        nameData = try RoutineDataCoder.encode(domain.name)
        isActive = domain.isActive
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
        deletedAt = domain.deletedAt
    }
}

extension RoutineRecord {
    convenience init(domain: Routine) throws {
        self.init(
            id: domain.id,
            routineSetID: domain.routineSetID,
            titleKey: domain.titleKey,
            titleData: try RoutineDataCoder.encode(domain.title),
            iconData: try RoutineDataCoder.encode(domain.icon),
            colorToken: domain.colorToken,
            order: domain.order,
            scheduledTime: domain.scheduledTime?.description,
            isActive: domain.isActive,
            createdAt: domain.createdAt,
            updatedAt: domain.updatedAt,
            deletedAt: domain.deletedAt
        )
    }

    func domainModel() throws -> Routine {
        let decodedTime: LocalTime?
        if let scheduledTime {
            do {
                decodedTime = try LocalTime(scheduledTime)
            } catch {
                throw RoutinePersistenceMappingError.invalidStoredLocalTime(scheduledTime)
            }
        } else {
            decodedTime = nil
        }

        return try Routine(
            id: id,
            routineSetID: routineSetID,
            titleKey: titleKey,
            title: RoutineDataCoder.decode(LocalizedText.self, from: titleData),
            icon: RoutineDataCoder.decode(IconRef.self, from: iconData),
            colorToken: colorToken,
            order: order,
            scheduledTime: decodedTime,
            isActive: isActive,
            createdAt: createdAt,
            updatedAt: updatedAt,
            deletedAt: deletedAt
        )
    }

    func apply(_ domain: Routine) throws {
        routineSetID = domain.routineSetID
        titleKey = domain.titleKey
        titleData = try RoutineDataCoder.encode(domain.title)
        iconData = try RoutineDataCoder.encode(domain.icon)
        colorToken = domain.colorToken
        order = domain.order
        scheduledTime = domain.scheduledTime?.description
        isActive = domain.isActive
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
        deletedAt = domain.deletedAt
    }
}

extension DailyLogRecord {
    convenience init(domain: DailyLog) {
        self.init(
            id: domain.id,
            date: domain.date,
            routineID: domain.routineID,
            routineSetID: domain.routineSetID,
            status: domain.status.rawValue,
            completedAt: domain.completedAt,
            createdAt: domain.createdAt,
            updatedAt: domain.updatedAt
        )
    }

    func domainModel() throws -> DailyLog {
        guard let decodedStatus = LogStatus(rawValue: status) else {
            throw RoutinePersistenceMappingError.invalidStoredLogStatus(status)
        }

        return try DailyLog(
            id: id,
            date: date,
            routineID: routineID,
            routineSetID: routineSetID,
            status: decodedStatus,
            completedAt: completedAt,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }

    func apply(_ domain: DailyLog) {
        date = domain.date
        routineID = domain.routineID
        routineSetID = domain.routineSetID
        status = domain.status.rawValue
        completedAt = domain.completedAt
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }
}

extension AppSettingsRecord {
    convenience init(domain: AppSettings) {
        self.init(
            id: domain.id,
            feedbackIntensity: domain.feedbackIntensity.rawValue,
            soundEnabled: domain.soundEnabled,
            ttsEnabled: domain.ttsEnabled,
            ttsRate: domain.ttsRate,
            ttsVolume: domain.ttsVolume,
            hapticEnabled: domain.hapticEnabled,
            undoDurationSeconds: domain.undoDurationSeconds,
            createdAt: domain.createdAt,
            updatedAt: domain.updatedAt
        )
    }

    func domainModel() throws -> AppSettings {
        guard let decodedIntensity = FeedbackIntensity(rawValue: feedbackIntensity) else {
            throw RoutinePersistenceMappingError.invalidStoredFeedbackIntensity(feedbackIntensity)
        }

        return try AppSettings(
            id: id,
            feedbackIntensity: decodedIntensity,
            soundEnabled: soundEnabled,
            ttsEnabled: ttsEnabled,
            ttsRate: ttsRate,
            ttsVolume: ttsVolume,
            hapticEnabled: hapticEnabled,
            undoDurationSeconds: undoDurationSeconds,
            createdAt: createdAt,
            updatedAt: updatedAt
        )
    }

    func apply(_ domain: AppSettings) {
        feedbackIntensity = domain.feedbackIntensity.rawValue
        soundEnabled = domain.soundEnabled
        ttsEnabled = domain.ttsEnabled
        ttsRate = domain.ttsRate
        ttsVolume = domain.ttsVolume
        hapticEnabled = domain.hapticEnabled
        undoDurationSeconds = domain.undoDurationSeconds
        createdAt = domain.createdAt
        updatedAt = domain.updatedAt
    }
}

private enum RoutineDataCoder {
    static func encode<T: Encodable>(_ value: T) throws -> Data {
        try JSONEncoder().encode(value)
    }

    static func decode<T: Decodable>(_ type: T.Type, from data: Data) throws -> T {
        try JSONDecoder().decode(type, from: data)
    }
}
