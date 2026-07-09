import Foundation

nonisolated enum RoutineRepositoryError: Error, Equatable {
    case duplicateID(UUID)
    case routineSetNotFound(UUID)
    case routineNotFound(UUID)
    case routineSetIsDeleted(UUID)
    case cannotDeleteActiveRoutineSet(UUID)
    case cannotDeactivateOnlyActiveRoutineSet(UUID)
    case duplicateOrder(routineSetID: UUID, order: Int)
    case invalidNextOrder(expected: Int, actual: Int)
    case routineSetCannotChange(routineID: UUID)
    case orderChangesRequireReorder(routineID: UUID)
    case invalidReorder
    case duplicateDailyLog(date: String, routineID: UUID)
    case dailyLogNotFound(date: String, routineID: UUID)
}

nonisolated struct RoutineRepositorySnapshot: Equatable, Sendable {
    let routineSets: [RoutineSet]
    let routines: [Routine]
    let dailyLogs: [DailyLog]
    let dailyRoutineAssignments: [DailyRoutineAssignment]
    let appSettings: AppSettings
}

@MainActor
protocol RoutineRepository {
    func createRoutineSet(_ routineSet: RoutineSet) throws
    func routineSet(id: UUID) throws -> RoutineSet?
    func routineSets(includeDeleted: Bool) throws -> [RoutineSet]
    func updateRoutineSet(_ routineSet: RoutineSet) throws
    func deleteRoutineSet(id: UUID, at date: Date) throws

    func createRoutine(_ routine: Routine) throws
    func routine(id: UUID) throws -> Routine?
    func routines(
        in routineSetID: UUID,
        includeInactive: Bool,
        includeDeleted: Bool
    ) throws -> [Routine]
    func updateRoutine(_ routine: Routine) throws
    func deleteRoutine(id: UUID, at date: Date) throws
    func reorderRoutines(in routineSetID: UUID, orderedIDs: [UUID], at date: Date) throws

    func dailyLogs(on date: String, routineSetID: UUID?) throws -> [DailyLog]
    func dailyLogDates() throws -> [String]
    func dailyLog(on date: String, routineID: UUID) throws -> DailyLog?
    func setRoutineCompleted(
        routineID: UUID,
        routineSetID: UUID,
        on date: String,
        at completedAt: Date
    ) throws -> DailyLog
    func undoRoutineCompletion(
        routineID: UUID,
        on date: String,
        at updatedAt: Date
    ) throws -> DailyLog?

    func dailyRoutineAssignment(on date: String) throws -> DailyRoutineAssignment?
    func assignRoutineSet(_ routineSetID: UUID, on date: String, at updatedAt: Date) throws -> DailyRoutineAssignment

    func appSettings() throws -> AppSettings
    func updateAppSettings(_ settings: AppSettings) throws

    func backupSnapshot() throws -> RoutineRepositorySnapshot
    func replaceAll(with snapshot: RoutineRepositorySnapshot) throws
}

extension RoutineRepository {
    func routineSets() throws -> [RoutineSet] {
        try routineSets(includeDeleted: false)
    }

    func routines(in routineSetID: UUID) throws -> [Routine] {
        try routines(in: routineSetID, includeInactive: false, includeDeleted: false)
    }
}
