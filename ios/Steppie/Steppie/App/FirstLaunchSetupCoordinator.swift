import Foundation
import Observation

nonisolated enum FirstLaunchSetupProgress: String, Equatable, Sendable {
    case notStarted
    case pinSetupPending
    case completed
}

@MainActor
protocol FirstLaunchSetupProgressStoring: AnyObject {
    func loadProgress() -> FirstLaunchSetupProgress
    func saveProgress(_ progress: FirstLaunchSetupProgress)
}

@MainActor
final class UserDefaultsFirstLaunchSetupProgressStore: FirstLaunchSetupProgressStoring {
    private static let progressKey = "firstLaunch.guardianPINSetup.progress.v1"
    private let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func loadProgress() -> FirstLaunchSetupProgress {
        guard let rawValue = defaults.string(forKey: Self.progressKey) else {
            return .notStarted
        }
        return FirstLaunchSetupProgress(rawValue: rawValue) ?? .notStarted
    }

    func saveProgress(_ progress: FirstLaunchSetupProgress) {
        defaults.set(progress.rawValue, forKey: Self.progressKey)
    }
}

@MainActor
@Observable
final class FirstLaunchSetupCoordinator {
    private(set) var progress: FirstLaunchSetupProgress
    private let store: any FirstLaunchSetupProgressStoring

    convenience init() {
        self.init(store: UserDefaultsFirstLaunchSetupProgressStore())
    }

    init(store: any FirstLaunchSetupProgressStoring) {
        self.store = store
        progress = store.loadProgress()
    }

    var requiresPINSetup: Bool {
        progress == .pinSetupPending
    }

    @discardableResult
    func beginPINSetup() -> Bool {
        guard progress == .notStarted else { return false }
        updateProgress(.pinSetupPending)
        return true
    }

    func completePINSetup() {
        guard progress == .pinSetupPending else { return }
        updateProgress(.completed)
    }

    func reconcile(hasGuardianPIN: Bool) {
        guard hasGuardianPIN, progress == .pinSetupPending else { return }
        updateProgress(.completed)
    }

    private func updateProgress(_ newProgress: FirstLaunchSetupProgress) {
        progress = newProgress
        store.saveProgress(newProgress)
    }
}
