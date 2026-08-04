import SwiftData

@MainActor
enum PersistenceInitialization {
    case ready(modelContainer: ModelContainer?, repository: any RoutineRepository)
    case failed(errorDescription: String)

    static func persistent() -> PersistenceInitialization {
        persistent(makeContainer: makePersistentContainer)
    }

    static func persistent(
        makeContainer: () throws -> ModelContainer
    ) -> PersistenceInitialization {
        do {
            let container = try makeContainer()
            return .ready(
                modelContainer: container,
                repository: SwiftDataRoutineRepository(modelContainer: container)
            )
        } catch {
            return .failed(errorDescription: String(describing: error))
        }
    }

    static func preview() -> PersistenceInitialization {
        do {
            return .ready(
                modelContainer: nil,
                repository: try RoutinePreviewStore.makeLightweightSampleRepository()
            )
        } catch {
            return .failed(errorDescription: String(describing: error))
        }
    }

    var repository: (any RoutineRepository)? {
        guard case let .ready(_, repository) = self else { return nil }
        return repository
    }

    var modelContainer: ModelContainer? {
        guard case let .ready(modelContainer, _) = self else { return nil }
        return modelContainer
    }

    var errorDescription: String? {
        guard case let .failed(errorDescription) = self else { return nil }
        return errorDescription
    }

    private static func makePersistentContainer() throws -> ModelContainer {
        let schema = RoutinePersistenceSchema.schema
        let configuration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
        return try ModelContainer(
            for: schema,
            configurations: [configuration]
        )
    }
}
