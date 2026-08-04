import Foundation
import Testing
@testable import Steppie

@MainActor
extension SteppieTests {
    func assignActiveRoutineSet(
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

    func makeFixture(
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

    func makeScheduledRoutinePlan(
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
final class FakeRoutineNotificationScheduler: RoutineNotificationScheduling {
    struct RescheduleCall: Equatable {
        let completedRoutineIDs: Set<UUID>
        let settings: AppSettings
    }

    var authorizationRequestCount = 0
    var rescheduleCalls: [RescheduleCall] = []
    var completedRescheduleCalls: [RescheduleCall] = []
    var shouldSuspendFirstReschedule = false
    private var firstRescheduleContinuation: CheckedContinuation<Void, Never>?

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
        let call = RescheduleCall(
            completedRoutineIDs: completedRoutineIDs,
            settings: settings
        )
        rescheduleCalls.append(call)

        if shouldSuspendFirstReschedule, rescheduleCalls.count == 1 {
            await withCheckedContinuation { continuation in
                firstRescheduleContinuation = continuation
            }
        }

        completedRescheduleCalls.append(call)
    }

    func resumeFirstReschedule() {
        firstRescheduleContinuation?.resume()
        firstRescheduleContinuation = nil
    }
}

@MainActor
final class FakeRoutineSoundPlayer: RoutineSoundPlaying {
    var playedCues: [RoutineFeedbackSoundCue] = []

    func play(_ cue: RoutineFeedbackSoundCue) {
        playedCues.append(cue)
    }
}

@MainActor
final class FakeRoutineSpeechGuide: RoutineSpeechGuiding {
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

final class FakeRoutinePhotoStore: RoutinePhotoStoring {
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
