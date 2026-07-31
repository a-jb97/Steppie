import Foundation
import Observation

struct RoutineSetStepDraft: Equatable, Identifiable {
    let id: UUID
    var title: String
    var icon: IconRef
    var colorToken: String
    var scheduledTime: LocalTime?

    init(
        id: UUID = UUID(),
        title: String = "",
        iconName: RoutineIconName = .star,
        colorToken: String = Routine.defaultColorToken,
        scheduledTime: LocalTime? = nil
    ) {
        self.id = id
        self.title = title
        self.icon = try! IconRef.builtin(name: iconName.rawValue)
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    init(
        id: UUID = UUID(),
        title: String = "",
        icon: IconRef,
        colorToken: String = Routine.defaultColorToken,
        scheduledTime: LocalTime? = nil
    ) {
        self.id = id
        self.title = title
        self.icon = icon
        self.colorToken = colorToken
        self.scheduledTime = scheduledTime
    }

    var isValid: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && Routine.allowedColorTokens.contains(colorToken)
    }

    var iconName: RoutineIconName {
        get {
            guard icon.type == .builtin,
                  let name = icon.name.flatMap(RoutineIconName.init(rawValue:)) else {
                return .star
            }
            return name
        }
        set {
            icon = try! IconRef.builtin(name: newValue.rawValue)
        }
    }
}

struct RoutineSetDraft: Equatable {
    var name: String
    var steps: [RoutineSetStepDraft]

    var isValid: Bool {
        !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !steps.isEmpty
            && steps.allSatisfy(\.isValid)
    }
}

@MainActor
@Observable
final class GuardianRoutineSetCreationState {
    private let repository: any RoutineRepository
    private let photoStore: any RoutinePhotoStoring
    private let now: () -> Date

    var draft: RoutineSetDraft?
    var selectedStepID: UUID?
    var stepDraft: RoutineSetStepDraft?

    var canSave: Bool {
        draft?.isValid == true
    }

    var selectedStep: RoutineSetStepDraft? {
        guard let draft else { return nil }
        guard let selectedStepID else { return draft.steps.first }
        return draft.steps.first { $0.id == selectedStepID } ?? draft.steps.first
    }

    var hasUnsavedDraft: Bool {
        guard let draft else { return false }
        return !draft.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            || !draft.steps.isEmpty
            || stepDraft?.isValid == true
    }

    init(
        repository: any RoutineRepository,
        photoStore: any RoutinePhotoStoring,
        now: @escaping () -> Date
    ) {
        self.repository = repository
        self.photoStore = photoStore
        self.now = now
    }

    func begin() {
        draft = RoutineSetDraft(name: "", steps: [])
        selectedStepID = nil
        stepDraft = nil
    }

    func cancel() {
        draft = nil
        selectedStepID = nil
        stepDraft = nil
    }

    func beginAddStep() {
        stepDraft = RoutineSetStepDraft()
        selectedStepID = nil
    }

    func beginEditStep(_ step: RoutineSetStepDraft) {
        selectedStepID = step.id
        stepDraft = step
    }

    func cancelStepDraft() {
        stepDraft = nil
    }

    func saveStepDraft() {
        guard let stepDraft, stepDraft.isValid else { return }
        if let index = draft?.steps.firstIndex(where: { $0.id == stepDraft.id }) {
            draft?.steps[index] = stepDraft
        } else {
            draft?.steps.append(stepDraft)
        }
        selectedStepID = stepDraft.id
        self.stepDraft = nil
    }

    func deleteStep(_ step: RoutineSetStepDraft) {
        draft?.steps.removeAll { $0.id == step.id }
        if selectedStepID == step.id {
            selectedStepID = draft?.steps.first?.id
        }
        if stepDraft?.id == step.id {
            stepDraft = nil
        }
    }

    func moveStep(_ step: RoutineSetStepDraft, direction: Int) {
        guard var steps = draft?.steps,
              let index = steps.firstIndex(where: { $0.id == step.id }) else { return }
        let destination = index + direction
        guard steps.indices.contains(destination) else { return }
        steps.swapAt(index, destination)
        draft?.steps = steps
    }

    func updateStepPhoto(data: Data) throws {
        guard stepDraft != nil else { return }
        stepDraft?.icon = try photoStore.savePhotoData(data)
    }

    func resetStepIconToDefault() {
        stepDraft?.icon = try! IconRef.builtin(name: RoutineIconName.star.rawValue)
    }

    func save(localeIdentifier: String) throws -> UUID? {
        guard let draft, draft.isValid else { return nil }
        let createdAt = now()
        let routineSet = try RoutineSet(
            name: LocalizedText([localeIdentifier: draft.name]),
            isActive: false,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(routineSet)

        for (order, step) in draft.steps.enumerated() {
            let routine = try Routine(
                routineSetID: routineSet.id,
                title: LocalizedText([localeIdentifier: step.title]),
                icon: step.icon,
                colorToken: step.colorToken,
                order: order,
                scheduledTime: step.scheduledTime,
                createdAt: createdAt,
                updatedAt: createdAt
            )
            try repository.createRoutine(routine)
        }

        cancel()
        return routineSet.id
    }
}
