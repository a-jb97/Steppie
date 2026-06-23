import Foundation
import SwiftData

@MainActor
final class SwiftDataRoutineRepository: RoutineRepository {
    private let context: ModelContext

    init(modelContainer: ModelContainer) {
        context = ModelContext(modelContainer)
        context.autosaveEnabled = false
    }

    func createRoutineSet(_ routineSet: RoutineSet) throws {
        try routineSet.validate()
        guard try routineSetRecord(id: routineSet.id) == nil else {
            throw RoutineRepositoryError.duplicateID(routineSet.id)
        }

        if routineSet.isActive {
            try deactivateOtherRoutineSets(except: routineSet.id, at: routineSet.updatedAt)
        }
        context.insert(try RoutineSetRecord(domain: routineSet))
        try context.save()
    }

    func routineSet(id: UUID) throws -> RoutineSet? {
        try routineSetRecord(id: id)?.domainModel()
    }

    func routineSets(includeDeleted: Bool) throws -> [RoutineSet] {
        let descriptor = FetchDescriptor<RoutineSetRecord>(
            sortBy: [SortDescriptor(\.createdAt), SortDescriptor(\.id)]
        )
        return try context.fetch(descriptor)
            .filter { includeDeleted || $0.deletedAt == nil }
            .map { try $0.domainModel() }
    }

    func updateRoutineSet(_ routineSet: RoutineSet) throws {
        try routineSet.validate()
        guard let record = try routineSetRecord(id: routineSet.id) else {
            throw RoutineRepositoryError.routineSetNotFound(routineSet.id)
        }

        if record.isActive && !routineSet.isActive {
            let hasOtherActiveSet = try routineSetRecords().contains {
                $0.id != record.id && $0.isActive && $0.deletedAt == nil
            }
            guard hasOtherActiveSet else {
                throw RoutineRepositoryError.cannotDeactivateOnlyActiveRoutineSet(record.id)
            }
        }
        if routineSet.isActive {
            try deactivateOtherRoutineSets(except: routineSet.id, at: routineSet.updatedAt)
        }
        try record.apply(routineSet)
        try context.save()
    }

    func deleteRoutineSet(id: UUID, at date: Date) throws {
        guard let record = try routineSetRecord(id: id) else {
            throw RoutineRepositoryError.routineSetNotFound(id)
        }
        guard !record.isActive else {
            throw RoutineRepositoryError.cannotDeleteActiveRoutineSet(id)
        }
        guard record.deletedAt == nil else { return }

        record.deletedAt = date
        record.updatedAt = date
        try context.save()
    }

    func createRoutine(_ routine: Routine) throws {
        try routine.validate()
        try validateParent(routine.routineSetID)
        guard try routineRecord(id: routine.id) == nil else {
            throw RoutineRepositoryError.duplicateID(routine.id)
        }
        try validateOrderAvailability(for: routine, excluding: nil)
        try validateNextOrder(for: routine)

        context.insert(try RoutineRecord(domain: routine))
        try touchRoutineSet(id: routine.routineSetID, at: routine.updatedAt)
        try context.save()
    }

    func routine(id: UUID) throws -> Routine? {
        try routineRecord(id: id)?.domainModel()
    }

    func routines(
        in routineSetID: UUID,
        includeInactive: Bool,
        includeDeleted: Bool
    ) throws -> [Routine] {
        try validateParent(routineSetID, allowDeleted: includeDeleted)
        return try routineRecords(in: routineSetID)
            .filter { includeInactive || $0.isActive }
            .filter { includeDeleted || $0.deletedAt == nil }
            .sorted {
                if $0.order == $1.order { return $0.createdAt < $1.createdAt }
                return $0.order < $1.order
            }
            .map { try $0.domainModel() }
    }

    func updateRoutine(_ routine: Routine) throws {
        try routine.validate()
        guard let record = try routineRecord(id: routine.id) else {
            throw RoutineRepositoryError.routineNotFound(routine.id)
        }
        guard record.routineSetID == routine.routineSetID else {
            throw RoutineRepositoryError.routineSetCannotChange(routineID: routine.id)
        }
        guard record.order == routine.order else {
            throw RoutineRepositoryError.orderChangesRequireReorder(routineID: routine.id)
        }
        try validateParent(routine.routineSetID)
        try validateOrderAvailability(for: routine, excluding: routine.id)

        let activeStateChanged = record.isActive != routine.isActive
        try record.apply(routine)
        if activeStateChanged {
            try compactActiveOrders(in: routine.routineSetID, at: routine.updatedAt)
        }
        try touchRoutineSet(id: routine.routineSetID, at: routine.updatedAt)
        try context.save()
    }

    func deleteRoutine(id: UUID, at date: Date) throws {
        guard let record = try routineRecord(id: id) else {
            throw RoutineRepositoryError.routineNotFound(id)
        }
        guard record.deletedAt == nil else { return }

        record.isActive = false
        record.deletedAt = date
        record.updatedAt = date
        try compactActiveOrders(in: record.routineSetID, at: date)
        try touchRoutineSet(id: record.routineSetID, at: date)
        try context.save()
    }

    func reorderRoutines(in routineSetID: UUID, orderedIDs: [UUID], at date: Date) throws {
        try validateParent(routineSetID)
        let records = try routineRecords(in: routineSetID)
            .filter { $0.isActive && $0.deletedAt == nil }

        guard Set(orderedIDs).count == orderedIDs.count,
              Set(orderedIDs) == Set(records.map(\.id)) else {
            throw RoutineRepositoryError.invalidReorder
        }

        let recordsByID = Dictionary(uniqueKeysWithValues: records.map { ($0.id, $0) })
        for (order, id) in orderedIDs.enumerated() {
            guard let record = recordsByID[id] else {
                throw RoutineRepositoryError.invalidReorder
            }
            record.order = order
            record.updatedAt = date
        }
        try touchRoutineSet(id: routineSetID, at: date)
        try context.save()
    }

    private func routineSetRecord(id: UUID) throws -> RoutineSetRecord? {
        let id = id
        let descriptor = FetchDescriptor<RoutineSetRecord>(
            predicate: #Predicate { $0.id == id }
        )
        return try context.fetch(descriptor).first
    }

    private func routineSetRecords() throws -> [RoutineSetRecord] {
        try context.fetch(FetchDescriptor<RoutineSetRecord>())
    }

    private func routineRecord(id: UUID) throws -> RoutineRecord? {
        let id = id
        let descriptor = FetchDescriptor<RoutineRecord>(
            predicate: #Predicate { $0.id == id }
        )
        return try context.fetch(descriptor).first
    }

    private func routineRecords(in routineSetID: UUID) throws -> [RoutineRecord] {
        let routineSetID = routineSetID
        let descriptor = FetchDescriptor<RoutineRecord>(
            predicate: #Predicate { $0.routineSetID == routineSetID }
        )
        return try context.fetch(descriptor)
    }

    private func validateParent(_ id: UUID, allowDeleted: Bool = false) throws {
        guard let parent = try routineSetRecord(id: id) else {
            throw RoutineRepositoryError.routineSetNotFound(id)
        }
        if !allowDeleted && parent.deletedAt != nil {
            throw RoutineRepositoryError.routineSetIsDeleted(id)
        }
    }

    private func validateOrderAvailability(for routine: Routine, excluding id: UUID?) throws {
        guard routine.isActive && routine.deletedAt == nil else { return }
        let hasConflict = try routineRecords(in: routine.routineSetID).contains {
            $0.id != id && $0.isActive && $0.deletedAt == nil && $0.order == routine.order
        }
        if hasConflict {
            throw RoutineRepositoryError.duplicateOrder(
                routineSetID: routine.routineSetID,
                order: routine.order
            )
        }
    }

    private func validateNextOrder(for routine: Routine) throws {
        guard routine.isActive && routine.deletedAt == nil else { return }
        let expectedOrder = try routineRecords(in: routine.routineSetID)
            .filter { $0.isActive && $0.deletedAt == nil }
            .count
        guard routine.order == expectedOrder else {
            throw RoutineRepositoryError.invalidNextOrder(
                expected: expectedOrder,
                actual: routine.order
            )
        }
    }

    private func deactivateOtherRoutineSets(except id: UUID, at date: Date) throws {
        let records = try routineSetRecords()
        for record in records where record.id != id && record.isActive && record.deletedAt == nil {
            record.isActive = false
            record.updatedAt = max(date, record.createdAt)
        }
    }

    private func touchRoutineSet(id: UUID, at date: Date) throws {
        guard let record = try routineSetRecord(id: id) else {
            throw RoutineRepositoryError.routineSetNotFound(id)
        }
        record.updatedAt = max(date, record.createdAt)
    }

    private func compactActiveOrders(in routineSetID: UUID, at date: Date) throws {
        let records = try routineRecords(in: routineSetID)
            .filter { $0.isActive && $0.deletedAt == nil }
            .sorted { $0.order < $1.order }

        for (order, record) in records.enumerated() where record.order != order {
            record.order = order
            record.updatedAt = date
        }
    }
}
