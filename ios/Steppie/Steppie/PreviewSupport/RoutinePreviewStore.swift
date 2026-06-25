import Foundation
import SwiftData

nonisolated struct RoutineSampleData: Sendable {
    let routineSet: RoutineSet
    let routines: [Routine]

    static func morning() throws -> RoutineSampleData {
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let routineSetID = UUID(uuidString: "11111111-1111-4111-8111-111111111111")!
        let routineSet = try RoutineSet(
            id: routineSetID,
            name: LocalizedText(["ko": "아침 루틴", "en": "Morning routine"]),
            isActive: true,
            createdAt: createdAt,
            updatedAt: createdAt
        )

        let definitions: [(String, String, String, String?)] = [
            ("22222222-2222-4222-8222-222222222220", "일어나기", "wake-up", "07:30"),
            ("22222222-2222-4222-8222-222222222221", "세수하기", "wash-face", nil),
            ("22222222-2222-4222-8222-222222222222", "양치하기", "brush-teeth", "08:00"),
        ]

        let routines = try definitions.enumerated().map { order, definition in
            let (id, title, iconName, time) = definition
            return try Routine(
                id: UUID(uuidString: id)!,
                routineSetID: routineSetID,
                title: LocalizedText(["ko": title]),
                icon: IconRef.builtin(name: iconName),
                colorToken: Routine.allowedColorTokens.sorted()[order],
                order: order,
                scheduledTime: try time.map { try LocalTime($0) },
                createdAt: createdAt,
                updatedAt: createdAt
            )
        }

        return RoutineSampleData(routineSet: routineSet, routines: routines)
    }
}

@MainActor
enum RoutinePreviewStore {
    static func makeInMemoryContainer() throws -> ModelContainer {
        let schema = RoutinePersistenceSchema.schema
        let configuration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: true)
        return try ModelContainer(for: schema, configurations: [configuration])
    }

    static func makeRepository(seed: RoutineSampleData? = nil) throws -> SwiftDataRoutineRepository {
        let repository = SwiftDataRoutineRepository(
            modelContainer: try makeInMemoryContainer()
        )
        if let seed {
            try repository.createRoutineSet(seed.routineSet)
            for routine in seed.routines {
                try repository.createRoutine(routine)
            }
        }
        return repository
    }

    static func makeSampleRepository() throws -> SwiftDataRoutineRepository {
        try makeRepository(seed: RoutineSampleData.morning())
    }

    static func makeLightweightSampleRepository() throws -> PreviewRoutineRepository {
        try PreviewRoutineRepository(seed: RoutineSampleData.morning())
    }
}

@MainActor
final class PreviewRoutineRepository: RoutineRepository {
    private var routineSets: [RoutineSet]
    private var routines: [Routine]
    private var dailyLogs: [DailyLog] = []
    private var settings: AppSettings

    init(seed: RoutineSampleData) throws {
        routineSets = [seed.routineSet]
        routines = seed.routines
        settings = try AppSettings()
    }

    func createRoutineSet(_ routineSet: RoutineSet) throws {
        routineSets.append(routineSet)
    }

    func routineSet(id: UUID) throws -> RoutineSet? {
        routineSets.first { $0.id == id }
    }

    func routineSets(includeDeleted: Bool) throws -> [RoutineSet] {
        routineSets.filter { includeDeleted || $0.deletedAt == nil }
    }

    func updateRoutineSet(_ routineSet: RoutineSet) throws {
        guard let index = routineSets.firstIndex(where: { $0.id == routineSet.id }) else { return }
        routineSets[index] = routineSet
    }

    func deleteRoutineSet(id: UUID, at date: Date) throws {}

    func createRoutine(_ routine: Routine) throws {
        routines.append(routine)
    }

    func routine(id: UUID) throws -> Routine? {
        routines.first { $0.id == id }
    }

    func routines(
        in routineSetID: UUID,
        includeInactive: Bool,
        includeDeleted: Bool
    ) throws -> [Routine] {
        routines
            .filter { $0.routineSetID == routineSetID }
            .filter { includeInactive || $0.isActive }
            .filter { includeDeleted || $0.deletedAt == nil }
            .sorted { $0.order < $1.order }
    }

    func updateRoutine(_ routine: Routine) throws {
        guard let index = routines.firstIndex(where: { $0.id == routine.id }) else { return }
        routines[index] = routine
    }

    func deleteRoutine(id: UUID, at date: Date) throws {}

    func reorderRoutines(in routineSetID: UUID, orderedIDs: [UUID], at date: Date) throws {}

    func dailyLogs(on date: String, routineSetID: UUID?) throws -> [DailyLog] {
        dailyLogs
            .filter { $0.date == date }
            .filter { routineSetID == nil || $0.routineSetID == routineSetID }
    }

    func dailyLog(on date: String, routineID: UUID) throws -> DailyLog? {
        dailyLogs.first { $0.date == date && $0.routineID == routineID }
    }

    func setRoutineCompleted(
        routineID: UUID,
        routineSetID: UUID,
        on date: String,
        at completedAt: Date
    ) throws -> DailyLog {
        let log = try DailyLog(
            date: date,
            routineID: routineID,
            routineSetID: routineSetID,
            status: .completed,
            completedAt: completedAt,
            createdAt: completedAt,
            updatedAt: completedAt
        )
        dailyLogs.removeAll { $0.date == date && $0.routineID == routineID }
        dailyLogs.append(log)
        return log
    }

    func undoRoutineCompletion(
        routineID: UUID,
        on date: String,
        at updatedAt: Date
    ) throws -> DailyLog? {
        guard let index = dailyLogs.firstIndex(where: { $0.date == date && $0.routineID == routineID }) else {
            return nil
        }
        let existing = dailyLogs[index]
        let log = try DailyLog(
            id: existing.id,
            date: existing.date,
            routineID: existing.routineID,
            routineSetID: existing.routineSetID,
            status: .undone,
            completedAt: nil,
            createdAt: existing.createdAt,
            updatedAt: updatedAt
        )
        dailyLogs[index] = log
        return log
    }

    func appSettings() throws -> AppSettings {
        settings
    }

    func updateAppSettings(_ settings: AppSettings) throws {
        self.settings = settings
    }
}
