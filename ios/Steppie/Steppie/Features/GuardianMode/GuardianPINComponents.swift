import SwiftUI

enum GuardianPINComponents {}

extension GuardianPINComponents {
    struct Header: View {
        let title: String
        let subtitle: String

        var body: some View {
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
    }

    struct Indicator: View {
        let enteredCount: Int
        let hint: String

        var body: some View {
            HStack(spacing: 18) {
                ForEach(0..<4, id: \.self) { index in
                    Circle()
                        .fill(index < enteredCount ? Color.steppieFocusRing : Color.steppieBorderSubtle)
                        .frame(width: 28, height: 28)
                        .accessibilityHidden(true)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(Text("PIN \(enteredCount)자리 입력됨"))
            .accessibilityHint(Text(hint))
        }
    }

    struct Keypad: View {
        let onDigit: (String) -> Void
        let onDelete: () -> Void

        var body: some View {
            VStack(spacing: 14) {
                ForEach([[1, 2, 3], [4, 5, 6], [7, 8, 9]], id: \.self) { row in
                    HStack(spacing: 14) {
                        ForEach(row, id: \.self) { number in
                            keyButton(label: "\(number)") {
                                onDigit("\(number)")
                            }
                        }
                    }
                }
                HStack(spacing: 14) {
                    Color.clear
                        .frame(width: 90, height: SteppieLayout.childMinimumTouchTarget)
                    keyButton(label: "0") {
                        onDigit("0")
                    }
                    keyButton(label: "delete.left", isSystemImage: true, action: onDelete)
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
    }

    struct ErrorMessage: View {
        let message: String

        var body: some View {
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
    }
}
