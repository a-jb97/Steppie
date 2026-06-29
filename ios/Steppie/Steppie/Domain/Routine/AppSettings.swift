import Foundation

nonisolated enum FeedbackIntensity: String, Codable, Equatable, Sendable {
    case strong
    case normal
    case quiet
    case off
}

nonisolated struct AppSettings: Codable, Equatable, Identifiable, Sendable {
    static let singletonID = "singleton"

    let id: String
    let guardianPinHash: String?
    let recoveryCodeHash: String?
    let feedbackIntensity: FeedbackIntensity
    let soundEnabled: Bool
    let ttsEnabled: Bool
    let ttsRate: Double
    let ttsVolume: Double
    let hapticEnabled: Bool
    let undoDurationSeconds: Int
    let notificationLeadTimes: [Int]
    let quietHoursStart: LocalTime?
    let quietHoursEnd: LocalTime?
    let locale: String?
    let createdAt: Date
    let updatedAt: Date

    init(
        id: String = AppSettings.singletonID,
        guardianPinHash: String? = nil,
        recoveryCodeHash: String? = nil,
        feedbackIntensity: FeedbackIntensity = .normal,
        soundEnabled: Bool = true,
        ttsEnabled: Bool = true,
        ttsRate: Double = 1.0,
        ttsVolume: Double = 1.0,
        hapticEnabled: Bool = true,
        undoDurationSeconds: Int = 5,
        notificationLeadTimes: [Int] = [10, 5],
        quietHoursStart: LocalTime? = nil,
        quietHoursEnd: LocalTime? = nil,
        locale: String? = nil,
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) throws {
        self.id = id
        self.guardianPinHash = guardianPinHash
        self.recoveryCodeHash = recoveryCodeHash
        self.feedbackIntensity = feedbackIntensity
        self.soundEnabled = soundEnabled
        self.ttsEnabled = ttsEnabled
        self.ttsRate = ttsRate
        self.ttsVolume = ttsVolume
        self.hapticEnabled = hapticEnabled
        self.undoDurationSeconds = undoDurationSeconds
        self.notificationLeadTimes = notificationLeadTimes
        self.quietHoursStart = quietHoursStart
        self.quietHoursEnd = quietHoursEnd
        self.locale = locale
        self.createdAt = createdAt
        self.updatedAt = updatedAt
        try validate()
    }

    func validate() throws {
        guard id == Self.singletonID else {
            throw RoutineDomainError.invalidAppSettingsID(id)
        }
        guard (0.5...1.5).contains(ttsRate) else {
            throw RoutineDomainError.invalidTTSRate(ttsRate)
        }
        guard (0.0...1.0).contains(ttsVolume) else {
            throw RoutineDomainError.invalidTTSVolume(ttsVolume)
        }
        guard [3, 5, 10].contains(undoDurationSeconds) else {
            throw RoutineDomainError.invalidUndoDuration(undoDurationSeconds)
        }
        let validLeadTimes = Set([5, 10])
        guard Set(notificationLeadTimes).count == notificationLeadTimes.count,
              notificationLeadTimes.allSatisfy(validLeadTimes.contains) else {
            throw RoutineDomainError.invalidNotificationLeadTimes(notificationLeadTimes)
        }
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
    }
}
