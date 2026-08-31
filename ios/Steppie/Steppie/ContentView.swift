import SwiftUI

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var childViewModel: ChildRoutineViewModel
    @State private var guardianViewModel: GuardianModeViewModel
    @State private var tutorialCoordinator = TutorialCoordinator()
    @State private var flowCoordinator = AppFlowCoordinator()
    @State private var firstLaunchSetupCoordinator = FirstLaunchSetupCoordinator()
    @State private var didCompleteInitialChildTutorial = false
    @State private var inactivityToken = UUID()
    private let notificationRouter: RoutineNotificationRouter?

    init(
        repository: any RoutineRepository,
        photoStore: any RoutinePhotoStoring,
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
                photoStore: photoStore,
                onDataChanged: {
                    childViewModel.load()
                }
            )
        )
    }

    var body: some View {
        Group {
            switch flowCoordinator.state.mode {
            case .child:
                ChildRoutineView(
                    viewModel: childViewModel,
                    tutorialCoordinator: tutorialCoordinator,
                    onGuardianEntryRequested: beginGuardianEntry
                )
            case .guardian:
                GuardianModeView(
                    viewModel: guardianViewModel,
                    tutorialCoordinator: tutorialCoordinator,
                    onDone: exitGuardianMode,
                    onSecurityAction: handleGuardianSecurityAction,
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
            .onChange(of: tutorialCoordinator.completion) { _, completion in
                guard completion?.screen == .childFocus else { return }
                didCompleteInitialChildTutorial = true
                evaluateInitialPINSetup()
            }
            .onChange(of: childViewModel.loadState) { _, _ in
                evaluateInitialPINSetup()
            }
            .sheet(isPresented: guardianSheetBinding, onDismiss: handleGuardianSheetDismiss) {
                if let guardianSheet = flowCoordinator.state.sheet {
                    guardianSheetContent(guardianSheet)
                }
            }
            .task(id: inactivityToken) {
                guard flowCoordinator.state.mode == .guardian else { return }
                try? await Task.sleep(for: .seconds(180))
                guard !Task.isCancelled, flowCoordinator.state.mode == .guardian else { return }
                exitGuardianMode()
            }
            .task {
                resumeInitialPINSetupIfNeeded()
            }
    }

    private func consumePendingNotificationRoute() {
        guard let route = notificationRouter?.consumePendingRoute() else { return }
        childViewModel.openNotificationRoute(route)
    }

    private func beginGuardianEntry() {
        childViewModel.setRoutineSpeechActive(false)
        guardianViewModel.load()
        flowCoordinator.beginGuardianEntry(
            hasGuardianPIN: guardianViewModel.hasGuardianPIN()
        )
    }

    private func evaluateInitialPINSetup() {
        if firstLaunchSetupCoordinator.requiresPINSetup {
            resumeInitialPINSetupIfNeeded()
            return
        }

        guard didCompleteInitialChildTutorial else { return }

        let hasNoRoutines: Bool
        switch childViewModel.loadState {
        case .empty:
            hasNoRoutines = true
        case .loaded:
            hasNoRoutines = false
        case .idle, .failed:
            return
        }

        let shouldPresentPINSetup = firstLaunchSetupCoordinator.handleMainTutorialCompletion(
            hasNoRoutines: hasNoRoutines,
            hasGuardianPIN: guardianViewModel.hasGuardianPIN()
        )
        guard shouldPresentPINSetup else { return }
        presentInitialPINSetup()
    }

    private func resumeInitialPINSetupIfNeeded() {
        firstLaunchSetupCoordinator.reconcile(
            hasGuardianPIN: guardianViewModel.hasGuardianPIN()
        )
        guard firstLaunchSetupCoordinator.requiresPINSetup else { return }
        presentInitialPINSetup()
    }

    private func presentInitialPINSetup() {
        guard flowCoordinator.state.mode == .child,
              flowCoordinator.state.sheet == nil else { return }
        childViewModel.setRoutineSpeechActive(false)
        guardianViewModel.load()
        flowCoordinator.beginInitialPINSetup()
    }

    private func handlePINSuccess(
        for purpose: GuardianPINPurpose,
        flow: GuardianPINFlow
    ) {
        if flow == .recoveryRegeneration {
            flowCoordinator.completeRecoveryCodeRegeneration(
                succeeded: guardianViewModel.regenerateRecoveryCode()
            )
            resetGuardianInactivityTimer()
            return
        }

        switch purpose {
        case .enter:
            guardianViewModel.selectedDestination = nil
            guardianViewModel.load()
            flowCoordinator.completePIN(purpose: purpose)
            resetGuardianInactivityTimer()
        case .setup:
            guardianViewModel.selectedDestination = nil
            guardianViewModel.load()
            if flow == .initialSetup {
                firstLaunchSetupCoordinator.completePINSetup()
            }
            flowCoordinator.completePIN(
                purpose: purpose,
                shouldDisplayRecoveryCode: guardianViewModel.oneTimeRecoveryCode != nil
            )
            resetGuardianInactivityTimer()
        case .change:
            guardianViewModel.load()
            flowCoordinator.completePIN(purpose: purpose)
            resetGuardianInactivityTimer()
        }
    }

    private func exitGuardianMode() {
        guardianViewModel.selectedDestination = nil
        flowCoordinator.exitGuardianMode()
        childViewModel.load()
    }

    private func resetGuardianInactivityTimer() {
        inactivityToken = UUID()
    }

    private func handleGuardianSecurityAction(_ action: GuardianSecurityAction) {
        switch action {
        case .changePIN:
            flowCoordinator.requestPINChange()
        case .regenerateRecoveryCode:
            flowCoordinator.requestRecoveryCodeRegeneration()
        }
    }

    private func savePINHandler(for purpose: GuardianPINPurpose) -> (String) -> Bool {
        switch purpose {
        case .setup:
            guardianViewModel.setPINAndGenerateRecoveryCode
        case .enter, .change:
            guardianViewModel.setPIN
        }
    }

    private var guardianSheetBinding: Binding<Bool> {
        Binding(
            get: { flowCoordinator.state.sheet != nil },
            set: {
                if !$0 {
                    flowCoordinator.dismissSheet()
                }
            }
        )
    }

    @ViewBuilder
    private func guardianSheetContent(_ sheet: GuardianSheetDestination) -> some View {
        switch sheet {
        case let .pin(pinPurpose, pinFlow):
            NavigationStack {
                GuardianPINView(
                    purpose: pinPurpose,
                    showsRecoveryReset: pinFlow == .guardianEntry && pinPurpose == .enter,
                    allowsCancellation: sheet.allowsDismissal,
                    verifyPIN: guardianViewModel.verifyPIN,
                    savePIN: savePINHandler(for: pinPurpose),
                    onSuccess: {
                        handlePINSuccess(for: pinPurpose, flow: pinFlow)
                    },
                    onCancel: {
                        flowCoordinator.dismissSheet()
                    },
                    onRecoveryRequested: {
                        guardianViewModel.clearRecoveryCodeError()
                        flowCoordinator.requestRecoveryCodeReset()
                    }
                )
                .tutorialTarget(.primary)
                .tutorialOverlay(coordinator: tutorialCoordinator, screen: .guardianPIN)
            }
            .presentationDetents([.large])
            .interactiveDismissDisabled(!sheet.allowsDismissal)
        case .recoveryCodeReset:
            NavigationStack {
                RecoveryCodeResetView(
                    errorMessage: guardianViewModel.recoveryCodeErrorMessage,
                    verifyRecoveryCode: guardianViewModel.verifyRecoveryCode,
                    onVerified: {
                        flowCoordinator.completeRecoveryCodeVerification()
                    },
                    onCancel: {
                        guardianViewModel.clearRecoveryCodeError()
                        flowCoordinator.dismissSheet()
                    }
                )
            }
            .presentationDetents([.medium, .large])
        case .recoveryCodeDisplay:
            if let code = guardianViewModel.oneTimeRecoveryCode {
                RecoveryCodeDisplayView(
                    code: code,
                    message: guardianViewModel.recoveryCodeStatusMessage,
                    onDone: {
                        guardianViewModel.clearOneTimeRecoveryCode()
                        flowCoordinator.dismissSheet()
                    }
                )
                .tutorialTarget(.primary)
                .tutorialOverlay(coordinator: tutorialCoordinator, screen: .recoveryCode)
                .presentationDetents([.medium, .large])
            }
        }
    }

    private func handleGuardianSheetDismiss() {
        if flowCoordinator.state.sheet == nil {
            guardianViewModel.clearRecoveryCodeError()
            guardianViewModel.clearOneTimeRecoveryCode()
            if flowCoordinator.state.mode == .child {
                childViewModel.setRoutineSpeechActive(true)
            }
        }
    }
}

private struct RecoveryCodeResetView: View {
    let errorMessage: String?
    let verifyRecoveryCode: (String) -> Bool
    let onVerified: () -> Void
    let onCancel: () -> Void

    @State private var recoveryCode = ""
    @AccessibilityFocusState private var errorFocused: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.large) {
                header
                TextField("6자리 복구 코드", text: recoveryCodeBinding)
                    .keyboardType(.numberPad)
                    .textContentType(.oneTimeCode)
                    .steppieTextStyle(.childScreenTitle)
                    .multilineTextAlignment(.center)
                    .padding(14)
                    .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget)
                    .background(Color.steppieBackgroundPrimary)
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
                    .accessibilityLabel(Text("복구 코드"))
                    .accessibilityValue(Text("\(recoveryCode.count)자리 입력됨"))
                if let errorMessage {
                    Text(errorMessage)
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieDanger)
                        .fixedSize(horizontal: false, vertical: true)
                        .accessibilityFocused($errorFocused)
                }
                SteppieButton(
                    "새 PIN 설정",
                    state: recoveryCode.count == 6 ? .enabled : .disabled
                ) {
                    if verifyRecoveryCode(recoveryCode) {
                        onVerified()
                    } else {
                        UIAccessibility.post(
                            notification: .announcement,
                            argument: String(localized: "복구 코드가 맞지 않아요. 6자리 숫자를 확인해 주세요.")
                        )
                        errorFocused = true
                    }
                }
                SteppieButton("취소", role: .secondary, action: onCancel)
            }
            .frame(maxWidth: SteppieLayout.focusCardPhoneMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text("복구 코드 확인")
                .steppieTextStyle(.guardianTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text("6자리 복구 코드를 입력하면 새 보호자 PIN을 설정할 수 있습니다.")
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var recoveryCodeBinding: Binding<String> {
        Binding(
            get: { recoveryCode },
            set: { recoveryCode = String($0.filter(\.isNumber).prefix(6)) }
        )
    }
}

private struct RecoveryCodeDisplayView: View {
    let code: String
    let message: String?
    let onDone: () -> Void
    @State private var isCopyAlertPresented = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SteppieSpacing.large) {
                VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
                    Text("복구 코드")
                        .steppieTextStyle(.guardianTitle)
                        .foregroundStyle(Color.steppieTextSecondary)
                        .fixedSize(horizontal: false, vertical: true)
                        .accessibilityAddTraits(.isHeader)
                    Text(message ?? String(localized: "이 코드는 한 번만 표시됩니다. 안전한 곳에 기록해 주세요."))
                        .steppieTextStyle(.guardianCaption)
                        .foregroundStyle(Color.steppieTextPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                }
                Text(spacedCode)
                    .steppieTextStyle(.childScreenTitle)
                    .foregroundStyle(Color.steppieTextSecondary)
                    .padding(18)
                    .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget)
                    .background(Color.steppieBackgroundPrimary)
                    .overlay {
                        RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                            .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
                    }
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
                    .accessibilityLabel(Text("복구 코드 \(spacedCode)"))
                Text("닫은 뒤에는 다시 볼 수 없고, 보안 화면에서 PIN 확인 후 새 코드만 만들 수 있습니다.")
                    .steppieTextStyle(.guardianCaption)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .fixedSize(horizontal: false, vertical: true)
                VStack(spacing: SteppieSpacing.small) {
                    SteppieButton("복사", role: .secondary) {
                        UIPasteboard.general.string = code
                        isCopyAlertPresented = true
                        UIAccessibility.post(
                            notification: .announcement,
                            argument: String(localized: "복구 코드를 복사했어요.")
                        )
                    }
                    SteppieButton("확인", action: onDone)
                }
            }
            .frame(maxWidth: SteppieLayout.focusCardPhoneMaximumWidth)
            .frame(maxWidth: .infinity)
            .padding(SteppieLayout.guardianScreenPadding)
        }
        .background(Color.steppieBackgroundSecondary)
        .alert("복사 완료", isPresented: $isCopyAlertPresented) {
            Button("확인", role: .cancel) {}
        } message: {
            Text("복구 코드를 클립보드에 복사했어요.")
        }
    }

    private var spacedCode: String {
        code.map(String.init).joined(separator: " ")
    }
}

#Preview {
    let repository = try! RoutinePreviewStore.makeLightweightSampleRepository()
    ContentView(
        repository: repository,
        photoStore: FileRoutinePhotoStore(),
        notificationScheduler: NoopRoutineNotificationScheduler(),
        isNotificationSchedulingEnabled: false
    )
}
