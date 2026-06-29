import SwiftUI

private enum SteppieAppMode {
    case child
    case guardian
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var childViewModel: ChildRoutineViewModel
    @State private var guardianViewModel: GuardianModeViewModel
    @State private var appMode: SteppieAppMode = .child
    @State private var pinPurpose: GuardianPINPurpose?
    @State private var inactivityToken = UUID()
    private let notificationRouter: RoutineNotificationRouter?

    init(
        repository: any RoutineRepository,
        speechGuide: (any RoutineSpeechGuiding)? = nil,
        feedbackPerformer: (any RoutineFeedbackPerforming)? = nil,
        notificationScheduler: (any RoutineNotificationScheduling)? = nil,
        isNotificationSchedulingEnabled: Bool = true,
        notificationRouter: RoutineNotificationRouter? = nil
    ) {
        self.notificationRouter = notificationRouter
        let childViewModel = ChildRoutineViewModel(
            repository: repository,
            speechGuide: speechGuide,
            feedbackPerformer: feedbackPerformer,
            notificationScheduler: notificationScheduler,
            isNotificationSchedulingEnabled: isNotificationSchedulingEnabled
        )
        _childViewModel = State(
            initialValue: childViewModel
        )
        _guardianViewModel = State(
            initialValue: GuardianModeViewModel(
                repository: repository,
                onDataChanged: {
                    childViewModel.load()
                }
            )
        )
    }

    var body: some View {
        Group {
            switch appMode {
            case .child:
                ChildRoutineView(
                    viewModel: childViewModel,
                    onGuardianEntryRequested: beginGuardianEntry
                )
            case .guardian:
                GuardianModeView(
                    viewModel: guardianViewModel,
                    onDone: exitGuardianMode,
                    onInteraction: resetGuardianInactivityTimer
                )
            }
        }
            .onChange(of: scenePhase) { _, newValue in
                guard newValue == .active else { return }
                childViewModel.appDidBecomeActive()
                consumePendingNotificationRoute()
            }
            .onChange(of: notificationRouter?.pendingRoute) { _, _ in
                consumePendingNotificationRoute()
            }
            .onReceive(NotificationCenter.default.publisher(for: .guardianPINChangeRequested)) { _ in
                pinPurpose = .change
            }
            .sheet(isPresented: pinSheetBinding) {
                if let pinPurpose {
                    NavigationStack {
                        GuardianPINView(
                            purpose: pinPurpose,
                            verifyPIN: guardianViewModel.verifyPIN,
                            savePIN: guardianViewModel.setPIN,
                            onSuccess: {
                                handlePINSuccess(for: pinPurpose)
                            },
                            onCancel: {
                                self.pinPurpose = nil
                            }
                        )
                    }
                    .presentationDetents([.large])
                }
            }
            .task(id: inactivityToken) {
                guard appMode == .guardian else { return }
                try? await Task.sleep(for: .seconds(180))
                guard !Task.isCancelled, appMode == .guardian else { return }
                exitGuardianMode()
            }
    }

    private func consumePendingNotificationRoute() {
        guard let route = notificationRouter?.consumePendingRoute() else { return }
        childViewModel.openNotificationRoute(route)
    }

    private func beginGuardianEntry() {
        guardianViewModel.load()
        pinPurpose = guardianViewModel.hasGuardianPIN() ? .enter : .setup
    }

    private func handlePINSuccess(for purpose: GuardianPINPurpose) {
        pinPurpose = nil
        switch purpose {
        case .enter, .setup:
            guardianViewModel.selectedDestination = nil
            appMode = .guardian
            guardianViewModel.load()
            resetGuardianInactivityTimer()
        case .change:
            guardianViewModel.load()
            resetGuardianInactivityTimer()
        }
    }

    private func exitGuardianMode() {
        pinPurpose = nil
        guardianViewModel.selectedDestination = nil
        appMode = .child
        childViewModel.load()
    }

    private func resetGuardianInactivityTimer() {
        inactivityToken = UUID()
    }

    private var pinSheetBinding: Binding<Bool> {
        Binding(
            get: { pinPurpose != nil },
            set: { if !$0 { pinPurpose = nil } }
        )
    }
}

#Preview {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ContentView(
        repository: repository,
        notificationScheduler: NoopRoutineNotificationScheduler(),
        isNotificationSchedulingEnabled: false
    )
}
