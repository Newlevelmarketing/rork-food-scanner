import Foundation
import SwiftUI

/// Runtime string catalogue backed by the bundled `strings.json`.
///
/// Deliberately independent of `Bundle.main.localizedString`: the user picks a
/// language inside ModernBodyFoodScanner rather than inheriting the device one, and that choice
/// has to apply without relaunching the app.
@Observable
final class Localization {
    static let shared = Localization()

    /// Changing this re-renders every view that reads a localized string.
    var language: AppLanguage = .english

    private let tables: [String: [String: String]]

    private init() {
        guard
            let url = Bundle.main.url(forResource: "strings", withExtension: "json"),
            let data = try? Data(contentsOf: url),
            let decoded = try? JSONDecoder().decode([String: [String: String]].self, from: data)
        else {
            tables = [:]
            return
        }
        tables = decoded
    }

    /// Looks up `key` in the active language, falling back to English and then
    /// to the key itself so a missing entry is visible but never crashes.
    func string(_ key: String) -> String {
        if let value = tables[language.code]?[key] { return value }
        if let fallback = tables["en"]?[key] { return fallback }
        return key
    }

    var layoutDirection: LayoutDirection {
        language.isRTL ? .rightToLeft : .leftToRight
    }
}

/// Shorthand for a localized UI string: `Text(L("tab.home"))`.
///
/// Reading `Localization.shared.language` inside a view body registers the
/// observation dependency, so switching language updates the screen instantly.
func L(_ key: String) -> String {
    Localization.shared.string(key)
}
