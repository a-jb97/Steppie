import Foundation
import Testing
@testable import Steppie

@MainActor
struct FirstLaunchSetupCoordinatorTests {
    @Test("최초 설정은 시작 전, PIN 설정 대기, 완료 순서로 진행된다")
    func progressesThroughPINSetupStates() {
        let store = InMemoryFirstLaunchSetupProgressStore()
        let coordinator = FirstLaunchSetupCoordinator(store: store)

        #expect(coordinator.progress == .notStarted)
        #expect(!coordinator.requiresPINSetup)

        #expect(coordinator.beginPINSetup())
        #expect(coordinator.progress == .pinSetupPending)
        #expect(coordinator.requiresPINSetup)
        #expect(store.progress == .pinSetupPending)

        coordinator.completePINSetup()
        #expect(coordinator.progress == .completed)
        #expect(!coordinator.requiresPINSetup)
        #expect(store.progress == .completed)
    }

    @Test("이미 시작하거나 완료한 최초 설정은 다시 시작하지 않는다")
    func doesNotRestartExistingProgress() {
        let pendingStore = InMemoryFirstLaunchSetupProgressStore(progress: .pinSetupPending)
        let completedStore = InMemoryFirstLaunchSetupProgressStore(progress: .completed)
        let pendingCoordinator = FirstLaunchSetupCoordinator(store: pendingStore)
        let completedCoordinator = FirstLaunchSetupCoordinator(store: completedStore)

        #expect(!pendingCoordinator.beginPINSetup())
        #expect(pendingCoordinator.progress == .pinSetupPending)
        #expect(!completedCoordinator.beginPINSetup())
        #expect(completedCoordinator.progress == .completed)
    }

    @Test("메인 튜토리얼 완료 시 루틴과 PIN이 모두 없을 때만 PIN 설정을 시작한다")
    func beginsPINSetupOnlyForEmptyFirstLaunch() {
        let emptyCoordinator = FirstLaunchSetupCoordinator(
            store: InMemoryFirstLaunchSetupProgressStore()
        )
        let routineCoordinator = FirstLaunchSetupCoordinator(
            store: InMemoryFirstLaunchSetupProgressStore()
        )
        let pinCoordinator = FirstLaunchSetupCoordinator(
            store: InMemoryFirstLaunchSetupProgressStore()
        )

        #expect(
            emptyCoordinator.handleMainTutorialCompletion(
                hasNoRoutines: true,
                hasGuardianPIN: false
            )
        )
        #expect(emptyCoordinator.progress == .pinSetupPending)

        #expect(
            !routineCoordinator.handleMainTutorialCompletion(
                hasNoRoutines: false,
                hasGuardianPIN: false
            )
        )
        #expect(routineCoordinator.progress == .completed)

        #expect(
            !pinCoordinator.handleMainTutorialCompletion(
                hasNoRoutines: true,
                hasGuardianPIN: true
            )
        )
        #expect(pinCoordinator.progress == .completed)
    }

    @Test("대기 중 이미 PIN이 있으면 최초 설정을 완료 상태로 정리한다")
    func reconcilesPendingProgressWhenPINExists() {
        let store = InMemoryFirstLaunchSetupProgressStore(progress: .pinSetupPending)
        let coordinator = FirstLaunchSetupCoordinator(store: store)

        coordinator.reconcile(hasGuardianPIN: false)
        #expect(coordinator.progress == .pinSetupPending)

        coordinator.reconcile(hasGuardianPIN: true)
        #expect(coordinator.progress == .completed)
        #expect(store.progress == .completed)
    }

    @Test("UserDefaults 저장소는 진행 상태를 재생성 후에도 복원한다")
    func userDefaultsStoreRestoresProgress() throws {
        let suiteName = "FirstLaunchSetupCoordinatorTests.\(UUID().uuidString)"
        let defaults = try #require(UserDefaults(suiteName: suiteName))
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let store = UserDefaultsFirstLaunchSetupProgressStore(defaults: defaults)
        #expect(store.loadProgress() == .notStarted)

        store.saveProgress(.pinSetupPending)

        let restoredStore = UserDefaultsFirstLaunchSetupProgressStore(defaults: defaults)
        #expect(restoredStore.loadProgress() == .pinSetupPending)
    }
}

@MainActor
private final class InMemoryFirstLaunchSetupProgressStore: FirstLaunchSetupProgressStoring {
    private(set) var progress: FirstLaunchSetupProgress

    init(progress: FirstLaunchSetupProgress = .notStarted) {
        self.progress = progress
    }

    func loadProgress() -> FirstLaunchSetupProgress {
        progress
    }

    func saveProgress(_ progress: FirstLaunchSetupProgress) {
        self.progress = progress
    }
}
