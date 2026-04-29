package com.forma.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Используем системные шрифты — плотные, современные.
 * SansSerif и Monospace дают хорошую читаемость и не требуют ассетов.
 */
private val DisplayFont = FontFamily.SansSerif
val NumericFont = FontFamily.Monospace

val FormaTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 56.sp, letterSpacing = (-1).sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 42.sp, letterSpacing = (-0.5).sp, lineHeight = 48.sp),
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = (-0.25).sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    headlineSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.5.sp),
)
