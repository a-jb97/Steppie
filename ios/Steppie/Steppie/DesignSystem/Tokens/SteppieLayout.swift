import CoreGraphics

enum SteppieSpacing {
    static let twoExtraSmall: CGFloat = 4
    static let extraSmall: CGFloat = 8
    static let small: CGFloat = 12
    static let medium: CGFloat = 16
    static let large: CGFloat = 24
    static let extraLarge: CGFloat = 32
    static let twoExtraLarge: CGFloat = 48
}

enum SteppieCornerRadius {
    static let card: CGFloat = 16
    static let control: CGFloat = 12
    static let sheet: CGFloat = 24
}

enum SteppieStroke {
    static let focus: CGFloat = 3
    static let divider: CGFloat = 1
}

enum SteppieLayout {
    static let childMinimumTouchTarget: CGFloat = 80
    static let guardianMinimumTouchTarget: CGFloat = 44
    static let focusCardPhoneMaximumWidth: CGFloat = 480
    static let focusCardTabletMaximumWidth: CGFloat = 640
    static let splitListWidth: CGFloat = 360
    static let childScreenPadding: CGFloat = 24
    static let guardianScreenPadding: CGFloat = 20
    static let splitMinimumWidth = splitListWidth
        + SteppieStroke.divider
        + (2 * SteppieSpacing.extraLarge)
        + focusCardPhoneMaximumWidth
}
