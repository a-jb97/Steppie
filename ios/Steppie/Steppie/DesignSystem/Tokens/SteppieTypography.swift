import SwiftUI

enum SteppieTextStyle: Sendable {
    case childCardTitle
    case childListTitle
    case childProgress
    case childScreenTitle
    case childPaneTitle
    case childSubtitle
    case childHint
    case childNavigation
    case childCaption
    case guardianTitle
    case guardianSection
    case guardianBody
    case guardianCaption
    case button

    fileprivate var size: CGFloat {
        switch self {
        case .childCardTitle: 32
        case .childListTitle: 24
        case .childProgress, .childHint: 20
        case .childScreenTitle: 32
        case .childPaneTitle: 28
        case .childSubtitle, .childCaption: 14
        case .childNavigation: 17
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
        case .guardianBody, .guardianCaption, .childSubtitle, .childCaption: .regular
        case .childCardTitle, .childScreenTitle, .childPaneTitle, .childHint,
             .childNavigation, .guardianTitle: .bold
        case .childListTitle, .childProgress, .guardianSection, .button: .semibold
        }
    }

    fileprivate var relativeTextStyle: Font.TextStyle {
        switch self {
        case .childCardTitle, .childScreenTitle: .largeTitle
        case .childListTitle, .childPaneTitle, .guardianTitle: .title2
        case .childProgress, .childHint, .guardianSection: .title3
        case .childNavigation, .guardianBody, .button: .body
        case .childSubtitle, .childCaption, .guardianCaption: .caption
        }
    }

    fileprivate var customFontName: String? {
        switch self {
        case .childCardTitle, .childListTitle, .childScreenTitle, .childPaneTitle,
             .childHint, .childNavigation:
            "MangoDdobak-B"
        case .childSubtitle, .childCaption:
            "MangoDdobak-R"
        default:
            nil
        }
    }
}

private struct SteppieTextStyleModifier: ViewModifier {
    private let style: SteppieTextStyle
    @ScaledMetric private var scaledSize: CGFloat

    init(style: SteppieTextStyle) {
        SteppieFontRegistrar.registerBundledFonts()
        self.style = style
        _scaledSize = ScaledMetric(
            wrappedValue: style.size,
            relativeTo: style.relativeTextStyle
        )
    }

    func body(content: Content) -> some View {
        if let customFontName = style.customFontName {
            content.font(.custom(customFontName, size: max(scaledSize, style.minimumSize)))
        } else {
            content.font(
                .system(
                    size: max(scaledSize, style.minimumSize),
                    weight: style.weight,
                    design: .default
                )
            )
        }
    }
}

extension View {
    func steppieTextStyle(_ style: SteppieTextStyle) -> some View {
        modifier(SteppieTextStyleModifier(style: style))
    }
}
