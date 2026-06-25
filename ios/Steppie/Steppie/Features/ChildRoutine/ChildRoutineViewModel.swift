import Foundation
import Observation

enum ChildRoutineLoadState: Equatable {
    case idle
    case loaded
    case empty
    case failed
}

enum ChildRoutinePage: Equatable {
    case focus
    case list
}

enum ChildRoutineLayout: Equatable {
    case singlePane
    case splitPane
}

enum ChildRoutineLayoutPolicy {
    static func layout(for availableWidth: CGFloat) -> ChildRoutineLayout {
        availableWidth >= SteppieLayout.splitMinimumWidth ? .splitPane : .singlePane
    }
}

@MainActor
@Observable
final class ChildRoutineViewModel {
    private let repository: any RoutineRepository

    private(set) var loadState: ChildRoutineLoadState = .idle
    private(set) var activeRoutineSet: RoutineSet?
    private(set) var routines: [Routine] = []
    private(set) var selectedRoutineID: UUID?
    private(set) var page: ChildRoutinePage = .focus

    init(repository: any RoutineRepository) {
        self.repository = repository
    }

    var currentRoutine: Routine? {
        routines.first
    }

    var selectedRoutine: Routine? {
        guard let selectedRoutineID else { return currentRoutine }
        return routines.first { $0.id == selectedRoutineID } ?? currentRoutine
    }

    var completedCount: Int { 0 }
    var totalCount: Int { routines.count }

    func loadIfNeeded() {
        guard loadState == .idle else { return }
        load()
    }

    func load() {
        do {
            guard let activeSet = try repository.routineSets().first(where: \.isActive) else {
                reset(to: .empty)
                return
            }

            let fetchedRoutines = try repository.routines(in: activeSet.id)
            guard !fetchedRoutines.isEmpty else {
                activeRoutineSet = activeSet
                resetRoutines(to: .empty)
                return
            }

            activeRoutineSet = activeSet
            routines = fetchedRoutines
            if !fetchedRoutines.contains(where: { $0.id == selectedRoutineID }) {
                selectedRoutineID = fetchedRoutines.first?.id
            }
            loadState = .loaded
        } catch {
            reset(to: .failed)
        }
    }

    func selectRoutine(_ routine: Routine, showFocus: Bool) {
        guard routines.contains(where: { $0.id == routine.id }) else { return }
        selectedRoutineID = routine.id
        if showFocus {
            page = .focus
        }
    }

    func showList() {
        page = .list
    }

    func showFocus() {
        page = .focus
    }

    func cardState(for routine: Routine) -> RoutineCardState {
        routine.id == currentRoutine?.id ? .current : .upcoming
    }

    private func reset(to state: ChildRoutineLoadState) {
        activeRoutineSet = nil
        resetRoutines(to: state)
    }

    private func resetRoutines(to state: ChildRoutineLoadState) {
        routines = []
        selectedRoutineID = nil
        page = .focus
        loadState = state
    }
}
