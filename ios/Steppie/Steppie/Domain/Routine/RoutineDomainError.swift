import Foundation

nonisolated enum RoutineDomainError: Error, Equatable {
    case uuidMustBeVersion4(field: String)
    case localizedTextRequiresValue
    case invalidLocaleTag(String)
    case invalidLocalTime(String)
    case invalidBuiltinIconName(String)
    case invalidPhotoAssetID
    case invalidBackupAssetName(String)
    case invalidColorToken(String)
    case invalidOrder(Int)
    case invalidLocalDate(String)
    case completedLogRequiresCompletedAt
    case undoneLogRequiresNoCompletedAt
    case invalidAppSettingsID(String)
    case invalidTTSRate(Double)
    case invalidTTSVolume(Double)
    case invalidUndoDuration(Int)
    case invalidNotificationLeadTimes([Int])
    case updatedAtPrecedesCreatedAt
    case activeRoutineSetCannotBeDeleted
}

extension UUID {
    nonisolated var isVersion4: Bool {
        let groups = uuidString.split(separator: "-")
        return groups.count == 5 && groups[2].first == "4"
    }
}
