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
}
