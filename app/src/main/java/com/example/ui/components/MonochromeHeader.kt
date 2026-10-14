package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MonochromeHeader(
    streakDays: Int,
    completedToday: Int,
    totalActive: Int,
    isReminderEnabled: Boolean = false,
    onOpenReminderSettings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    val todayFormatted = dateFormat.format(Date()).uppercase()
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Top date, reminders, about & streak indicator
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
                // About / Creator Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(PaperSurface)
                        .border(1.dp, HairlineBorder, CircleShape)
                        .clickable { showAboutDialog = true }
                        .testTag("header_about_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "About App & Creator",
                        tint = SlateGraphite,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Reminder Bell Button
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isReminderEnabled) ObsidianBlack else PaperSurface)
                        .border(
                            1.dp,
                            if (isReminderEnabled) ObsidianBlack else HairlineBorder,
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
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Streak Capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(ObsidianBlack)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("streak_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Streak",
                        tint = PaperSurface,
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

        Spacer(modifier = Modifier.height(6.dp))

        // Main Title & Quick Stat
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.clickable { showAboutDialog = true }
            ) {
                Text(
                    text = "MonoDo",
                    style = MaterialTheme.typography.displayMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = "by Vishal Dhangar",
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

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Column {
                    Text(
                        text = "MonoDo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    Text(
                        text = "v${com.example.BuildConfig.VERSION_NAME} (Build ${com.example.BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateGraphite
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Minimalist Monochrome Task Manager designed for deep focus and clean productivity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PaperSurface)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "CREATOR & DEVELOPER",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Vishal Dhangar",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                        }
                    }
                    Text(
                        text = "• 100% Native Kotlin & Jetpack Compose\n• Local Room Database (No cloud tracking)\n• Continuous Integration via GitHub Actions",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showAboutDialog = false }
                ) {
                    Text(
                        text = "Close",
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
