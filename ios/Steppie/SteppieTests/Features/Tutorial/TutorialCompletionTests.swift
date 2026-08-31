import Testing
@testable import Steppie

@MainActor
struct TutorialCompletionTests {
    @Test("완료와 건너뛰기는 완료된 튜토리얼 화면을 알린다")
    func publishesCompletedTutorialScreen() throws {
        let coordinator = TutorialCoordinator(store: InMemoryTutorialProgressStore())

        coordinator.presentIfNeeded(.childFocus)
        coordinator.skip()
        let skippedCompletion = try #require(coordinator.completion)

        #expect(skippedCompletion.screen == .childFocus)

        coordinator.resetAndPresent(.security)
        while !coordinator.isLastStep {
            coordinator.next()
        }
        coordinator.next()
        let finishedCompletion = try #require(coordinator.completion)

        #expect(finishedCompletion.screen == .security)
        #expect(finishedCompletion.id != skippedCompletion.id)
    }
}
