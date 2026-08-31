import Observation
import SwiftUI

enum TutorialScreen: String, CaseIterable, Sendable {
    case childFocus
    case childList
    case guardianPIN
    case recoveryCode
    case guardianHome
    case routineSetCreator
    case routineTemplates
    case routineEditor
    case feedbackSettings
    case records
    case recordCalendar
    case security
    case backupRestore
}

enum TutorialTarget: String, Hashable, Sendable {
    case primary, secondary, tertiary, quaternary, guardianEntry, done
}

struct TutorialStep: Identifiable, Equatable, Sendable {
    let id: String
    let target: TutorialTarget?
    let titleKey: String
    let messageKey: String
}

struct TutorialCompletion: Equatable, Sendable {
    let id = UUID()
    let screen: TutorialScreen
}

protocol TutorialProgressStoring: AnyObject {
    func contains(_ token: String) -> Bool
    func insert(_ token: String)
    func removeAll()
}

final class UserDefaultsTutorialProgressStore: TutorialProgressStoring {
    private let defaults: UserDefaults
    private let key = "tutorial.completed.tokens"

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func contains(_ token: String) -> Bool {
        Set(defaults.stringArray(forKey: key) ?? []).contains(token)
    }

    func insert(_ token: String) {
        var values = Set(defaults.stringArray(forKey: key) ?? [])
        values.insert(token)
        defaults.set(Array(values).sorted(), forKey: key)
    }

    func removeAll() {
        defaults.removeObject(forKey: key)
    }
}

@MainActor
@Observable
final class TutorialCoordinator {
    private(set) var activeScreen: TutorialScreen?
    private(set) var stepIndex = 0
    private(set) var completion: TutorialCompletion?
    private let store: any TutorialProgressStoring

    convenience init() {
        self.init(store: UserDefaultsTutorialProgressStore())
    }

    init(store: any TutorialProgressStoring) {
        self.store = store
        if ProcessInfo.processInfo.arguments.contains("-resetTutorialProgress") {
            store.removeAll()
        }
    }

    var isPresented: Bool { activeScreen != nil }
    var steps: [TutorialStep] { activeScreen.map(Self.steps(for:)) ?? [] }
    var currentStep: TutorialStep? { steps.indices.contains(stepIndex) ? steps[stepIndex] : nil }
    var isFirstStep: Bool { stepIndex == 0 }
    var isLastStep: Bool { stepIndex == steps.count - 1 }

    func presentIfNeeded(_ screen: TutorialScreen) {
        guard activeScreen == nil, !store.contains(Self.token(for: screen)) else { return }
        activeScreen = screen
        stepIndex = 0
    }

    func previous() {
        stepIndex = max(0, stepIndex - 1)
    }

    func next() {
        guard !isLastStep else {
            finish()
            return
        }
        stepIndex += 1
    }

    func skip() {
        finish()
    }

    func resetAndPresent(_ screen: TutorialScreen) {
        store.removeAll()
        activeScreen = screen
        stepIndex = 0
    }

    private func finish() {
        guard let activeScreen else { return }
        store.insert(Self.token(for: activeScreen))
        completion = TutorialCompletion(screen: activeScreen)
        self.activeScreen = nil
        stepIndex = 0
    }

    private static func token(for screen: TutorialScreen) -> String {
        "\(screen.rawValue):1"
    }

    private static func steps(for screen: TutorialScreen) -> [TutorialStep] {
        let targets: [TutorialTarget?]
        switch screen {
        case .childFocus: targets = [.primary, .secondary, .tertiary, .guardianEntry]
        case .childList: targets = [.primary, .secondary]
        case .guardianHome: targets = [.primary, .secondary, .tertiary, .done]
        case .routineSetCreator, .routineTemplates, .routineEditor, .feedbackSettings,
             .records, .recordCalendar, .security, .backupRestore:
            targets = [.primary, .secondary]
        case .guardianPIN, .recoveryCode: targets = [.primary]
        }
        return targets.enumerated().map { index, target in
            TutorialStep(
                id: "\(screen.rawValue).\(index + 1)",
                target: target,
                titleKey: "tutorial.\(screen.rawValue).\(index + 1).title",
                messageKey: "tutorial.\(screen.rawValue).\(index + 1).message"
            )
        }
    }
}

private struct TutorialTargetPreferenceKey: PreferenceKey {
    static var defaultValue: [TutorialTarget: CGRect] = [:]
    static func reduce(value: inout [TutorialTarget: CGRect], nextValue: () -> [TutorialTarget: CGRect]) {
        value.merge(nextValue(), uniquingKeysWith: { _, new in new })
    }
}

extension View {
    func tutorialTarget(_ target: TutorialTarget) -> some View {
        id(target.scrollID)
            .background {
                GeometryReader { proxy in
                    Color.clear.preference(
                        key: TutorialTargetPreferenceKey.self,
                        value: [target: proxy.frame(in: .global)]
                    )
                }
            }
    }

    func tutorialOverlay(
        coordinator: TutorialCoordinator,
        screen: TutorialScreen,
        childMode: Bool = false,
        onPresentationChanged: ((Bool) -> Void)? = nil
    ) -> some View {
        modifier(TutorialOverlayModifier(
            coordinator: coordinator,
            screen: screen,
            childMode: childMode,
            onPresentationChanged: onPresentationChanged
        ))
    }
}

private struct TutorialOverlayModifier: ViewModifier {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var hidesTargetDuringScroll = false
    let coordinator: TutorialCoordinator
    let screen: TutorialScreen
    let childMode: Bool
    let onPresentationChanged: ((Bool) -> Void)?

    func body(content: Content) -> some View {
        ScrollViewReader { scrollProxy in
            content
                .accessibilityHidden(coordinator.activeScreen == screen)
                .overlayPreferenceValue(TutorialTargetPreferenceKey.self) { frames in
                    GeometryReader { proxy in
                        if coordinator.activeScreen == screen, let step = coordinator.currentStep {
                            let overlayOrigin = proxy.frame(in: .global).origin
                            TutorialOverlayView(
                                step: step,
                                stepIndex: coordinator.stepIndex,
                                stepCount: coordinator.steps.count,
                                targetFrame: hidesTargetDuringScroll ? nil : step.target.flatMap { target in
                                    frames[target].map {
                                        $0.offsetBy(dx: -overlayOrigin.x, dy: -overlayOrigin.y)
                                    }
                                },
                                childMode: childMode,
                                reduceMotion: reduceMotion,
                                isFirst: coordinator.isFirstStep,
                                isLast: coordinator.isLastStep,
                                onPrevious: coordinator.previous,
                                onNext: coordinator.next,
                                onSkip: coordinator.skip
                            )
                            .transition(reduceMotion ? .identity : .opacity)
                        }
                    }
                }
                .task(id: screen) {
                    coordinator.presentIfNeeded(screen)
                }
                .onChange(of: coordinator.currentStep?.id) { _, _ in
                    guard coordinator.activeScreen == screen,
                          let step = coordinator.currentStep,
                          let target = step.target else { return }
                    hidesTargetDuringScroll = true
                    if reduceMotion {
                        scrollProxy.scrollTo(target.scrollID, anchor: .center)
                    } else {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            scrollProxy.scrollTo(target.scrollID, anchor: .center)
                        }
                    }
                    Task { @MainActor in
                        if !reduceMotion {
                            try? await Task.sleep(for: .milliseconds(250))
                        }
                        guard coordinator.currentStep?.id == step.id else { return }
                        hidesTargetDuringScroll = false
                    }
                }
                .onChange(of: coordinator.activeScreen) { _, value in
                    onPresentationChanged?(value == screen)
                }
        }
    }
}

private extension TutorialTarget {
    var scrollID: String { "tutorial.target.\(rawValue)" }
}

private struct TutorialOverlayView: View {
    @AccessibilityFocusState private var titleFocused: Bool
    let step: TutorialStep
    let stepIndex: Int
    let stepCount: Int
    let targetFrame: CGRect?
    let childMode: Bool
    let reduceMotion: Bool
    let isFirst: Bool
    let isLast: Bool
    let onPrevious: () -> Void
    let onNext: () -> Void
    let onSkip: () -> Void

    var body: some View {
        GeometryReader { proxy in
            ZStack {
                Color.black.opacity(0.68).ignoresSafeArea()
                if let targetFrame {
                    RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                        .strokeBorder(Color.white, lineWidth: 4)
                        .frame(width: targetFrame.width, height: targetFrame.height)
                        .position(x: targetFrame.midX, y: targetFrame.midY)
                        .accessibilityHidden(true)
                }
                card
                    .frame(maxWidth: 520)
                    .padding(childMode ? SteppieLayout.childScreenPadding : SteppieLayout.guardianScreenPadding)
                    .position(cardPosition(in: proxy.size))
            }
            .contentShape(.rect)
            .accessibilityAddTraits(.isModal)
        }
        .onAppear { titleFocused = true }
        .onChange(of: step.id) { _, _ in titleFocused = true }
    }

    private var card: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.medium) {
            Text("\(stepIndex + 1) / \(stepCount)")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextSecondary)
            Text(LocalizedStringKey(step.titleKey))
                .steppieTextStyle(childMode ? .childPaneTitle : .guardianTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .accessibilityFocused($titleFocused)
                .accessibilityIdentifier("tutorial.title")
            Text(LocalizedStringKey(step.messageKey))
                .steppieTextStyle(childMode ? .childSubtitle : .guardianBody)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
            HStack(spacing: SteppieSpacing.small) {
                if !isFirst {
                    tutorialButton("action.back", action: onPrevious)
                }
                tutorialButton(isLast ? "action.done" : "action.next", prominent: true, action: onNext)
            }
            tutorialButton("tutorial.action.skip", action: onSkip)
        }
        .padding(SteppieSpacing.large)
        .background(Color.steppieBackgroundPrimary)
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.sheet))
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.sheet)
                .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
        }
    }

    private func tutorialButton(_ key: LocalizedStringKey, prominent: Bool = false, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(key)
                .steppieTextStyle(.button)
                .frame(maxWidth: .infinity, minHeight: childMode ? 80 : 44)
                .foregroundStyle(prominent ? Color.white : Color.steppieTextPrimary)
                .background(prominent ? Color.steppieFocusRing : Color.steppieBackgroundSecondary)
                .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier(prominent ? "tutorial.primaryAction" : "tutorial.secondaryAction")
    }

    private func cardPosition(in size: CGSize) -> CGPoint {
        guard let targetFrame else { return CGPoint(x: size.width / 2, y: size.height / 2) }
        let placeBelow = targetFrame.midY < size.height / 2
        return CGPoint(x: size.width / 2, y: placeBelow ? size.height * 0.70 : size.height * 0.30)
    }
}
