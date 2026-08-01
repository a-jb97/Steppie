import SwiftUI

enum ChildRoutinePanePresentation {
    case phone
    case split
}

struct ChildRoutineHeader: View {
    let title: LocalizedStringKey
    let subtitle: LocalizedStringKey
    let titleStyle: SteppieTextStyle
    var titleColor: Color = .steppieTextSecondary

    var body: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.twoExtraSmall) {
            Text(title)
                .steppieTextStyle(titleStyle)
                .foregroundStyle(titleColor)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)

            Text(subtitle)
                .steppieTextStyle(.childSubtitle)
                .foregroundStyle(Color.steppieTextPrimary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

struct ChildRoutineProgressView: View {
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    let completedCount: Int
    let totalCount: Int

    var body: some View {
        VStack(alignment: .leading, spacing: SteppieSpacing.extraSmall) {
            if !dynamicTypeSize.isAccessibilitySize {
                HStack(spacing: SteppieSpacing.extraSmall) {
                    ForEach(0..<totalCount, id: \.self) { index in
                        Circle()
                            .fill(
                                index < completedCount
                                    ? Color.steppieFocusRing
                                    : Color.steppieBorderSubtle
                            )
                            .frame(width: 24, height: 24)
                            .accessibilityHidden(true)
                    }
                }
            }

            Text(verbatim: "\(completedCount)/\(totalCount)")
                .steppieTextStyle(.childProgress)
                .foregroundStyle(Color.steppieTextSecondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            Text("screen.progress.accessibility")
            + Text(verbatim: " \(completedCount)/\(totalCount)")
        )
    }
}

struct ChildRoutinePaneSwitchButton: View {
    let systemImage: String
    let title: LocalizedStringKey
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: SteppieSpacing.extraSmall) {
                Image(systemName: systemImage)
                    .font(.system(size: 24, weight: .bold))
                    .accessibilityHidden(true)
                Text(title)
                    .steppieTextStyle(.childNavigation)
            }
            .foregroundStyle(Color.steppieTextSecondary)
            .frame(maxWidth: .infinity, minHeight: SteppieLayout.childMinimumTouchTarget)
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(Text(title))
    }
}
