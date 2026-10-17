package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val MonochromeLightColorScheme = lightColorScheme(
    primary = RoyalBlue,
    onPrimary = Color.White,
    primaryContainer = RoyalBlueSoft,
    onPrimaryContainer = RoyalBlue,
    secondary = ObsidianBlack,
    onSecondary = PaperSurface,
    background = PaperCanvas,
    onBackground = ObsidianBlack,
    surface = PaperSurface,
    onSurface = ObsidianBlack,
    surfaceVariant = PaperSurfaceVariant,
    onSurfaceVariant = SlateGraphite,
    outline = HairlineBorder
)

val MonochromeDarkColorScheme = darkColorScheme(
    primary = RoyalBlueMuted,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = PaperSurface,
    onSecondary = ObsidianBlack,
    background = Color(0xFF121212),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF282828),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF333333)
)

@Composable
fun MonoDoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MonoDoTheme(darkTheme = darkTheme, content = content)
