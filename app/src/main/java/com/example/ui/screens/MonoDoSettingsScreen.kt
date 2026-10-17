package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MonoDoSettingsScreen(
    viewModel: TaskViewModel,
    onResetAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val reminderPrefs = remember { ReminderPreferences(context) }
    var reminderSettings by remember { mutableStateOf(reminderPrefs.getSettings()) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearTasksDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    var hasLocalSnapshot by remember { mutableStateOf(viewModel.hasInternalBackupFile()) }

    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val json = viewModel.exportJsonBackup()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                viewModel.saveBackupToInternalStorage()
                hasLocalSnapshot = true
                Toast.makeText(context, "Backup saved to device (.json)!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                val success = viewModel.importJsonBackup(jsonString)
                if (success) {
                    Toast.makeText(context, "Backup successfully restored!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to parse backup JSON file", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error importing file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val formattedReminderTime = remember(reminderSettings.hour, reminderSettings.minute) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminderSettings.hour)
            set(Calendar.MINUTE, reminderSettings.minute)
        }
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(cal.time)
    }

    val showTimePicker = {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                reminderPrefs.saveSettings(reminderSettings.isEnabled, hour, minute)
                reminderSettings = reminderSettings.copy(hour = hour, minute = minute)
                if (reminderSettings.isEnabled) {
                    ReminderScheduler.scheduleDailyReminder(context, hour, minute)
                }
            },
            reminderSettings.hour,
            reminderSettings.minute,
            false
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mono_do_settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ObsidianBlack,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Preferences, notifications & offline storage",
            style = MaterialTheme.typography.bodySmall,
            color = SlateGraphite
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. User Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .clickable { showEditProfileDialog = true }
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(RoyalBlueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = userProfile.userName.ifBlank { "MonoDo User" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                            Text(
                                text = userProfile.workspaceTitle.ifBlank { "MonoDo Workspace" },
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (userProfile.birthDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Birth Date",
                            tint = RoyalBlue,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Born: ${userProfile.birthDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Daily Focus Reminder Card
        Text(
            text = "REMINDERS",
            style = MaterialTheme.typography.labelSmall,
            color = SlateGraphite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DateTileBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (reminderSettings.isEnabled) RoyalBlue else SlateGraphite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Focus Alert",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = ObsidianBlack
                            )
                            Text(
                                text = "Prompts you once per day",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }

                    Switch(
                        checked = reminderSettings.isEnabled,
                        onCheckedChange = { enabled ->
                            reminderPrefs.saveSettings(enabled, reminderSettings.hour, reminderSettings.minute)
                            reminderSettings = reminderSettings.copy(isEnabled = enabled)
                            if (enabled) {
                                ReminderScheduler.scheduleDailyReminder(context, reminderSettings.hour, reminderSettings.minute)
                            } else {
                                ReminderScheduler.cancelDailyReminder(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RoyalBlue,
                            uncheckedThumbColor = SlateGraphite,
                            uncheckedTrackColor = DateTileBackground
                        )
                    )
                }

                if (reminderSettings.isEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperCanvas)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                            .clickable { showTimePicker() }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = RoyalBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedReminderTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                        }

                        Text(
                            text = "Change Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Data Management
        Text(
            text = "DATA MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = SlateGraphite,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
        ) {
            SettingsActionRow(
                icon = Icons.Default.FileDownload,
                iconColor = RoyalBlue,
                title = "Export Backup (.json)",
                subtitle = "Save offline .json backup to device storage",
                onClick = {
                    try {
                        viewModel.saveBackupToInternalStorage()
                        hasLocalSnapshot = true
                        exportDocumentLauncher.launch("monodo_backup.json")
                    } catch (e: Exception) {
                        Toast.makeText(context, "Saved local snapshot backup!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )

            SettingsActionRow(
                icon = Icons.Default.FileUpload,
                iconColor = RoyalBlue,
                title = "Import Backup (.json)",
                subtitle = if (hasLocalSnapshot) "Pick a .json file or restore saved device backup" else "Restore tasks, notes & habits from a .json file",
                onClick = {
                    try {
                        importLauncher.launch("*/*")
                    } catch (e: Exception) {
                        if (viewModel.restoreFromInternalStorage()) {
                            Toast.makeText(context, "Restored from device snapshot!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Could not open file picker", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )

            SettingsActionRow(
                icon = Icons.Default.DeleteSweep,
                iconColor = SlateGraphite,
                title = "Clear Completed Tasks",
                subtitle = "Permanently remove finished items",
                onClick = { showClearTasksDialog = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )

            SettingsActionRow(
                icon = Icons.Default.RestartAlt,
                iconColor = Color(0xFFDC2626),
                title = "Reset All Data & Setup",
                subtitle = "Wipes database and restarts onboarding",
                onClick = { showResetAllDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Branding & Credits
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MonoDo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = ObsidianBlack,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version 2.0.0 • 100% Offline & Private",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Created by Vishal D. (Vishal Dhangar)",
                    style = MaterialTheme.typography.bodySmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        var editName by remember { mutableStateOf(userProfile.userName) }
        var editWorkspace by remember { mutableStateOf(userProfile.workspaceTitle) }
        var editBirthDate by remember { mutableStateOf(userProfile.birthDate) }
        var editBirthTimestamp by remember { mutableStateOf(userProfile.birthTimestamp) }

        val showEditDatePicker = {
            val cal = Calendar.getInstance()
            if (editBirthTimestamp > 0) cal.timeInMillis = editBirthTimestamp
            val dialog = DatePickerDialog(
                context,
                { _, year, month, day ->
                    val chosen = Calendar.getInstance().apply { set(year, month, day, 0, 0, 0) }
                    editBirthTimestamp = chosen.timeInMillis
                    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
                    editBirthDate = sdf.format(Date(editBirthTimestamp))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.example.ui.components.CompactFormTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = "Your Name",
                        singleLine = true,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    com.example.ui.components.CompactFormTextField(
                        value = editWorkspace,
                        onValueChange = { editWorkspace = it },
                        placeholder = "Workspace Title",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .clickable { showEditDatePicker() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (editBirthDate.isNotBlank()) editBirthDate else "Set Date of Birth",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianBlack
                        )
                        Icon(Icons.Default.Cake, contentDescription = null, tint = RoyalBlue)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveUserProfile(
                            name = editName,
                            workspaceTitle = editWorkspace,
                            birthDate = editBirthDate,
                            birthTimestamp = editBirthTimestamp,
                            reminderHour = userProfile.reminderHour,
                            reminderMinute = userProfile.reminderMinute
                        )
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoyalBlue)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            }
        )
    }

    // Clear Tasks Dialog
    if (showClearTasksDialog) {
        AlertDialog(
            onDismissRequest = { showClearTasksDialog = false },
            title = { Text("Clear Completed Tasks?", fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all completed tasks.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedTasks()
                        showClearTasksDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTasksDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            }
        )
    }

    // Reset All Dialog
    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            title = { Text("Reset All Data & Setup?", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
            text = { Text("This will delete all tasks, notes, profile settings and return you to the initial setup screen.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetAllDialog = false
                        onResetAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            }
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DateTileBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ObsidianBlack
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = SlateGraphite,
            modifier = Modifier.size(18.dp)
        )
    }
}
