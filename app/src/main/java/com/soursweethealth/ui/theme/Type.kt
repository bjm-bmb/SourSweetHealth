package com.soursweethealth.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 6 级字阶：32/22/17/15/13/11sp，标题用 Medium，全 App 字重仅 Medium/Normal
val AppTypography = Typography(
    displaySmall = TextStyle(  // 大数字/展示
        fontSize = BrandTypeScale.display,
        fontWeight = FontWeight.Medium,
        lineHeight = 40.sp
    ),
    headlineSmall = TextStyle( // 页面标题
        fontSize = BrandTypeScale.pageTitle,
        fontWeight = FontWeight.Medium,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(   // 区块标题
        fontSize = BrandTypeScale.sectionTitle,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(     // 正文
        fontSize = BrandTypeScale.body,
        fontWeight = FontWeight.Normal,
        lineHeight = 24.sp
    ),
    bodySmall = TextStyle(     // 辅助说明
        fontSize = BrandTypeScale.caption,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(    // 标签/角标
        fontSize = BrandTypeScale.label,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )
)