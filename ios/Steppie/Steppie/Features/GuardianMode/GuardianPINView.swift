import SwiftUI

struct GuardianPINView: View {
    let purpose: GuardianPINPurpose
    let showsRecoveryReset: Bool
    let allowsCancellation: Bool
    let verifyPIN: (String) -> Bool
    let savePIN: (String) -> Bool
    let onSuccess: () -> Void
    let onCancel: () -> Void
    let onRecoveryRequested: (() -> Void)?

    @State private var firstPIN = ""
    @State private var currentPIN = ""
    @State private var enteredPIN = ""
    @State private var step: Step = .current
    @State private var errorMessage: String?
    @AccessibilityFocusState private var errorFocused: Bool

    private enum Step {
        case current
        case new
        case confirm
    }

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(alignment: .leading, spacing: SteppieSpacing.large) {
                    GuardianPINComponents.Header(
                        title: title,
                        subtitle: subtitle
                    )
                    GuardianPINComponents.Indicator(
                        enteredCount: enteredPIN.count,
                        hint: subtitle
                    )
                        .frame(maxWidth: .infinity)
                        .padding(.top, SteppieSpacing.medium)
                    GuardianPINComponents.Keypad(
                        onDigit: appendDigit,
                        onDelete: removeDigit
                    )
                        .frame(maxWidth: .infinity)
                        .padding(.top, SteppieSpacing.large)
                    if purpose == .enter, showsRecoveryReset, let onRecoveryRequested {
                        Button("복구 코드로 PIN 재설정", action: onRecoveryRequested)
                            .steppieTextStyle(.button)
                            .foregroundStyle(Color.steppieTextSecondary)
                            .frame(maxWidth: .infinity, minHeight: SteppieLayout.guardianMinimumTouchTarget)
                            .accessibilityHint(Text("6자리 복구 코드로 새 PIN을 설정합니다"))
                    }
                    if let errorMessage {
                        GuardianPINComponents.ErrorMessage(message: errorMessage)
                            .accessibilityFocused($errorFocused)
                    }
                    Spacer(minLength: SteppieSpacing.large)
                }
                .frame(maxWidth: 345)
                .frame(maxWidth: .infinity)
                .padding(.horizontal, SteppieLayout.guardianScreenPadding)
                .padding(.top, SteppieSpacing.extraLarge)
            }
        }
        .background(Color.steppieBackgroundSecondary)
        .toolbar {
            if allowsCancellation {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소", action: onCancel)
                }
            }
        }
    }

    private func appendDigit(_ digit: String) {
        guard enteredPIN.count < 4 else { return }
        enteredPIN.append(digit)
        errorMessage = nil
        if enteredPIN.count == 4 {
            submitPIN()
        }
    }

    private func removeDigit() {
        guard !enteredPIN.isEmpty else { return }
        enteredPIN.removeLast()
        errorMessage = nil
    }

    private func submitPIN() {
        switch purpose {
        case .enter:
            if verifyPIN(enteredPIN) {
                onSuccess()
            } else {
                fail(String(localized: "PIN이 맞지 않아요. 다시 입력해 주세요."))
            }
        case .setup:
            submitSetupPIN()
        case .change:
            submitChangePIN()
        }
    }

    private func submitSetupPIN() {
        switch step {
        case .current:
            firstPIN = enteredPIN
            enteredPIN = ""
            step = .confirm
            announcePINStep()
        case .confirm:
            guard enteredPIN == firstPIN else {
                firstPIN = ""
                step = .current
                fail(String(localized: "PIN이 서로 달라요. 다시 설정해 주세요."))
                return
            }
            savePIN(enteredPIN) ? onSuccess() : fail(String(localized: "4자리 숫자로 입력해 주세요."))
        case .new:
            break
        }
    }

    private func submitChangePIN() {
        switch step {
        case .current:
            guard verifyPIN(enteredPIN) else {
                fail(String(localized: "현재 PIN이 맞지 않아요."))
                return
            }
            currentPIN = enteredPIN
            enteredPIN = ""
            step = .new
            announcePINStep()
        case .new:
            firstPIN = enteredPIN
            enteredPIN = ""
            step = .confirm
            announcePINStep()
        case .confirm:
            guard enteredPIN == firstPIN else {
                firstPIN = ""
                step = .new
                fail(String(localized: "새 PIN이 서로 달라요."))
                return
            }
            savePIN(enteredPIN) ? onSuccess() : fail(String(localized: "PIN을 변경하지 못했어요."))
        }
    }

    private func fail(_ message: String) {
        enteredPIN = ""
        errorMessage = message
        UIAccessibility.post(notification: .announcement, argument: message)
        errorFocused = true
    }

    private func announcePINStep() {
        UIAccessibility.post(notification: .announcement, argument: subtitle)
    }

    private var title: String {
        switch purpose {
        case .enter: String(localized: "보호자 확인")
        case .setup: String(localized: "PIN 설정")
        case .change: String(localized: "PIN 변경")
        }
    }

    private var subtitle: String {
        switch (purpose, step) {
        case (.enter, _): String(localized: "4자리 PIN을 입력해 주세요")
        case (.setup, .current): String(localized: "새 보호자 PIN 4자리를 입력해 주세요")
        case (.setup, .confirm): String(localized: "같은 PIN을 한 번 더 입력해 주세요")
        case (.change, .current): String(localized: "현재 PIN을 입력해 주세요")
        case (.change, .new): String(localized: "새 PIN 4자리를 입력해 주세요")
        case (.change, .confirm): String(localized: "새 PIN을 한 번 더 입력해 주세요")
        default: String(localized: "4자리 PIN을 입력해 주세요")
        }
    }
}
