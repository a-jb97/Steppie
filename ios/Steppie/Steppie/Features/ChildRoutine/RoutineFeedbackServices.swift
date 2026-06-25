import AVFoundation
import UIKit

@MainActor
protocol RoutineSpeechGuiding {
    func speak(_ text: String, settings: AppSettings)
    func stop()
}

@MainActor
protocol RoutineFeedbackPerforming {
    func routineCompleted(settings: AppSettings)
    func allRoutinesCompleted(settings: AppSettings)
}

struct NoopRoutineSpeechGuide: RoutineSpeechGuiding {
    init() {}

    func speak(_ text: String, settings: AppSettings) {}
    func stop() {}
}

struct NoopRoutineFeedbackPerformer: RoutineFeedbackPerforming {
    init() {}

    func routineCompleted(settings: AppSettings) {}
    func allRoutinesCompleted(settings: AppSettings) {}
}

@MainActor
final class IOSRoutineSpeechGuide: RoutineSpeechGuiding {
    private let synthesizer = AVSpeechSynthesizer()

    func speak(_ text: String, settings: AppSettings) {
        guard settings.ttsEnabled, !text.isEmpty else { return }
        synthesizer.stopSpeaking(at: .immediate)

        let utterance = AVSpeechUtterance(string: text)
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * Float(settings.ttsRate)
        utterance.volume = Float(settings.ttsVolume)
        utterance.voice = AVSpeechSynthesisVoice(language: Locale.autoupdatingCurrent.identifier)
            ?? AVSpeechSynthesisVoice(language: "ko-KR")
        synthesizer.speak(utterance)
    }

    func stop() {
        synthesizer.stopSpeaking(at: .immediate)
    }
}

@MainActor
struct IOSRoutineFeedbackPerformer: RoutineFeedbackPerforming {
    func routineCompleted(settings: AppSettings) {
        guard allowsHaptics(settings) else { return }
        let style: UIImpactFeedbackGenerator.FeedbackStyle = settings.feedbackIntensity == .strong
            ? .medium
            : .light
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.prepare()
        generator.impactOccurred()
    }

    func allRoutinesCompleted(settings: AppSettings) {
        guard allowsHaptics(settings) else { return }
        let generator = UINotificationFeedbackGenerator()
        generator.prepare()
        generator.notificationOccurred(.success)
    }

    private func allowsHaptics(_ settings: AppSettings) -> Bool {
        settings.hapticEnabled
            && settings.feedbackIntensity != .quiet
            && settings.feedbackIntensity != .off
    }
}
