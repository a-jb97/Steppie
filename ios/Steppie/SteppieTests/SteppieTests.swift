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
        try await Task.sleep(nanoseconds: 100_000_000)

        #expect(scheduler.authorizationRequestCount == 1)
        let call = try #require(scheduler.rescheduleCalls.first)
        #expect(call.completedRoutineIDs.isEmpty)
        #expect(call.settings.notificationLeadTimes == [10, 5])
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
        try await Task.sleep(nanoseconds: 100_000_000)
        let firstRoutine = try #require(viewModel.currentRoutine)

        viewModel.completeSelectedRoutine()
        try await Task.sleep(nanoseconds: 100_000_000)
        #expect(scheduler.rescheduleCalls.last?.completedRoutineIDs == [firstRoutine.id])

        viewModel.undoLastCompletion()
        try await Task.sleep(nanoseconds: 100_000_000)
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

    @Test("보호자 PIN은 4자리 숫자만 허용하고 원본을 저장하지 않는다")
    func guardianPINHashing() throws {
        let hash = try GuardianPinService.makeHash(for: "1234", salt: "test-salt")

        #expect(hash != "1234")
        #expect(GuardianPinService.verify("1234", against: hash))
        #expect(!GuardianPinService.verify("0000", against: hash))
        #expect(throws: GuardianPinError.invalidPIN) {
            try GuardianPinService.makeHash(for: "12a4")
        }
    }

    @Test("보호자 ViewModel은 PIN 설정과 검증을 AppSettings에 저장한다")
    func guardianViewModelStoresPINHash() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}

        #expect(!viewModel.hasGuardianPIN())
        #expect(viewModel.setPIN("1234"))
        #expect(viewModel.hasGuardianPIN())
        #expect(viewModel.verifyPIN("1234"))
        #expect(!viewModel.verifyPIN("0000"))
        #expect(try repository.appSettings().guardianPinHash != "1234")
    }

    @Test("보호자 루틴 편집은 추가 수정 삭제 순서 변경을 Repository와 연결한다")
    func guardianRoutineEditing() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        var changeCount = 0
        let viewModel = GuardianModeViewModel(repository: repository) {
            changeCount += 1
        }
        viewModel.load()
        let activeSetID = try #require(viewModel.activeRoutineSet?.id)

        viewModel.beginAddRoutine()
        viewModel.draft?.title = "학교 버스"
        viewModel.draft?.iconName = .bus
        viewModel.draft?.colorToken = "color.card.lemon"
        viewModel.draft?.scheduledTime = try LocalTime("08:30")
        viewModel.saveDraft(localeIdentifier: "ko")

        var routines = try repository.routines(in: activeSetID)
        #expect(routines.count == 4)
        #expect(routines.map(\.order) == [0, 1, 2, 3])
        let added = try #require(routines.last)
        #expect(added.title.resolved(appLocale: "ko") == "학교 버스")

        viewModel.beginEditRoutine(added)
        viewModel.draft?.title = "버스 타기"
        viewModel.saveDraft(localeIdentifier: "ko")
        #expect(try repository.routine(id: added.id)?.title.resolved(appLocale: "ko") == "버스 타기")

        viewModel.moveRoutine(try #require(viewModel.routines.last), direction: -1)
        routines = try repository.routines(in: activeSetID)
        #expect(routines.map(\.order) == [0, 1, 2, 3])

        viewModel.requestDelete(try #require(viewModel.routines.first))
        viewModel.confirmDelete()
        routines = try repository.routines(in: activeSetID)
        #expect(routines.count == 3)
        #expect(routines.map(\.order) == [0, 1, 2])
        #expect(changeCount >= 4)
    }

    @Test("보호자 모드는 활성 루틴 세트가 없어도 루틴 세트 생성으로 진입할 수 있다")
    func guardianModeCanCreateRoutineSetFromEmptyRepository() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}

        viewModel.load()
        #expect(viewModel.loadState == .empty)
        #expect(viewModel.activeRoutineSet == nil)

        viewModel.beginCreateRoutineSet()
        #expect(viewModel.selectedDestination == .routineSetCreator)
        #expect(viewModel.routineSetDraft?.steps.isEmpty == true)
        #expect(!viewModel.canSaveRoutineSetDraft)
    }

    @Test("루틴 세트 생성은 최소 1개 단계를 요구한다")
    func guardianRoutineSetCreationRequiresAtLeastOneStep() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "등원 루틴"
        #expect(!viewModel.canSaveRoutineSetDraft)

        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "가방 챙기기"
        viewModel.routineSetStepDraft?.iconName = .packBag
        viewModel.routineSetStepDraft?.colorToken = "color.card.mint"
        viewModel.saveRoutineSetStepDraft()

        #expect(viewModel.routineSetDraft?.steps.count == 1)
        #expect(viewModel.canSaveRoutineSetDraft)
    }

    @Test("루틴 세트 생성은 활성 세트와 단계들을 0부터 연속 order로 저장한다")
    func guardianRoutineSetCreationSavesActiveSetAndOrderedSteps() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        var changeCount = 0
        let viewModel = GuardianModeViewModel(repository: repository) {
            changeCount += 1
        }

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "아침 준비"

        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "일어나기"
        viewModel.routineSetStepDraft?.iconName = .wakeUp
        viewModel.routineSetStepDraft?.colorToken = "color.card.sky"
        viewModel.saveRoutineSetStepDraft()

        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "세수하기"
        viewModel.routineSetStepDraft?.iconName = .washFace
        viewModel.routineSetStepDraft?.colorToken = "color.card.lemon"
        viewModel.routineSetStepDraft?.scheduledTime = try LocalTime("07:40")
        viewModel.saveRoutineSetStepDraft()

        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")

        let activeSet = try #require(try repository.routineSets().first(where: \.isActive))
        let routines = try repository.routines(in: activeSet.id)
        #expect(activeSet.name.resolved(appLocale: "ko") == "아침 준비")
        #expect(routines.map { $0.title.resolved(appLocale: "ko") } == ["일어나기", "세수하기"])
        #expect(routines.map(\.order) == [0, 1])
        #expect(routines.map(\.colorToken) == ["color.card.sky", "color.card.lemon"])
        #expect(routines[1].scheduledTime?.description == "07:40")
        #expect(viewModel.loadState == .loaded)
        #expect(viewModel.activeRoutineSet?.id == activeSet.id)
        #expect(viewModel.selectedDestination == .routineEditor)
        #expect(changeCount == 1)
    }

    @Test("루틴 관리는 여러 루틴 세트를 목록으로 유지하고 선택한 세트의 단계만 편집한다")
    func guardianRoutineManagementKeepsMultipleRoutineSets() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        viewModel.load()
        let originalSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "하교 루틴"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "가방 정리"
        viewModel.routineSetStepDraft?.iconName = .packBag
        viewModel.routineSetStepDraft?.colorToken = "color.card.peach"
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")

        #expect(viewModel.routineSets.count == 2)
        #expect(viewModel.routineSets.map { $0.name.resolved(appLocale: "ko") } == ["아침 루틴", "하교 루틴"])
        #expect(viewModel.activeRoutineSet?.name.resolved(appLocale: "ko") == "하교 루틴")
        #expect(viewModel.selectedRoutineSet?.name.resolved(appLocale: "ko") == "하교 루틴")
        #expect(viewModel.routines.map { $0.title.resolved(appLocale: "ko") } == ["가방 정리"])

        viewModel.selectRoutineSet(originalSet)
        #expect(viewModel.selectedRoutineSet?.id == originalSet.id)
        #expect(viewModel.routines.map { $0.title.resolved(appLocale: "ko") } == ["일어나기", "세수하기", "양치하기"])

        viewModel.beginAddRoutine()
        viewModel.draft?.title = "물 마시기"
        viewModel.draft?.iconName = .snack
        viewModel.draft?.colorToken = "color.card.mint"
        viewModel.saveDraft(localeIdentifier: "ko")

        let originalRoutines = try repository.routines(in: originalSet.id)
        let latestActiveSet = try #require(try repository.routineSets().first(where: \.isActive))
        let latestActiveRoutines = try repository.routines(in: latestActiveSet.id)
        #expect(originalRoutines.map { $0.title.resolved(appLocale: "ko") }.contains("물 마시기"))
        #expect(latestActiveRoutines.map { $0.title.resolved(appLocale: "ko") } == ["가방 정리"])
    }

    @Test("루틴 세트 편집 모드는 세트 이름 변경과 세트 삭제를 Repository에 반영한다")
    func guardianRoutineSetEditModeRenamesAndDeletesRoutineSets() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        viewModel.load()
        let originalSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "하교 루틴"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "가방 정리"
        viewModel.routineSetStepDraft?.iconName = .packBag
        viewModel.routineSetStepDraft?.colorToken = "color.card.peach"
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")
        let createdSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginRenameRoutineSet(originalSet)
        viewModel.routineSetNameDraft?.name = "평일 아침"
        viewModel.saveRoutineSetName(localeIdentifier: "ko")
        #expect(try repository.routineSet(id: originalSet.id)?.name.resolved(appLocale: "ko") == "평일 아침")

        viewModel.requestDeleteRoutineSet(createdSet)
        viewModel.confirmDeleteRoutineSet()

        let visibleSets = try repository.routineSets()
        #expect(visibleSets.map(\.id) == [originalSet.id])
        #expect(visibleSets.first?.isActive == true)
        #expect(viewModel.routineSets.map(\.id) == [originalSet.id])
        #expect(viewModel.selectedRoutineSet?.id == originalSet.id)
    }

    @Test("새 루틴 세트 생성 후 아이 모드는 새 활성 세트를 읽는다")
    func childRoutineLoadsNewlyCreatedRoutineSet() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let childViewModel = ChildRoutineViewModel(repository: repository)
        let guardianViewModel = GuardianModeViewModel(repository: repository) {
            childViewModel.load()
        }

        childViewModel.load()
        #expect(childViewModel.loadState == .empty)

        guardianViewModel.beginCreateRoutineSet()
        guardianViewModel.routineSetDraft?.name = "저녁 루틴"
        guardianViewModel.beginAddRoutineSetStep()
        guardianViewModel.routineSetStepDraft?.title = "책 읽기"
        guardianViewModel.routineSetStepDraft?.iconName = .book
        guardianViewModel.routineSetStepDraft?.colorToken = "color.card.lavender"
        guardianViewModel.saveRoutineSetStepDraft()
        guardianViewModel.saveRoutineSetDraft(localeIdentifier: "ko")

        #expect(childViewModel.loadState == .loaded)
        #expect(childViewModel.activeRoutineSet?.name.resolved(appLocale: "ko") == "저녁 루틴")
        #expect(childViewModel.routines.map { $0.title.resolved(appLocale: "ko") } == ["책 읽기"])
        #expect(childViewModel.selectedRoutineID == childViewModel.routines.first?.id)
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
