package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayEditorSheet(
    sheetState: SheetState,
    birthdayToEdit: BirthdayEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        month: Int,
        day: Int,
        birthYear: Int?,
        relationship: String,
        note: String,
        hoursBeforeMidnight: Int,
        notifyAtMidnight: Boolean
    ) -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val isEditing = birthdayToEdit != null

    val todayCal = remember { Calendar.getInstance() }
    var name by remember(birthdayToEdit) { mutableStateOf(birthdayToEdit?.name ?: "") }
    var month by remember(birthdayToEdit) { mutableIntStateOf(birthdayToEdit?.month ?: (todayCal.get(Calendar.MONTH) + 1)) }
    var day by remember(birthdayToEdit) { mutableIntStateOf(birthdayToEdit?.day ?: todayCal.get(Calendar.DAY_OF_MONTH)) }
    var includeYear by remember(birthdayToEdit) { mutableStateOf(birthdayToEdit?.birthYear != null) }
    var birthYear by remember(birthdayToEdit) { mutableIntStateOf(birthdayToEdit?.birthYear ?: 2000) }
    var relationship by remember(birthdayToEdit) { mutableStateOf(birthdayToEdit?.relationship ?: "Friends") }
    var note by remember(birthdayToEdit) { mutableStateOf(birthdayToEdit?.note ?: "") }
    var hoursBeforeMidnight by remember(birthdayToEdit) { mutableIntStateOf(birthdayToEdit?.hoursBeforeMidnight ?: 3) }
    var notifyAtMidnight by remember(birthdayToEdit) { mutableStateOf(birthdayToEdit?.notifyAtMidnight ?: true) }
    var nameError by remember { mutableStateOf(false) }

    val relationships = listOf("Friends", "Family", "Partner", "Work", "Other")
    val leadPresets = listOf(
        4 to "4h before (8 PM)",
        3 to "3h before (9 PM)",
        2 to "2h before (10 PM)",
        1 to "1h before (11 PM)",
        15 to "Day before (9 AM)"
    )

    val commitSave = {
        onSave(
            name.trim(),
            month,
            day,
            if (includeYear) birthYear else null,
            relationship,
            note.trim(),
            hoursBeforeMidnight,
            notifyAtMidnight
        )
        onDismiss()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // Always save even if permission was declined; alarms remain ready when granted
        commitSave()
    }

    val openDatePicker = {
        DatePickerDialog(
            context,
            { _, pickedYear, pickedMonthZeroBased, pickedDay ->
                month = pickedMonthZeroBased + 1
                day = pickedDay
                if (pickedYear in 1900..todayCal.get(Calendar.YEAR)) {
                    birthYear = pickedYear
                }
            },
            if (includeYear) birthYear else 2000,
            (month - 1).coerceIn(0, 11),
            day.coerceIn(1, 31)
        ).show()
    }

    val formattedDate = remember(month, day, includeYear, birthYear) {
        val short = BirthdayDateCalculator.formatShortDate(month, day)
        if (includeYear) "$short, $birthYear" else short
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.border)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .testTag("birthday_editor_sheet")
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.accentSoft),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEditing) "Edit Birthday" else "Add Birthday",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "Offline exact-time eve & midnight reminders",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colors.tileBg)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unified Paper Composer Card (Name + Optional Gift/Wish Note)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.canvas)
                    .border(
                        1.dp,
                        if (nameError) colors.danger else colors.border,
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                CompactFormTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    placeholder = "Person's Name (e.g., Rahul, Mom, Priya)",
                    singleLine = true,
                    fontWeight = FontWeight.Bold,
                    isError = nameError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birthday_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                CompactFormTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "Optional gift idea or wish note...",
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("birthday_note_input")
                )
            }

            if (nameError) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Please enter a name",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.danger
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Birth Date Selector
            Text(
                text = "BIRTH DATE",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { openDatePicker() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .testTag("birthday_date_picker_row"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        val turningAge = if (includeYear) {
                            BirthdayDateCalculator.turningAge(month, day, birthYear)
                        } else null
                        if (turningAge != null) {
                            Text(
                                text = "Turns $turningAge on next birthday",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "Pick Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.tileBg.copy(alpha = 0.65f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Track birth year (shows age)",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Medium
                )
                Switch(
                    checked = includeYear,
                    onCheckedChange = { includeYear = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.secondaryInk,
                        uncheckedTrackColor = colors.border
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Relationship Tags
            Text(
                text = "RELATIONSHIP",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                relationships.forEach { rel ->
                    val isSelected = relationship == rel
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) colors.primaryInk else colors.canvas)
                            .border(
                                1.dp,
                                if (isSelected) colors.primaryInk else colors.border,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                relationship = rel
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = rel,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) colors.canvas else colors.secondaryInk,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Advance Eve Reminder Lead Time
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ADVANCE REMINDER (BEFORE 12:00 AM)",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                leadPresets.forEach { (hours, label) ->
                    val isSelected = hoursBeforeMidnight == hours
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.accentSoft else colors.canvas)
                            .border(
                                1.dp,
                                if (isSelected) colors.accent else colors.border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                hoursBeforeMidnight = hours
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("birthday_lead_${hours}h")
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) colors.accent else colors.primaryInk,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Midnight 12:00 AM Wish Alert Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Also alert at 12:00 AM midnight",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Second reminder right when the birthday starts",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk,
                        fontSize = 10.5.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = notifyAtMidnight,
                    onCheckedChange = { notifyAtMidnight = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = colors.accent,
                        uncheckedThumbColor = colors.secondaryInk,
                        uncheckedTrackColor = colors.tileBg
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        nameError = true
                        return@Button
                    }
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        commitSave()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_birthday_button")
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditing) "Update Birthday & Alarms" else "Save Birthday & Arm Alarms",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
