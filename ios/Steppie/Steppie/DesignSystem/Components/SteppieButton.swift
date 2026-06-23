import SwiftUI

enum SteppieButtonRole: Equatable, Sendable {
    case primary
    case secondary
    case danger
}

enum SteppieButtonSize: Equatable, Sendable {
    case regular
    case childLarge

    fileprivate var minimumHeight: CGFloat {
        switch self {
        case .regular: 54
        case .childLarge: SteppieLayout.childMinimumTouchTarget
        }
    }
}

enum SteppieButtonState: Equatable, Sendable {
    case enabled
    case pressed
    case disabled
    case loading
}

struct SteppieButton: View {
    @FocusState private var isFocused: Bool

    let title: LocalizedStringKey
    let accessibilityLabel: LocalizedStringKey
    let role: SteppieButtonRole
    let size: SteppieButtonSize
    let state: SteppieButtonState
    let action: () -> Void

    init(
        _ title: LocalizedStringKey,
        accessibilityLabel: LocalizedStringKey? = nil,
        role: SteppieButtonRole = .primary,
        size: SteppieButtonSize = .regular,
        state: SteppieButtonState = .enabled,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.accessibilityLabel = accessibilityLabel ?? title
        self.role = role
        self.size = size
        self.state = state
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            label
                .frame(maxWidth: .infinity)
                .frame(minHeight: size.minimumHeight)
                .padding(.horizontal, SteppieSpacing.large)
                .contentShape(.rect)
        }
        .buttonStyle(SteppieButtonStyle(role: role, forcedState: state))
        .disabled(state == .disabled || state == .loading)
        .focused($isFocused)
        .overlay {
            if isFocused {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                    .stroke(Color.steppieFocusRing, lineWidth: SteppieStroke.focus)
                    .padding(-SteppieStroke.focus)
            }
        }
        .accessibilityLabel(accessibilityLabel)
        .accessibilityValue(state == .loading ? Text("component.button.loading") : Text(""))
        .accessibilityHint(state == .loading ? Text("component.button.loadingHint") : Text(""))
    }

    @ViewBuilder
    private var label: some View {
        if state == .loading {
            ProgressView()
                .controlSize(.regular)
                .tint(labelColor)
                .accessibilityHidden(true)
        } else {
            Text(title)
                .steppieTextStyle(.button)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        }
    }

    private var labelColor: Color {
        role == .secondary ? .steppieTextPrimary : .steppieBackgroundPrimary
    }
}

private struct SteppieButtonStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    private let role: SteppieButtonRole
    private let forcedState: SteppieButtonState

    init(role: SteppieButtonRole, forcedState: SteppieButtonState) {
        self.role = role
        self.forcedState = forcedState
    }

    func makeBody(configuration: Configuration) -> some View {
        let isPressed = configuration.isPressed || forcedState == .pressed

        configuration.label
            .foregroundStyle(foregroundColor)
            .background(backgroundColor)
            .overlay {
                if role == .secondary {
                    RoundedRectangle(cornerRadius: SteppieCornerRadius.control)
                        .stroke(Color.steppieBorderSubtle, lineWidth: SteppieStroke.divider)
                }
            }
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.control))
            .opacity(forcedState == .disabled ? 0.4 : (isPressed ? 0.86 : 1))
            .scaleEffect(isPressed && !reduceMotion ? 0.96 : 1)
            .animation(.easeOut(duration: 0.2), value: isPressed)
    }

    private var foregroundColor: Color {
        role == .secondary ? .steppieTextPrimary : .steppieBackgroundPrimary
    }

    private var backgroundColor: Color {
        switch role {
        case .primary: .steppieFocusRing
        case .secondary: .steppieBackgroundPrimary
        case .danger: .steppieDanger
        }
    }
}

#Preview("Button · Roles") {
    VStack(spacing: SteppieSpacing.medium) {
        SteppieButton("저장", role: .primary) {}
        SteppieButton("취소", role: .secondary) {}
        SteppieButton("삭제", role: .danger) {}
    }
    .padding()
    .background(Color.steppieBackgroundSecondary)
}

#Preview("Button · States") {
    VStack(spacing: SteppieSpacing.medium) {
        SteppieButton("활성", state: .enabled) {}
        SteppieButton("눌림", state: .pressed) {}
        SteppieButton("비활성", state: .disabled) {}
        SteppieButton("저장", state: .loading) {}
    }
    .padding()
    .background(Color.steppieBackgroundSecondary)
}
