@testable import Steppie

@MainActor
final class InMemoryTutorialProgressStore: TutorialProgressStoring {
    private var tokens: Set<String> = []

    func contains(_ token: String) -> Bool {
        tokens.contains(token)
    }

    func insert(_ token: String) {
        tokens.insert(token)
    }

    func removeAll() {
        tokens.removeAll()
    }
}
