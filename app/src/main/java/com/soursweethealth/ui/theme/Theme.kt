package com.soursweethealth.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightBlue50 = Color(0xFFE1F5FE)
val LightBlue100 = Color(0xFFB3E5FC)
val LightBlue200 = Color(0xFF81D4FA)
val LightBlue400 = Color(0xFF29B6F6)
val LightBlue600 = Color(0xFF039BE5)
val LightBlue700 = Color(0xFF0288D1)
val White = Color(0xFFFFFFFF)
val LightGray = Color(0xFFF5F5F5)

val HealthGreen = Color(0xFF4CAF50)
val HealthYellow = Color(0xFFFFC107)
val HealthRed = Color(0xFFF44336)

private val LightColorScheme = lightColorScheme(
    primary = LightBlue600,
    onPrimary = White,
    primaryContainer = LightBlue100,
    onPrimaryContainer = Color(0xFF01579B),
    secondary = LightBlue400,
    onSecondary = White,
    background = LightGray,
    onBackground = Color(0xFF1C1B1F),
    surface = White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = LightBlue50,
    outline = Color(0xFFBDBDBD)
)

@Composable
fun SourSweetHealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
