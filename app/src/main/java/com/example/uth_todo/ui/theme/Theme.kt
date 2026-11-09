package com.example.uth_todo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val UthColorScheme = lightColorScheme(
    primary = UthBlue,
    onPrimary = Color.White,
    primaryContainer = UthLavender,
    onPrimaryContainer = UthBlue,
    background = UthBackground,
    onBackground = UthInk,
    surface = Color.White,
    onSurface = UthInk,
    onSurfaceVariant = UthMuted,
    outline = Color(0xFFE0E5F0),
)

// Keep the light reference design and brand colors consistent across devices.
@Composable
fun UTHTODOTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = UthColorScheme, typography = Typography, content = content)
}
