import SwiftUI

enum SteppieTextStyle: Sendable {
    case childCardTitle
    case childListTitle
    case childProgress
    case guardianTitle
    case guardianSection
    case guardianBody
    case guardianCaption
    case button

    fileprivate var size: CGFloat {
        switch self {
        case .childCardTitle: 32
        case .childListTitle: 24
        case .childProgress: 22
        case .guardianTitle: 28
        case .guardianSection: 20
        case .guardianBody, .button: 17
        case .guardianCaption: 14
        }
    }

    fileprivate var minimumSize: CGFloat {
        switch self {
        case .childCardTitle, .childListTitle: 24
        default: 1
        }
    }

    fileprivate var weight: Font.Weight {
        switch self {
        case .guardianBody, .guardianCaption: .regular
        case .childCardTitle, .guardianTitle: .bold
        case .childListTitle, .childProgress, .guardianSection, .button: .semibold
        }
    }

    fileprivate var relativeTextStyle: Font.TextStyle {
        switch self {
        case .childCardTitle: .largeTitle
        case .childListTitle, .guardianTitle: .title2
        case .childProgress, .guardianSection: .title3
        case .guardianBody, .button: .body
        case .guardianCaption: .caption
        }
    }
}

private struct SteppieTextStyleModifier: ViewModifier {
    private let style: SteppieTextStyle
    @ScaledMetric private var scaledSize: CGFloat

    init(style: SteppieTextStyle) {
        self.style = style
        _scaledSize = ScaledMetric(
            wrappedValue: style.size,
            relativeTo: style.relativeTextStyle
        )
    }

    func body(content: Content) -> some View {
        content.font(
            .system(
                size: max(scaledSize, style.minimumSize),
                weight: style.weight,
                design: .default
            )
        )
    }
}

extension View {
    func steppieTextStyle(_ style: SteppieTextStyle) -> some View {
        modifier(SteppieTextStyleModifier(style: style))
    }
}
