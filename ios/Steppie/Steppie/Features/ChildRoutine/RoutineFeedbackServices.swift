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

enum RoutineFeedbackSoundCue: Equatable {
    case routineCompleted
    case allRoutinesCompleted
}

@MainActor
protocol RoutineSoundPlaying: AnyObject {
    func play(_ cue: RoutineFeedbackSoundCue)
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
final class IOSRoutineSoundPlayer: RoutineSoundPlaying {
    private var player: AVAudioPlayer?

    func play(_ cue: RoutineFeedbackSoundCue) {
        player?.stop()
        do {
            let player = try Self.makePlayer(for: cue)
            player.volume = 0.35
            player.prepareToPlay()
            self.player = player
            player.play()
        } catch {
            self.player = nil
        }
    }

    static func makePlayer(for cue: RoutineFeedbackSoundCue) throws -> AVAudioPlayer {
        try AVAudioPlayer(data: RoutineFeedbackSoundWaveform.data(for: cue))
    }
}

@MainActor
struct IOSRoutineFeedbackPerformer: RoutineFeedbackPerforming {
    private let soundPlayer: any RoutineSoundPlaying

    init() {
        soundPlayer = IOSRoutineSoundPlayer()
    }

    init(soundPlayer: any RoutineSoundPlaying) {
        self.soundPlayer = soundPlayer
    }

    func routineCompleted(settings: AppSettings) {
        if allowsSound(settings) {
            soundPlayer.play(.routineCompleted)
        }
        guard allowsHaptics(settings) else { return }
        let style: UIImpactFeedbackGenerator.FeedbackStyle = settings.feedbackIntensity == .strong
            ? .medium
            : .light
        let generator = UIImpactFeedbackGenerator(style: style)
        generator.prepare()
        generator.impactOccurred()
    }

    func allRoutinesCompleted(settings: AppSettings) {
        if allowsSound(settings) {
            soundPlayer.play(.allRoutinesCompleted)
        }
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

    private func allowsSound(_ settings: AppSettings) -> Bool {
        settings.soundEnabled
            && settings.feedbackIntensity != .quiet
            && settings.feedbackIntensity != .off
    }
}

private enum RoutineFeedbackSoundWaveform {
    private static let sampleRate: UInt32 = 44_100

    static func data(for cue: RoutineFeedbackSoundCue) -> Data {
        switch cue {
        case .routineCompleted:
            makeWaveFile(notes: [(frequency: 659.25, duration: 0.18)])
        case .allRoutinesCompleted:
            makeWaveFile(notes: [
                (frequency: 659.25, duration: 0.14),
                (frequency: 783.99, duration: 0.24),
            ])
        }
    }

    private static func makeWaveFile(notes: [(frequency: Double, duration: Double)]) -> Data {
        let pcmSamples = notes.flatMap { note in
            makeSamples(frequency: note.frequency, duration: note.duration)
        }
        let dataSize = UInt32(pcmSamples.count * MemoryLayout<Int16>.size)
        var data = Data()
        data.appendASCII("RIFF")
        data.appendLittleEndian(UInt32(36) + dataSize)
        data.appendASCII("WAVE")
        data.appendASCII("fmt ")
        data.appendLittleEndian(UInt32(16))
        data.appendLittleEndian(UInt16(1))
        data.appendLittleEndian(UInt16(1))
        data.appendLittleEndian(sampleRate)
        data.appendLittleEndian(sampleRate * UInt32(MemoryLayout<Int16>.size))
        data.appendLittleEndian(UInt16(MemoryLayout<Int16>.size))
        data.appendLittleEndian(UInt16(16))
        data.appendASCII("data")
        data.appendLittleEndian(dataSize)
        for sample in pcmSamples {
            data.appendLittleEndian(sample)
        }
        return data
    }

    private static func makeSamples(frequency: Double, duration: Double) -> [Int16] {
        let sampleCount = Int(Double(sampleRate) * duration)
        let attackDuration = min(0.015, duration / 2)
        let releaseDuration = min(0.05, duration / 2)
        let amplitude = Double(Int16.max) * 0.2

        return (0..<sampleCount).map { index in
            let time = Double(index) / Double(sampleRate)
            let attack = min(time / attackDuration, 1)
            let release = min(max((duration - time) / releaseDuration, 0), 1)
            let envelope = min(attack, release)
            let value = sin(2 * Double.pi * frequency * time) * amplitude * envelope
            return Int16(value.rounded())
        }
    }
}

private extension Data {
    mutating func appendASCII(_ string: String) {
        append(contentsOf: string.utf8)
    }

    mutating func appendLittleEndian<Value: FixedWidthInteger>(_ value: Value) {
        var littleEndianValue = value.littleEndian
        Swift.withUnsafeBytes(of: &littleEndianValue) { bytes in
            append(contentsOf: bytes)
        }
    }
}
