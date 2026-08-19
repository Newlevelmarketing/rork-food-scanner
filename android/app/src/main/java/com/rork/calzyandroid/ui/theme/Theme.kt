package com.rork.calzyandroid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.rork.calzyandroid.R

/**
 * Calzy design system — mirrors `web/src/index.css` tokens and
 * `ios-calzy/.../Theme.swift`. Light-only, soft pastel mist over off-white.
 */
object CalzyColors {
    val background = Color(0xFFF6F5F8)
    val ink = Color(0xFF0B0B0C)
    val inkSoft = Color(0xFF6B6B72)
    val inkFaint = Color(0xFFA8A8AF)
    val well = Color(0xFFF2F2F5)
    val flame = Color(0xFFFF6B2C)
    val water = Color(0xFF399FFF)
    val protein = Color(0xFFFF5A6D)
    val carbs = Color(0xFF4C8EFF)
    val fat = Color(0xFFF5A524)
    val mint = Color(0xFF2FBF70)
    val plum = Color(0xFFAF6AF0)

    val mistLavender = Color(0xFFE6DCF7)
    val mistPeach = Color(0xFFFFE2E0)
    val mistSky = Color(0xFFDCE9FF)
    val mistApricot = Color(0xFFFFEADC)
    val mistSage = Color(0xFFDDF1E9)

    val cardFill = Color.White.copy(alpha = 0.78f)
    val cardBorder = Color.White.copy(alpha = 0.65f)
}

/** Rounded numeric font used for all metrics — Nunito, like the web `.metric` class. */
val MetricFontFamily: FontFamily = FontFamily(
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

private val LightColors = lightColorScheme(
    primary = CalzyColors.ink,
    onPrimary = Color.White,
    secondary = CalzyColors.flame,
    onSecondary = Color.White,
    background = CalzyColors.background,
    onBackground = CalzyColors.ink,
    surface = Color.White,
    onSurface = CalzyColors.ink,
    surfaceVariant = CalzyColors.well,
    onSurfaceVariant = CalzyColors.inkSoft,
    outline = CalzyColors.inkFaint,
    error = CalzyColors.protein,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    // The Calzy palette is intentionally light-only on every platform.
    isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
