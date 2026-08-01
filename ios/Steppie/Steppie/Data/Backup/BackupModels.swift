import Foundation

enum BackupError: Error, Equatable {
    case invalidPackage
    case missingFile(String)
    case invalidManifest
    case invalidChecksum
    case unsupportedSchemaVersion(Int)
    case unsupportedPlatform(String)
    case duplicateID(String)
    case invalidReference(String)
    case invalidData
}

struct BackupChecksum: Codable, Equatable, Sendable {
    let algorithm: String
    let dataJson: String
}

struct BackupManifest: Codable, Equatable, Sendable {
    let app: String
    let backupSchemaVersion: Int
    let createdAt: Date
    let sourcePlatform: String
    let appVersion: String
    let dataFile: String
    let assetDirectory: String
    let checksum: BackupChecksum
}

struct BackupData: Codable, Equatable, Sendable {
    let schemaVersion: Int
    let exportedAt: Date
    let routineSets: [RoutineSet]
    let routines: [Routine]
    let dailyLogs: [DailyLog]
    let dailyRoutineAssignments: [DailyRoutineAssignment]
    let appSettings: AppSettings

    init(
        schemaVersion: Int,
        exportedAt: Date,
        routineSets: [RoutineSet],
        routines: [Routine],
        dailyLogs: [DailyLog],
        dailyRoutineAssignments: [DailyRoutineAssignment] = [],
        appSettings: AppSettings
    ) {
        self.schemaVersion = schemaVersion
        self.exportedAt = exportedAt
        self.routineSets = routineSets
        self.routines = routines
        self.dailyLogs = dailyLogs
        self.dailyRoutineAssignments = dailyRoutineAssignments
        self.appSettings = appSettings
    }

    private enum CodingKeys: String, CodingKey {
        case schemaVersion
        case exportedAt
        case routineSets
        case routines
        case dailyLogs
        case dailyRoutineAssignments
        case appSettings
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        schemaVersion = try container.decode(Int.self, forKey: .schemaVersion)
        exportedAt = try container.decode(Date.self, forKey: .exportedAt)
        routineSets = try container.decode([RoutineSet].self, forKey: .routineSets)
        routines = try container.decode([Routine].self, forKey: .routines)
        dailyLogs = try container.decode([DailyLog].self, forKey: .dailyLogs)
        dailyRoutineAssignments = try container.decodeIfPresent(
            [DailyRoutineAssignment].self,
            forKey: .dailyRoutineAssignments
        ) ?? []
        appSettings = try container.decode(AppSettings.self, forKey: .appSettings)
    }
}

struct BackupPackage: Equatable, Sendable {
    let fileName: String
    let archiveData: Data
    let manifest: BackupManifest
}

struct BackupRestorePayload: Equatable, Sendable {
    let snapshot: RoutineRepositorySnapshot
    let assets: [String: Data]
}
