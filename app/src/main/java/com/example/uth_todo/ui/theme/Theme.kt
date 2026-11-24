package com.example.uth_todo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.uth_todo.model.ThemeMode

private val UthLightColorScheme = lightColorScheme(
    primary = UthBlue,
    onPrimary = Color.White,
    primaryContainer = UthLavender,
    onPrimaryContainer = UthBlue,
    secondary = Color(0xFF276B59),
    tertiary = Color(0xFF986230),
    background = UthBackground,
    onBackground = UthInk,
    surface = Color.White,
    onSurface = UthInk,
    onSurfaceVariant = UthMuted,
    surfaceVariant = Color(0xFFE7EBF6),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = UthBackground,
    surfaceContainer = Color(0xFFF1F3FC),
    surfaceContainerHigh = UthLavender,
    surfaceContainerHighest = Color(0xFFE7EBF6),
    surfaceTint = UthBlue,
    outline = Color(0xFFE0E5F0),
)

private val UthDarkColorScheme = darkColorScheme(
    primary = Color(0xFFBAC3FF),
    onPrimary = Color(0xFF17266E),
    primaryContainer = Color(0xFF2D3D88),
    onPrimaryContainer = Color(0xFFDEE2FF),
    secondary = Color(0xFF8CD6BC),
    onSecondary = Color(0xFF00382B),
    tertiary = Color(0xFFF0BE8C),
    onTertiary = Color(0xFF4A2805),
    background = Color(0xFF10131E),
    onBackground = Color(0xFFE3E6F4),
    surface = Color(0xFF191D2B),
    onSurface = Color(0xFFE3E6F4),
    onSurfaceVariant = Color(0xFFBFC5DA),
    surfaceVariant = Color(0xFF343949),
    surfaceContainerLowest = Color(0xFF0C0F18),
    surfaceContainerLow = Color(0xFF191D2B),
    surfaceContainer = Color(0xFF202534),
    surfaceContainerHigh = Color(0xFF2A3040),
    surfaceContainerHighest = Color(0xFF343949),
    surfaceTint = Color(0xFFBAC3FF),
    outline = Color(0xFF8D94AC),
    outlineVariant = Color(0xFF42495D),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun UTHTODOTheme(mode: ThemeMode = ThemeMode.SYSTEM, systemDark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (mode.usesDarkColors(systemDark)) UthDarkColorScheme else UthLightColorScheme,
        typography = Typography, content = content)
}
