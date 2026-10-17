package com.soursweethealth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 健康水平语义色（随深浅色切换），图表与卡片徽标使用。 */
data class HealthLevelColors(
    val normal: Color,
    val high: Color,
    val low: Color,
    val critical: Color
)

private val LightHealthLevels = HealthLevelColors(
    normal = HealthGreen,
    high = HealthYellow,
    low = HealthOrange,
    critical = HealthRed
)

private val DarkHealthLevels = HealthLevelColors(
    normal = HealthGreenDark,
    high = HealthYellowDark,
    low = HealthOrangeDark,
    critical = HealthRedDark
)

val LocalHealthLevels = staticCompositionLocalOf { LightHealthLevels }

/** 图表配色：折线用加深主色，网格用最浅分隔线。 */
data class ChartColors(
    val line: Color,
    val grid: Color
)

private val LightChartColors = ChartColors(
    line = BrandColors.accentStrongLight,
    grid = NeutralOutlineVariant
)

private val DarkChartColors = ChartColors(
    line = BrandColors.accentStrongDark,
    grid = NeutralOutlineVariantDark
)

val LocalChartColors = staticCompositionLocalOf { LightChartColors }

private val LightColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = OnAccentContainer,
    secondary = AccentSecondary,
    onSecondary = OnAccent,
    secondaryContainer = AccentSecondaryContainer,
    onSecondaryContainer = NeutralText,
    background = NeutralBackground,
    onBackground = NeutralText,
    surface = NeutralSurface,
    onSurface = NeutralText,
    surfaceVariant = NeutralSurfaceVariant,
    onSurfaceVariant = NeutralTextSecondary,
    outline = NeutralOutline,
    outlineVariant = NeutralOutlineVariant,
    error = HealthRed,
    onError = OnAccent,
    errorContainer = HealthErrorContainer,
    onErrorContainer = NeutralText
)

private val DarkColorScheme = darkColorScheme(
    primary = AccentDark,
    onPrimary = OnAccentDark,
    primaryContainer = AccentContainerDark,
    onPrimaryContainer = OnAccentContainerDark,
    secondary = AccentSecondaryDark,
    onSecondary = OnAccentDark,
    secondaryContainer = AccentSecondaryContainerDark,
    onSecondaryContainer = NeutralTextDark,
    background = NeutralBackgroundDark,
    onBackground = NeutralTextDark,
    surface = NeutralSurfaceDark,
    onSurface = NeutralTextDark,
    surfaceVariant = NeutralSurfaceVariantDark,
    onSurfaceVariant = NeutralTextSecondaryDark,
    outline = NeutralOutlineDark,
    outlineVariant = NeutralOutlineVariantDark,
    error = HealthRedDark,
    onError = OnAccentDark,
    errorContainer = HealthErrorContainerDark,
    onErrorContainer = NeutralTextDark
)

@Composable
fun SourSweetHealthTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalHealthLevels provides if (darkTheme) DarkHealthLevels else LightHealthLevels,
        LocalChartColors provides if (darkTheme) DarkChartColors else LightChartColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content
        )
    }
}
