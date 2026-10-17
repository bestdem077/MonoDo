package com.example.data.model

import androidx.compose.ui.graphics.Color
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
}
