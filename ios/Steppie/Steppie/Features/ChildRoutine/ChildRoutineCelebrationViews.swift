import SwiftUI

struct PraiseFeedbackMark: View {
    let imageName: String
    let size: CGSize
    let intensity: FeedbackIntensity
    let reduceMotion: Bool
    let showsParticles: Bool

    @State private var hasAnimated = false

    var body: some View {
        Image(imageName)
            .resizable()
            .scaledToFit()
            .frame(width: size.width, height: size.height)
            .scaleEffect(markScale)
            .opacity(markOpacity)
            .overlay {
                if shouldShowParticles {
                    PraiseParticles(isExpanded: hasAnimated)
                        .frame(width: size.width + 96, height: size.height + 96)
                        .accessibilityHidden(true)
                }
            }
            .accessibilityHidden(true)
            .onAppear {
                startAnimationIfNeeded()
            }
    }

    private var shouldAnimate: Bool {
        !reduceMotion && (intensity == .strong || intensity == .normal)
    }

    private var shouldShowParticles: Bool {
        shouldAnimate && intensity == .strong && showsParticles
    }

    private var markScale: CGFloat {
        guard shouldAnimate else { return 1 }
        if hasAnimated { return 1 }
        return intensity == .strong ? 0.74 : 0.86
    }

    private var markOpacity: Double {
        guard shouldAnimate else { return 1 }
        return hasAnimated ? 1 : 0
    }

    private func startAnimationIfNeeded() {
        guard shouldAnimate, !hasAnimated else { return }
        let animation: Animation = intensity == .strong
            ? .spring(response: 0.42, dampingFraction: 0.58)
            : .easeOut(duration: 0.28)
        withAnimation(animation) {
            hasAnimated = true
        }
    }
}

struct PraiseFeedbackOutline: View {
    let cornerRadius: CGFloat
    let intensity: FeedbackIntensity
    let reduceMotion: Bool
    let lineWidth: CGFloat

    @State private var hasAnimated = false

    var body: some View {
        RoundedRectangle(cornerRadius: cornerRadius)
            .stroke(Color.steppieSuccess.opacity(primaryOpacity), lineWidth: lineWidth)
            .overlay {
                if shouldPulse {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .stroke(Color.steppieSuccess.opacity(hasAnimated ? 0 : 0.38), lineWidth: lineWidth)
                        .scaleEffect(hasAnimated ? 1.035 : 1)
                }
            }
            .onAppear {
                startAnimationIfNeeded()
            }
    }

    private var shouldPulse: Bool {
        !reduceMotion && (intensity == .strong || intensity == .normal)
    }

    private var primaryOpacity: Double {
        intensity == .off ? 0 : 1
    }

    private func startAnimationIfNeeded() {
        guard shouldPulse, !hasAnimated else { return }
        withAnimation(.easeOut(duration: intensity == .strong ? 0.52 : 0.34)) {
            hasAnimated = true
        }
    }
}

private struct PraiseParticles: View {
    let isExpanded: Bool

    private let particles: [PraiseParticle] = [
        PraiseParticle(id: 0, x: -44, y: -36, size: 12, color: .steppieCardPeach),
        PraiseParticle(id: 1, x: -18, y: -58, size: 9, color: .steppieFocusRing),
        PraiseParticle(id: 2, x: 24, y: -54, size: 10, color: .steppieCardLemon),
        PraiseParticle(id: 3, x: 50, y: -22, size: 11, color: .steppieCardRose),
        PraiseParticle(id: 4, x: 42, y: 34, size: 8, color: .steppieChildAction),
        PraiseParticle(id: 5, x: 4, y: 58, size: 10, color: .steppieCardLavender),
        PraiseParticle(id: 6, x: -38, y: 42, size: 9, color: .steppieSuccess),
        PraiseParticle(id: 7, x: -58, y: 8, size: 8, color: .steppieCardSky)
    ]

    var body: some View {
        ZStack {
            ForEach(particles) { particle in
                Circle()
                    .fill(particle.color)
                    .frame(width: particle.size, height: particle.size)
                    .offset(
                        x: isExpanded ? particle.x : particle.x * 0.18,
                        y: isExpanded ? particle.y : particle.y * 0.18
                    )
                    .opacity(isExpanded ? 0 : 0.9)
                    .scaleEffect(isExpanded ? 1 : 0.35)
            }
        }
    }
}

private struct PraiseParticle: Identifiable {
    let id: Int
    let x: CGFloat
    let y: CGFloat
    let size: CGFloat
    let color: Color
}

struct AllDoneFireworks: View {
    private let bursts: [FireworkBurstSpec] = [
        FireworkBurstSpec(
            id: 0,
            xRatio: 0.22,
            yRatio: 0.22,
            radius: 74,
            delay: 0,
            colors: [.steppieCardRose, .steppieCardLemon, .steppieFocusRing]
        ),
        FireworkBurstSpec(
            id: 1,
            xRatio: 0.78,
            yRatio: 0.26,
            radius: 82,
            delay: 0.16,
            colors: [.steppieCardPeach, .steppieChildAction, .steppieCardLavender]
        ),
        FireworkBurstSpec(
            id: 2,
            xRatio: 0.50,
            yRatio: 0.18,
            radius: 62,
            delay: 0.32,
            colors: [.steppieSuccess, .steppieCardSky, .steppieCardLemon]
        )
    ]

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                ForEach(bursts) { burst in
                    FireworkBurst(
                        spec: burst,
                        center: CGPoint(
                            x: proxy.size.width * burst.xRatio,
                            y: proxy.size.height * burst.yRatio
                        )
                    )
                    .frame(width: proxy.size.width, height: proxy.size.height)
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .clipped()
        }
        .allowsHitTesting(false)
    }
}

private struct FireworkBurst: View {
    let spec: FireworkBurstSpec
    let center: CGPoint

    @State private var phase: CGFloat = 0
    @State private var isVisible = false

    var body: some View {
        ZStack {
            ForEach(0..<12, id: \.self) { index in
                fireworkSpark(at: index)
            }

            Circle()
                .stroke(spec.colors[0].opacity(centerOpacity), lineWidth: 3)
                .frame(width: centerRingSize, height: centerRingSize)
                .position(center)
        }
        .task {
            await FireworkBurstAnimationTimeline.run(initialDelay: spec.delay) { event in
                switch event {
                case .show:
                    isVisible = true
                case .expand:
                    withAnimation(.easeOut(duration: 0.72)) {
                        phase = 1
                    }
                case .hide:
                    withAnimation(.easeOut(duration: 0.28)) {
                        isVisible = false
                    }
                }
            }
        }
    }

    private func fireworkSpark(at index: Int) -> some View {
        let angle = (Double(index) / 12.0) * Double.pi * 2
        let distance = spec.radius * phase
        let x = CGFloat(cos(angle)) * distance
        let y = CGFloat(sin(angle)) * distance
        let isLongSpark = index.isMultiple(of: 3)

        return Capsule()
            .fill(spec.colors[index % spec.colors.count])
            .frame(width: isLongSpark ? 7 : 9, height: isLongSpark ? 22 : 12)
            .rotationEffect(.radians(angle + Double.pi / 2))
            .scaleEffect(0.45 + phase * 0.65)
            .opacity(sparkOpacity(for: index))
            .position(x: center.x + x, y: center.y + y)
    }

    private func sparkOpacity(for index: Int) -> Double {
        guard isVisible else { return 0 }
        return index.isMultiple(of: 3) ? 0.95 : 0.82
    }

    private var centerOpacity: Double {
        isVisible ? 0.64 : 0
    }

    private var centerRingSize: CGFloat {
        16 + phase * 36
    }
}

enum FireworkBurstAnimationTimeline {
    enum Event: Equatable {
        case show
        case expand
        case hide
    }

    @MainActor
    static func run(
        initialDelay: Double,
        apply: (Event) -> Void
    ) async {
        guard await wait(for: initialDelay) else { return }
        apply(.show)

        guard await wait(for: 0.08) else { return }
        apply(.expand)

        guard await wait(for: 0.58) else { return }
        apply(.hide)
    }

    private static func wait(for seconds: Double) async -> Bool {
        guard !Task.isCancelled else { return false }
        guard seconds > 0 else { return true }

        do {
            try await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
            return !Task.isCancelled
        } catch {
            return false
        }
    }
}

private struct FireworkBurstSpec: Identifiable {
    let id: Int
    let xRatio: CGFloat
    let yRatio: CGFloat
    let radius: CGFloat
    let delay: Double
    let colors: [Color]
}
