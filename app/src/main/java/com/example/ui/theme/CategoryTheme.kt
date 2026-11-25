package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class CategoryStyle(
    val backgroundColor: Color,
    val textColor: Color,
    val borderColor: Color,
    val accentColor: Color
)

object CategoryTheme {
    private val lightStyles = mapOf(
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
        "Ideas" to CategoryStyle(
            backgroundColor = Color(0xFFF0FDFA),
            textColor = Color(0xFF0F766E),
            borderColor = Color(0xFF99F6E4),
            accentColor = Color(0xFF0D9488)
        ),
        "General" to CategoryStyle(
            backgroundColor = Color(0xFFF3F4F6),
            textColor = Color(0xFF374151),
            borderColor = Color(0xFFE5E7EB),
            accentColor = Color(0xFF6B7280)
        )
    )

    private val darkStyles = mapOf(
        "Work" to CategoryStyle(
            backgroundColor = Color(0xFF172554).copy(alpha = 0.55f),
            textColor = Color(0xFF93C5FD),
            borderColor = Color(0xFF1E3A8A),
            accentColor = Color(0xFF60A5FA)
        ),
        "Personal" to CategoryStyle(
            backgroundColor = Color(0xFF14532D).copy(alpha = 0.45f),
            textColor = Color(0xFF86EFAC),
            borderColor = Color(0xFF166534),
            accentColor = Color(0xFF4ADE80)
        ),
        "Health" to CategoryStyle(
            backgroundColor = Color(0xFF7F1D1D).copy(alpha = 0.45f),
            textColor = Color(0xFFFCA5A5),
            borderColor = Color(0xFF991B1B),
            accentColor = Color(0xFFF87171)
        ),
        "Focus" to CategoryStyle(
            backgroundColor = Color(0xFF78350F).copy(alpha = 0.45f),
            textColor = Color(0xFFFCD34D),
            borderColor = Color(0xFF92400E),
            accentColor = Color(0xFFFBBF24)
        ),
        "Finance" to CategoryStyle(
            backgroundColor = Color(0xFF4C1D95).copy(alpha = 0.45f),
            textColor = Color(0xFFC4B5FD),
            borderColor = Color(0xFF5B21B6),
            accentColor = Color(0xFFA78BFA)
        ),
        "Ideas" to CategoryStyle(
            backgroundColor = Color(0xFF134E4A).copy(alpha = 0.45f),
            textColor = Color(0xFF5EEAD4),
            borderColor = Color(0xFF115E59),
            accentColor = Color(0xFF2DD4BF)
        ),
        "General" to CategoryStyle(
            backgroundColor = Color(0xFF27272A),
            textColor = Color(0xFFD4D4D8),
            borderColor = Color(0xFF3F3F46),
            accentColor = Color(0xFFA1A1AA)
        )
    )

    private val customHexColors = mutableMapOf<String, Color>()

    fun registerCustomStyle(category: String, colorHex: String) {
        try {
            customHexColors[category] = Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            // fallback
        }
    }

    fun getStyle(category: String, isDark: Boolean = false): CategoryStyle {
        val map = if (isDark) darkStyles else lightStyles
        map[category]?.let { return it }

        customHexColors[category]?.let { baseColor ->
            return if (isDark) {
                CategoryStyle(
                    backgroundColor = baseColor.copy(alpha = 0.18f),
                    textColor = baseColor.copy(alpha = 0.95f),
                    borderColor = baseColor.copy(alpha = 0.4f),
                    accentColor = baseColor
                )
            } else {
                CategoryStyle(
                    backgroundColor = baseColor.copy(alpha = 0.12f),
                    textColor = baseColor,
                    borderColor = baseColor.copy(alpha = 0.35f),
                    accentColor = baseColor
                )
            }
        }

        val hash = category.hashCode().let { if (it < 0) -it else it }
        val keys = listOf("Work", "Personal", "Health", "Focus", "Finance")
        return map[keys[hash % keys.size]]!!
    }
}
