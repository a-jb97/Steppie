import SwiftUI

extension Color {
    static let steppieBackgroundPrimary = Color("ColorBackgroundPrimary")
    static let steppieBackgroundSecondary = Color("ColorBackgroundSecondary")
    static let steppieTextPrimary = Color("ColorTextPrimary")
    static let steppieTextSecondary = Color("ColorTextSecondary")
    static let steppieBorderSubtle = Color("ColorBorderSubtle")
    static let steppieFocusRing = Color("ColorFocusRing")
    static let steppieSuccess = Color("ColorSuccess")
    static let steppieWarning = Color("ColorWarning")
    static let steppieDanger = Color("ColorDanger")
    static let steppieCardSky = Color("ColorCardSky")
    static let steppieCardMint = Color("ColorCardMint")
    static let steppieCardLemon = Color("ColorCardLemon")
    static let steppieCardPeach = Color("ColorCardPeach")
    static let steppieCardLavender = Color("ColorCardLavender")
    static let steppieCardRose = Color("ColorCardRose")
    static let steppieChildAction = Color("ColorChildAction")
}

enum SteppieCardColor: CaseIterable, Sendable {
    case sky
    case mint
    case lemon
    case peach
    case lavender
    case rose

    var color: Color {
        switch self {
        case .sky: .steppieCardSky
        case .mint: .steppieCardMint
        case .lemon: .steppieCardLemon
        case .peach: .steppieCardPeach
        case .lavender: .steppieCardLavender
        case .rose: .steppieCardRose
        }
    }

    init(colorToken: String) {
        switch colorToken {
        case "color.card.mint": self = .mint
        case "color.card.lemon": self = .lemon
        case "color.card.peach": self = .peach
        case "color.card.lavender": self = .lavender
        case "color.card.rose": self = .rose
        default: self = .sky
        }
    }
}
