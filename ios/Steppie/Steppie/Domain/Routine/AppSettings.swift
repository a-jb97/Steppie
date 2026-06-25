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
    let feedbackIntensity: FeedbackIntensity
    let soundEnabled: Bool
    let ttsEnabled: Bool
    let ttsRate: Double
    let ttsVolume: Double
    let hapticEnabled: Bool
    let undoDurationSeconds: Int
    let createdAt: Date
    let updatedAt: Date

    init(
        id: String = AppSettings.singletonID,
        feedbackIntensity: FeedbackIntensity = .normal,
        soundEnabled: Bool = true,
        ttsEnabled: Bool = true,
        ttsRate: Double = 1.0,
        ttsVolume: Double = 1.0,
        hapticEnabled: Bool = true,
        undoDurationSeconds: Int = 5,
        createdAt: Date = .now,
        updatedAt: Date = .now
    ) throws {
        self.id = id
        self.feedbackIntensity = feedbackIntensity
        self.soundEnabled = soundEnabled
        self.ttsEnabled = ttsEnabled
        self.ttsRate = ttsRate
        self.ttsVolume = ttsVolume
        self.hapticEnabled = hapticEnabled
        self.undoDurationSeconds = undoDurationSeconds
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
        guard updatedAt >= createdAt else {
            throw RoutineDomainError.updatedAtPrecedesCreatedAt
        }
    }
}
