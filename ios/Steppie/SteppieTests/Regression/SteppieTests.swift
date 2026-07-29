import CryptoKit
import Foundation
import Testing
@testable import Steppie

@MainActor
struct SteppieTests {
    @Test("아이 모드가 활성 루틴을 순서대로 불러오고 첫 항목을 current로 선택한다")
    func childRoutineViewModelLoadsActiveRoutines() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        try assignActiveRoutineSet(in: repository, on: Date())
        let viewModel = ChildRoutineViewModel(repository: repository)

        viewModel.load()

        #expect(viewModel.loadState == .loaded)
        #expect(viewModel.routines.map(\.order) == [0, 1, 2])
        #expect(viewModel.selectedRoutineID == viewModel.routines.first?.id)
        #expect(viewModel.currentRoutine?.id == viewModel.routines.first?.id)
        #expect(viewModel.completedCount == 0)
        #expect(viewModel.totalCount == 3)
    }

    @Test("아이 모드는 오늘 배정된 루틴 세트를 활성 세트보다 우선 불러온다")
    func childRoutineViewModelLoadsAssignedRoutineSetForToday() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_312_000)
        let date = DailyLog.localDateString(for: now)
        let afternoonSet = try RoutineSet(
            name: LocalizedText(["ko": "오후 루틴"]),
            isActive: false,
            createdAt: now,
            updatedAt: now
        )
        try repository.createRoutineSet(afternoonSet)
        let routine = try Routine(
            routineSetID: afternoonSet.id,
            title: LocalizedText(["ko": "간식 먹기"]),
            icon: IconRef.builtin(name: "snack"),
            colorToken: "color.card.mint",
            order: 0,
            createdAt: now,
            updatedAt: now
        )
        try repository.createRoutine(routine)
        _ = try repository.assignRoutineSet(afternoonSet.id, on: date, at: now)

        let viewModel = ChildRoutineViewModel(repository: repository, now: { now })
        viewModel.load()

        #expect(viewModel.activeRoutineSet?.id == afternoonSet.id)
        #expect(viewModel.routines.map { $0.title.resolved(appLocale: "ko") } == ["간식 먹기"])
    }

    @Test("목록의 upcoming 항목 선택은 포커스만 바꾸고 저장 데이터를 변경하지 않는다")
    func childRoutineSelectionIsReadOnly() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        try assignActiveRoutineSet(in: repository, on: Date())
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

    @Test("오늘의 순서에서 지금 할 일을 누르면 현재 루틴으로 돌아간다")
    func childRoutineShowFocusReturnsToCurrentRoutine() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        try assignActiveRoutineSet(in: repository, on: Date())
        let viewModel = ChildRoutineViewModel(repository: repository)
        viewModel.load()
        let current = try #require(viewModel.currentRoutine)
        let upcoming = try #require(viewModel.routines.last)

        viewModel.showList()
        viewModel.selectRoutine(upcoming, showFocus: true)
        #expect(viewModel.selectedRoutineID == upcoming.id)

        viewModel.showList()
        viewModel.showFocus()

        #expect(viewModel.page == .focus)
        #expect(viewModel.selectedRoutineID == current.id)
        #expect(viewModel.selectedRoutine?.id == current.id)
        #expect(viewModel.cardState(for: current) == .current)
    }

    @Test("루틴 음성은 지금 할 일 화면에서만 재생한다")
    func childRoutineSpeechOnlyPlaysOnFocusScreen() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let speechGuide = FakeRoutineSpeechGuide()
        try assignActiveRoutineSet(in: repository, on: Date())
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            speechGuide: speechGuide
        )

        viewModel.load()
        #expect(speechGuide.spokenTexts.count == 1)

        viewModel.showList()
        viewModel.load()
        #expect(speechGuide.spokenTexts.count == 1)
        #expect(speechGuide.stopCallCount == 1)

        viewModel.showFocus()
        #expect(speechGuide.spokenTexts.count == 2)

        viewModel.setRoutineSpeechActive(false)
        viewModel.load()
        #expect(speechGuide.spokenTexts.count == 2)
        #expect(speechGuide.stopCallCount == 2)
    }

    @Test("포커스 카드 완료는 피드백 화면을 유지하고 다음 카드 선택 후 current를 이동한다")
    func childRoutineCompletionWaitsForManualAdvance() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let speechGuide = FakeRoutineSpeechGuide()
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        let completedDate = DailyLog.localDateString(for: completedAt)
        try assignActiveRoutineSet(in: repository, on: completedAt)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            speechGuide: speechGuide,
            now: { completedAt }
        )
        viewModel.load()
        let firstRoutine = try #require(viewModel.currentRoutine)

        viewModel.completeSelectedRoutine()
        #expect(viewModel.completedCount == 1)
        #expect(speechGuide.spokenTexts.contains("일어나기 완료! 잘했어요!"))
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
        try assignActiveRoutineSet(in: repository, on: completedAt)
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
        try assignActiveRoutineSet(in: repository, on: completedAt)
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

    @Test("첫 루틴 세트 시작 전에는 다음 세트와 시작 시각을 표시하며 대기한다")
    func childRoutineWaitsUntilFirstScheduledSetStarts() throws {
        let calendar = Calendar(identifier: .gregorian)
        let now = try #require(calendar.date(from: DateComponents(
            year: 2026, month: 1, day: 2, hour: 7, minute: 30
        )))
        let fixture = try makeScheduledRoutinePlan(createdAt: now)
        let viewModel = ChildRoutineViewModel(
            repository: fixture.repository,
            now: { now },
            calendar: calendar,
            isNotificationSchedulingEnabled: false
        )

        viewModel.load()

        #expect(viewModel.loadState == .loaded)
        #expect(viewModel.isWaitingForNextRoutineSet)
        #expect(viewModel.activeRoutineSet == nil)
        #expect(viewModel.nextScheduledRoutineSet?.id == fixture.morningSet.id)
    }

    @Test("현재 세트를 일찍 완료하면 다음 세트 시작 시각까지 대기한다")
    func childRoutineWaitsAfterEarlySetCompletion() throws {
        let calendar = Calendar(identifier: .gregorian)
        let now = try #require(calendar.date(from: DateComponents(
            year: 2026, month: 1, day: 2, hour: 8, minute: 30
        )))
        let fixture = try makeScheduledRoutinePlan(createdAt: now)
        let viewModel = ChildRoutineViewModel(
            repository: fixture.repository,
            now: { now },
            calendar: calendar,
            isNotificationSchedulingEnabled: false
        )
        viewModel.load()

        #expect(viewModel.activeRoutineSet?.id == fixture.morningSet.id)
        viewModel.completeSelectedRoutine()
        #expect(!viewModel.canStartNextRoutineSetAfterFeedback)
        viewModel.proceedAfterCompletionFeedback()

        #expect(viewModel.isWaitingForNextRoutineSet)
        #expect(viewModel.nextScheduledRoutineSet?.id == fixture.schoolSet.id)
        #expect(!viewModel.isAllCompleted)

        viewModel.completeSelectedRoutine()
        let date = DailyLog.localDateString(for: now, calendar: calendar)
        let schoolRoutine = try #require(
            try fixture.repository.routines(in: fixture.schoolSet.id).first
        )
        #expect(try fixture.repository.dailyLog(on: date, routineID: schoolRoutine.id) == nil)
    }

    @Test("다음 세트 시각이 지나도 이전 세트가 미완료면 이전 세트를 우선한다")
    func childRoutineKeepsIncompleteEarlierSetAfterNextStartTime() throws {
        let calendar = Calendar(identifier: .gregorian)
        let now = try #require(calendar.date(from: DateComponents(
            year: 2026, month: 1, day: 2, hour: 9, minute: 30
        )))
        let fixture = try makeScheduledRoutinePlan(createdAt: now)
        let viewModel = ChildRoutineViewModel(
            repository: fixture.repository,
            now: { now },
            calendar: calendar,
            isNotificationSchedulingEnabled: false
        )
        viewModel.load()

        #expect(viewModel.activeRoutineSet?.id == fixture.morningSet.id)
        viewModel.completeSelectedRoutine()
        #expect(viewModel.canStartNextRoutineSetAfterFeedback)
        viewModel.proceedAfterCompletionFeedback()

        #expect(viewModel.activeRoutineSet?.id == fixture.schoolSet.id)
        #expect(viewModel.currentRoutine?.routineSetID == fixture.schoolSet.id)
    }

    @Test("포커스 로드는 알림 권한을 요청하고 오늘 남은 루틴 알림을 재예약한다")
    func childRoutineLoadRequestsNotificationAuthorizationAndSchedulesReminders() async throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let scheduler = FakeRoutineNotificationScheduler()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        try assignActiveRoutineSet(in: repository, on: now)
        let viewModel = ChildRoutineViewModel(
            repository: repository,
            notificationScheduler: scheduler,
            now: { now },
            locale: { Locale(identifier: "ko_KR") }
        )

        viewModel.load()
        for _ in 0..<20 where scheduler.rescheduleCalls.isEmpty {
            try await Task.sleep(nanoseconds: 100_000_000)
        }

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
        try assignActiveRoutineSet(in: repository, on: now)
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

    @Test("보호자 복구 코드는 6자리 숫자만 허용하고 원본을 저장하지 않는다")
    func guardianRecoveryCodeHashing() throws {
        let generatedCode = GuardianPinService.generateRecoveryCode()
        let hash = try GuardianPinService.makeRecoveryCodeHash(for: "123456", salt: "recovery-test-salt")

        #expect(generatedCode.count == 6)
        #expect(generatedCode.allSatisfy { $0.isNumber })
        #expect(hash != "123456")
        #expect(GuardianPinService.verifyRecoveryCode("123456", against: hash))
        #expect(!GuardianPinService.verifyRecoveryCode("000000", against: hash))
        #expect(throws: GuardianPinError.invalidRecoveryCode) {
            try GuardianPinService.makeRecoveryCodeHash(for: "1234")
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

    @Test("보호자 ViewModel은 PIN 생성 시 복구 코드 hash를 저장하고 원본은 1회 표시 후 폐기한다")
    func guardianViewModelStoresRecoveryCodeHashOnPINSetup() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}

        #expect(viewModel.setPINAndGenerateRecoveryCode("1234"))
        let recoveryCode = try #require(viewModel.oneTimeRecoveryCode)
        let settings = try repository.appSettings()

        #expect(recoveryCode.count == 6)
        #expect(settings.recoveryCodeHash != nil)
        #expect(settings.recoveryCodeHash != recoveryCode)
        #expect(viewModel.verifyRecoveryCode(recoveryCode))

        viewModel.clearOneTimeRecoveryCode()
        #expect(viewModel.oneTimeRecoveryCode == nil)
        #expect(viewModel.verifyRecoveryCode(recoveryCode))
    }

    @Test("보호자 ViewModel은 복구 코드 재생성 시 이전 코드를 무효화한다")
    func guardianViewModelRegeneratesRecoveryCode() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}

        #expect(viewModel.setPINAndGenerateRecoveryCode("1234"))
        let firstCode = try #require(viewModel.oneTimeRecoveryCode)
        let firstHash = try #require(try repository.appSettings().recoveryCodeHash)

        #expect(viewModel.regenerateRecoveryCode())
        let secondCode = try #require(viewModel.oneTimeRecoveryCode)
        let secondHash = try #require(try repository.appSettings().recoveryCodeHash)

        #expect(firstHash != secondHash)
        #expect(!viewModel.verifyRecoveryCode(firstCode))
        #expect(viewModel.verifyRecoveryCode(secondCode))
    }

    @Test("보호자 환경 설정은 AppSettings에 저장되고 변경 콜백을 호출한다")
    func guardianFeedbackSettingsPersist() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        var changeCount = 0
        let viewModel = GuardianModeViewModel(repository: repository) {
            changeCount += 1
        }
        viewModel.load()

        viewModel.updateFeedbackSettings {
            try $0.replacing(
                feedbackIntensity: .quiet,
                soundEnabled: false,
                ttsEnabled: false,
                ttsRate: 0.8,
                ttsVolume: 0.4,
                hapticEnabled: false
            )
        }

        let settings = try repository.appSettings()
        #expect(settings.feedbackIntensity == .quiet)
        #expect(!settings.soundEnabled)
        #expect(!settings.ttsEnabled)
        #expect(settings.ttsRate == 0.8)
        #expect(settings.ttsVolume == 0.4)
        #expect(!settings.hapticEnabled)
        #expect(changeCount == 1)
    }

    @Test("보호자 환경 설정은 알림 리드타임 조합과 방해 금지 시간을 저장한다")
    func guardianNotificationSettingsPersist() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        viewModel.load()

        viewModel.updateFeedbackSettings {
            try $0.replacing(notificationLeadTimes: [10])
        }
        #expect(try repository.appSettings().notificationLeadTimes == [10])

        viewModel.updateFeedbackSettings {
            try $0.replacing(notificationLeadTimes: [5])
        }
        #expect(try repository.appSettings().notificationLeadTimes == [5])

        viewModel.updateFeedbackSettings {
            try $0.replacing(notificationLeadTimes: [10, 5])
        }
        #expect(try repository.appSettings().notificationLeadTimes == [10, 5])

        viewModel.updateFeedbackSettings {
            try $0.replacing(notificationLeadTimes: [])
        }
        #expect(try repository.appSettings().notificationLeadTimes.isEmpty)

        viewModel.updateFeedbackSettings {
            try $0.replacing(
                quietHours: (
                    try LocalTime(hour: 21, minute: 0),
                    try LocalTime(hour: 7, minute: 0)
                )
            )
        }
        #expect(try repository.appSettings().quietHoursStart?.description == "21:00")
        #expect(try repository.appSettings().quietHoursEnd?.description == "07:00")

        viewModel.updateFeedbackSettings {
            try $0.replacing(quietHours: (nil, nil))
        }
        #expect(try repository.appSettings().quietHoursStart == nil)
        #expect(try repository.appSettings().quietHoursEnd == nil)
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

    @Test("보호자 루틴 편집은 사진을 IconRef.photo로 저장하고 기본 아이콘으로 되돌릴 수 있다")
    func guardianRoutinePhotoEditing() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let photoStore = FakeRoutinePhotoStore(
            icon: try IconRef.photo(
                localAssetID: UUID(uuidString: "66666666-6666-4666-8666-666666666666")!,
                backupAssetName: "routine-photo-66666666-6666-4666-8666-666666666666.jpg"
            )
        )
        let viewModel = GuardianModeViewModel(repository: repository, photoStore: photoStore) {}
        viewModel.load()
        let activeSetID = try #require(viewModel.activeRoutineSet?.id)

        viewModel.beginAddRoutine()
        viewModel.draft?.title = "사진 루틴"
        viewModel.updateDraftPhoto(data: Data([0xff, 0xd8, 0xff]))
        viewModel.saveDraft(localeIdentifier: "ko")

        let added = try #require(try repository.routines(in: activeSetID).last)
        #expect(added.icon.type == .photo)
        #expect(added.icon.backupAssetName == "routine-photo-66666666-6666-4666-8666-666666666666.jpg")
        #expect(photoStore.savedData == Data([0xff, 0xd8, 0xff]))

        viewModel.beginEditRoutine(added)
        viewModel.resetDraftIconToDefault()
        viewModel.saveDraft(localeIdentifier: "ko")

        let updated = try #require(try repository.routine(id: added.id))
        #expect(updated.icon.type == .builtin)
        #expect(updated.icon.name == RoutineIconName.star.rawValue)
    }

    @Test("루틴 세트 단계 편집은 카드 편집처럼 사진을 저장하고 기본 아이콘으로 되돌릴 수 있다")
    func guardianRoutineSetStepPhotoEditing() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let photoStore = FakeRoutinePhotoStore(
            icon: try IconRef.photo(
                localAssetID: UUID(uuidString: "77777777-7777-4777-8777-777777777777")!,
                backupAssetName: "routine-photo-77777777-7777-4777-8777-777777777777.jpg"
            )
        )
        let viewModel = GuardianModeViewModel(repository: repository, photoStore: photoStore) {}

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "사진 루틴 세트"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "사진 단계"
        viewModel.updateRoutineSetStepDraftPhoto(data: Data([0xff, 0xd8, 0xff]))
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")

        let createdSet = try #require(viewModel.selectedRoutineSet)
        let added = try #require(try repository.routines(in: createdSet.id).first)
        #expect(added.icon.type == .photo)
        #expect(added.icon.backupAssetName == "routine-photo-77777777-7777-4777-8777-777777777777.jpg")
        #expect(photoStore.savedData == Data([0xff, 0xd8, 0xff]))

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "기본 아이콘 루틴 세트"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "기본 아이콘 단계"
        viewModel.updateRoutineSetStepDraftPhoto(data: Data([0x01]))
        viewModel.resetRoutineSetStepDraftIconToDefault()
        viewModel.saveRoutineSetStepDraft()
        let resetStep = try #require(viewModel.routineSetDraft?.steps.first)
        #expect(resetStep.icon.type == .builtin)
        #expect(resetStep.icon.name == RoutineIconName.star.rawValue)
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

    @Test("루틴 세트 생성은 보관 세트와 단계들을 0부터 연속 order로 저장한다")
    func guardianRoutineSetCreationSavesStoredSetAndOrderedSteps() throws {
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

        let createdSet = try #require(viewModel.selectedRoutineSet)
        let routines = try repository.routines(in: createdSet.id)
        #expect(createdSet.name.resolved(appLocale: "ko") == "아침 준비")
        #expect(!createdSet.isActive)
        #expect(routines.map { $0.title.resolved(appLocale: "ko") } == ["일어나기", "세수하기"])
        #expect(routines.map(\.order) == [0, 1])
        #expect(routines.map(\.colorToken) == ["color.card.sky", "color.card.lemon"])
        #expect(routines[1].scheduledTime?.description == "07:40")
        #expect(viewModel.loadState == .loaded)
        #expect(viewModel.activeRoutineSet == nil)
        #expect(viewModel.selectedDestination == .routineEditor)
        #expect(changeCount == 1)
    }

    @Test("내장 루틴 템플릿은 morning school bedtime 3종과 한국어 영어 라벨을 제공한다")
    func builtInRoutineTemplatesExposeRequiredLocalizedContent() throws {
        let templates = BuiltInRoutineTemplates.all

        #expect(templates.map(\.id) == ["morning", "school", "bedtime"])
        for template in templates {
            #expect(template.name.values["ko"]?.isEmpty == false)
            #expect(template.name.values["en"]?.isEmpty == false)
            #expect(!template.steps.isEmpty)
            for step in template.steps {
                #expect(step.title.values["ko"]?.isEmpty == false)
                #expect(step.title.values["en"]?.isEmpty == false)
                #expect(Routine.allowedColorTokens.contains(step.colorToken))
                #expect(RoutineIconName(rawValue: step.iconName.rawValue) != nil)
            }
        }
    }

    @Test("템플릿 저장은 새 보관 루틴 세트와 ordered 루틴을 생성한다")
    func guardianTemplateSaveCreatesStoredRoutineSetAndOrderedRoutines() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        var changeCount = 0
        let viewModel = GuardianModeViewModel(repository: repository) {
            changeCount += 1
        }

        viewModel.beginTemplateSelection()
        viewModel.selectTemplate(try #require(viewModel.routineTemplates.first { $0.id == "morning" }))
        viewModel.saveSelectedTemplate()

        let createdSet = try #require(viewModel.selectedRoutineSet)
        let routines = try repository.routines(in: createdSet.id)
        #expect(createdSet.name.resolved(appLocale: "ko") == "아침 루틴")
        #expect(createdSet.name.resolved(appLocale: "en") == "Morning routine")
        #expect(!createdSet.isActive)
        #expect(routines.map { $0.title.resolved(appLocale: "ko") } == ["일어나기", "세수하기", "양치하기", "옷 입기", "아침 먹기", "가방 챙기기"])
        #expect(routines.map(\.order) == [0, 1, 2, 3, 4, 5])
        #expect(routines.map(\.titleKey) == ["routine.wakeUp", "routine.washFace", "routine.brushTeeth", "routine.getDressed", "routine.breakfast", "routine.packBag"])
        #expect(Set(routines.map(\.id)).count == routines.count)
        #expect(viewModel.selectedRoutineSet?.id == createdSet.id)
        #expect(viewModel.selectedDestination == .routineEditor)
        #expect(changeCount == 1)
    }

    @Test("같은 템플릿을 두 번 저장해도 RoutineSet과 Routine UUID는 중복되지 않는다")
    func guardianTemplateSaveGeneratesFreshIDsEveryTime() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        let bedtime = try #require(viewModel.routineTemplates.first { $0.id == "bedtime" })

        viewModel.beginTemplateSelection()
        viewModel.selectTemplate(bedtime)
        viewModel.saveSelectedTemplate()
        let firstSetID = try #require(viewModel.selectedRoutineSet?.id)
        let firstRoutineIDs = try Set(repository.routines(in: firstSetID).map(\.id))

        viewModel.beginTemplateSelection()
        viewModel.selectTemplate(bedtime)
        viewModel.saveSelectedTemplate()
        let secondSetID = try #require(viewModel.selectedRoutineSet?.id)
        let secondRoutineIDs = try Set(repository.routines(in: secondSetID).map(\.id))

        #expect(firstSetID != secondSetID)
        #expect(firstRoutineIDs.isDisjoint(with: secondRoutineIDs))
        #expect(try repository.routineSets().count == 2)
    }

    @Test("템플릿으로 생성된 루틴은 일반 루틴처럼 편집 삭제 정렬할 수 있다")
    func guardianTemplateRoutinesRemainEditable() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        let school = try #require(viewModel.routineTemplates.first { $0.id == "school" })

        viewModel.beginTemplateSelection()
        viewModel.selectTemplate(school)
        viewModel.saveSelectedTemplate()
        let setID = try #require(viewModel.selectedRoutineSet?.id)

        let first = try #require(viewModel.routines.first)
        viewModel.beginEditRoutine(first)
        viewModel.draft?.title = "스쿨버스 타기"
        viewModel.draft?.colorToken = "color.card.rose"
        viewModel.saveDraft(localeIdentifier: "ko")
        #expect(try repository.routine(id: first.id)?.title.resolved(appLocale: "ko") == "스쿨버스 타기")

        let last = try #require(viewModel.routines.last)
        viewModel.moveRoutine(last, to: 0)
        #expect(try repository.routines(in: setID).first?.id == last.id)
        #expect(try repository.routines(in: setID).map(\.order) == [0, 1, 2, 3])

        viewModel.requestDelete(last)
        viewModel.confirmDelete()
        #expect(try repository.routines(in: setID).count == 3)
        #expect(try repository.routines(in: setID).map(\.order) == [0, 1, 2])
        #expect(try repository.routine(id: last.id)?.deletedAt != nil)
    }

    @Test("템플릿 저장 후 설정하면 아이 모드는 오늘 배정 세트를 읽고 백업에도 포함된다")
    func templateSaveRefreshesChildModeAndBackupSnapshot() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let childViewModel = ChildRoutineViewModel(repository: repository)
        let guardianViewModel = GuardianModeViewModel(repository: repository) {
            childViewModel.load()
        }
        let bedtime = try #require(guardianViewModel.routineTemplates.first { $0.id == "bedtime" })

        childViewModel.load()
        #expect(childViewModel.loadState == .empty)

        guardianViewModel.beginTemplateSelection()
        guardianViewModel.selectTemplate(bedtime)
        guardianViewModel.saveSelectedTemplate()
        guardianViewModel.assignRoutineSetForToday(try #require(guardianViewModel.selectedRoutineSet))

        let activeSet = try #require(childViewModel.activeRoutineSet)
        #expect(childViewModel.loadState == .loaded)
        #expect(activeSet.name.resolved(appLocale: "ko") == "취침 루틴")
        #expect(childViewModel.routines.map { $0.title.resolved(appLocale: "ko") } == ["목욕하기", "잠옷 입기", "잠자기"])

        let snapshot = try repository.backupSnapshot()
        #expect(snapshot.routineSets.contains { $0.id == activeSet.id })
        #expect(snapshot.routines.filter { $0.routineSetID == activeSet.id }.count == 3)
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
        #expect(viewModel.activeRoutineSet?.name.resolved(appLocale: "ko") == "아침 루틴")
        #expect(viewModel.todayAssignedRoutineSetID == nil)
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
        #expect(latestActiveRoutines.map { $0.title.resolved(appLocale: "ko") }.contains("물 마시기"))
    }

    @Test("루틴 관리 설정 액션은 선택한 세트를 오늘 루틴으로 배정하고 아이 모드를 갱신한다")
    func guardianRoutineSetAssignmentSetsTodayRoutine() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_312_000)
        var childReloadCount = 0
        let viewModel = GuardianModeViewModel(repository: repository, now: { now }) {
            childReloadCount += 1
        }
        viewModel.load()

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "하교 루틴"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "가방 정리"
        viewModel.routineSetStepDraft?.iconName = .packBag
        viewModel.routineSetStepDraft?.colorToken = "color.card.peach"
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")
        let createdSet = try #require(viewModel.selectedRoutineSet)

        #expect(viewModel.todayAssignedRoutineSetID == nil)
        #expect(viewModel.requiresTodayRoutineSelection)

        viewModel.assignRoutineSetForToday(createdSet)

        let date = DailyLog.localDateString(for: now)
        #expect(try repository.dailyRoutineAssignment(on: date)?.routineSetID == createdSet.id)
        #expect(viewModel.todayAssignedRoutineSetID == createdSet.id)
        #expect(!viewModel.requiresTodayRoutineSelection)
        #expect(childReloadCount == 2)
    }

    @Test("루틴 관리는 여러 세트의 매일 시작 시각을 저장하고 중복 시각을 거부한다")
    func guardianSchedulesMultipleRoutineSetsDaily() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let viewModel = GuardianModeViewModel(repository: repository) {}
        viewModel.load()
        let morningSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "학교 루틴"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "학교 가기"
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")
        let schoolSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginScheduleRoutineSet(morningSet)
        viewModel.routineSetScheduleDraft?.startTime = try LocalTime(hour: 8, minute: 0)
        viewModel.saveRoutineSetSchedule()
        viewModel.beginScheduleRoutineSet(schoolSet)
        viewModel.routineSetScheduleDraft?.startTime = try LocalTime(hour: 9, minute: 0)
        viewModel.saveRoutineSetSchedule()

        #expect(viewModel.scheduledRoutineSets.map(\.id) == [morningSet.id, schoolSet.id])
        #expect(try repository.routineSet(id: morningSet.id)?.dailyStartTime == LocalTime(hour: 8, minute: 0))
        #expect(try repository.routineSet(id: schoolSet.id)?.dailyStartTime == LocalTime(hour: 9, minute: 0))

        viewModel.beginScheduleRoutineSet(schoolSet)
        viewModel.routineSetScheduleDraft?.startTime = try LocalTime(hour: 8, minute: 0)
        viewModel.saveRoutineSetSchedule()

        #expect(viewModel.errorMessage != nil)
        #expect(try repository.routineSet(id: schoolSet.id)?.dailyStartTime == LocalTime(hour: 9, minute: 0))
    }

    @Test("루틴 관리 진입 시 오늘 배정된 루틴 세트를 선택한다")
    func guardianSelectsTodayAssignedRoutineSetForRoutineManagement() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_312_000)
        let viewModel = GuardianModeViewModel(repository: repository, now: { now }) {}
        viewModel.load()
        let originalSet = try #require(viewModel.selectedRoutineSet)

        viewModel.beginCreateRoutineSet()
        viewModel.routineSetDraft?.name = "저녁 루틴"
        viewModel.beginAddRoutineSetStep()
        viewModel.routineSetStepDraft?.title = "저녁 먹기"
        viewModel.saveRoutineSetStepDraft()
        viewModel.saveRoutineSetDraft(localeIdentifier: "ko")
        let assignedSet = try #require(viewModel.selectedRoutineSet)
        viewModel.assignRoutineSetForToday(assignedSet)

        viewModel.selectRoutineSet(originalSet)
        #expect(viewModel.selectedRoutineSet?.id == originalSet.id)

        viewModel.selectTodayAssignedRoutineSet()

        #expect(viewModel.selectedRoutineSet?.id == assignedSet.id)
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

    @Test("진행 기록은 DailyLog가 없는 날짜를 빈 상태로 표시한다")
    func guardianRecordsShowEmptyDateWhenNoDailyLogExists() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { now }
        ) {}

        viewModel.load()

        let detail = try #require(viewModel.selectedRecordDetail)
        #expect(detail.date == DailyLog.localDateString(for: now))
        #expect(detail.isEmpty)
        #expect(detail.completedCount == 0)
        #expect(detail.totalCount == 0)
    }

    @Test("진행 기록은 선택 날짜의 완료 수 전체 수 완료율을 DailyLog 기준으로 계산한다")
    func guardianRecordsCalculateSelectedDateProgress() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routines = try repository.routines(in: routineSet.id)
        let logDate = "2026-01-01"
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        try repository.setRoutineCompleted(
            routineID: routines[0].id,
            routineSetID: routineSet.id,
            on: logDate,
            at: completedAt
        )
        try repository.setRoutineCompleted(
            routineID: routines[1].id,
            routineSetID: routineSet.id,
            on: logDate,
            at: completedAt
        )
        let addedAfterLogDate = try Routine(
            routineSetID: routineSet.id,
            title: LocalizedText(["ko": "다음 날 추가"]),
            icon: try IconRef.builtin(name: RoutineIconName.star.rawValue),
            colorToken: "color.card.rose",
            order: routines.count,
            createdAt: Date(timeIntervalSince1970: 1_767_312_000),
            updatedAt: Date(timeIntervalSince1970: 1_767_312_000)
        )
        try repository.createRoutine(addedAfterLogDate)
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_767_744_000) }
        ) {}

        viewModel.load()
        viewModel.selectRecordDate(logDate)

        let summary = try #require(viewModel.recordSummaries.first { $0.date == logDate })
        let detail = try #require(viewModel.selectedRecordDetail)
        #expect(summary.completedCount == 2)
        #expect(summary.totalCount == 3)
        #expect(summary.remainingCount == 1)
        #expect(summary.percentage == 67)
        #expect(detail.rows.map(\.status) == [.completed, .completed, .undone])
        #expect(!detail.rows.map(\.title).contains("다음 날 추가"))
    }

    @Test("진행 기록 달력은 저장된 DailyLog 날짜를 전체 범위에서 제공한다")
    func guardianRecordCalendarLoadsAllDailyLogDates() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routines = try repository.routines(in: routineSet.id)
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        try repository.setRoutineCompleted(
            routineID: routines[0].id,
            routineSetID: routineSet.id,
            on: "2025-12-20",
            at: completedAt
        )
        try repository.setRoutineCompleted(
            routineID: routines[1].id,
            routineSetID: routineSet.id,
            on: "2026-01-01",
            at: completedAt
        )
        try repository.setRoutineCompleted(
            routineID: routines[2].id,
            routineSetID: routineSet.id,
            on: "2026-01-01",
            at: completedAt
        )

        #expect(try repository.dailyLogDates() == ["2026-01-01", "2025-12-20"])

        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_769_000_000) }
        ) {}
        viewModel.load()
        viewModel.prepareRecordCalendar()

        #expect(viewModel.recordCalendarDates == ["2025-12-20", "2026-01-01"])
        #expect(viewModel.selectedCalendarRecordDate == "2026-01-21")
        #expect(viewModel.selectedCalendarRecordDetail == nil)
    }

    @Test("진행 기록 달력은 오늘 기록이 있으면 처음 진입 시 오늘을 선택한다")
    func guardianRecordCalendarSelectsTodayOnEntryWhenTodayHasLog() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routine = try #require(try repository.routines(in: routineSet.id).first)
        let today = "2026-01-21"
        try repository.setRoutineCompleted(
            routineID: routine.id,
            routineSetID: routineSet.id,
            on: today,
            at: Date(timeIntervalSince1970: 1_769_000_000)
        )
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_769_000_000) }
        ) {}

        viewModel.load()
        viewModel.prepareRecordCalendar()

        #expect(viewModel.selectedCalendarRecordDate == today)
        #expect(viewModel.selectedCalendarRecordDetail?.date == today)
    }

    @Test("진행 기록 달력은 최근 7일 밖의 기록 날짜를 선택해 상세를 계산한다")
    func guardianRecordCalendarSelectsOlderRecordDate() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routines = try repository.routines(in: routineSet.id)
        let oldLogDate = "2026-01-01"
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        try repository.setRoutineCompleted(
            routineID: routines[0].id,
            routineSetID: routineSet.id,
            on: oldLogDate,
            at: completedAt
        )
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_769_000_000) }
        ) {}

        viewModel.load()
        #expect(!viewModel.recordSummaries.map(\.date).contains(oldLogDate))

        viewModel.prepareRecordCalendar()
        viewModel.selectCalendarRecordDate(oldLogDate)

        let detail = try #require(viewModel.selectedCalendarRecordDetail)
        #expect(detail.date == oldLogDate)
        #expect(detail.completedCount == 1)
        #expect(detail.totalCount == 3)
    }

    @Test("진행 기록 달력은 기록 없는 날짜 선택 요청을 무시한다")
    func guardianRecordCalendarIgnoresDatesWithoutLogs() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routine = try #require(try repository.routines(in: routineSet.id).first)
        try repository.setRoutineCompleted(
            routineID: routine.id,
            routineSetID: routineSet.id,
            on: "2026-01-01",
            at: Date(timeIntervalSince1970: 1_767_229_200)
        )
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_769_000_000) }
        ) {}

        viewModel.load()
        viewModel.prepareRecordCalendar()
        viewModel.selectCalendarRecordDate("2025-12-31")

        #expect(viewModel.selectedCalendarRecordDate == "2026-01-21")
        #expect(viewModel.selectedCalendarRecordDetail == nil)
    }

    @Test("진행 기록은 삭제된 루틴의 완료 이력을 이해 가능한 상태로 유지한다")
    func guardianRecordsKeepDeletedRoutineHistoryVisible() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let routineSet = try #require(try repository.routineSets().first)
        let routine = try #require(try repository.routines(in: routineSet.id).first)
        let logDate = "2026-01-01"
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        try repository.setRoutineCompleted(
            routineID: routine.id,
            routineSetID: routineSet.id,
            on: logDate,
            at: completedAt
        )
        try repository.deleteRoutine(
            id: routine.id,
            at: Date(timeIntervalSince1970: 1_767_312_000)
        )
        let viewModel = GuardianModeViewModel(
            repository: repository,
            now: { Date(timeIntervalSince1970: 1_767_744_000) }
        ) {}

        viewModel.load()
        viewModel.selectRecordDate(logDate)

        let detail = try #require(viewModel.selectedRecordDetail)
        let deletedRow = try #require(detail.rows.first { $0.id == routine.id })
        #expect(deletedRow.title == "일어나기")
        #expect(deletedRow.isDeleted)
        #expect(deletedRow.isCompleted)
        #expect(deletedRow.availabilityText == "삭제된 활동")
    }

    @Test("새 루틴 세트 생성 후 설정하면 아이 모드는 오늘 배정 세트를 읽는다")
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
        guardianViewModel.assignRoutineSetForToday(try #require(guardianViewModel.selectedRoutineSet))

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
        try assignActiveRoutineSet(in: repository, on: now)
        let routineSets = try repository.routineSets()
        let routines = try repository.routines(in: routineSets[0].id)
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
        try assignActiveRoutineSet(in: repository, on: now)
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
        try assignActiveRoutineSet(in: repository, on: now)
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

    @Test("로컬 날짜가 바뀌면 루틴 세트를 자동 선택하지 않고 빈 상태를 표시한다")
    func childRoutineDateChangeRequiresNewAssignment() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        var now = Date(timeIntervalSince1970: 1_767_225_600)
        try assignActiveRoutineSet(in: repository, on: now)
        let viewModel = ChildRoutineViewModel(repository: repository, now: { now })

        viewModel.load()
        #expect(viewModel.loadState == .loaded)

        now = try #require(Calendar.current.date(byAdding: .day, value: 1, to: now))
        viewModel.appDidBecomeActive()

        #expect(viewModel.loadState == .empty)
        #expect(viewModel.activeRoutineSet == nil)
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

    @Test("백업 export는 manifest와 data checksum을 포함하고 replace 복원한다")
    func backupExportAndReplaceRestore() throws {
        let source = try RoutinePreviewStore.makeSampleRepository()
        let recoveryCode = "654321"
        let settingsWithPIN = try AppSettings(
            guardianPinHash: GuardianPinService.makeHash(for: "1234", salt: "backup-test"),
            recoveryCodeHash: GuardianPinService.makeRecoveryCodeHash(for: recoveryCode, salt: "backup-recovery-test")
        )
        try source.updateAppSettings(settingsWithPIN)
        let activeSet = try #require(try source.routineSets().first)
        let firstRoutine = try #require(try source.routines(in: activeSet.id).first)
        let completedAt = Date(timeIntervalSince1970: 1_767_229_200)
        _ = try source.setRoutineCompleted(
            routineID: firstRoutine.id,
            routineSetID: activeSet.id,
            on: DailyLog.localDateString(for: completedAt),
            at: completedAt
        )

        let package = try BackupService(
            repository: source,
            now: { Date(timeIntervalSince1970: 1_767_230_000) },
            appVersion: { "1.0.0" }
        ).exportPackage()
        let entries = try SteppieZipArchive.readArchive(package.archiveData)
        #expect(entries["manifest.json"] != nil)
        #expect(entries["data.json"] != nil)
        let dataJSON = String(data: try #require(entries["data.json"]), encoding: .utf8)
        #expect(dataJSON?.contains("1234") == false)
        #expect(dataJSON?.contains(recoveryCode) == false)

        let target = try RoutinePreviewStore.makeRepository()
        try BackupService(repository: target).restorePackage(package.archiveData)

        #expect(try target.routineSets().map(\.id) == [activeSet.id])
        #expect(try target.routines(in: activeSet.id).map(\.id).contains(firstRoutine.id))
        #expect(try target.appSettings().guardianPinHash == settingsWithPIN.guardianPinHash)
        #expect(try target.appSettings().recoveryCodeHash == settingsWithPIN.recoveryCodeHash)
        #expect(try target.dailyLogs(on: DailyLog.localDateString(for: completedAt), routineSetID: activeSet.id).count == 1)
    }

    @Test("복원 당일의 루틴 세트 배정은 복원하지 않고 아이 모드는 빈 상태를 표시한다")
    func restoreClearsRoutineAssignmentForRestoreDate() throws {
        let now = Date(timeIntervalSince1970: 1_767_225_600)
        let today = DailyLog.localDateString(for: now)
        let source = try RoutinePreviewStore.makeSampleRepository()
        try assignActiveRoutineSet(in: source, on: now)
        let package = try BackupService(repository: source, now: { now }).exportPackage()
        let target = try RoutinePreviewStore.makeRepository()

        try BackupService(repository: target, now: { now }).restorePackage(package.archiveData)

        #expect(try target.dailyRoutineAssignment(on: today) == nil)
        let childViewModel = ChildRoutineViewModel(repository: target, now: { now })
        childViewModel.load()
        #expect(childViewModel.loadState == .empty)
        #expect(childViewModel.selectedRoutine == nil)
    }

    @Test("복원 중 DB replace가 실패하면 새로 저장한 사진 에셋을 정리한다")
    func restoreCleansSavedAssetsWhenReplaceFails() throws {
        let assetName = "routine-photo-77777777-7777-4777-8777-777777777777.jpg"
        let assetStore = FakeBackupAssetStore()
        let repository = FailingReplaceRepository()
        let payload = BackupRestorePayload(
            snapshot: RoutineRepositorySnapshot(
                routineSets: [],
                routines: [],
                dailyLogs: [],
                dailyRoutineAssignments: [],
                appSettings: try AppSettings()
            ),
            assets: [assetName: Data([0xff, 0xd8, 0xff])]
        )

        #expect(throws: FailingReplaceRepository.ReplaceError.failed) {
            try BackupService(repository: repository, assetStore: assetStore).restorePayload(payload)
        }
        #expect(assetStore.assets[assetName] == nil)
        #expect(assetStore.removedAssetNames == [assetName])
    }

    @Test("checksum이 손상된 백업은 복원을 거부한다")
    func backupRejectsInvalidChecksum() throws {
        let repository = try RoutinePreviewStore.makeSampleRepository()
        let service = BackupService(repository: repository)
        let package = try service.exportPackage()
        let entries = try SteppieZipArchive.readArchive(package.archiveData)
        let damagedArchive = try SteppieZipArchive.makeArchive(entries: [
            SteppieZipEntry(path: "manifest.json", data: try #require(entries["manifest.json"])),
            SteppieZipEntry(path: "data.json", data: Data(#"{"schemaVersion":1}"#.utf8)),
        ])

        #expect(throws: BackupError.invalidChecksum) {
            try service.validatePackage(damagedArchive)
        }
    }

    @Test("백업 파일의 다중 활성 RoutineSet은 가장 최근 세트 하나로 보정된다")
    func backupNormalizesMultipleActiveRoutineSets() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let firstCreatedAt = Date(timeIntervalSince1970: 1_767_225_600)
        let secondCreatedAt = firstCreatedAt.addingTimeInterval(60)
        let first = try RoutineSet(
            name: LocalizedText(["ko": "첫 루틴"]),
            isActive: true,
            createdAt: firstCreatedAt,
            updatedAt: firstCreatedAt
        )
        let second = try RoutineSet(
            name: LocalizedText(["ko": "둘째 루틴"]),
            isActive: true,
            createdAt: secondCreatedAt,
            updatedAt: secondCreatedAt
        )
        let snapshot = RoutineRepositorySnapshot(
            routineSets: [first, second],
            routines: [],
            dailyLogs: [],
            dailyRoutineAssignments: [],
            appSettings: try AppSettings()
        )
        try repository.replaceAll(with: snapshot)

        let package = try BackupService(repository: repository).exportPackage()
        let restoredSnapshot = try BackupService(repository: repository).validatePackage(package.archiveData)

        #expect(restoredSnapshot.routineSets.filter(\.isActive).map(\.id) == [second.id])
    }

    @Test("사진 에셋이 누락된 백업은 루틴을 유지하고 placeholder 아이콘으로 복원한다")
    func backupMissingPhotoAssetUsesPlaceholderIcon() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let routineSet = try RoutineSet(
            name: LocalizedText(["ko": "사진 루틴"]),
            isActive: true,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(routineSet)
        let routine = try Routine(
            routineSetID: routineSet.id,
            title: LocalizedText(["ko": "사진 보기"]),
            icon: IconRef.photo(
                localAssetID: UUID(uuidString: "33333333-3333-4333-8333-333333333333")!,
                backupAssetName: "routine-photo-33333333-3333-4333-8333-333333333333.jpg"
            ),
            order: 0,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutine(routine)

        let package = try BackupService(repository: repository).exportPackage()
        let restoredSnapshot = try BackupService(repository: repository).validatePackage(package.archiveData)

        #expect(restoredSnapshot.routines.first?.id == routine.id)
        #expect(restoredSnapshot.routines.first?.icon.name == RoutineIconName.star.rawValue)
    }

    @Test("사진 에셋이 있는 백업은 assets 디렉터리에 파일을 포함하고 복원 시 저장한다")
    func backupIncludesAndRestoresPhotoAsset() throws {
        let assetName = "routine-photo-44444444-4444-4444-8444-444444444444.jpg"
        let sourceAssetStore = FakeBackupAssetStore(assets: [assetName: Data([0xff, 0xd8, 0xff])])
        let targetAssetStore = FakeBackupAssetStore()
        let repository = try RoutinePreviewStore.makeRepository()
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let routineSet = try RoutineSet(
            name: LocalizedText(["ko": "사진 루틴"]),
            isActive: true,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutineSet(routineSet)
        let routine = try Routine(
            routineSetID: routineSet.id,
            title: LocalizedText(["ko": "사진 보기"]),
            icon: IconRef.photo(
                localAssetID: UUID(uuidString: "44444444-4444-4444-8444-444444444444")!,
                backupAssetName: assetName
            ),
            order: 0,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        try repository.createRoutine(routine)

        let package = try BackupService(repository: repository, assetStore: sourceAssetStore).exportPackage()
        let entries = try SteppieZipArchive.readArchive(package.archiveData)
        #expect(entries["assets/\(assetName)"] == Data([0xff, 0xd8, 0xff]))

        let target = try RoutinePreviewStore.makeRepository()
        try BackupService(repository: target, assetStore: targetAssetStore).restorePackage(package.archiveData)

        #expect(try target.routines(in: routineSet.id).first?.icon.backupAssetName == assetName)
        #expect(targetAssetStore.assets[assetName] == Data([0xff, 0xd8, 0xff]))
    }

    @Test("RoutineSet 없이 routines가 남은 백업은 거부한다")
    func backupRejectsRoutinesWithoutRoutineSet() throws {
        let repository = try RoutinePreviewStore.makeRepository()
        let createdAt = Date(timeIntervalSince1970: 1_767_225_600)
        let orphanRoutine = try Routine(
            routineSetID: UUID(uuidString: "55555555-5555-4555-8555-555555555555")!,
            title: LocalizedText(["ko": "고아 루틴"]),
            icon: IconRef.builtin(name: "star"),
            order: 0,
            createdAt: createdAt,
            updatedAt: createdAt
        )
        let data = BackupData(
            schemaVersion: 1,
            exportedAt: createdAt,
            routineSets: [],
            routines: [orphanRoutine],
            dailyLogs: [],
            appSettings: try AppSettings()
        )
        let dataJson = try backupTestJSONEncoder.encode(data)
        let manifest = BackupManifest(
            app: "Steppie",
            backupSchemaVersion: 1,
            createdAt: createdAt,
            sourcePlatform: "ios",
            appVersion: "1.0.0",
            dataFile: "data.json",
            assetDirectory: "assets",
            checksum: BackupChecksum(
                algorithm: "sha256",
                dataJson: SHA256.hash(data: dataJson).map { String(format: "%02x", $0) }.joined()
            )
        )
        let archive = try SteppieZipArchive.makeArchive(entries: [
            SteppieZipEntry(path: "manifest.json", data: try backupTestJSONEncoder.encode(manifest)),
            SteppieZipEntry(path: "data.json", data: dataJson),
        ])

        #expect(throws: BackupError.invalidReference("RoutineSet")) {
            try BackupService(repository: repository).validatePackage(archive)
        }
    }

    @Test("보호자 ViewModel 복원은 PIN 확인 전 replace를 실행하지 않는다")
    func guardianRestoreRequiresPINConfirmation() throws {
        let source = try RoutinePreviewStore.makeSampleRepository()
        let package = try BackupService(repository: source).exportPackage()
        let target = try RoutinePreviewStore.makeRepository()
        let viewModel = GuardianModeViewModel(repository: target) {}

        #expect(viewModel.setPIN("1234"))
        viewModel.validateRestorePackage(package.archiveData)
        viewModel.restorePIN = "0000"
        viewModel.confirmRestore()
        #expect(try target.routineSets().isEmpty)

        viewModel.restorePIN = "1234"
        viewModel.confirmRestore()
        #expect(try target.routineSets().isEmpty == false)
    }

    @Test("보호자 복원 성공은 데이터 변경 콜백을 호출해 아이 모드 재로딩 경로를 연다")
    func guardianRestoreSuccessCallsDataChanged() throws {
        let source = try RoutinePreviewStore.makeSampleRepository()
        let package = try BackupService(repository: source).exportPackage()
        let target = try RoutinePreviewStore.makeRepository()
        var changeCount = 0
        let viewModel = GuardianModeViewModel(repository: target) {
            changeCount += 1
        }

        #expect(viewModel.setPIN("1234"))
        viewModel.validateRestorePackage(package.archiveData)
        viewModel.restorePIN = "1234"
        viewModel.confirmRestore()

        #expect(changeCount == 2)
        #expect(try target.routineSets().isEmpty == false)
    }

    private func assignActiveRoutineSet(
        in repository: SwiftDataRoutineRepository,
        on date: Date
    ) throws {
        let routineSet = try #require(try repository.routineSets().first(where: \.isActive))
        _ = try repository.assignRoutineSet(
            routineSet.id,
            on: DailyLog.localDateString(for: date),
            at: date
        )
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

    private func makeScheduledRoutinePlan(
        createdAt: Date
    ) throws -> (
        repository: SwiftDataRoutineRepository,
        morningSet: RoutineSet,
        schoolSet: RoutineSet
    ) {
        let repository = try RoutinePreviewStore.makeRepository()
        let morningSet = try RoutineSet(
            name: LocalizedText(["ko": "아침 루틴"]),
            isActive: true,
            dailyStartTime: LocalTime(hour: 8, minute: 0),
            createdAt: createdAt,
            updatedAt: createdAt
        )
        let schoolCreatedAt = createdAt.addingTimeInterval(1)
        let schoolSet = try RoutineSet(
            name: LocalizedText(["ko": "학교 루틴"]),
            dailyStartTime: LocalTime(hour: 9, minute: 0),
            createdAt: schoolCreatedAt,
            updatedAt: schoolCreatedAt
        )
        try repository.createRoutineSet(morningSet)
        try repository.createRoutineSet(schoolSet)
        try repository.createRoutine(
            Routine(
                routineSetID: morningSet.id,
                title: LocalizedText(["ko": "일어나기"]),
                icon: IconRef.builtin(name: "wake-up"),
                order: 0,
                createdAt: createdAt,
                updatedAt: createdAt
            )
        )
        try repository.createRoutine(
            Routine(
                routineSetID: schoolSet.id,
                title: LocalizedText(["ko": "학교 가기"]),
                icon: IconRef.builtin(name: "school"),
                order: 0,
                createdAt: schoolCreatedAt,
                updatedAt: schoolCreatedAt
            )
        )
        return (repository, morningSet, schoolSet)
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

private final class FakeBackupAssetStore: BackupAssetStore {
    var assets: [String: Data]

    init(assets: [String: Data] = [:]) {
        self.assets = assets
    }

    func data(forBackupAssetName name: String) throws -> Data? {
        assets[name]
    }

    func saveAssetData(_ data: Data, backupAssetName name: String) throws {
        assets[name] = data
    }

    var removedAssetNames: [String] = []

    func removeAssetData(backupAssetName name: String) throws {
        assets[name] = nil
        removedAssetNames.append(name)
    }
}

@MainActor
private final class FakeRoutineSpeechGuide: RoutineSpeechGuiding {
    var spokenTexts: [String] = []
    var stopCallCount = 0

    func speak(_ text: String, settings: AppSettings) {
        guard settings.ttsEnabled else { return }
        spokenTexts.append(text)
    }

    func stop() {
        stopCallCount += 1
    }
}

@MainActor
private final class FailingReplaceRepository: RoutineRepository {
    enum ReplaceError: Error, Equatable {
        case failed
    }

    func createRoutineSet(_ routineSet: RoutineSet) throws {}
    func routineSet(id: UUID) throws -> RoutineSet? { nil }
    func routineSets(includeDeleted: Bool) throws -> [RoutineSet] { [] }
    func updateRoutineSet(_ routineSet: RoutineSet) throws {}
    func deleteRoutineSet(id: UUID, at date: Date) throws {}
    func createRoutine(_ routine: Routine) throws {}
    func routine(id: UUID) throws -> Routine? { nil }
    func routines(in routineSetID: UUID, includeInactive: Bool, includeDeleted: Bool) throws -> [Routine] { [] }
    func updateRoutine(_ routine: Routine) throws {}
    func deleteRoutine(id: UUID, at date: Date) throws {}
    func reorderRoutines(in routineSetID: UUID, orderedIDs: [UUID], at date: Date) throws {}
    func dailyLogs(on date: String, routineSetID: UUID?) throws -> [DailyLog] { [] }
    func dailyLogDates() throws -> [String] { [] }
    func dailyLog(on date: String, routineID: UUID) throws -> DailyLog? { nil }
    func setRoutineCompleted(routineID: UUID, routineSetID: UUID, on date: String, at completedAt: Date) throws -> DailyLog {
        throw ReplaceError.failed
    }
    func undoRoutineCompletion(routineID: UUID, on date: String, at updatedAt: Date) throws -> DailyLog? { nil }
    func dailyRoutineAssignment(on date: String) throws -> DailyRoutineAssignment? { nil }
    func assignRoutineSet(_ routineSetID: UUID, on date: String, at updatedAt: Date) throws -> DailyRoutineAssignment {
        throw ReplaceError.failed
    }
    func appSettings() throws -> AppSettings { try AppSettings() }
    func updateAppSettings(_ settings: AppSettings) throws {}
    func backupSnapshot() throws -> RoutineRepositorySnapshot {
        RoutineRepositorySnapshot(
            routineSets: [],
            routines: [],
            dailyLogs: [],
            dailyRoutineAssignments: [],
            appSettings: try AppSettings()
        )
    }
    func replaceAll(with snapshot: RoutineRepositorySnapshot) throws {
        throw ReplaceError.failed
    }
}

private final class FakeRoutinePhotoStore: RoutinePhotoStoring {
    let icon: IconRef
    var savedData: Data?

    init(icon: IconRef) {
        self.icon = icon
    }

    func data(forBackupAssetName name: String) throws -> Data? {
        savedData
    }

    func savePhotoData(_ data: Data) throws -> IconRef {
        savedData = data
        return icon
    }
}

private let backupTestJSONEncoder: JSONEncoder = {
    let encoder = JSONEncoder()
    encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
    encoder.dateEncodingStrategy = .iso8601
    return encoder
}()

@MainActor
struct TutorialCoordinatorTests {
    @Test("튜토리얼은 화면별 최초 진입에만 표시된다")
    func presentsOnlyOncePerScreen() {
        let store = InMemoryTutorialProgressStore()
        let coordinator = TutorialCoordinator(store: store)

        coordinator.presentIfNeeded(.childFocus)
        #expect(coordinator.activeScreen == .childFocus)
        while !coordinator.isLastStep { coordinator.next() }
        coordinator.next()
        #expect(coordinator.activeScreen == nil)

        coordinator.presentIfNeeded(.childFocus)
        #expect(coordinator.activeScreen == nil)
        coordinator.presentIfNeeded(.childList)
        #expect(coordinator.activeScreen == .childList)
    }

    @Test("이전과 다음은 화면의 단계 경계를 지킨다")
    func navigatesWithinStepBounds() {
        let coordinator = TutorialCoordinator(store: InMemoryTutorialProgressStore())
        coordinator.presentIfNeeded(.childFocus)

        coordinator.previous()
        #expect(coordinator.stepIndex == 0)
        coordinator.next()
        #expect(coordinator.stepIndex == 1)
        coordinator.previous()
        #expect(coordinator.stepIndex == 0)
    }

    @Test("건너뛰기와 전체 초기화가 완료 상태를 갱신한다")
    func skipsAndResetsProgress() {
        let coordinator = TutorialCoordinator(store: InMemoryTutorialProgressStore())
        coordinator.presentIfNeeded(.security)
        coordinator.skip()
        coordinator.presentIfNeeded(.security)
        #expect(coordinator.activeScreen == nil)

        coordinator.resetAndPresent(.security)
        #expect(coordinator.activeScreen == .security)
        #expect(coordinator.stepIndex == 0)
    }
}

@MainActor
private final class InMemoryTutorialProgressStore: TutorialProgressStoring {
    private var tokens: Set<String> = []
    func contains(_ token: String) -> Bool { tokens.contains(token) }
    func insert(_ token: String) { tokens.insert(token) }
    func removeAll() { tokens.removeAll() }
}
