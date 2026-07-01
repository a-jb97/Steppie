import SwiftUI

private enum SteppieAppMode {
    case child
    case guardian
}

private enum GuardianPINFlow {
    case guardianEntry
    case pinChange
    case recoveryRegeneration
    case recoveryReset
}

private enum GuardianSheet {
    case pin
    case recoveryCodeReset
    case recoveryCodeDisplay
}

struct ContentView: View {
    @Environment(\.scenePhase) private var scenePhase
    @State private var childViewModel: ChildRoutineViewModel
    @State private var guardianViewModel: GuardianModeViewModel
    @State private var appMode: SteppieAppMode = .child
    @State private var pinPurpose: GuardianPINPurpose?
    @State private var pinFlow: GuardianPINFlow?
    @State private var guardianSheet: GuardianSheet?
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
                pinFlow = .pinChange
                guardianSheet = .pin
            }
            .onReceive(NotificationCenter.default.publisher(for: .guardianRecoveryCodeRegenerationRequested)) { _ in
                pinPurpose = .enter
                pinFlow = .recoveryRegeneration
                guardianSheet = .pin
            }
            .sheet(isPresented: guardianSheetBinding, onDismiss: handleGuardianSheetDismiss) {
                if let guardianSheet {
                    guardianSheetContent(guardianSheet)
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
        pinFlow = .guardianEntry
        pinPurpose = guardianViewModel.hasGuardianPIN() ? .enter : .setup
        guardianSheet = .pin
    }

    private func handlePINSuccess(for purpose: GuardianPINPurpose) {
        let completedFlow = pinFlow
        self.pinPurpose = nil
        pinFlow = nil

        if completedFlow == .recoveryRegeneration {
            if guardianViewModel.regenerateRecoveryCode() {
                guardianSheet = .recoveryCodeDisplay
            } else {
                guardianSheet = nil
            }
            resetGuardianInactivityTimer()
            return
        }

        switch purpose {
        case .enter:
            guardianViewModel.selectedDestination = nil
            appMode = .guardian
            guardianViewModel.load()
            guardianSheet = nil
            resetGuardianInactivityTimer()
        case .setup:
            guardianViewModel.selectedDestination = nil
            appMode = .guardian
            guardianViewModel.load()
            if guardianViewModel.oneTimeRecoveryCode != nil {
                guardianSheet = .recoveryCodeDisplay
            } else {
                guardianSheet = nil
            }
            resetGuardianInactivityTimer()
        case .change:
            guardianViewModel.load()
            guardianSheet = nil
            resetGuardianInactivityTimer()
        }
    }

    private func exitGuardianMode() {
        pinPurpose = nil
        pinFlow = nil
        guardianSheet = nil
        guardianViewModel.selectedDestination = nil
        appMode = .child
        childViewModel.load()
    }

    private func resetGuardianInactivityTimer() {
        inactivityToken = UUID()
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
            get: { guardianSheet != nil },
            set: {
                if !$0 {
                    guardianSheet = nil
                }
            }
        )
    }

    @ViewBuilder
    private func guardianSheetContent(_ sheet: GuardianSheet) -> some View {
        switch sheet {
        case .pin:
            if let pinPurpose {
                NavigationStack {
                    GuardianPINView(
                        purpose: pinPurpose,
                        showsRecoveryReset: pinFlow == .guardianEntry && pinPurpose == .enter,
                        verifyPIN: guardianViewModel.verifyPIN,
                        savePIN: savePINHandler(for: pinPurpose),
                        onSuccess: {
                            handlePINSuccess(for: pinPurpose)
                        },
                        onCancel: {
                            guardianSheet = nil
                            self.pinPurpose = nil
                            pinFlow = nil
                        },
                        onRecoveryRequested: {
                            self.pinPurpose = nil
                            pinFlow = nil
                            guardianViewModel.clearRecoveryCodeError()
                            guardianSheet = .recoveryCodeReset
                        }
                    )
                }
                .presentationDetents([.large])
            }
        case .recoveryCodeReset:
            NavigationStack {
                RecoveryCodeResetView(
                    errorMessage: guardianViewModel.recoveryCodeErrorMessage,
                    verifyRecoveryCode: guardianViewModel.verifyRecoveryCode,
                    onVerified: {
                        pinPurpose = .setup
                        pinFlow = .recoveryReset
                        guardianSheet = .pin
                    },
                    onCancel: {
                        guardianViewModel.clearRecoveryCodeError()
                        guardianSheet = nil
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
                        guardianSheet = nil
                    }
                )
                .presentationDetents([.medium, .large])
            }
        }
    }

    private func handleGuardianSheetDismiss() {
        if guardianSheet == nil {
            pinPurpose = nil
            pinFlow = nil
            guardianViewModel.clearRecoveryCodeError()
            guardianViewModel.clearOneTimeRecoveryCode()
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
                        UIAccessibility.post(notification: .announcement, argument: "복구 코드가 맞지 않아요. 6자리 숫자를 확인해 주세요.")
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
                    Text(message ?? "이 코드는 한 번만 표시됩니다. 안전한 곳에 기록해 주세요.")
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
                        UIAccessibility.post(notification: .announcement, argument: "복구 코드를 복사했어요.")
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
        notificationScheduler: NoopRoutineNotificationScheduler(),
        isNotificationSchedulingEnabled: false
    )
}
