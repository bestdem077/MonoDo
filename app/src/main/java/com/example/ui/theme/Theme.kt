package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = WhiteText,
    onPrimary = DarkCanvas,
    primaryContainer = DarkSurfaceVariant,
    onPrimaryContainer = WhiteText,
    secondary = DarkMuted,
    onSecondary = WhiteText,
    background = DarkCanvas,
    onBackground = WhiteText,
    surface = DarkSurface,
    onSurface = WhiteText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

private val MonochromeLightColorScheme = lightColorScheme(
    primary = ObsidianBlack,
    onPrimary = PaperSurface,
    primaryContainer = PaperSurfaceVariant,
    onPrimaryContainer = ObsidianBlack,
    secondary = SlateGraphite,
    onSecondary = PaperSurface,
    background = PaperCanvas,
    onBackground = ObsidianBlack,
    surface = PaperSurface,
    onSurface = ObsidianBlack,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = SlateGraphite,
    outline = HairlineBorder,
    outlineVariant = HairlineBorder
)

@Composable
fun MonoDoTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Pure paper light theme locked for consistent high contrast as confirmed by user
    val colorScheme = MonochromeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
