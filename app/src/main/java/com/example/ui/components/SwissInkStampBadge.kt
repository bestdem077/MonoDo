package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper

/**
 * Sleek minimalist completion summary banner shown strictly when all tasks are complete.
 */
@Composable
fun SwissInkStampBadge(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    completedCount: Int = 0
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(visible) {
        if (visible) {
            HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.accentBorder, RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
                .testTag("swiss_ink_stamp_celebration"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accentBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "All tasks completed",
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "All tasks completed",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (completedCount > 0) {
                            "$completedCount ${if (completedCount == 1) "task" else "tasks"} cleared • Enjoy your focus"
                        } else {
                            "Your list is completely clear."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryInk,
                        fontSize = 12.sp
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss Banner",
                    tint = colors.secondaryInk,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
