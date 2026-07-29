import Foundation
@testable import Steppie

final class FakeBackupAssetStore: BackupAssetStore {
    var assets: [String: Data]
    var removedAssetNames: [String] = []

    init(assets: [String: Data] = [:]) {
        self.assets = assets
    }

    func data(forBackupAssetName name: String) throws -> Data? {
        assets[name]
    }

    func saveAssetData(_ data: Data, backupAssetName name: String) throws {
        assets[name] = data
    }

    func removeAssetData(backupAssetName name: String) throws {
        assets[name] = nil
        removedAssetNames.append(name)
    }
}

@MainActor
final class FailingReplaceRepository: RoutineRepository {
    enum ReplaceError: Error, Equatable {
        case failed
    }

    func createRoutineSet(_ routineSet: RoutineSet) throws {}
    func routineSet(id: UUID) throws -> RoutineSet? { nil }
    func routineSets(includeDeleted: Bool) throws -> [RoutineSet] { [] }
    func updateRoutineSet(_ routineSet: RoutineSet) throws {}
    func deleteRoutineSet(id: UUID, at date: Date) throws {}
    func createRoutine(_ routine: Routine) throws {}
    func routine(id: UUID) throws -> Routine? { nil }
    func routines(in routineSetID: UUID, includeInactive: Bool, includeDeleted: Bool) throws -> [Routine] { [] }
    func updateRoutine(_ routine: Routine) throws {}
    func deleteRoutine(id: UUID, at date: Date) throws {}
    func reorderRoutines(in routineSetID: UUID, orderedIDs: [UUID], at date: Date) throws {}
    func dailyLogs(on date: String, routineSetID: UUID?) throws -> [DailyLog] { [] }
    func dailyLogDates() throws -> [String] { [] }
    func dailyLog(on date: String, routineID: UUID) throws -> DailyLog? { nil }
    func setRoutineCompleted(routineID: UUID, routineSetID: UUID, on date: String, at completedAt: Date) throws -> DailyLog {
        throw ReplaceError.failed
    }
    func undoRoutineCompletion(routineID: UUID, on date: String, at updatedAt: Date) throws -> DailyLog? { nil }
    func dailyRoutineAssignment(on date: String) throws -> DailyRoutineAssignment? { nil }
    func assignRoutineSet(_ routineSetID: UUID, on date: String, at updatedAt: Date) throws -> DailyRoutineAssignment {
        throw ReplaceError.failed
    }
    func appSettings() throws -> AppSettings { try AppSettings() }
    func updateAppSettings(_ settings: AppSettings) throws {}
    func backupSnapshot() throws -> RoutineRepositorySnapshot {
        RoutineRepositorySnapshot(
            routineSets: [],
            routines: [],
            dailyLogs: [],
            dailyRoutineAssignments: [],
            appSettings: try AppSettings()
        )
    }
    func replaceAll(with snapshot: RoutineRepositorySnapshot) throws {
        throw ReplaceError.failed
    }
}

let backupTestJSONEncoder: JSONEncoder = {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
    encoder.dateEncodingStrategy = .iso8601
    return encoder
}()
