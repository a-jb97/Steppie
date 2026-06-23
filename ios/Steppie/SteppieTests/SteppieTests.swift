import Foundation
import Testing
@testable import Steppie

@MainActor
struct SteppieTests {
    @Test("RoutineSet과 Routine을 생성하고 순서대로 조회한다")
    func createAndRead() throws {
        let fixture = try makeFixture(routineCount: 3)

        let fetchedSet = try fixture.repository.routineSet(id: fixture.routineSet.id)
        let fetchedRoutines = try fixture.repository.routines(in: fixture.routineSet.id)

        #expect(fetchedSet == fixture.routineSet)
        #expect(fetchedRoutines.map(\.id) == fixture.routines.map(\.id))
        #expect(fetchedRoutines.map(\.order) == [0, 1, 2])
        #expect(fetchedRoutines[0].scheduledTime?.description == "08:00")
    }

    @Test("Routine 내용을 수정하되 ID와 상위 세트는 유지한다")
    func updateRoutine() throws {
        let fixture = try makeFixture(routineCount: 1)
        let original = fixture.routines[0]
        let updatedAt = original.updatedAt.addingTimeInterval(60)
        let updated = try Routine(
            id: original.id,
            routineSetID: original.routineSetID,
            titleKey: "routine.brushTeeth",
            title: LocalizedText(["ko": "꼼꼼히 양치하기", "en": "Brush carefully"]),
            icon: IconRef.builtin(name: "brush-teeth"),
            colorToken: "color.card.mint",
            order: original.order,
            scheduledTime: LocalTime(hour: 8, minute: 30),
            isActive: true,
            createdAt: original.createdAt,
            updatedAt: updatedAt
        )

        try fixture.repository.updateRoutine(updated)

        #expect(try fixture.repository.routine(id: original.id) == updated)
        #expect(try fixture.repository.routineSet(id: fixture.routineSet.id)?.updatedAt == updatedAt)
    }

    @Test("Routine 삭제는 소프트 삭제하고 남은 순서를 압축한다")
    func softDeleteRoutine() throws {
        let fixture = try makeFixture(routineCount: 3)
        let deletedID = fixture.routines[1].id
        let deletedAt = fixture.routines[1].createdAt.addingTimeInterval(120)

        try fixture.repository.deleteRoutine(id: deletedID, at: deletedAt)

        let visible = try fixture.repository.routines(in: fixture.routineSet.id)
        let deleted = try fixture.repository.routine(id: deletedID)
        #expect(visible.map(\.id) == [fixture.routines[0].id, fixture.routines[2].id])
        #expect(visible.map(\.order) == [0, 1])
        #expect(deleted?.deletedAt == deletedAt)
        #expect(deleted?.isActive == false)
    }

    @Test("Routine 순서 변경은 0부터 연속된 order로 저장한다")
    func reorderRoutines() throws {
        let fixture = try makeFixture(routineCount: 3)
        let reversedIDs = fixture.routines.map(\.id).reversed()
        let reorderedAt = fixture.routineSet.createdAt.addingTimeInterval(180)

        try fixture.repository.reorderRoutines(
            in: fixture.routineSet.id,
            orderedIDs: Array(reversedIDs),
            at: reorderedAt
        )

        let fetched = try fixture.repository.routines(in: fixture.routineSet.id)
        #expect(fetched.map(\.id) == Array(reversedIDs))
        #expect(fetched.map(\.order) == [0, 1, 2])
        #expect(fetched.allSatisfy { $0.updatedAt == reorderedAt })
    }

    @Test("RoutineSet을 수정하고 비활성 세트를 소프트 삭제한다")
    func updateAndDeleteRoutineSet() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let original = try RoutineSet(
            name: LocalizedText(["ko": "주말 루틴"]),
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(original)

        let updatedAt = createdAt.addingTimeInterval(60)
        let updated = try RoutineSet(
            id: original.id,
            name: LocalizedText(["ko": "토요일 루틴", "en": "Saturday routine"]),
            createdAt: createdAt,
            updatedAt: updatedAt
        )
        try repository.updateRoutineSet(updated)
        #expect(try repository.routineSet(id: original.id) == updated)

        let deletedAt = createdAt.addingTimeInterval(120)
        try repository.deleteRoutineSet(id: original.id, at: deletedAt)
        #expect(try repository.routineSets().isEmpty)
        #expect(try repository.routineSet(id: original.id)?.deletedAt == deletedAt)
    }

    @Test("UUID, colorToken, LocalTime 규칙을 거부한다")
    func validatesContractValues() throws {
        let validSetID = UUID()
        let invalidUUID = UUID(uuidString: "00000000-0000-0000-0000-000000000000")!
        let title = try LocalizedText(["ko": "양치하기"])
        let icon = try IconRef.builtin(name: "brush-teeth")
        let encodedTitle = try JSONEncoder().encode(title)
        let titleObject = try JSONDecoder().decode([String: String].self, from: encodedTitle)

        #expect(titleObject == ["ko": "양치하기"])

        #expect(throws: RoutineDomainError.invalidLocalTime("8:00")) {
            try LocalTime("8:00")
        }
        #expect(throws: RoutineDomainError.invalidColorToken("blue")) {
            try Routine(
                routineSetID: validSetID,
                title: title,
                icon: icon,
                colorToken: "blue",
                order: 0
            )
        }
        #expect(throws: RoutineDomainError.uuidMustBeVersion4(field: "Routine.id")) {
            try Routine(
                id: invalidUUID,
                routineSetID: validSetID,
                title: title,
                icon: icon,
                order: 0
            )
        }
    }

    @Test("내장 아이콘은 공통 카탈로그에 등록된 이름만 허용한다")
    func validatesBuiltinIconCatalog() throws {
        for iconName in RoutineIconName.allCases {
            let icon = try IconRef.builtin(name: iconName.rawValue)
            #expect(icon.name == iconName.rawValue)
        }

        #expect(throws: RoutineDomainError.invalidBuiltinIconName("unknown-icon")) {
            try IconRef.builtin(name: "unknown-icon")
        }
    }
    @Test("활성 Routine의 중복 order를 거부한다")
    func rejectsDuplicateOrder() throws {
        let fixture = try makeFixture(routineCount: 1)
        let duplicate = try Routine(
            routineSetID: fixture.routineSet.id,
            title: LocalizedText(["ko": "아침 먹기"]),
            icon: IconRef.builtin(name: "breakfast"),
            order: 0,
            createdAt: fixture.routineSet.createdAt,
            updatedAt: fixture.routineSet.createdAt
        )

        #expect(
            throws: RoutineRepositoryError.duplicateOrder(
                routineSetID: fixture.routineSet.id,
                order: 0
            )
        ) {
            try fixture.repository.createRoutine(duplicate)
        }
    }

    @Test("Routine 생성 order는 현재 활성 개수의 next index여야 한다")
    func rejectsNonContiguousCreationOrder() throws {
        let fixture = try makeFixture(routineCount: 1)
        let skippedOrder = try Routine(
            routineSetID: fixture.routineSet.id,
            title: LocalizedText(["ko": "아침 먹기"]),
            icon: IconRef.builtin(name: "breakfast"),
            order: 2,
            createdAt: fixture.routineSet.createdAt,
            updatedAt: fixture.routineSet.createdAt
        )

        #expect(throws: RoutineRepositoryError.invalidNextOrder(expected: 1, actual: 2)) {
            try fixture.repository.createRoutine(skippedOrder)
        }
    }

    private func makeFixture(
        routineCount: Int
    ) throws -> (
        repository: SwiftDataRoutineRepository,
        routineSet: RoutineSet,
        routines: [Routine]
    ) {
        let repository = try RoutinePreviewStore.makeRepository()
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let routineSet = try RoutineSet(
            name: LocalizedText(["ko": "아침 루틴", "en": "Morning routine"]),
            isActive: true,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(routineSet)

        let iconNames = ["wake-up", "wash-face", "brush-teeth"]
        let routines = try (0..<routineCount).map { order in
            try Routine(
                routineSetID: routineSet.id,
                title: LocalizedText(["ko": "루틴 \(order)"]),
                icon: IconRef.builtin(name: iconNames[order]),
                colorToken: "color.card.sky",
                order: order,
                scheduledTime: order == 0 ? LocalTime(hour: 8, minute: 0) : nil,
                createdAt: createdAt,
                updatedAt: createdAt
            )
        }
        for routine in routines {
            try repository.createRoutine(routine)
        }
        return (repository, routineSet, routines)
    }
}
