import SwiftUI

struct GuardianPINView: View {
    let purpose: GuardianPINPurpose
    let verifyPIN: (String) -> Bool
    let savePIN: (String) -> Bool
    let onSuccess: () -> Void
    let onCancel: () -> Void

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
                    header
                    pinDots
                        .frame(maxWidth: .infinity)
                        .padding(.top, SteppieSpacing.medium)
                    keypad
                        .frame(maxWidth: .infinity)
                        .padding(.top, SteppieSpacing.large)
                    if let errorMessage {
                        errorView(errorMessage)
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
            ToolbarItem(placement: .topBarLeading) {
                Button("취소", action: onCancel)
            }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(.childScreenTitle)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            Text(subtitle)
                .steppieTextStyle(.guardianCaption)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var pinDots: some View {
        HStack(spacing: 18) {
            ForEach(0..<4, id: \.self) { index in
                Circle()
                    .fill(index < enteredPIN.count ? Color.steppieFocusRing : Color.steppieBorderSubtle)
                    .frame(width: 28, height: 28)
                    .accessibilityHidden(true)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Text("PIN \(enteredPIN.count)자리 입력됨"))
        .accessibilityHint(Text(subtitle))
    }

    private var keypad: some View {
        VStack(spacing: 14) {
            ForEach([[1, 2, 3], [4, 5, 6], [7, 8, 9]], id: \.self) { row in
                HStack(spacing: 14) {
                    ForEach(row, id: \.self) { number in
                        keyButton(label: "\(number)") {
                            appendDigit("\(number)")
                        }
                    }
                }
            }
            HStack(spacing: 14) {
                Color.clear
                    .frame(width: 90, height: SteppieLayout.childMinimumTouchTarget)
                keyButton(label: "0") {
                    appendDigit("0")
                }
                keyButton(label: "delete.left", isSystemImage: true) {
                    removeDigit()
                }
                .accessibilityLabel(Text("지우기"))
            }
        }
    }

    private func keyButton(
        label: String,
        isSystemImage: Bool = false,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            if isSystemImage {
                Image(systemName: label)
                    .font(.system(size: 32, weight: .bold))
                    .foregroundStyle(Color.steppieTextPrimary)
                    .frame(width: 90, height: SteppieLayout.childMinimumTouchTarget)
                    .background(Color.steppieBackgroundPrimary)
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            } else {
                Text(label)
                    .steppieTextStyle(.childCardTitle)
                    .foregroundStyle(Color.steppieTextPrimary)
                    .frame(width: 90, height: SteppieLayout.childMinimumTouchTarget)
                    .background(Color.steppieBackgroundPrimary)
                    .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(isSystemImage ? "지우기" : "\(label)"))
    }

    private func errorView(_ message: String) -> some View {
        HStack(spacing: SteppieSpacing.extraSmall) {
            Text("!")
                .steppieTextStyle(.guardianSection)
            Text(message)
                .steppieTextStyle(.guardianCaption)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(Color.steppieDanger)
        .padding(.horizontal, 14)
        .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
        .background(Color.steppieCardRose)
        .overlay {
            RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                .stroke(Color.steppieDanger, lineWidth: SteppieStroke.divider)
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
        .accessibilityElement(children: .combine)
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
                fail("PIN이 맞지 않아요. 다시 입력해 주세요.")
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
                fail("PIN이 서로 달라요. 다시 설정해 주세요.")
                return
            }
            savePIN(enteredPIN) ? onSuccess() : fail("4자리 숫자로 입력해 주세요.")
        case .new:
            break
        }
    }

    private func submitChangePIN() {
        switch step {
        case .current:
            guard verifyPIN(enteredPIN) else {
                fail("현재 PIN이 맞지 않아요.")
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
                fail("새 PIN이 서로 달라요.")
                return
            }
            savePIN(enteredPIN) ? onSuccess() : fail("PIN을 변경하지 못했어요.")
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
        case .enter: "보호자 확인"
        case .setup: "PIN 설정"
        case .change: "PIN 변경"
        }
    }

    private var subtitle: String {
        switch (purpose, step) {
        case (.enter, _): "4자리 PIN을 입력해 주세요"
        case (.setup, .current): "새 보호자 PIN 4자리를 입력해 주세요"
        case (.setup, .confirm): "같은 PIN을 한 번 더 입력해 주세요"
        case (.change, .current): "현재 PIN을 입력해 주세요"
        case (.change, .new): "새 PIN 4자리를 입력해 주세요"
        case (.change, .confirm): "새 PIN을 한 번 더 입력해 주세요"
        default: "4자리 PIN을 입력해 주세요"
        }
    }
}
