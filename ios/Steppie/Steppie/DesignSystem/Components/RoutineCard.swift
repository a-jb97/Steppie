import SwiftUI

enum RoutineCardPresentation: Equatable, Sendable {
    case focus
    case list
}

enum RoutineCardState: Hashable, Sendable {
    case current
    case completed
    case upcoming

    fileprivate var accessibilityValue: LocalizedStringKey {
        switch self {
        case .current, .upcoming: "a11y.status.notCompleted"
        case .completed: "a11y.status.completed"
        }
    }
}

struct RoutineCard<Visual: View>: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @FocusState private var isFocused: Bool

    let title: Text
    let metadata: Text
    let presentation: RoutineCardPresentation
    let state: RoutineCardState
    let cardColor: SteppieCardColor
    let isActionEnabled: Bool
    let focusMinimumHeight: CGFloat
    let action: () -> Void
    let visual: () -> Visual

    init(
        title: Text,
        metadata: Text,
        presentation: RoutineCardPresentation,
        state: RoutineCardState,
        cardColor: SteppieCardColor = .sky,
        isActionEnabled: Bool = true,
        focusMinimumHeight: CGFloat = 448,
        action: @escaping () -> Void,
        @ViewBuilder visual: @escaping () -> Visual
    ) {
        self.title = title
        self.metadata = metadata
        self.presentation = presentation
        self.state = state
        self.cardColor = cardColor
        self.isActionEnabled = isActionEnabled
        self.focusMinimumHeight = focusMinimumHeight
        self.action = action
        self.visual = visual
    }

    var body: some View {
        Button(action: action) {
            switch presentation {
            case .focus:
                focusContent
            case .list:
                listContent
            }
        }
        .buttonStyle(RoutineCardButtonStyle(reduceMotion: reduceMotion))
        .disabled(!isActionEnabled || (presentation == .focus && state != .current))
        .focused($isFocused)
        .overlay {
            if isFocused {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                    .stroke(Color.steppieFocusRing, lineWidth: SteppieStroke.focus)
                    .padding(-SteppieStroke.focus)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(title)
        .accessibilityValue(Text(state.accessibilityValue))
        .accessibilityHint(accessibilityHint)
    }

    private var focusContent: some View {
        VStack(spacing: SteppieSpacing.large) {
            visual()
                .frame(width: 128, height: 128)
                .frame(minHeight: 150)
                .accessibilityHidden(true)

            title
                .steppieTextStyle(.childCardTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)

            HStack(spacing: SteppieSpacing.extraSmall) {
                if state == .completed {
                    Image(systemName: "checkmark.circle.fill")
                        .accessibilityHidden(true)
                }

                metadata
                    .steppieTextStyle(.childHint)
                    .multilineTextAlignment(.center)
            }
            .foregroundStyle(focusMetadataColor)
            .fixedSize(horizontal: false, vertical: true)
        }
        .padding(36)
        .frame(maxWidth: focusMaximumWidth)
        .frame(minHeight: focusMinimumHeight)
        .background(cardColor.color)
        .overlay {
            if state == .completed {
                RoundedRectangle(cornerRadius: SteppieCornerRadius.card)
                    .stroke(Color.steppieSuccess, lineWidth: 2)
            }
        }
        .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
        .contentShape(.rect)
    }

    @ViewBuilder
    private var listContent: some View {
        if dynamicTypeSize.isAccessibilitySize {
            VStack(alignment: .leading, spacing: SteppieSpacing.small) {
                HStack {
                    listVisual
                    Spacer(minLength: SteppieSpacing.medium)
                    statusImage
                }
                listCopy
            }
            .listCardContainer(cardColor.color)
        } else {
            HStack(spacing: 14) {
                listVisual
                listCopy
                Spacer(minLength: SteppieSpacing.extraSmall)
                statusImage
            }
            .listCardContainer(cardColor.color)
        }
    }

    private var listVisual: some View {
        visual()
            .frame(width: 48, height: 48)
            .frame(width: 64, height: 64)
            .background(Color.steppieBackgroundPrimary)
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            .accessibilityHidden(true)
    }

    private var listCopy: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            title
                .steppieTextStyle(.childListTitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .lineLimit(dynamicTypeSize.isAccessibilitySize ? nil : 2)
                .fixedSize(horizontal: false, vertical: true)

            metadata
                .steppieTextStyle(.childCaption)
                .foregroundStyle(state == .completed ? Color.steppieSuccess : Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var statusImage: some View {
        Image(systemName: state == .completed ? "checkmark" : "chevron.right")
            .font(.title3.weight(.semibold))
            .foregroundStyle(state == .completed ? Color.steppieSuccess : Color.steppieTextSecondary)
            .frame(minWidth: 44, minHeight: 44)
            .accessibilityHidden(true)
    }

    private var focusMaximumWidth: CGFloat {
        horizontalSizeClass == .regular
            ? SteppieLayout.focusCardTabletMaximumWidth
            : SteppieLayout.focusCardPhoneMaximumWidth
    }

    private var focusMetadataColor: Color {
        switch state {
        case .completed: .steppieSuccess
        case .current: .steppieChildAction
        case .upcoming: .steppieTextSecondary
        }
    }

    private var accessibilityHint: Text {
        switch presentation {
        case .focus where state == .current && isActionEnabled:
            Text("component.focus.completeHint")
        case .focus:
            Text("")
        case .list:
            Text("component.list.openHint")
        }
    }
}

private struct RoutineCardButtonStyle: ButtonStyle {
    let reduceMotion: Bool

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.86 : 1)
            .scaleEffect(configuration.isPressed && !reduceMotion ? 0.96 : 1)
            .animation(.easeOut(duration: 0.2), value: configuration.isPressed)
    }
}

private extension View {
    func listCardContainer(_ color: Color) -> some View {
        padding(.vertical, SteppieSpacing.small)
            .padding(.horizontal, SteppieSpacing.medium)
            .frame(maxWidth: .infinity, minHeight: 104, alignment: .leading)
            .background(color)
            .clipShape(.rect(cornerRadius: SteppieCornerRadius.card))
            .contentShape(.rect)
    }
}

#Preview("RoutineCard · Focus") {
    HStack(spacing: SteppieSpacing.large) {
        RoutineCard(
            title: Text("양치하기"),
            metadata: Text("카드를 누르면 완료"),
            presentation: .focus,
            state: .current
        ) {} visual: {
            SteppieRoutineIcon(.brushTeeth, size: .card)
        }

        RoutineCard(
            title: Text("양치하기 완료"),
            metadata: Text("완료했어요"),
            presentation: .focus,
            state: .completed,
            cardColor: .mint
        ) {} visual: {
            SteppieRoutineIcon(.brushTeeth, size: .card)
        }
    }
    .padding()
    .background(Color.steppieBackgroundPrimary)
}

#Preview("RoutineCard · List") {
    VStack(spacing: SteppieSpacing.medium) {
        ForEach([RoutineCardState.current, .completed, .upcoming], id: \.self) { state in
            RoutineCard(
                title: Text("양치하기"),
                metadata: Text(state == .completed ? "1 · 완료" : "2 · 지금"),
                presentation: .list,
                state: state,
                cardColor: state == .completed ? .mint : .sky
            ) {} visual: {
                SteppieRoutineIcon(.brushTeeth, size: .list)
            }
        }
    }
    .padding()
    .background(Color.steppieBackgroundPrimary)
}
