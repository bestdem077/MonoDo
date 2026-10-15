package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.user.UserProfile
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@Composable
fun MonoDoSettingsScreen(
    viewModel: TaskViewModel,
    userProfile: UserProfile,
    onUpdateProfile: (userName: String, workspaceTitle: String) -> Unit,
    isReminderEnabled: Boolean,
    onOpenReminderSettings: () -> Unit,
    onToggleReminder: (Boolean) -> Unit,
    onResetAllData: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var editUserName by remember { mutableStateOf(userProfile.userName) }
    var editWorkspaceTitle by remember { mutableStateOf(userProfile.workspaceTitle) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showResetAllConfirmDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Page Title
        item {
            Column {
                Text(
                    text = "PREFERENCES",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settings & Profile",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // 1. Personalized User Profile & Workspace Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userProfile.userName.isNotBlank()) userProfile.userName.take(1).uppercase() else "M",
                            style = MaterialTheme.typography.titleLarge,
                            color = PaperSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (userProfile.userName.isNotBlank()) userProfile.userName else "Personal Workspace",
                            style = MaterialTheme.typography.titleLarge,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Workspace: ${userProfile.workspaceTitle.ifBlank { "MonoDo" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGraphite
                        )
                    }

                    // Edit Profile Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(DateTileBackground)
                            .clickable {
                                editUserName = userProfile.userName
                                editWorkspaceTitle = userProfile.workspaceTitle
                                showEditProfileDialog = true
                            }
                            .testTag("edit_profile_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile & Workspace Name",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "100% Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Private SQLite",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DateTileBackground)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Zero Telemetry",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Notification & Daily Reminders Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "NOTIFICATIONS & REMINDERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Habit Reminder",
                            style = MaterialTheme.typography.titleMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (isReminderEnabled) "Scheduled daily notification active" else "Turn on to maintain streaks daily",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGraphite
                        )
                    }

                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            onToggleReminder(it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PaperSurface,
                            checkedTrackColor = ObsidianBlack,
                            uncheckedThumbColor = SlateGraphite,
                            uncheckedTrackColor = DateTileBackground
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            onOpenReminderSettings()
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Reminder Timing",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Configure Reminder Time & Sound",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 3. Data Management (Offline Backup & Restore)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "DATA MANAGEMENT (OFFLINE BACKUP)",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Download / Export Backup Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                            val json = viewModel.exportTasksAsJson()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("MonoDo Backup", json))

                            // Trigger Android share sheet
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "application/json"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Backup"))
                            Toast.makeText(context, "Backup copied to clipboard and ready to save!", Toast.LENGTH_LONG).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Backup",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Export Data (JSON Backup)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Save tasks to local file or clipboard",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Export",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Upload / Import Backup Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            importJsonText = ""
                            showImportDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Import Backup",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import Data (Restore JSON)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Restore tasks and checklists from backup",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Import",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Clear Completed Tasks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                            showClearConfirmDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = "Clear Completed",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Clear Completed Tasks",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Declutter old completed tasks from database",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Clear",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Reset All Data & Setup (Erases data and restores initial onboarding)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                            showResetAllConfirmDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Reset All Data",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Reset All Data & Setup",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Erase tasks, notes & reset onboarding",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Reset",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 4. About & Information Card (Relocated from Top Header "i" Button)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Text(
                    text = "ABOUT & INFORMATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clickable row to open About info dialog
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DateTileBackground)
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            showAboutDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About App",
                            tint = ObsidianBlack,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "About ${userProfile.workspaceTitle.ifBlank { "MonoDo" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Architecture, privacy & credits",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Details",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Version row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "App Version",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGraphite
                    )
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Storage Architecture row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGraphite
                    )
                    Text(
                        text = "100% Offline SQLite",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    // Modal Dialog: Edit Profile & Workspace Name
    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text(
                    text = "Edit Profile & Workspace",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                Column {
                    Text(
                        text = "Customize your displayed name and workspace title:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editUserName,
                        onValueChange = { editUserName = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("e.g. Alex", color = SlateGraphite) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            focusedBorderColor = SoftBorderGrey,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = FocusedBackground,
                            unfocusedContainerColor = PaperCanvas
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editWorkspaceTitle,
                        onValueChange = { editWorkspaceTitle = it },
                        label = { Text("Workspace / App Name") },
                        placeholder = { Text("e.g. MonoDo", color = SlateGraphite) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            focusedBorderColor = SoftBorderGrey,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = FocusedBackground,
                            unfocusedContainerColor = PaperCanvas
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onUpdateProfile(editUserName.trim(), editWorkspaceTitle.trim().ifBlank { "MonoDo" })
                        showEditProfileDialog = false
                        Toast.makeText(context, "Profile updated!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Save", color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog: Relocated About MonoDo Info Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "M",
                            style = MaterialTheme.typography.titleMedium,
                            color = PaperSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "About ${userProfile.workspaceTitle.ifBlank { "MonoDo" }}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.app_version),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ObsidianBlack
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "A distraction-free, minimalist monochrome task and habit manager designed for clarity and deep focus.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGraphite,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DateTileBackground)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "100% Offline & Private",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ObsidianBlack
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "All tasks, notes, checklists, and streak data remain strictly on your device in a secure local Room SQLite database.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Created with focus by Vishal D.",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Close", color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog: Restore JSON Backup
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text(
                    text = "Restore Tasks Backup",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                Column {
                    Text(
                        text = "Paste your previously exported JSON backup below to restore your tasks:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON here...", color = SlateGraphite) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            focusedBorderColor = SoftBorderGrey,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = PaperCanvas,
                            unfocusedContainerColor = PaperCanvas
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val count = viewModel.importTasksFromJson(importJsonText)
                            if (count > 0) {
                                HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                                Toast.makeText(context, "Successfully restored $count tasks!", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "Invalid JSON backup format. Please check the text.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Restore Tasks", color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog: Clear Completed Tasks Confirmation
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = "Clear Completed Tasks?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all completed tasks from your local storage to declutter your list. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGraphite
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                        viewModel.clearCompletedTasks()
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Completed tasks cleared.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Clear All", color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Modal Dialog: Reset All Data & Setup Confirmation
    if (showResetAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All App Data?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all tasks, notes, categories, and reset your workspace setup back to initial onboarding. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateGraphite
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                        showResetAllConfirmDialog = false
                        onResetAllData()
                        Toast.makeText(context, "App reset to initial setup.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack)
                ) {
                    Text("Reset Everything", color = PaperSurface, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllConfirmDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
