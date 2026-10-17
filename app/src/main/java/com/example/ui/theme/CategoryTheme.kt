package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class CategoryStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val accentColor: Color
)

object CategoryTheme {
    private val styles = mapOf(
        "Work" to CategoryStyle(
            backgroundColor = Color(0xFFEFF6FF),
            textColor = Color(0xFF1D4ED8),
            borderColor = Color(0xFFBFDBFE),
            accentColor = Color(0xFF2563EB)
        ),
        "Personal" to CategoryStyle(
            backgroundColor = Color(0xFFF0FDF4),
            textColor = Color(0xFF15803D),
            borderColor = Color(0xFFBBF7D0),
            accentColor = Color(0xFF16A34A)
        ),
        "Health" to CategoryStyle(
            backgroundColor = Color(0xFFFEF2F2),
            textColor = Color(0xFFB91C1C),
            borderColor = Color(0xFFFECACA),
            accentColor = Color(0xFFDC2626)
        ),
        "Focus" to CategoryStyle(
            backgroundColor = Color(0xFFFFFBEB),
            textColor = Color(0xFFB45309),
            borderColor = Color(0xFFFDE68A),
            accentColor = Color(0xFFD97706)
        ),
        "Finance" to CategoryStyle(
            backgroundColor = Color(0xFFF5F3FF),
            textColor = Color(0xFF6D28D9),
            borderColor = Color(0xFFDDD6FE),
            accentColor = Color(0xFF7C3AED)
        ),
        "General" to CategoryStyle(
            backgroundColor = Color(0xFFF3F4F6),
            textColor = Color(0xFF374151),
            borderColor = Color(0xFFE5E7EB),
            accentColor = Color(0xFF6B7280)
        )
    )

    private val customStyles = mutableMapOf<String, CategoryStyle>()

    fun registerCustomStyle(category: String, colorHex: String) {
        try {
            val baseColor = Color(android.graphics.Color.parseColor(colorHex))
            val style = CategoryStyle(
                backgroundColor = baseColor.copy(alpha = 0.12f),
                textColor = baseColor,
                borderColor = baseColor.copy(alpha = 0.35f),
                accentColor = baseColor
            )
            customStyles[category] = style
        } catch (e: Exception) {
            // fallback
        }
    }

    fun getStyle(category: String): CategoryStyle {
        return styles[category] ?: customStyles[category] ?: run {
            // Deterministically generate a clean subtle pastel theme for custom categories
            val hash = category.hashCode().let { if (it < 0) -it else it }
            val hues = listOf(
                CategoryStyle(Color(0xFFEFF6FF), Color(0xFF1D4ED8), Color(0xFFBFDBFE), Color(0xFF2563EB)),
                CategoryStyle(Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFFBBF7D0), Color(0xFF16A34A)),
                CategoryStyle(Color(0xFFFEF2F2), Color(0xFFB91C1C), Color(0xFFFECACA), Color(0xFFDC2626)),
                CategoryStyle(Color(0xFFFFFBEB), Color(0xFFB45309), Color(0xFFFDE68A), Color(0xFFD97706)),
                CategoryStyle(Color(0xFFF5F3FF), Color(0xFF6D28D9), Color(0xFFDDD6FE), Color(0xFF7C3AED))
            )
            hues[hash % hues.size]
        }
    }
}
