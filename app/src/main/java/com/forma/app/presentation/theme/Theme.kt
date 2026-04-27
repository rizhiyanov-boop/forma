package com.forma.app.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val FormaDarkColors = darkColorScheme(
    primary = AccentLime,
    onPrimary = AccentDark,
    primaryContainer = AccentDim,
    onPrimaryContainer = AccentLime,
    secondary = AccentMint,
    onSecondary = AccentDark,
    background = BgBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    outlineVariant = BorderFocused,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun FormaTheme(
    darkTheme: Boolean = true,   // приложение всегда в тёмной теме
    content: @Composable () -> Unit
) {
    val colorScheme = FormaDarkColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgBlack.toArgb()
            window.navigationBarColor = BgBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = FormaTypography,
        content = content
    )
}
