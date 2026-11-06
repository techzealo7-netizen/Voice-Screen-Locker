package com.voicelocker.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.voicelocker.app.data.local.Store

private val LightScheme = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Color(0xFFF4F6FB),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    primaryContainer = Color(0xFFC5E1FB),
    onPrimaryContainer = Blue,
    surfaceVariant = Color(0xFFE8F2FF),
    onSurfaceVariant = Color(0xFF8B93A7),
    error = Color(0xFFFF4B4B),
    outline = Color(0xFFE3E7EF),
)

private val DarkScheme = darkColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    background = Color(0xFF0E1320),
    onBackground = Color(0xFFEEF2F8),
    surface = Color(0xFF192033),
    onSurface = Color(0xFFEEF2F8),
    primaryContainer = Color(0xFF1D3A60),
    onPrimaryContainer = Color(0xFF8CC4FF),
    surfaceVariant = Color(0xFF232C44),
    onSurfaceVariant = Color(0xFF8A94AA),
    error = Color(0xFFFF6B6B),
    outline = Color(0xFF2A3550),
)

@Composable
fun VoiceLockTheme(content: @Composable () -> Unit) {
    val dark = when (Store.themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    SideEffect {
        com.voicelocker.ads.ThemeManager.setDarkMode(dark)
        (view.context as? Activity)?.window?.let {
            val c = WindowCompat.getInsetsController(it, view)
            c.isAppearanceLightStatusBars = !dark
            c.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content
    )
}

