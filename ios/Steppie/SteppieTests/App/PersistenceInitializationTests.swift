import Testing
@testable import Steppie

@MainActor
struct PersistenceInitializationTests {
    private enum ExpectedFailure: Error {
        case modelContainerUnavailable
    }

    @Test("영구 저장소 초기화 실패는 앱 종료 대신 실패 상태를 반환한다")
    func persistentInitializationReturnsFailureState() {
        let initialization = PersistenceInitialization.persistent {
            throw ExpectedFailure.modelContainerUnavailable
        }

        #expect(initialization.repository == nil)
        #expect(initialization.modelContainer == nil)
        #expect(initialization.errorDescription != nil)
    }

    @Test("영구 저장소 초기화 성공은 container와 repository를 함께 유지한다")
    func persistentInitializationKeepsContainerAndRepository() throws {
        let initialization = PersistenceInitialization.persistent {
            try RoutinePreviewStore.makeInMemoryContainer()
        }

        #expect(initialization.repository != nil)
        #expect(initialization.modelContainer != nil)
        #expect(initialization.errorDescription == nil)
    }

    @Test("영속 스키마는 데이터 계약 모델만 포함한다")
    func persistenceSchemaContainsOnlyContractModels() {
        let expectedModels: Set<ObjectIdentifier> = [
            ObjectIdentifier(RoutineSetRecord.self),
            ObjectIdentifier(RoutineRecord.self),
            ObjectIdentifier(DailyLogRecord.self),
            ObjectIdentifier(AppSettingsRecord.self),
            ObjectIdentifier(DailyRoutineAssignmentRecord.self),
        ]
        let actualModels = Set(RoutinePersistenceSchema.models.map(ObjectIdentifier.init))

        #expect(actualModels == expectedModels)
    }
}
