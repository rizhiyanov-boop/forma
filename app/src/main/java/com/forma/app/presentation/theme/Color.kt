package com.forma.app.presentation.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// База
val BgBlack = Color(0xFF0A0A0B)
val SurfaceDark = Color(0xFF141416)
val SurfaceElevated = Color(0xFF1C1C1F)
val SurfaceHighlight = Color(0xFF26262A)

// Текст
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFFA1A1AA)
val TextTertiary = Color(0xFF6B6B70)
val TextDim = Color(0xFF4B4B50)

// Акцент — лаймовый градиент (свежо, не банально)
val AccentLime = Color(0xFFD4FF3A)
val AccentMint = Color(0xFF7AFFBD)
val AccentDark = Color(0xFF0F1F0A)
val AccentDim = Color(0xFF5A7A1C)

// Семантика
val SuccessGreen = Color(0xFF4ADE80)
val WarnAmber = Color(0xFFFBBF24)
val ErrorRed = Color(0xFFF87171)
val InfoBlue = Color(0xFF60A5FA)

// Бордеры
val BorderSubtle = Color(0xFF26262A)
val BorderFocused = Color(0xFF3F3F46)

val AccentGradient: Brush
    get() = Brush.linearGradient(colors = listOf(AccentLime, AccentMint))
