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
}

extension RoutineRepository {
    func routineSets() throws -> [RoutineSet] {
        try routineSets(includeDeleted: false)
    }

    func routines(in routineSetID: UUID) throws -> [Routine] {
        try routines(in: routineSetID, includeInactive: false, includeDeleted: false)
    }
}
