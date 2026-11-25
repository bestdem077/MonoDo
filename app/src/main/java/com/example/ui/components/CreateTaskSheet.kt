package com.example.ui.components

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.DefaultRoutineTemplates
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrence
import kotlinx.coroutines.delay
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateTaskSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    availableCategories: List<String> = listOf("Work", "Personal", "Health", "Focus", "Finance", "General"),
    onAddCustomCategory: ((name: String, colorHex: String) -> Unit)? = null,
    taskToEdit: TaskEntity? = null,
    initialDueDate: Long? = null,
    savedTemplates: List<RoutineTemplateEntity> = emptyList(),
    onSaveAsTemplate: ((title: String, category: String, priority: TaskPriority, subtasks: List<String>) -> Unit)? = null,
    onDeleteTemplate: ((RoutineTemplateEntity) -> Unit)? = null,
    onCreateTask: (
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Long?,
        subtasks: List<String>,
        recurrence: TaskRecurrence,
        reminderAt: Long?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdateTask: (
        existing: TaskEntity,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Long?,
        subtasks: List<SubTask>,
        recurrence: TaskRecurrence,
        reminderAt: Long?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val isEditMode = taskToEdit != null

    val titleFocusRequester = remember { FocusRequester() }
    LaunchedEffect(isEditMode) {
        if (!isEditMode) {
            delay(140L)
            runCatching { titleFocusRequester.requestFocus() }
        }
    }

    var title by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.description ?: "") }
    var selectedCategory by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.category ?: "Work") }
    var selectedPriority by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.priority ?: TaskPriority.MEDIUM) }
    var selectedRecurrence by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.recurrence ?: TaskRecurrence.NONE) }
    var dueDate by remember(taskToEdit?.id, initialDueDate) {
        mutableStateOf(
            if (isEditMode) taskToEdit?.dueDate
            else initialDueDate ?: QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY)
        )
    }
    var reminderAt by remember(taskToEdit?.id) { mutableStateOf(taskToEdit?.reminderAt) }
    var newSubtaskInput by remember { mutableStateOf("") }
    val subtasksList = remember(taskToEdit?.id) {
        mutableStateListOf<SubTask>().apply {
            taskToEdit?.subtasks?.let { addAll(it) }
        }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showTemplatesDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var selectedCategoryColorHex by remember { mutableStateOf("#2563EB") }
    var pendingReminderTimestamp by remember { mutableStateOf<Long?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingReminderTimestamp?.let { ts ->
                reminderAt = ts
                pendingReminderTimestamp = null
            }
        } else {
            pendingReminderTimestamp = null
            Toast.makeText(
                context,
                "Allow notifications to receive task reminder alerts.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun applyReminderTimestamp(targetMillis: Long) {
        val needsPerm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        if (needsPerm) {
            pendingReminderTimestamp = targetMillis
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            reminderAt = targetMillis
        }
        // Keep due date aligned with reminder date if dueDate was empty
        if (dueDate == null) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = targetMillis
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }
            dueDate = cal.timeInMillis
        }
    }

    val categories = remember(availableCategories) {
        val filtered = availableCategories.filter { !it.equals("All", ignoreCase = true) }
        if (filtered.isEmpty()) listOf("Work", "Personal", "Health", "Focus", "Finance", "General") else filtered
    }

    val submitTaskNow = {
        if (title.isNotBlank()) {
            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
            if (taskToEdit != null) {
                onUpdateTask(
                    taskToEdit,
                    title.trim(),
                    description.trim(),
                    selectedCategory,
                    selectedPriority,
                    dueDate,
                    subtasksList.toList(),
                    selectedRecurrence,
                    reminderAt
                )
            } else {
                onCreateTask(
                    title.trim(),
                    description.trim(),
                    selectedCategory,
                    selectedPriority,
                    dueDate,
                    subtasksList.map { it.title },
                    selectedRecurrence,
                    reminderAt
                )
            }
            onDismiss()
        }
    }

    val showDatePicker = {
        val cal = Calendar.getInstance()
        if (dueDate != null) cal.timeInMillis = dueDate!!
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val chosen = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                dueDate = chosen.timeInMillis
                // Also shift existing reminder date if user changed the due date
                reminderAt?.let { existingRem ->
                    val remCal = Calendar.getInstance().apply { timeInMillis = existingRem }
                    val shifted = Calendar.getInstance().apply {
                        set(year, month, day, remCal.get(Calendar.HOUR_OF_DAY), remCal.get(Calendar.MINUTE), 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    reminderAt = shifted.timeInMillis
                }
                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val showReminderTimePicker = {
        val now = Calendar.getInstance()
        val initialCal = Calendar.getInstance().apply {
            if (reminderAt != null) {
                timeInMillis = reminderAt!!
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
            }
        }
        TimePickerDialog(
            context,
            { _, hour, minute ->
                val baseCal = Calendar.getInstance().apply {
                    if (dueDate != null) timeInMillis = dueDate!!
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                if (baseCal.timeInMillis <= now.timeInMillis) {
                    baseCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                applyReminderTimestamp(baseCal.timeInMillis)
            },
            initialCal.get(Calendar.HOUR_OF_DAY),
            initialCal.get(Calendar.MINUTE),
            false
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.softBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Task" else "New Task",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isEditMode) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    showTemplatesDialog = true
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp)
                                .testTag("open_task_templates_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BookmarkBorder,
                                    contentDescription = "Templates",
                                    tint = colors.accent,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Templates",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primaryInk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (title.isNotBlank()) colors.accent else colors.canvas)
                            .border(
                                1.dp,
                                if (title.isNotBlank()) colors.accent else colors.border,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable(enabled = title.isNotBlank()) { submitTaskNow() }
                            .padding(horizontal = 13.dp, vertical = 6.dp)
                            .testTag("quick_instant_save_task_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = "Quick Save",
                                tint = if (title.isNotBlank()) Color.White else colors.secondaryInk,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isEditMode) "Save" else "Quick Save",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (title.isNotBlank()) Color.White else colors.secondaryInk,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Unified Mobile Composer Card (Title + Notes cleanly grouped like flagship apps)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = colors.primaryInk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.5.sp
                        ),
                        cursorBrush = SolidColor(colors.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { submitTaskNow() }),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(titleFocusRequester)
                            .testTag("create_task_title_input"),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (title.isEmpty()) {
                                    Text(
                                        text = "What needs to be done?",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = colors.secondaryInk,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.5.sp
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )

                    IconButton(
                        onClick = { submitTaskNow() },
                        enabled = title.isNotBlank(),
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (title.isNotBlank()) colors.accent else colors.tileBg)
                            .testTag("inline_title_quick_add_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save Task",
                            tint = if (title.isNotBlank()) Color.White else colors.mutedInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = description,
                    onValueChange = { description = it },
                    singleLine = false,
                    minLines = 2,
                    maxLines = 4,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.primaryInk,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp
                    ),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_task_notes_input"),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Add notes, links, or context (optional)...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.secondaryInk,
                                        fontSize = 13.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Quick Due Date Chips
            Text(
                text = "DUE DATE",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            QuickDateChipsRow(
                selectedDueDate = dueDate,
                onDueDateChange = { newDue ->
                    dueDate = newDue
                    if (newDue != null && reminderAt != null) {
                        val remCal = Calendar.getInstance().apply { timeInMillis = reminderAt!! }
                        val dueCal = Calendar.getInstance().apply {
                            timeInMillis = newDue
                            set(Calendar.HOUR_OF_DAY, remCal.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, remCal.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        reminderAt = dueCal.timeInMillis
                    }
                },
                onCustomDateClick = { showDatePicker() }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Exact Task Reminder Time Bar (1-Tap Presets + Custom Time Picker)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = if (reminderAt != null) colors.accent else colors.secondaryInk,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "REMINDER ALERT",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (reminderAt != null) colors.accent else colors.secondaryInk,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        fontSize = 10.sp
                    )
                }

                if (reminderAt != null) {
                    Text(
                        text = "Clear",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.danger,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                reminderAt = null
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (reminderAt != null) {
                val remLabel = remember(reminderAt) {
                    val ts = reminderAt!!
                    val timePart = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ts))
                    val dayPart = when {
                        LocalDayUtils.isToday(ts) -> "Today"
                        LocalDayUtils.isTomorrow(ts) -> "Tomorrow"
                        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(ts))
                    }
                    "$dayPart at $timePart"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.accentSoft)
                        .border(1.dp, colors.accentBorder, RoundedCornerShape(12.dp))
                        .clickable { showReminderTimePicker() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("active_reminder_time_pill"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = remLabel,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val quickReminders = listOf(
                        "In 1 hr" to {
                            System.currentTimeMillis() + 60 * 60_000L
                        },
                        "6:00 PM" to {
                            Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
                            }.timeInMillis
                        },
                        "Tomorrow 9 AM" to {
                            Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }
                    )

                    quickReminders.forEach { (label, tsProvider) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    applyReminderTimestamp(tsProvider())
                                }
                                .padding(horizontal = 11.dp, vertical = 7.dp)
                                .testTag("reminder_preset_${label.lowercase().replace(" ", "_")}")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accentSoft)
                            .border(1.dp, colors.accentBorder, RoundedCornerShape(10.dp))
                            .clickable { showReminderTimePicker() }
                            .padding(horizontal = 11.dp, vertical = 7.dp)
                            .testTag("reminder_pick_custom_time")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = "Pick Reminder Time",
                                tint = colors.accent,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Pick Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Priority Selector
            Text(
                text = "PRIORITY LEVEL",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                TaskPriority.entries.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val animatedBg by animateColorAsState(
                        targetValue = if (isSelected) priority.badgeBackground(colors) else Color.Transparent,
                        label = "priorityBg"
                    )
                    val labelColor = if (isSelected) priority.badgeTextColor(colors) else colors.secondaryInk
                    val dotColor = if (isSelected) {
                        if (priority == TaskPriority.HIGH) priority.badgeTextColor(colors)
                        else priority.indicatorColor(colors)
                    } else priority.indicatorColor(colors)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(animatedBg)
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedPriority = priority
                            }
                            .padding(vertical = 8.dp)
                            .testTag("priority_selector_${priority.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = priority.indicatorSymbol,
                                style = MaterialTheme.typography.labelMedium,
                                color = dotColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = priority.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = labelColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Recurrence Selector (Once, Daily, Weekly, Monthly)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = null,
                    tint = colors.secondaryInk,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "REPEAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TaskRecurrence.entries.forEach { rec ->
                    val isSelected = selectedRecurrence == rec
                    val bg by animateColorAsState(
                        targetValue = if (isSelected) colors.accent else Color.Transparent,
                        label = "recBg"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(bg)
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedRecurrence = rec
                            }
                            .padding(vertical = 8.dp)
                            .testTag("recurrence_selector_${rec.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (rec == TaskRecurrence.NONE) "Once" else rec.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) Color.White else colors.secondaryInk,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 6. Category Selector
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    val style = CategoryTheme.getStyle(category, colors.isDark)
                    val selectedBg = colors.primaryInk
                    val selectedInk = colors.canvas

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) selectedBg else style.backgroundColor)
                            .border(
                                1.dp,
                                if (isSelected) selectedBg else style.borderColor,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                selectedCategory = category
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("create_category_chip_${category.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) selectedInk else style.accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) selectedInk else style.textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (onAddCustomCategory != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.canvas)
                            .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                showAddCategoryDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("sheet_add_custom_category_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Category",
                                tint = colors.accent,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Custom",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 7. Subtasks Checklist
            Text(
                text = "SUBTASKS (${subtasksList.size})",
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            subtasksList.forEachIndexed { index, subtask ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (subtask.isCompleted) colors.accent else colors.secondaryInk)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (subtask.isCompleted) colors.mutedInk else colors.primaryInk
                        )
                    }
                    IconButton(
                        onClick = { subtasksList.removeAt(index) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove subtask",
                            tint = colors.secondaryInk,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompactFormTextField(
                    value = newSubtaskInput,
                    onValueChange = { newSubtaskInput = it },
                    placeholder = "Add subtask step (press Next to keep adding)...",
                    singleLine = true,
                    imeAction = ImeAction.Next,
                    onImeAction = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasksList.add(SubTask(UUID.randomUUID().toString(), newSubtaskInput.trim(), false))
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (newSubtaskInput.isNotBlank()) {
                            subtasksList.add(SubTask(UUID.randomUUID().toString(), newSubtaskInput.trim(), false))
                            newSubtaskInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (newSubtaskInput.isNotBlank()) colors.accent else colors.canvas)
                        .border(
                            1.dp,
                            if (newSubtaskInput.isNotBlank()) colors.accent else colors.border,
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add subtask",
                        tint = if (newSubtaskInput.isNotBlank()) Color.White else colors.secondaryInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 8. Submit Button
            Button(
                onClick = { submitTaskNow() },
                enabled = title.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = if (title.isNotBlank() && !colors.isDark) 5.dp else 0.dp,
                        shape = RoundedCornerShape(26.dp),
                        spotColor = colors.accent.copy(alpha = 0.35f)
                    )
                    .testTag("submit_create_task_btn"),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                    disabledContainerColor = colors.softBorder,
                    disabledContentColor = colors.secondaryInk
                )
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Save Changes" else "Create Task",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (onSaveAsTemplate != null && title.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.canvas)
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            onSaveAsTemplate(
                                title.trim(),
                                selectedCategory,
                                selectedPriority,
                                subtasksList.map { it.title }
                            )
                        }
                        .padding(vertical = 10.dp)
                        .testTag("save_as_template_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BookmarkBorder,
                            contentDescription = "Save as Template",
                            tint = colors.accent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save as Reusable Template",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.primaryInk,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showTemplatesDialog) {
        val allPickerTemplates = remember(savedTemplates) {
            savedTemplates + DefaultRoutineTemplates.starters
        }
        AlertDialog(
            onDismissRequest = { showTemplatesDialog = false },
            containerColor = colors.surface,
            title = {
                Text(
                    text = "Task Templates",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tap any routine checklist to pre-fill this task in 1 tap.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.secondaryInk
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    allPickerTemplates.forEach { tpl ->
                        val isCustom = tpl.id > 0L
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    title = tpl.title
                                    selectedCategory = tpl.category
                                    selectedPriority = tpl.priority
                                    subtasksList.clear()
                                    tpl.subtaskTitles.forEach { st ->
                                        subtasksList.add(SubTask(UUID.randomUUID().toString(), st, false))
                                    }
                                    showTemplatesDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tpl.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.primaryInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${tpl.category} • ${tpl.subtaskTitles.size} steps" + if (!isCustom) " • Starter" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk
                                )
                            }
                            if (isCustom && onDeleteTemplate != null) {
                                IconButton(
                                    onClick = { onDeleteTemplate(tpl) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Template",
                                        tint = colors.danger,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatesDialog = false }) {
                    Text("Close", color = colors.secondaryInk)
                }
            }
        )
    }

    if (showAddCategoryDialog && onAddCustomCategory != null) {
        val paletteOptions = listOf(
            "#2563EB", "#16A34A", "#D97706", "#DC2626",
            "#7C3AED", "#0D9488", "#E11D48", "#4B5563"
        )
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                newCategoryName = ""
            },
            title = {
                Text(
                    text = "Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryInk
                )
            },
            text = {
                Column {
                    CompactFormTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = "e.g. College, Side Project, Fitness",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        paletteOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedCategoryColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) colors.primaryInk else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedCategoryColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newCategoryName.trim()
                        if (clean.isNotBlank()) {
                            onAddCustomCategory(clean, selectedCategoryColorHex)
                            selectedCategory = clean
                            newCategoryName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create & Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            },
            containerColor = colors.surface
        )
    }
}

/**
 * Sleek, compact text input field designed for mobile density and full 3-theme adaptation.
 */
@Composable
fun CompactFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 3,
    fontWeight: FontWeight = FontWeight.Normal,
    isError: Boolean = false,
    imeAction: ImeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    val colors = MonoDoThemeTokens.colors
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = when {
        isError -> colors.danger
        isFocused -> colors.accent
        else -> colors.border
    }
    val containerColor = if (isFocused) colors.focusedInput else colors.canvas

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            color = colors.primaryInk,
            fontWeight = fontWeight,
            fontSize = 14.sp,
            lineHeight = 19.sp
        ),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(
            onDone = { onImeAction?.invoke() },
            onSend = { onImeAction?.invoke() },
            onGo = { onImeAction?.invoke() }
        ),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.secondaryInk,
                            fontSize = 13.5.sp
                        )
                    )
                }
                innerTextField()
            }
        }
    )
}
