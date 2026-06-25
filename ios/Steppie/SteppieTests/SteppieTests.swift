import Foundation
import Testing
@testable import Steppie

@MainActor
struct SteppieTests {
    @Test("아이 모드가 활성 루틴을 순서대로 불러오고 첫 항목을 current로 선택한다")
    func childRoutineViewModelLoadsActiveRoutines() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = ChildRoutineViewModel(repository: repository)

        viewModel.load()

        #expect(viewModel.loadState == .loaded)
        #expect(viewModel.routines.map(\.order) == [0, 1, 2])
        #expect(viewModel.selectedRoutineID == viewModel.routines.first?.id)
        #expect(viewModel.currentRoutine?.id == viewModel.routines.first?.id)
        #expect(viewModel.completedCount == 0)
        #expect(viewModel.totalCount == 3)
    }

    @Test("목록의 upcoming 항목 선택은 포커스만 바꾸고 저장 데이터를 변경하지 않는다")
    func childRoutineSelectionIsReadOnly() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = ChildRoutineViewModel(repository: repository)
        viewModel.load()
        let activeSetID = try #require(viewModel.activeRoutineSet?.id)
        let originalRoutines = try repository.routines(in: activeSetID)
        let upcoming = try #require(viewModel.routines.last)

        viewModel.showList()
        viewModel.selectRoutine(upcoming, showFocus: true)

        #expect(viewModel.page == .focus)
        #expect(viewModel.selectedRoutineID == upcoming.id)
        #expect(viewModel.cardState(for: upcoming) == .upcoming)
        #expect(try repository.routines(in: upcoming.routineSetID) == originalRoutines)
    }

    @Test("포커스 카드 완료는 피드백 화면을 유지하고 다음 카드 선택 후 current를 이동한다")
    func childRoutineCompletionWaitsForManualAdvance() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        let completedDate = DailyLog.localDateString(for: completedAt)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { completedAt }
        )
        viewModel.load()
        let firstRoutine = try #require(viewModel.currentRoutine)

        viewModel.completeSelectedRoutine()
        #expect(viewModel.completedCount == 1)
        #expect(viewModel.cardState(for: firstRoutine) == .completed)
        #expect(try repository.dailyLog(on: completedDate, routineID: firstRoutine.id)?.status == .completed)
        #expect(viewModel.isShowingCompletionFeedback)
        #expect(viewModel.selectedRoutine?.id == firstRoutine.id)
        #expect(viewModel.nextRoutineAfterFeedback?.id == viewModel.routines[1].id)
        #expect(viewModel.cardState(for: viewModel.routines[1]) == .upcoming)

        viewModel.proceedAfterCompletionFeedback()
        #expect(viewModel.currentRoutine?.id == viewModel.routines[1].id)
        #expect(viewModel.selectedRoutineID == viewModel.routines[1].id)
        #expect(viewModel.cardState(for: viewModel.routines[1]) == .current)
    }

    @Test("완료 직후 undo는 DailyLog를 undone으로 되돌리고 같은 루틴을 다시 current로 만든다")
    func childRoutineUndoRestoresCompletedRoutine() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        let completedDate = DailyLog.localDateString(for: completedAt)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { completedAt }
        )
        viewModel.load()
        let firstRoutine = try #require(viewModel.currentRoutine)

        viewModel.completeSelectedRoutine()
        viewModel.undoLastCompletion()

        #expect(viewModel.completedCount == 0)
        #expect(viewModel.currentRoutine?.id == firstRoutine.id)
        #expect(viewModel.cardState(for: firstRoutine) == .current)
        #expect(try repository.dailyLog(on: completedDate, routineID: firstRoutine.id)?.status == .undone)
    }

    @Test("모든 루틴 완료 후 사용자가 진행하면 전체 완료 상태가 된다")
    func childRoutineAllDoneState() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { completedAt }
        )
        viewModel.load()

        for _ in viewModel.routines {
            viewModel.completeSelectedRoutine()
            #expect(viewModel.isShowingCompletionFeedback)
            viewModel.proceedAfterCompletionFeedback()
        }

        #expect(viewModel.completedCount == 3)
        #expect(viewModel.currentRoutine == nil)
        #expect(viewModel.isAllCompleted)
    }

    @Test("포커스 로드는 알림 권한을 요청하고 오늘 남은 루틴 알림을 재예약한다")
    func childRoutineLoadRequestsNotificationAuthorizationAndSchedulesReminders() async throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let scheduler = FakeRoutineNotificationScheduler()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            notificationScheduler: scheduler,
            now: { now },
            locale: { Locale(identifier: "ko_KR") }
        )

        viewModel.load()
        try await Task.sleep(nanoseconds: 10_000_000)

        #expect(scheduler.authorizationRequestCount == 1)
        #expect(scheduler.rescheduleCalls.count == 1)
        #expect(scheduler.rescheduleCalls[0].completedRoutineIDs.isEmpty)
        #expect(scheduler.rescheduleCalls[0].settings.notificationLeadTimes == [10, 5])
    }

    @Test("완료와 undo는 알림 예약 상태를 다시 계산한다")
    func childRoutineCompletionAndUndoRescheduleReminders() async throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let scheduler = FakeRoutineNotificationScheduler()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            notificationScheduler: scheduler,
            now: { now },
            locale: { Locale(identifier: "ko_KR") }
        )
        viewModel.load()
        try await Task.sleep(nanoseconds: 10_000_000)
        let firstRoutine = try #require(viewModel.currentRoutine)

        viewModel.completeSelectedRoutine()
        try await Task.sleep(nanoseconds: 10_000_000)
        #expect(scheduler.rescheduleCalls.last?.completedRoutineIDs == [firstRoutine.id])

        viewModel.undoLastCompletion()
        try await Task.sleep(nanoseconds: 10_000_000)
        #expect(scheduler.rescheduleCalls.last?.completedRoutineIDs.isEmpty == true)
    }

    @Test("알림 요청 계산은 완료, 과거 시각, 알림 없음 설정, 방해 금지 시간을 제외한다")
    func notificationRequestCalculationFiltersIneligibleReminders() throws {
        let fixture = try makeFixture(routineCount: 1)
        let routine = fixture.routines[0]
        let date = "2026-01-01"
        let calendar = Calendar(identifier: .gregorian)
        let beforeRoutine = try #require(calendar.date(from: DateComponents(
            year: 2026,
            month: 1,
            day: 1,
            hour: 7,
            minute: 52
        )))

        let requests = IOSRoutineNotificationScheduler.notificationRequests(
            routines: fixture.routines,
            completedRoutineIDs: [],
            routineSetID: fixture.routineSet.id,
            date: date,
            settings: try AppSettings(notificationLeadTimes: [10, 5]),
            now: beforeRoutine,
            calendar: calendar,
            locale: Locale(identifier: "ko_KR")
        )
        #expect(requests.map(\.routineID) == [routine.id])
        #expect(requests.map(\.body) == ["5분 뒤 루틴 0을 시작해요."])

        let completedRequests = IOSRoutineNotificationScheduler.notificationRequests(
            routines: fixture.routines,
            completedRoutineIDs: [routine.id],
            routineSetID: fixture.routineSet.id,
            date: date,
            settings: try AppSettings(notificationLeadTimes: [10, 5]),
            now: beforeRoutine,
            calendar: calendar,
            locale: Locale(identifier: "ko_KR")
        )
        #expect(completedRequests.isEmpty)

        let disabledRequests = IOSRoutineNotificationScheduler.notificationRequests(
            routines: fixture.routines,
            completedRoutineIDs: [],
            routineSetID: fixture.routineSet.id,
            date: date,
            settings: try AppSettings(notificationLeadTimes: []),
            now: beforeRoutine,
            calendar: calendar,
            locale: Locale(identifier: "ko_KR")
        )
        #expect(disabledRequests.isEmpty)

        let quietRequests = IOSRoutineNotificationScheduler.notificationRequests(
            routines: fixture.routines,
            completedRoutineIDs: [],
            routineSetID: fixture.routineSet.id,
            date: date,
            settings: try AppSettings(
                notificationLeadTimes: [5],
                quietHoursStart: LocalTime(hour: 7, minute: 0),
                quietHoursEnd: LocalTime(hour: 8, minute: 0)
            ),
            now: beforeRoutine,
            calendar: calendar,
            locale: Locale(identifier: "ko_KR")
        )
        #expect(quietRequests.isEmpty)
    }

    @Test("AppSettings는 알림 리드타임과 방해 금지 시간을 검증한다")
    func appSettingsNotificationFieldsValidate() throws {
        let settings = try AppSettings(
            notificationLeadTimes: [10, 5],
            quietHoursStart: LocalTime(hour: 22, minute: 0),
            quietHoursEnd: LocalTime(hour: 7, minute: 0)
        )

        #expect(settings.notificationLeadTimes == [10, 5])
        #expect(settings.quietHoursStart?.description == "22:00")
        #expect(settings.quietHoursEnd?.description == "07:00")
        #expect(throws: RoutineDomainError.invalidNotificationLeadTimes([5, 5])) {
            try AppSettings(notificationLeadTimes: [5, 5])
        }
        #expect(throws: RoutineDomainError.invalidNotificationLeadTimes([15])) {
            try AppSettings(notificationLeadTimes: [15])
        }
    }

    @Test("알림 payload는 루틴 포커스 route로 변환된다")
    func notificationPayloadBuildsRoute() throws {
        let routineID = UUID()
        let routineSetID = UUID()
        let route = try #require(RoutineNotificationRoute(userInfo: [
            "routineID": routineID.uuidString,
            "routineSetID": routineSetID.uuidString,
            "date": "2026-01-01",
        ]))

        #expect(route.routineID == routineID)
        #expect(route.routineSetID == routineSetID)
        #expect(route.date == "2026-01-01")
        #expect(RoutineNotificationRoute(userInfo: ["routineID": "bad"]) == nil)
    }

    @Test("load 전 수신한 알림 route는 load 후 해당 루틴을 포커스한다")
    func pendingNotificationRouteFocusesRoutineAfterLoad() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let date = DailyLog.localDateString(for: now)
        let routines = try repository.routines(in: repository.routineSets()[0].id)
        let target = routines[2]
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { now }
        )

        viewModel.openNotificationRoute(
            RoutineNotificationRoute(
                routineID: target.id,
                routineSetID: target.routineSetID,
                date: date
            )
        )
        viewModel.load()

        #expect(viewModel.page == .focus)
        #expect(viewModel.selectedRoutineID == target.id)
    }

    @Test("loaded 상태에서 수신한 알림 route는 즉시 해당 루틴을 포커스한다")
    func loadedNotificationRouteFocusesRoutineImmediately() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let date = DailyLog.localDateString(for: now)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { now }
        )
        viewModel.load()
        let target = viewModel.routines[2]

        viewModel.openNotificationRoute(
            RoutineNotificationRoute(
                routineID: target.id,
                routineSetID: target.routineSetID,
                date: date
            )
        )

        #expect(viewModel.page == .focus)
        #expect(viewModel.selectedRoutineID == target.id)
    }

    @Test("다른 날짜나 없는 루틴의 알림 route는 무시한다")
    func invalidNotificationRouteIsIgnored() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            now: { now }
        )
        viewModel.load()
        let originalSelectedID = viewModel.selectedRoutineID
        let activeSetID = try #require(viewModel.activeRoutineSet?.id)

        viewModel.openNotificationRoute(
            RoutineNotificationRoute(
                routineID: UUID(),
                routineSetID: activeSetID,
                date: "2026-01-01"
            )
        )

        #expect(viewModel.selectedRoutineID == originalSelectedID)
    }

    @Test("반응형 레이아웃은 905pt에서 split pane으로 전환한다")
    func childRoutineResponsiveBreakpoint() {
        #expect(SteppieLayout.splitMinimumWidth == 905)
        #expect(ChildRoutineLayoutPolicy.layout(for: 904) == .singlePane)
        #expect(ChildRoutineLayoutPolicy.layout(for: 905) == .splitPane)
    }

    @Test("활성 루틴 세트가 없으면 아이 모드는 빈 상태를 표시한다")
    func childRoutineViewModelHandlesEmptyRepository() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let viewModel = ChildRoutineViewModel(repository: repository)

        viewModel.load()

        #expect(viewModel.loadState == .empty)
        #expect(viewModel.routines.isEmpty)
        #expect(viewModel.selectedRoutine == nil)
    }

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

    @Test("DailyLog 완료와 되돌리기는 date + routineId 조합을 하나로 유지한다")
    func completeAndUndoDailyLog() throws {
        let fixture = try makeFixture(routineCount: 1)
        let routine = fixture.routines[0]
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        let completedDate = DailyLog.localDateString(for: completedAt)

        let completed = try fixture.repository.setRoutineCompleted(
            routineID: routine.id,
            routineSetID: fixture.routineSet.id,
            on: completedDate,
            at: completedAt
        )
        let undone = try fixture.repository.undoRoutineCompletion(
            routineID: routine.id,
            on: completedDate,
            at: completedAt.addingTimeInterval(10)
        )

        let logs = try fixture.repository.dailyLogs(on: completedDate, routineSetID: fixture.routineSet.id)
        #expect(completed.status == .completed)
        #expect(undone?.id == completed.id)
        #expect(undone?.status == .undone)
        #expect(logs.count == 1)
        #expect(logs[0].completedAt == nil)
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

    @Test("유일한 활성 RoutineSet은 직접 비활성화할 수 없다")
    func rejectsDeactivatingOnlyActiveRoutineSet() throws {
        let fixture = try makeFixture(routineCount: 0)
        let inactive = try RoutineSet(
            id: fixture.routineSet.id,
            name: fixture.routineSet.name,
            isActive: false,
            createdAt: fixture.routineSet.createdAt,
            updatedAt: fixture.routineSet.updatedAt.addingTimeInterval(60)
        )

        #expect(
            throws: RoutineRepositoryError.cannotDeactivateOnlyActiveRoutineSet(
                fixture.routineSet.id
            )
        ) {
            try fixture.repository.updateRoutineSet(inactive)
        }
        #expect(try fixture.repository.routineSet(id: fixture.routineSet.id)?.isActive == true)
    }

    @Test("다른 RoutineSet 활성화는 기존 세트를 비활성화하고 활성 세트를 하나로 유지한다")
    func switchesActiveRoutineSet() throws {
        let fixture = try makeFixture(routineCount: 0)
        let createdAt = fixture.routineSet.createdAt.addingTimeInterval(60)
        let second = try RoutineSet(
            name: LocalizedText(["ko": "저녁 루틴"]),
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try fixture.repository.createRoutineSet(second)

        let activated = try RoutineSet(
            id: second.id,
            name: second.name,
            isActive: true,
            createdAt: second.createdAt,
            updatedAt: second.updatedAt.addingTimeInterval(60)
        )
        try fixture.repository.updateRoutineSet(activated)

        let activeSets = try fixture.repository.routineSets().filter(\.isActive)
        #expect(activeSets.map(\.id) == [second.id])
        #expect(try fixture.repository.routineSet(id: fixture.routineSet.id)?.isActive == false)
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

@MainActor
private final class FakeRoutineNotificationScheduler: RoutineNotificationScheduling {
    struct RescheduleCall: Equatable {
        let completedRoutineIDs: Set<UUID>
        let settings: AppSettings
    }

    var authorizationRequestCount = 0
    var rescheduleCalls: [RescheduleCall] = []

    func requestAuthorizationIfNeeded() async -> RoutineNotificationAuthorizationStatus {
        authorizationRequestCount += 1
        return .authorized
    }

    func rescheduleTodayReminders(
        routines: [Routine],
        completedRoutineIDs: Set<UUID>,
        routineSetID: UUID,
        date: String,
        settings: AppSettings,
        now: Date,
        calendar: Calendar,
        locale: Locale
    ) async {
        rescheduleCalls.append(
            RescheduleCall(
                completedRoutineIDs: completedRoutineIDs,
                settings: settings
            )
        )
    }
}
