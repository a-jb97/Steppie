import Foundation
import Observation

struct BuiltInRoutineTemplateStep: Equatable, Identifiable {
    let id: String
    let titleKey: String
    let title: LocalizedText
    let iconName: RoutineIconName
    let colorToken: String
    let scheduledTime: LocalTime?

    var icon: IconRef {
        try! IconRef.builtin(name: iconName.rawValue)
    }
}

struct BuiltInRoutineTemplate: Equatable, Identifiable {
    let id: String
    let name: LocalizedText
    let steps: [BuiltInRoutineTemplateStep]

    var routineCountText: String {
        "\(steps.count)개 활동"
    }
}

enum BuiltInRoutineTemplates {
    static let all: [BuiltInRoutineTemplate] = [
        template(
            id: "morning",
            name: ["ko": "아침 루틴", "en": "Morning routine"],
            steps: [
                ("routine.wakeUp", ["ko": "일어나기", "en": "Wake up"], .wakeUp, "color.card.sky"),
                ("routine.washFace", ["ko": "세수하기", "en": "Wash face"], .washFace, "color.card.mint"),
                ("routine.brushTeeth", ["ko": "양치하기", "en": "Brush teeth"], .brushTeeth, "color.card.lemon"),
                ("routine.getDressed", ["ko": "옷 입기", "en": "Get dressed"], .getDressed, "color.card.peach"),
                ("routine.breakfast", ["ko": "아침 먹기", "en": "Eat breakfast"], .breakfast, "color.card.lavender"),
                ("routine.packBag", ["ko": "가방 챙기기", "en": "Pack bag"], .packBag, "color.card.rose"),
            ]
        ),
        template(
            id: "school",
            name: ["ko": "학교 루틴", "en": "School routine"],
            steps: [
                ("routine.goSchool", ["ko": "학교 가기", "en": "Go to school"], .bus, "color.card.sky"),
                ("routine.readBook", ["ko": "책 읽기", "en": "Read book"], .book, "color.card.mint"),
                ("routine.lunch", ["ko": "점심 먹기", "en": "Eat lunch"], .lunch, "color.card.lemon"),
                ("routine.play", ["ko": "놀이하기", "en": "Play"], .playground, "color.card.peach"),
            ]
        ),
        template(
            id: "bedtime",
            name: ["ko": "취침 루틴", "en": "Bedtime routine"],
            steps: [
                ("routine.bath", ["ko": "목욕하기", "en": "Take a bath"], .bath, "color.card.sky"),
                ("routine.pajamas", ["ko": "잠옷 입기", "en": "Put on pajamas"], .pajamas, "color.card.mint"),
                ("routine.sleep", ["ko": "잠자기", "en": "Sleep"], .sleep, "color.card.lavender"),
            ]
        ),
    ]

    private static func template(
        id: String,
        name: [String: String],
        steps: [(String, [String: String], RoutineIconName, String)]
    ) -> BuiltInRoutineTemplate {
        BuiltInRoutineTemplate(
            id: id,
            name: try! LocalizedText(name),
            steps: steps.map { titleKey, title, iconName, colorToken in
                BuiltInRoutineTemplateStep(
                    id: titleKey,
                    titleKey: titleKey,
                    title: try! LocalizedText(title),
                    iconName: iconName,
                    colorToken: colorToken,
                    scheduledTime: nil
                )
            }
        )
    }
}

@MainActor
@Observable
final class GuardianRoutineTemplateState {
    private let repository: any RoutineRepository
    private let now: () -> Date

    var selectedTemplateID: String?

    var templates: [BuiltInRoutineTemplate] {
        BuiltInRoutineTemplates.all
    }

    var selectedTemplate: BuiltInRoutineTemplate? {
        guard let selectedTemplateID else { return templates.first }
        return templates.first { $0.id == selectedTemplateID } ?? templates.first
    }

    init(
        repository: any RoutineRepository,
        now: @escaping () -> Date
    ) {
        self.repository = repository
        self.now = now
    }

    func beginSelection() {
        selectedTemplateID = selectedTemplateID ?? templates.first?.id
    }

    func clearSelection() {
        selectedTemplateID = nil
    }

    func select(_ template: BuiltInRoutineTemplate) {
        selectedTemplateID = template.id
    }

    func saveSelectedTemplate() throws -> UUID? {
        guard let selectedTemplate else { return nil }
        let createdAt = now()
        let routineSet = try RoutineSet(
            name: selectedTemplate.name,
            isActive: false,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(routineSet)

        for (order, step) in selectedTemplate.steps.enumerated() {
            let routine = try Routine(
                routineSetID: routineSet.id,
                titleKey: step.titleKey,
                title: step.title,
                icon: step.icon,
                colorToken: step.colorToken,
                order: order,
                scheduledTime: step.scheduledTime,
                createdAt: createdAt,
                updatedAt: createdAt
            )
            try repository.createRoutine(routine)
        }

        selectedTemplateID = selectedTemplate.id
        return routineSet.id
    }
}
