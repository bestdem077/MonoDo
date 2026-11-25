package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Locale

data class DayColumnInfo(
    val epochDay: Long,
    val dayAbbr: String,
    val dayNumber: String,
    val isToday: Boolean
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitDotMatrixView(
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    onToggleHabitDay: (habitId: Long, epochDay: Long) -> Unit,
    onAddHabit: (title: String, colorHex: String) -> Unit,
    onDeleteHabit: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateHabit: ((habit: HabitEntity, title: String, colorHex: String) -> Unit)? = null,
    getHabitStreaks: ((habitId: Long) -> Pair<Int, Int>)? = null
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var isExpanded by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var habitToManage by remember { mutableStateOf<HabitEntity?>(null) }

    // Timezone-safe local calendar day keys (fixes IST 12 AM - 5:30 AM off-by-one-day bug!)
    val todayLocalEpochDay = LocalDayUtils.toLocalEpochDay()
    val dayColumns = remember(todayLocalEpochDay) {
        val abbrFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val numFormat = SimpleDateFormat("d", Locale.getDefault())
        (6 downTo 0).map { offset ->
            val targetDay = todayLocalEpochDay - offset
            val cal = LocalDayUtils.calendarForLocalEpochDay(targetDay)
            DayColumnInfo(
                epochDay = targetDay,
                dayAbbr = abbrFormat.format(cal.time).take(1).uppercase(),
                dayNumber = numFormat.format(cal.time),
                isToday = offset == 0
            )
        }
    }

    val colorOptions = listOf(
        "#2563EB", "#16A34A", "#D97706", "#DC2626",
        "#7C3AED", "#0D9488", "#E11D48", "#4B5563"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("habit_dot_matrix_view")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HABIT MATRIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${habits.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.secondaryInk,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Habits",
                    tint = colors.secondaryInk,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    showAddDialog = true
                },
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(colors.canvas)
                    .border(1.dp, colors.border, CircleShape)
                    .testTag("add_habit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = colors.primaryInk,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Column headers for days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dayColumns.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(26.dp)
                            ) {
                                Text(
                                    text = day.dayAbbr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) colors.accent else colors.secondaryInk,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    ),
                                    color = if (day.isToday) colors.primaryInk else colors.secondaryInk,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (habits.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SwissEmptyStateArt(
                            type = SwissEmptyArtType.HABITS,
                            size = 56.dp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Track daily consistency • Tap + or pick a quick habit:",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryInk
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickHabits = listOf(
                                "Read 20m" to "#2563EB",
                                "Deep Work 2h" to "#4B5563",
                                "Workout" to "#16A34A",
                                "Meditate" to "#7C3AED"
                            )
                            quickHabits.forEach { (presetTitle, presetColor) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(colors.canvas)
                                        .border(1.dp, colors.border, RoundedCornerShape(999.dp))
                                        .clickable {
                                            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                                            onAddHabit(presetTitle, presetColor)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("quick_habit_preset_${presetTitle.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = "+ $presetTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.primaryInk,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.5.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    habits.forEach { habit ->
                        val habitColor = try {
                            Color(android.graphics.Color.parseColor(habit.colorHex))
                        } catch (_: Exception) {
                            colors.accent
                        }
                        val streaks = getHabitStreaks?.invoke(habit.id) ?: (0 to 0)
                        val currentStreak = streaks.first

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Habit Title + Current Streak badge (opens Manage/Edit dialog on tap)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                        habitToManage = habit
                                    }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(habitColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = habit.title,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.primaryInk,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (currentStreak > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 13.dp, top = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = habitColor,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "${currentStreak}d streak",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = colors.secondaryInk,
                                            fontSize = 9.5.sp
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 7 Day Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                dayColumns.forEach { day ->
                                    val isCompleted = habitLogs.any {
                                        it.habitId == habit.id && it.dateEpochDay == day.epochDay && it.isCompleted
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isCompleted -> habitColor
                                                    day.isToday -> colors.accentSoft
                                                    else -> colors.canvas
                                                }
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isCompleted -> habitColor
                                                    day.isToday -> colors.accent
                                                    else -> colors.border
                                                },
                                                CircleShape
                                            )
                                            .clickable {
                                                HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                                                onToggleHabitDay(habit.id, day.epochDay)
                                            }
                                            .testTag("habit_dot_${habit.id}_${day.epochDay}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#2563EB") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    "New Daily Habit",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryInk
                )
            },
            text = {
                Column {
                    CompactFormTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = "Habit name (e.g. Read 20m, Workout)",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Accent Color",
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
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex

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
                                    .clickable { selectedColorHex = hex },
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
                        if (newTitle.isNotBlank()) {
                            onAddHabit(newTitle.trim(), selectedColorHex)
                            showAddDialog = false
                        }
                    },
                    enabled = newTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Habit")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            },
            containerColor = colors.surface
        )
    }

    // Manage / Edit Habit Dialog (replaces accidental tap-to-delete popup!)
    habitToManage?.let { habit ->
        var editTitle by remember(habit.id) { mutableStateOf(habit.title) }
        var editColorHex by remember(habit.id) { mutableStateOf(habit.colorHex) }
        val (currentStreak, bestStreak) = getHabitStreaks?.invoke(habit.id) ?: (0 to 0)

        AlertDialog(
            onDismissRequest = { habitToManage = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Manage Habit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryInk
                    )
                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performDeleteHaptic(view, haptic)
                            onDeleteHabit(habit.id)
                            habitToManage = null
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Habit",
                            tint = colors.danger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            text = {
                Column {
                    // Streak summary pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${currentStreak}d",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Current Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Text(
                                    text = "${bestStreak}d",
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = colors.primaryInk,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Best Streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    CompactFormTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = "Habit name",
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        "Accent Color",
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
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = editColorHex.equals(hex, ignoreCase = true)

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
                                    .clickable { editColorHex = hex },
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
                        if (editTitle.isNotBlank()) {
                            onUpdateHabit?.invoke(habit, editTitle.trim(), editColorHex)
                            habitToManage = null
                        }
                    },
                    enabled = editTitle.isNotBlank(),
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
                TextButton(onClick = { habitToManage = null }) {
                    Text("Cancel", color = colors.secondaryInk)
                }
            },
            containerColor = colors.surface
        )
    }
}
