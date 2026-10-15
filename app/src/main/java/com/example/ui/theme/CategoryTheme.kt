package com.example.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs

/**
 * Visual styling definition for a task category.
 * Includes distinct accent color, light pastel background tint,
 * border color, text color, and semantic icon.
 */
data class CategoryStyle(
    val name: String,
    val accentColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val textColor: Color,
    val icon: ImageVector
)

object CategoryTheme {

    // Predefined core categories requested by users
    val Work = CategoryStyle(
        name = "Work",
        accentColor = Color(0xFF2563EB),       // Electric Cobalt Blue
        backgroundColor = Color(0xFFEFF6FF),   // Soft Blue Tint
        borderColor = Color(0xFFBFDBFE),       // Pastel Blue Border
        textColor = Color(0xFF1D4ED8),         // Deep Contrast Blue
        icon = Icons.Default.WorkOutline
    )

    val Personal = CategoryStyle(
        name = "Personal",
        accentColor = Color(0xFF059669),       // Vibrant Emerald Green
        backgroundColor = Color(0xFFECFDF5),   // Soft Mint Tint
        borderColor = Color(0xFFA7F3D0),       // Pastel Emerald Border
        textColor = Color(0xFF047857),         // Deep Contrast Green
        icon = Icons.Default.PersonOutline
    )

    val Health = CategoryStyle(
        name = "Health",
        accentColor = Color(0xFFE11D48),       // Vibrant Rose/Coral Red
        backgroundColor = Color(0xFFFFF1F2),   // Soft Rose Tint
        borderColor = Color(0xFFFECDD3),       // Pastel Rose Border
        textColor = Color(0xFFBE123C),         // Deep Contrast Coral
        icon = Icons.Default.FavoriteBorder
    )

    val Focus = CategoryStyle(
        name = "Focus",
        accentColor = Color(0xFFD97706),       // Warm Amber Gold
        backgroundColor = Color(0xFFFFFBEB),   // Soft Amber Tint
        borderColor = Color(0xFFFDE68A),       // Pastel Amber Border
        textColor = Color(0xFFB45309),         // Deep Contrast Amber
        icon = Icons.Default.Bolt
    )

    val Ideas = CategoryStyle(
        name = "Ideas",
        accentColor = Color(0xFF7C3AED),       // Creative Violet
        backgroundColor = Color(0xFFF5F3FF),   // Soft Purple Tint
        borderColor = Color(0xFFDDD6FE),       // Pastel Purple Border
        textColor = Color(0xFF6D28D9),         // Deep Contrast Violet
        icon = Icons.Default.Lightbulb
    )

    val Finance = CategoryStyle(
        name = "Finance",
        accentColor = Color(0xFF0D9488),       // Modern Teal
        backgroundColor = Color(0xFFF0FDFA),   // Soft Teal Tint
        borderColor = Color(0xFF99F6E4),       // Pastel Teal Border
        textColor = Color(0xFF0F766E),         // Deep Contrast Teal
        icon = Icons.Default.Paid
    )

    val General = CategoryStyle(
        name = "General",
        accentColor = Color(0xFF4B5563),       // Slate Graphite
        backgroundColor = Color(0xFFF3F4F6),   // Soft Grey Tint
        borderColor = Color(0xFFE5E7EB),       // Pastel Grey Border
        textColor = Color(0xFF374151),         // Deep Contrast Grey
        icon = Icons.Default.FolderOpen
    )

    /**
     * Preset list of main categories in recommended order.
     */
    val defaultCategories = listOf(
        Work,
        Personal,
        Health,
        Focus,
        Ideas
    )

    val defaultCategoryNames = listOf("Work", "Personal", "Health", "Focus", "Ideas")

    // Curated fallback palette for custom categories and user tags
    private val customPalette = listOf(
        Triple(Color(0xFF0284C7), Color(0xFFF0F9FF), Color(0xFFBAE6FD)), // Sky Blue
        Triple(Color(0xFF4F46E5), Color(0xFFEEF2FF), Color(0xFFC7D2FE)), // Indigo
        Triple(Color(0xFFC026D3), Color(0xFFFDF4FF), Color(0xFFF5D0FE)), // Fuchsia
        Triple(Color(0xFFEA580C), Color(0xFFFFF7ED), Color(0xFFFFEDD5)), // Orange
        Triple(Color(0xFF0D9488), Color(0xFFF0FDFA), Color(0xFF99F6E4)), // Teal
        Triple(Color(0xFF65A30D), Color(0xFFF7FEE7), Color(0xFFD9F99D)), // Lime
        Triple(Color(0xFFDB2777), Color(0xFFFDF2F8), Color(0xFFFBCFE8)), // Pink
        Triple(Color(0xFF0891B2), Color(0xFFECFEFF), Color(0xFFA5F3FC))  // Cyan
    )

    /**
     * Resolves visual CategoryStyle for any category name (case-insensitive)
     * including user-created tags and custom categories.
     */
    fun getStyle(categoryName: String): CategoryStyle {
        val trimmed = categoryName.trim().removePrefix("#")
        return when (trimmed.lowercase()) {
            "work" -> Work
            "personal" -> Personal
            "health" -> Health
            "focus" -> Focus
            "ideas" -> Ideas
            "finance" -> Finance
            "general", "" -> General
            else -> {
                // Deterministic color assignment based on category string hash
                val hash = abs(trimmed.hashCode())
                val (accent, bg, border) = customPalette[hash % customPalette.size]
                val icon = if (categoryName.startsWith("#")) Icons.Default.Tag else Icons.Default.BookmarkBorder
                CategoryStyle(
                    name = trimmed,
                    accentColor = accent,
                    backgroundColor = bg,
                    borderColor = border,
                    textColor = accent,
                    icon = icon
                )
            }
        }
    }
}
