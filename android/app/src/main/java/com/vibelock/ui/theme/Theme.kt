package com.vibelock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VibePurple,
    onPrimary = Color.Black,
    secondary = VibePink,
    onSecondary = Color.Black,
    tertiary = VibeBlue,
    background = VibeSurface,
    surface = VibeCard,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = VibeBorder,
)

@Composable
fun VibeLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
