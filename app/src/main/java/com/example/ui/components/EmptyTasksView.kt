package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonoDoThemeTokens

@Composable
fun EmptyTasksView(
    hasActiveFilters: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 24.dp)
            .testTag("empty_tasks_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        SwissEmptyStateArt(
            type = SwissEmptyArtType.TASKS,
            size = 92.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (hasActiveFilters) "No matching tasks" else "Your workspace is clear",
            style = MaterialTheme.typography.titleMedium,
            color = colors.primaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (hasActiveFilters) {
                "Try switching categories or clearing filters"
            } else {
                "Tap the + button below to add your first task"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = colors.secondaryInk,
            textAlign = TextAlign.Center
        )
    }
}
