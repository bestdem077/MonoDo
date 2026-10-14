package com.example.ui.components

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LightPill
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSettingsChanged: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { ReminderPreferences(context) }
    val initialSettings = remember { prefs.getSettings() }

    var isEnabled by remember { mutableStateOf(initialSettings.isEnabled) }
    var selectedHour by remember { mutableIntStateOf(initialSettings.hour) }
    var selectedMinute by remember { mutableIntStateOf(initialSettings.minute) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isEnabled = true
        } else {
            Toast.makeText(
                context,
                "Notification permission is required to receive reminders",
                Toast.LENGTH_SHORT
            ).show()
            isEnabled = false
        }
    }

    val timePickerDialog = remember {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
            },
            selectedHour,
            selectedMinute,
            false // 12-hour format with AM/PM
        )
    }

    fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val hour12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.getDefault(), "%02d:%02d %s", hour12, minute, amPm)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = PaperSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isEnabled) ObsidianBlack else LightPill),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                            contentDescription = "Reminders",
                            tint = if (isEnabled) PaperSurface else SlateGraphite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Daily Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Powered by WorkManager",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontSize = 11.sp
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = SlateGraphite
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enable/Disable Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable Daily Notifications",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Trigger reminders for pending tasks every day",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    isEnabled = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                isEnabled = true
                            }
                        } else {
                            isEnabled = false
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PaperSurface,
                        checkedTrackColor = ObsidianBlack,
                        uncheckedThumbColor = SlateGraphite,
                        uncheckedTrackColor = LightPill
                    ),
                    modifier = Modifier.testTag("reminder_switch")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Time Selector (Visible when enabled)
            AnimatedVisibility(visible = isEnabled) {
                Column {
                    Text(
                        text = "REMINDER TIME",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(FocusedBackground)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
                            .clickable { timePickerDialog.show() }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("pick_reminder_time_row"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = "Time Picker",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Trigger Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = formatTime(selectedHour, selectedMinute),
                                style = MaterialTheme.typography.titleMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isEnabled) {
                        "WorkManager will dispatch a local reminder daily at ${formatTime(selectedHour, selectedMinute)} summarizing your active to-dos."
                    } else {
                        "Turn on daily reminders to get a scheduled nudge for your highest-priority tasks."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Test Notification Button
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                return@OutlinedButton
                            }
                        }
                        ReminderScheduler.triggerInstantTestReminder(context)
                        Toast.makeText(context, "Test reminder triggered!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("send_test_notification_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ObsidianBlack
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(HairlineBorder)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Test Notification",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Test Alert",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Save Schedule Button
                Button(
                    onClick = {
                        prefs.saveSettings(isEnabled, selectedHour, selectedMinute)
                        if (isEnabled) {
                            ReminderScheduler.scheduleDailyReminder(
                                context,
                                selectedHour,
                                selectedMinute
                            )
                            Toast.makeText(
                                context,
                                "Reminder scheduled for ${formatTime(selectedHour, selectedMinute)} daily",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            ReminderScheduler.cancelDailyReminder(context)
                            Toast.makeText(context, "Daily reminders turned off", Toast.LENGTH_SHORT).show()
                        }
                        onSettingsChanged()
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp)
                        .testTag("save_reminder_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ObsidianBlack,
                        contentColor = PaperSurface
                    )
                ) {
                    Text(
                        text = "Save Schedule",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
