package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ScheduleSend
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.R
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.VersionComparator
import com.example.data.user.AppThemeMode
import com.example.ui.components.CompactFormTextField
import com.example.ui.theme.AmoledBlackColors
import com.example.ui.theme.DarkSlateColors
import com.example.ui.theme.LightPaperColors
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.AppLockHelper
import com.example.ui.util.HapticFeedbackHelper
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
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val userProfile by viewModel.userProfile.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val isPinnedTodayEnabled by viewModel.isPinnedTodayEnabled.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val reminderPrefs = remember { ReminderPreferences(context) }
    var reminderSettings by remember { mutableStateOf(reminderPrefs.getSettings()) }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showClearTasksDialog by remember { mutableStateOf(false) }
    var showResetAllDialog by remember { mutableStateOf(false) }

    var hasLocalSnapshot by remember { mutableStateOf(viewModel.hasInternalBackupFile()) }
    var canScheduleExactAlarms by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(ReminderScheduler.isIgnoringBatteryOptimizations(context)) }
    var pendingBriefingPreview by remember { mutableStateOf(false) }
    var pendingPinnedTodayToggle by remember { mutableStateOf(false) }
    var pendingAppLockTarget by remember { mutableStateOf<Boolean?>(null) }

    val deviceCredentialToggleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val target = pendingAppLockTarget
        pendingAppLockTarget = null
        if (result.resultCode == Activity.RESULT_OK && target != null) {
            viewModel.setAppLockEnabled(target)
        }
    }

    val pinnedNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingPinnedTodayToggle) {
            viewModel.setPinnedTodayEnabled(true)
        } else if (!granted) {
            Toast.makeText(context, "Allow notifications to pin Today's Tasks.", Toast.LENGTH_SHORT).show()
        }
        pendingPinnedTodayToggle = false
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                canScheduleExactAlarms = ReminderScheduler.canScheduleExactAlarms(context)
                isBatteryUnrestricted = ReminderScheduler.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Android 13+ POST_NOTIFICATIONS runtime permission launcher
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            if (pendingBriefingPreview) {
                pendingBriefingPreview = false
                viewModel.sendTestBriefingNotification()
                Toast.makeText(context, "Daily Morning Briefing sent to notification panel!", Toast.LENGTH_SHORT).show()
            } else {
                reminderPrefs.saveSettings(true, reminderSettings.hour, reminderSettings.minute)
                reminderSettings = reminderSettings.copy(isEnabled = true)
                ReminderScheduler.scheduleDailyReminder(context, reminderSettings.hour, reminderSettings.minute)
                Toast.makeText(context, "Daily Morning Briefing enabled!", Toast.LENGTH_SHORT).show()
            }
        } else {
            pendingBriefingPreview = false
            reminderPrefs.saveSettings(false, reminderSettings.hour, reminderSettings.minute)
            reminderSettings = reminderSettings.copy(isEnabled = false)
            Toast.makeText(
                context,
                "Notification permission is required for briefings and task reminders.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

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
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(cal.time)
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
            .background(colors.canvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mono_do_settings_screen")
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.primaryInk,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Appearance, notifications & offline storage",
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondaryInk
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 1. User Profile Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .clickable { showEditProfileDialog = true }
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(colors.accentSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = userProfile.userName.ifBlank { "MonoDo User" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = userProfile.workspaceTitle.ifBlank { "MonoDo Workspace" },
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (userProfile.birthDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.tileBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Birth Date",
                            tint = colors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Born: ${userProfile.birthDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.primaryInk,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Appearance — 3-Theme Switcher (Light Paper, Dark Slate, Pure Black AMOLED)
        Text(
            text = "APPEARANCE",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_selector_row"),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ThemeOptionCard(
                mode = AppThemeMode.LIGHT_PAPER,
                previewCanvas = LightPaperColors.canvas,
                previewSurface = LightPaperColors.surface,
                previewInk = LightPaperColors.primaryInk,
                isSelected = currentThemeMode == AppThemeMode.LIGHT_PAPER,
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    viewModel.setThemeMode(AppThemeMode.LIGHT_PAPER)
                },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                mode = AppThemeMode.DARK_SLATE,
                previewCanvas = DarkSlateColors.canvas,
                previewSurface = DarkSlateColors.surface,
                previewInk = DarkSlateColors.primaryInk,
                isSelected = currentThemeMode == AppThemeMode.DARK_SLATE,
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    viewModel.setThemeMode(AppThemeMode.DARK_SLATE)
                },
                modifier = Modifier.weight(1f)
            )
            ThemeOptionCard(
                mode = AppThemeMode.AMOLED_BLACK,
                previewCanvas = AmoledBlackColors.canvas,
                previewSurface = AmoledBlackColors.surface,
                previewInk = AmoledBlackColors.primaryInk,
                isSelected = currentThemeMode == AppThemeMode.AMOLED_BLACK,
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    viewModel.setThemeMode(AppThemeMode.AMOLED_BLACK)
                },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Notifications & Daily Morning Briefing Card
        Text(
            text = "NOTIFICATIONS & REMINDERS",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.tileBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (reminderSettings.isEnabled) colors.accent else colors.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Daily Morning Briefing",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryInk
                            )
                            Text(
                                text = "Expandable summary of overdue & today's tasks. Individual tasks also support exact alarm times.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryInk,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = reminderSettings.isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                if (needsPermission) {
                                    pendingBriefingPreview = false
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    reminderPrefs.saveSettings(true, reminderSettings.hour, reminderSettings.minute)
                                    reminderSettings = reminderSettings.copy(isEnabled = true)
                                    ReminderScheduler.scheduleDailyReminder(context, reminderSettings.hour, reminderSettings.minute)
                                }
                            } else {
                                reminderPrefs.saveSettings(false, reminderSettings.hour, reminderSettings.minute)
                                reminderSettings = reminderSettings.copy(isEnabled = false)
                                ReminderScheduler.cancelDailyReminder(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.secondaryInk,
                            uncheckedTrackColor = colors.tileBg
                        )
                    )
                }

                if (reminderSettings.isEnabled) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .clickable { showTimePicker() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = colors.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = formattedReminderTime,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primaryInk
                                )
                            }

                            Text(
                                text = "Edit",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentSoft)
                                .border(1.dp, colors.accentBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.POST_NOTIFICATIONS
                                        ) != PackageManager.PERMISSION_GRANTED
                                    if (needsPermission) {
                                        pendingBriefingPreview = true
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        viewModel.sendTestBriefingNotification()
                                        Toast.makeText(
                                            context,
                                            "Briefing preview sent to notification shade",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("test_briefing_notification_btn"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ScheduleSend,
                                contentDescription = "Preview Briefing",
                                tint = colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Doze-Proof Background Alarm Reliability Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (canScheduleExactAlarms) colors.successSoft else colors.accentSoft)
                        .border(
                            1.dp,
                            if (canScheduleExactAlarms) colors.successBorder else colors.accentBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                runCatching {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("exact_alarm_status_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (canScheduleExactAlarms) colors.success else colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (canScheduleExactAlarms) "Exact Background Alarms Active" else "Allow Exact Alarm Timing",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryInk
                            )
                            Text(
                                text = if (canScheduleExactAlarms) {
                                    "Fires on the exact minute even in Doze mode or after reboots"
                                } else {
                                    "Tap to enable exact minute delivery even when app is closed for days"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryInk,
                                fontSize = 10.5.sp
                            )
                        }
                    }

                    if (!canScheduleExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Enable",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6-Month Idle / OEM Battery Protection Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isBatteryUnrestricted) colors.successSoft else colors.canvas)
                        .border(
                            1.dp,
                            if (isBatteryUnrestricted) colors.successBorder else colors.border,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(enabled = !isBatteryUnrestricted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                runCatching {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("battery_unrestricted_status_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBatteryUnrestricted) "Unrestricted Battery Active" else "6-Month Background Protection",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = if (isBatteryUnrestricted) {
                                "Birthdays & task alarms stay awake even if MonoDo is unopened for months"
                            } else {
                                "Recommended on Xiaomi/Samsung/Vivo: set MonoDo battery to Unrestricted"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontSize = 10.5.sp
                        )
                    }

                    if (!isBatteryUnrestricted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Optional Pinned "Today's Tasks" Notification (OFF by default)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(colors.tileBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isPinnedTodayEnabled) colors.accent else colors.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pinned Today's Tasks Card",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primaryInk
                            )
                            Text(
                                text = "Silent ongoing summary in your notification shade showing today's pending tasks.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryInk,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isPinnedTodayEnabled,
                        onCheckedChange = { enabled ->
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            if (enabled) {
                                val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                if (needsPerm) {
                                    pendingPinnedTodayToggle = true
                                    pinnedNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setPinnedTodayEnabled(true)
                                }
                            } else {
                                viewModel.setPinnedTodayEnabled(false)
                            }
                        },
                        modifier = Modifier.testTag("pinned_today_notification_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colors.accent,
                            uncheckedThumbColor = colors.secondaryInk,
                            uncheckedTrackColor = colors.tileBg
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3.5 Privacy & Security (Device Fingerprint / Screen PIN App Lock)
        Text(
            text = "PRIVACY & SECURITY",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isAppLockEnabled) colors.accentSoft else colors.tileBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = if (isAppLockEnabled) colors.accent else colors.secondaryInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "App Lock (Fingerprint / PIN)",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "Require device fingerprint or screen PIN whenever MonoDo opens.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = isAppLockEnabled,
                    onCheckedChange = { wantEnabled ->
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        val activity = context as? Activity
                        if (activity == null) {
                            Toast.makeText(context, "Unable to verify device lock.", Toast.LENGTH_SHORT).show()
                            return@Switch
                        }
                        if (!AppLockHelper.isDeviceSecure(context)) {
                            Toast.makeText(
                                context,
                                "Set a Fingerprint or Screen PIN in Android Settings first.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Switch
                        }
                        pendingAppLockTarget = wantEnabled
                        AppLockHelper.authenticate(
                            activity = activity,
                            title = if (wantEnabled) "Enable MonoDo App Lock" else "Disable MonoDo App Lock",
                            subtitle = "Confirm with your fingerprint or screen PIN",
                            onSuccess = {
                                pendingAppLockTarget = null
                                viewModel.setAppLockEnabled(wantEnabled)
                            },
                            onError = { err ->
                                pendingAppLockTarget = null
                                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                            },
                            onFallbackCredentialIntent = { intent ->
                                deviceCredentialToggleLauncher.launch(intent)
                            }
                        )
                    },
                    modifier = Modifier.testTag("app_lock_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.secondaryInk,
                        uncheckedTrackColor = colors.tileBg
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Data Management
        Text(
            text = "DATA MANAGEMENT",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
        ) {
            SettingsActionRow(
                icon = Icons.Default.FileDownload,
                iconColor = colors.accent,
                title = "Export Backup (.json)",
                subtitle = "Save offline .json backup to device storage",
                onClick = {
                    try {
                        viewModel.saveBackupToInternalStorage()
                        hasLocalSnapshot = true
                        exportDocumentLauncher.launch("monodo_backup.json")
                    } catch (_: Exception) {
                        Toast.makeText(context, "Saved local snapshot backup!", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            SettingsActionRow(
                icon = Icons.Default.FileUpload,
                iconColor = colors.accent,
                title = "Import Backup (.json)",
                subtitle = if (hasLocalSnapshot) "Pick a .json file or restore saved device backup" else "Restore tasks, notes & habits from a .json file",
                onClick = {
                    try {
                        importLauncher.launch("*/*")
                    } catch (_: Exception) {
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
                    .background(colors.border)
            )

            SettingsActionRow(
                icon = Icons.Default.DeleteSweep,
                iconColor = colors.secondaryInk,
                title = "Clear Completed Tasks",
                subtitle = "Permanently remove finished items",
                onClick = { showClearTasksDialog = true }
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.border)
            )

            SettingsActionRow(
                icon = Icons.Default.RestartAlt,
                iconColor = colors.danger,
                title = "Reset All Data & Setup",
                subtitle = "Wipes database and restarts onboarding",
                onClick = { showResetAllDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4.5 App Updates (GitHub Releases — In-App Download & Downloads Folder Fallback)
        Text(
            text = "APP UPDATES",
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .clickable {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    when (appUpdateState) {
                        is AppUpdateState.UpdateAvailable,
                        is AppUpdateState.Downloading,
                        is AppUpdateState.ReadyToInstall -> viewModel.openUpdateSheet()
                        else -> viewModel.checkForAppUpdates(userInitiated = true)
                    }
                }
                .padding(16.dp)
                .testTag("settings_check_updates_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val isUpdateReadyOrAvailable =
                        appUpdateState is AppUpdateState.UpdateAvailable ||
                            appUpdateState is AppUpdateState.ReadyToInstall ||
                            appUpdateState is AppUpdateState.Downloading
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when (appUpdateState) {
                                    is AppUpdateState.ReadyToInstall, is AppUpdateState.UpToDate -> colors.successSoft
                                    is AppUpdateState.UpdateAvailable, is AppUpdateState.Downloading -> colors.accentSoft
                                    else -> colors.tileBg
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "App Updates",
                            tint = when (appUpdateState) {
                                is AppUpdateState.ReadyToInstall, is AppUpdateState.UpToDate -> colors.success
                                is AppUpdateState.UpdateAvailable, is AppUpdateState.Downloading -> colors.accent
                                else -> colors.primaryInk
                            },
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (val st = appUpdateState) {
                                is AppUpdateState.Checking -> "Checking GitHub..."
                                is AppUpdateState.UpdateAvailable -> "Update ${VersionComparator.displayTag(st.release.tagName)} Available"
                                is AppUpdateState.Downloading -> "Downloading Update (${st.progressPercent}%)"
                                is AppUpdateState.ReadyToInstall -> "Ready to Install ${VersionComparator.displayTag(st.release.tagName)}"
                                is AppUpdateState.UpToDate -> "MonoDo is Up to Date"
                                else -> "Check for Updates"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = when (val st = appUpdateState) {
                                is AppUpdateState.UpdateAvailable -> "Tap to download directly & save to Downloads"
                                is AppUpdateState.Downloading -> "Tap to view live progress"
                                is AppUpdateState.ReadyToInstall -> "Tap to install in-place without losing data"
                                is AppUpdateState.UpToDate -> "Installed ${st.currentVersion} • github.com/${AppUpdateManager.getGitHubRepoSlug()}"
                                is AppUpdateState.Error -> st.message
                                else -> "Installed ${AppUpdateManager.getCurrentInstalledTag()} • Official GitHub Releases"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (appUpdateState) {
                                is AppUpdateState.ReadyToInstall -> colors.success
                                is AppUpdateState.UpdateAvailable, is AppUpdateState.Downloading -> colors.accent
                                else -> colors.tileBg
                            }
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (appUpdateState) {
                            is AppUpdateState.Checking -> "..."
                            is AppUpdateState.UpdateAvailable -> "Update"
                            is AppUpdateState.Downloading -> "View"
                            is AppUpdateState.ReadyToInstall -> "Install"
                            else -> "Check"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (appUpdateState) {
                            is AppUpdateState.ReadyToInstall,
                            is AppUpdateState.UpdateAvailable,
                            is AppUpdateState.Downloading -> Color.White
                            else -> colors.primaryInk
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Branding & Credits (Dynamic BuildConfig.GITHUB_RELEASE_TAG)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_monodo_obsidian_mark),
                        contentDescription = "MonoDo App Icon",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "MonoDo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = colors.primaryInk,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} • Offline & Private",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Created by Vishal D. (Vishal Dhangar)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primaryInk,
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
        var editBirthTimestamp by remember { mutableLongStateOf(userProfile.birthTimestamp) }

        val showEditDatePicker = {
            val cal = Calendar.getInstance()
            if (editBirthTimestamp > 0) cal.timeInMillis = editBirthTimestamp
            DatePickerDialog(
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
            ).show()
        }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    "Edit Profile",
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryInk
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompactFormTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        placeholder = "Your Name",
                        singleLine = true,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompactFormTextField(
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
                            .background(colors.tileBg)
                            .clickable { showEditDatePicker() }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (editBirthDate.isNotBlank()) editBirthDate else "Set Date of Birth",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.primaryInk
                        )
                        Icon(Icons.Default.Cake, contentDescription = null, tint = colors.accent)
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            }
        )
    }

    // Clear Tasks Dialog
    if (showClearTasksDialog) {
        AlertDialog(
            onDismissRequest = { showClearTasksDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    "Clear Completed Tasks?",
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryInk
                )
            },
            text = {
                Text(
                    "This will permanently remove all completed tasks.",
                    color = colors.secondaryInk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCompletedTasks()
                        showClearTasksDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primaryInk,
                        contentColor = colors.canvas
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearTasksDialog = false }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            }
        )
    }

    // Reset All Dialog
    if (showResetAllDialog) {
        AlertDialog(
            onDismissRequest = { showResetAllDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    "Reset All Data & Setup?",
                    fontWeight = FontWeight.Bold,
                    color = colors.danger
                )
            },
            text = {
                Text(
                    "This will delete all tasks, notes, habits, focus sessions, and profile settings and return you to the initial setup screen.",
                    color = colors.secondaryInk
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetAllDialog = false
                        onResetAll()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.danger,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetAllDialog = false }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionCard(
    mode: AppThemeMode,
    previewCanvas: Color,
    previewSurface: Color,
    previewInk: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) colors.accent else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(10.dp)
            .testTag("theme_option_${mode.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Miniature UI Preview Swatch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(previewCanvas)
                .border(1.dp, colors.border.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(6.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.65f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(previewInk.copy(alpha = 0.75f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(previewSurface)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = mode.title,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) colors.accent else colors.primaryInk,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.tileBg),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primaryInk
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryInk
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.secondaryInk,
            modifier = Modifier.size(18.dp)
        )
    }
}
