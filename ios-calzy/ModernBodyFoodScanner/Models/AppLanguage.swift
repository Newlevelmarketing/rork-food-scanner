import Foundation

/// A language ModernBodyFoodScanner can present itself in.
///
/// `code` matches a top-level key in `strings.json` and is also handed to the
/// nutrition model so meal names come back in the same language the interface
/// is wearing.
nonisolated struct AppLanguage: Identifiable, Hashable, Sendable {
    let code: String
    /// Name in English, used for search so "Spanish" finds Español.
    let englishName: String
    /// Endonym — what speakers call the language themselves.
    let nativeName: String
    let flag: String
    let isRTL: Bool

    var id: String { code }

    init(_ code: String, _ englishName: String, _ nativeName: String, _ flag: String, rtl: Bool = false) {
        self.code = code
        self.englishName = englishName
        self.nativeName = nativeName
        self.flag = flag
        isRTL = rtl
    }

    static let english = AppLanguage("en", "English", "English", "🇺🇸")

    /// Ordered roughly by global reach, English first.
    static let all: [AppLanguage] = [
        english,
        AppLanguage("zh-Hans", "Chinese (Simplified)", "简体中文", "🇨🇳"),
        AppLanguage("zh-Hant", "Chinese (Traditional)", "繁體中文", "🇹🇼"),
        AppLanguage("es", "Spanish", "Español", "🇪🇸"),
        AppLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
        AppLanguage("ar", "Arabic", "العربية", "🇸🇦", rtl: true),
        AppLanguage("pt", "Portuguese", "Português", "🇧🇷"),
        AppLanguage("bn", "Bengali", "বাংলা", "🇧🇩"),
        AppLanguage("ru", "Russian", "Русский", "🇷🇺"),
        AppLanguage("ja", "Japanese", "日本語", "🇯🇵"),
        AppLanguage("de", "German", "Deutsch", "🇩🇪"),
        AppLanguage("fr", "French", "Français", "🇫🇷"),
        AppLanguage("ko", "Korean", "한국어", "🇰🇷"),
        AppLanguage("it", "Italian", "Italiano", "🇮🇹"),
        AppLanguage("tr", "Turkish", "Türkçe", "🇹🇷"),
        AppLanguage("vi", "Vietnamese", "Tiếng Việt", "🇻🇳"),
        AppLanguage("id", "Indonesian", "Bahasa Indonesia", "🇮🇩"),
        AppLanguage("ms", "Malay", "Bahasa Melayu", "🇲🇾"),
        AppLanguage("th", "Thai", "ไทย", "🇹🇭"),
        AppLanguage("pl", "Polish", "Polski", "🇵🇱"),
        AppLanguage("nl", "Dutch", "Nederlands", "🇳🇱"),
        AppLanguage("uk", "Ukrainian", "Українська", "🇺🇦"),
        AppLanguage("fa", "Persian", "فارسی", "🇮🇷", rtl: true),
        AppLanguage("he", "Hebrew", "עברית", "🇮🇱", rtl: true),
        AppLanguage("el", "Greek", "Ελληνικά", "🇬🇷"),
        AppLanguage("cs", "Czech", "Čeština", "🇨🇿"),
        AppLanguage("ro", "Romanian", "Română", "🇷🇴"),
        AppLanguage("hu", "Hungarian", "Magyar", "🇭🇺"),
        AppLanguage("sv", "Swedish", "Svenska", "🇸🇪"),
        AppLanguage("nb", "Norwegian", "Norsk", "🇳🇴"),
        AppLanguage("da", "Danish", "Dansk", "🇩🇰"),
        AppLanguage("fi", "Finnish", "Suomi", "🇫🇮")
    ]

    static func named(_ code: String?) -> AppLanguage? {
        guard let code else { return nil }
        return all.first { $0.code == code }
    }

    /// Best match for the device's preferred languages, falling back to English.
    ///
    /// Chinese needs script-level matching (`zh-Hans` vs `zh-Hant`); everything
    /// else matches on the base language code.
    static var deviceDefault: AppLanguage {
        for preferred in Locale.preferredLanguages {
            if preferred.hasPrefix("zh") {
                let isTraditional = preferred.contains("Hant")
                    || preferred.contains("TW")
                    || preferred.contains("HK")
                    || preferred.contains("MO")
                return named(isTraditional ? "zh-Hant" : "zh-Hans") ?? english
            }
            let base = preferred.split(separator: "-").first.map(String.init) ?? preferred
            if let match = all.first(where: { $0.code == base }) { return match }
            // Apple reports Norwegian Bokmål as "nb" but also plain "no".
            if base == "no", let match = named("nb") { return match }
        }
        return english
    }
}
