package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CategoryTheme
import com.example.ui.util.HapticFeedbackHelper

/**
 * Reusable color-coded category tag for tasks.
 * Renders a soft tinted pill container, vivid accent color dot or icon,
 * and clear label. Optionally clickable (e.g. to filter tasks by category).
 */
@Composable
fun CategoryTag(
    category: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDot: Boolean = true,
    showIcon: Boolean = false,
    fontSize: TextUnit = 10.sp,
    dotSize: Dp = 6.dp,
    horizontalPadding: Dp = 7.dp,
    verticalPadding: Dp = 2.5.dp
) {
    val style = CategoryTheme.getStyle(category)
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val clickableModifier = if (onClick != null) {
        Modifier.clickable {
            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
            onClick()
        }
    } else Modifier

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(style.backgroundColor)
            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
            .then(clickableModifier)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .testTag("category_tag_${category.lowercase().replace(" ", "_")}")
    ) {
        if (showIcon) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                tint = style.accentColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
        } else if (showDot) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(style.accentColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }

        Text(
            text = style.name.ifBlank { "General" },
            style = MaterialTheme.typography.labelSmall,
            color = style.textColor,
            fontWeight = FontWeight.SemiBold,
            fontSize = fontSize,
            letterSpacing = 0.2.sp
        )
    }
}
