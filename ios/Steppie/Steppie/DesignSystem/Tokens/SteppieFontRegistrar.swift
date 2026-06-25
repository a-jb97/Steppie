import CoreText
import Foundation

enum SteppieFontRegistrar {
    private static let registration: Void = {
        ["MangoDdobak-L(otf)", "MangoDdobak-R(otf)", "MangoDdobak-B(otf)"]
            .compactMap { Bundle.main.url(forResource: $0, withExtension: "otf") }
            .forEach { url in
                CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
            }
    }()

    static func registerBundledFonts() {
        _ = registration
    }
}
