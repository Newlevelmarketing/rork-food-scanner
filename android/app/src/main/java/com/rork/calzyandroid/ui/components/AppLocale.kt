package com.rork.calzyandroid.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.rork.calzyandroid.data.AppLanguage
import com.rork.calzyandroid.data.I18n

val LocalAppLanguage = staticCompositionLocalOf<AppLanguage> { I18n.languages.first() }

/** Translator bound to the active language — usage: `val t = LocalT.current`. */
val LocalT = staticCompositionLocalOf<(String) -> String> { { key -> key } }

/**
 * Provides the active language, its translator and the matching layout
 * direction (RTL for Arabic, Hebrew, Persian) to the whole tree.
 */
@Composable
fun ProvideAppLanguage(languageCode: String?, content: @Composable () -> Unit) {
    val language = I18n.languageFor(languageCode) ?: I18n.systemLanguage()
    val translate: (String) -> String = { key -> I18n.translate(language.code, key) }
    CompositionLocalProvider(
        LocalAppLanguage provides language,
        LocalT provides translate,
        LocalLayoutDirection provides if (language.isRTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        },
        content = content,
    )
}
