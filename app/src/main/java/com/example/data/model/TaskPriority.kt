package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.MonoDoColors
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PriorityHighBackground
import com.example.ui.theme.PriorityHighIcon
import com.example.ui.theme.PriorityLowBackground
import com.example.ui.theme.PriorityLowIcon
import com.example.ui.theme.PriorityMediumBackground
import com.example.ui.theme.PriorityMediumIcon
import com.example.ui.theme.SlateGraphite

enum class TaskPriority(
    val label: String,
    val level: Int,
    val indicatorSymbol: String
) {
    LOW(label = "Low", level = 1, indicatorSymbol = "•"),
    MEDIUM(label = "Medium", level = 2, indicatorSymbol = "••"),
    HIGH(label = "High", level = 3, indicatorSymbol = "•••");

    val monochromeIndicatorColor: Color
        get() = when (this) {
            LOW -> PriorityLowIcon
            MEDIUM -> PriorityMediumIcon
            HIGH -> PriorityHighIcon
        }

    val monochromeBackgroundColor: Color
        get() = when (this) {
            LOW -> PriorityLowBackground
            MEDIUM -> PriorityMediumBackground
            HIGH -> PriorityHighBackground
        }

    val monochromeTextColor: Color
        get() = when (this) {
            LOW -> SlateGraphite
            MEDIUM -> ObsidianBlack
            HIGH -> Color.White
        }

    fun indicatorColor(colors: MonoDoColors): Color = if (colors.isDark) {
        when (this) {
            LOW -> Color(0xFF6B7280)
            MEDIUM -> Color(0xFFCBD5E1)
            HIGH -> Color(0xFFFAFAFA)
        }
    } else {
        monochromeIndicatorColor
    }

    fun badgeBackground(colors: MonoDoColors): Color = if (colors.isDark) {
        when (this) {
            LOW -> colors.tileBg
            MEDIUM -> colors.surfaceVariant
            HIGH -> colors.primaryInk
        }
    } else {
        monochromeBackgroundColor
    }

    fun badgeTextColor(colors: MonoDoColors): Color = if (colors.isDark) {
        when (this) {
            LOW -> colors.secondaryInk
            MEDIUM -> colors.primaryInk
            HIGH -> colors.canvas
        }
    } else {
        monochromeTextColor
    }
}
