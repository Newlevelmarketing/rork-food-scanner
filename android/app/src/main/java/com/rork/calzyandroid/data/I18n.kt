package com.rork.calzyandroid.data

import android.content.Context
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A language the app can present itself in. Mirrors `web/src/lib/i18n.ts`. */
data class AppLanguage(
    val code: String,
    /** Name in English, so searching "Spanish" finds Español. */
    val englishName: String,
    /** Endonym — what speakers call the language themselves. */
    val nativeName: String,
    val flag: String,
    val isRTL: Boolean = false,
)

object I18n {

    /** Ordered roughly by global reach, English first. */
    val languages: List<AppLanguage> = listOf(
        AppLanguage("en", "English", "English", "\uD83C\uDDFA\uD83C\uDDF8"),
        AppLanguage("zh-Hans", "Chinese (Simplified)", "简体中文", "\uD83C\uDDE8\uD83C\uDDF3"),
        AppLanguage("zh-Hant", "Chinese (Traditional)", "繁體中文", "\uD83C\uDDF9\uD83C\uDDFC"),
        AppLanguage("es", "Spanish", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
        AppLanguage("hi", "Hindi", "हिन्दी", "\uD83C\uDDEE\uD83C\uDDF3"),
        AppLanguage("ar", "Arabic", "العربية", "\uD83C\uDDF8\uD83C\uDDE6", isRTL = true),
        AppLanguage("pt", "Portuguese", "Português", "\uD83C\uDDE7\uD83C\uDDF7"),
        AppLanguage("bn", "Bengali", "বাংলা", "\uD83C\uDDE7\uD83C\uDDE9"),
        AppLanguage("ru", "Russian", "Русский", "\uD83C\uDDF7\uD83C\uDDFA"),
        AppLanguage("ja", "Japanese", "日本語", "\uD83C\uDDEF\uD83C\uDDF5"),
        AppLanguage("de", "German", "Deutsch", "\uD83C\uDDE9\uD83C\uDDEA"),
        AppLanguage("fr", "French", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
        AppLanguage("ko", "Korean", "한국어", "\uD83C\uDDF0\uD83C\uDDF7"),
        AppLanguage("it", "Italian", "Italiano", "\uD83C\uDDEE\uD83C\uDDF9"),
        AppLanguage("tr", "Turkish", "Türkçe", "\uD83C\uDDF9\uD83C\uDDF7"),
        AppLanguage("vi", "Vietnamese", "Tiếng Việt", "\uD83C\uDDFB\uD83C\uDDF3"),
        AppLanguage("id", "Indonesian", "Bahasa Indonesia", "\uD83C\uDDEE\uD83C\uDDE9"),
        AppLanguage("ms", "Malay", "Bahasa Melayu", "\uD83C\uDDF2\uD83C\uDDFE"),
        AppLanguage("th", "Thai", "ไทย", "\uD83C\uDDF9\uD83C\uDDED"),
        AppLanguage("pl", "Polish", "Polski", "\uD83C\uDDF5\uD83C\uDDF1"),
        AppLanguage("nl", "Dutch", "Nederlands", "\uD83C\uDDF3\uD83C\uDDF1"),
        AppLanguage("uk", "Ukrainian", "Українська", "\uD83C\uDDFA\uD83C\uDDE6"),
        AppLanguage("fa", "Persian", "فارسی", "\uD83C\uDDEE\uD83C\uDDF7", isRTL = true),
        AppLanguage("he", "Hebrew", "עברית", "\uD83C\uDDEE\uD83C\uDDF1", isRTL = true),
        AppLanguage("el", "Greek", "Ελληνικά", "\uD83C\uDDEC\uD83C\uDDF7"),
        AppLanguage("cs", "Czech", "Čeština", "\uD83C\uDDE8\uD83C\uDDFF"),
        AppLanguage("ro", "Romanian", "Română", "\uD83C\uDDF7\uD83C\uDDF4"),
        AppLanguage("hu", "Hungarian", "Magyar", "\uD83C\uDDED\uD83C\uDDFA"),
        AppLanguage("sv", "Swedish", "Svenska", "\uD83C\uDDF8\uD83C\uDDEA"),
        AppLanguage("nb", "Norwegian", "Norsk", "\uD83C\uDDF3\uD83C\uDDF4"),
        AppLanguage("da", "Danish", "Dansk", "\uD83C\uDDE9\uD83C\uDDF0"),
        AppLanguage("fi", "Finnish", "Suomi", "\uD83C\uDDEB\uD83C\uDDEE"),
    )

    private val english: AppLanguage = languages.first()

    private var tables: Map<String, Map<String, String>> = emptyMap()

    /** Loads the shared 32-language string catalogue from assets/strings.json. */
    fun load(context: Context) {
        if (tables.isNotEmpty()) return
        tables = try {
            val raw = context.assets.open("strings.json").bufferedReader().use { it.readText() }
            val root = Json.parseToJsonElement(raw).jsonObject
            root.entries.associate { (code, table) ->
                code to (table as JsonObject).entries.associate { (key, value) ->
                    key to value.jsonPrimitive.content
                }
            }
        } catch (error: Exception) {
            emptyMap()
        }
    }

    fun languageFor(code: String?): AppLanguage? =
        code?.let { c -> languages.find { it.code == c } }

    /** Best match for the device locale, falling back to English. */
    fun systemLanguage(): AppLanguage {
        val locale = Locale.getDefault()
        val tag = locale.toLanguageTag()
        if (tag.startsWith("zh")) {
            val traditional = Regex("Hant|TW|HK|MO", RegexOption.IGNORE_CASE).containsMatchIn(tag)
            return languageFor(if (traditional) "zh-Hant" else "zh-Hans") ?: english
        }
        val base = tag.split("-").first()
        languages.find { it.code == base }?.let { return it }
        if (base == "no") return languageFor("nb") ?: english
        return english
    }

    /**
     * Looks up [key] in [code], falling back to English and then to the key
     * itself so a missing entry is visible but never crashes.
     */
    fun translate(code: String, key: String): String =
        tables[code]?.get(key) ?: tables["en"]?.get(key) ?: key
}
