package com.soursweethealth.ui.theme

import androidx.compose.ui.graphics.Color

// 品牌色统一从 BrandTokens.kt 读取，唯一来源是 docs/brand/brand-vi.md；
// 本文件只做界面层语义命名映射与派生色调，界面代码不得再写死色值。

// ---------------------------------------------------------------------------
// 中性色阶（近黑 / 近白，承担约 95% 界面）
// ---------------------------------------------------------------------------
val NeutralText = BrandColors.textPrimaryLight
val NeutralTextSecondary = BrandColors.textSecondaryLight
val NeutralBackground = BrandColors.backgroundLight
val NeutralSurface = BrandColors.surfaceLight
val NeutralSurfaceVariant = Color(0xFFF0EFEC)   // 选中/弱强调底色
val NeutralOutline = BrandColors.outlineLight
val NeutralOutlineVariant = Color(0xFFECEAE6)   // 细分隔线
val OnAccent = BrandColors.onAccentLight

val NeutralTextDark = BrandColors.textPrimaryDark
val NeutralTextSecondaryDark = BrandColors.textSecondaryDark
val NeutralBackgroundDark = BrandColors.backgroundDark
val NeutralSurfaceDark = BrandColors.surfaceDark
val NeutralSurfaceVariantDark = Color(0xFF2A2E33)
val NeutralOutlineDark = BrandColors.outlineDark
val NeutralOutlineVariantDark = Color(0xFF2A2D31)
val OnAccentDark = BrandColors.onAccentDark

// ---------------------------------------------------------------------------
// 唯一强调色（近黑）及其派生容器色
// ---------------------------------------------------------------------------
val Accent = BrandColors.accentLight
val AccentContainer = BrandColors.accentContainerLight
val OnAccentContainer = BrandColors.onAccentContainerLight
val AccentSecondary = BrandColors.textSecondaryLight
val AccentSecondaryContainer = BrandColors.accentContainerLight

val AccentDark = BrandColors.accentDark
val AccentContainerDark = BrandColors.accentContainerDark
val OnAccentContainerDark = BrandColors.onAccentContainerDark
val AccentSecondaryDark = BrandColors.textSecondaryDark
val AccentSecondaryContainerDark = BrandColors.accentContainerDark

// ---------------------------------------------------------------------------
// 语义色（全 App 唯一允许的彩色，仅健康状态与错误态使用）
// ---------------------------------------------------------------------------
val HealthGreen = BrandColors.successLight
val HealthYellow = BrandColors.warningLight
val HealthOrange = BrandColors.warningAltLight
val HealthRed = BrandColors.errorLight
val HealthErrorContainer = Color(0xFFF4E4E0)

val HealthGreenDark = BrandColors.successDark
val HealthYellowDark = BrandColors.warningDark
val HealthOrangeDark = BrandColors.warningAltDark
val HealthRedDark = BrandColors.errorDark
val HealthErrorContainerDark = Color(0xFF5C332C)