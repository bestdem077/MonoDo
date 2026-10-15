package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueBorder
import com.example.ui.theme.SlateGraphite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonochromeHeader(
    streakDays: Int,
    completedToday: Int,
    totalActive: Int,
    userName: String = "",
    workspaceTitle: String = "MonoDo",
    isReminderEnabled: Boolean = false,
    onOpenReminderSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayFormatted = dateFormat.format(Date()).uppercase()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top date, reminders & streak indicator (NO "i" info button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = todayFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Reminder Bell Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isReminderEnabled) RoyalBlue else PaperSurface)
                        .border(
                            1.dp,
                            if (isReminderEnabled) RoyalBlue else HairlineBorder,
                            CircleShape
                        )
                        .clickable { onOpenReminderSettings() }
                        .testTag("header_reminder_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        contentDescription = "Daily Reminders",
                        tint = if (isReminderEnabled) PaperSurface else SlateGraphite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Streak Capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianBlack)
                        .border(1.dp, RoyalBlueBorder.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("streak_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = RoyalBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakDays DAY${if (streakDays != 1) "S" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        color = PaperSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Main Title (customizable workspace title) & Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = workspaceTitle.ifBlank { "MonoDo" },
                    style = MaterialTheme.typography.displayMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (userName.isNotBlank()) "Focus for $userName" else "Focus & Clarity",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            }

            // Completed Counter Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed Today",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$completedToday done",
                    style = MaterialTheme.typography.labelLarge,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(SlateGraphite)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "$totalActive left",
                    style = MaterialTheme.typography.labelLarge,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
