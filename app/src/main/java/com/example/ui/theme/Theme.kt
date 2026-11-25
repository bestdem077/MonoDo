package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.user.AppThemeMode

@Immutable
data class MonoDoColors(
    val canvas: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primaryInk: Color,
    val secondaryInk: Color,
    val mutedInk: Color,
    val border: Color,
    val softBorder: Color,
    val focusedInput: Color,
    val tileBg: Color,
    val accent: Color,
    val accentHover: Color,
    val accentSoft: Color,
    val accentBorder: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerBorder: Color,
    val success: Color,
    val successSoft: Color,
    val successBorder: Color,
    val cardHighlight: Color,
    val isDark: Boolean
)

val LightPaperColors = MonoDoColors(
    canvas = Color(0xFFFAF9F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F1ED),
    primaryInk = Color(0xFF111111),
    secondaryInk = Color(0xFF6B6B6B),
    mutedInk = Color(0xFF9E9E9E),
    border = Color(0xFFE5E5DF),
    softBorder = Color(0xFFDCDCD7),
    focusedInput = Color(0xFFF5F5F0),
    tileBg = Color(0xFFF4F3EF),
    accent = Color(0xFF2563EB),
    accentHover = Color(0xFF1D4ED8),
    accentSoft = Color(0xFFEFF6FF),
    accentBorder = Color(0xFFBFDBFE),
    danger = Color(0xFFDC2626),
    dangerSoft = Color(0xFFFEF2F2),
    dangerBorder = Color(0xFFFECACA),
    success = Color(0xFF15803D),
    successSoft = Color(0xFFF0FDF4),
    successBorder = Color(0xFFBBF7D0),
    cardHighlight = Color(0xFFF4F6FA),
    isDark = false
)

val DarkSlateColors = MonoDoColors(
    canvas = Color(0xFF121316),
    surface = Color(0xFF1B1D22),
    surfaceVariant = Color(0xFF24272E),
    primaryInk = Color(0xFFF4F4F0),
    secondaryInk = Color(0xFF9CA3AF),
    mutedInk = Color(0xFF6B7280),
    border = Color(0xFF2C3039),
    softBorder = Color(0xFF363B47),
    focusedInput = Color(0xFF22252C),
    tileBg = Color(0xFF252830),
    accent = Color(0xFF3B82F6),
    accentHover = Color(0xFF60A5FA),
    accentSoft = Color(0xFF172554),
    accentBorder = Color(0xFF1E3A8A),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0xFF3B1518),
    dangerBorder = Color(0xFF7F1D1D),
    success = Color(0xFF4ADE80),
    successSoft = Color(0xFF112A1C),
    successBorder = Color(0xFF166534),
    cardHighlight = Color(0xFF232730),
    isDark = true
)

val AmoledBlackColors = MonoDoColors(
    canvas = Color(0xFF000000),
    surface = Color(0xFF0E0F12),
    surfaceVariant = Color(0xFF181A1F),
    primaryInk = Color(0xFFFAFAFA),
    secondaryInk = Color(0xFF9398A2),
    mutedInk = Color(0xFF636873),
    border = Color(0xFF22252B),
    softBorder = Color(0xFF2D313A),
    focusedInput = Color(0xFF14161B),
    tileBg = Color(0xFF181A1F),
    accent = Color(0xFF3B82F6),
    accentHover = Color(0xFF60A5FA),
    accentSoft = Color(0xFF0F1D3A),
    accentBorder = Color(0xFF1E3A8A),
    danger = Color(0xFFF87171),
    dangerSoft = Color(0xFF2C1013),
    dangerBorder = Color(0xFF7F1D1D),
    success = Color(0xFF4ADE80),
    successSoft = Color(0xFF0B2216),
    successBorder = Color(0xFF166534),
    cardHighlight = Color(0xFF16181E),
    isDark = true
)

val LocalMonoDoColors = staticCompositionLocalOf { LightPaperColors }

object MonoDoThemeTokens {
    val colors: MonoDoColors
        @Composable
        get() = LocalMonoDoColors.current
}

private fun MonoDoColors.toMaterialColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accentSoft,
        onPrimaryContainer = accentHover,
        secondary = primaryInk,
        onSecondary = canvas,
        background = canvas,
        onBackground = primaryInk,
        surface = surface,
        onSurface = primaryInk,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = secondaryInk,
        outline = border,
        error = danger,
        onError = Color.White
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = Color.White,
        primaryContainer = accentSoft,
        onPrimaryContainer = accent,
        secondary = primaryInk,
        onSecondary = surface,
        background = canvas,
        onBackground = primaryInk,
        surface = surface,
        onSurface = primaryInk,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = secondaryInk,
        outline = border,
        error = danger,
        onError = Color.White
    )
}

@Composable
fun MonoDoTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT_PAPER,
    content: @Composable () -> Unit
) {
    val palette = when (themeMode) {
        AppThemeMode.LIGHT_PAPER -> LightPaperColors
        AppThemeMode.DARK_SLATE -> DarkSlateColors
        AppThemeMode.AMOLED_BLACK -> AmoledBlackColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !palette.isDark
                controller.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalMonoDoColors provides palette) {
        MaterialTheme(
            colorScheme = palette.toMaterialColorScheme(),
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = MonoDoTheme(
    themeMode = if (darkTheme) AppThemeMode.DARK_SLATE else AppThemeMode.LIGHT_PAPER,
    content = content
)
