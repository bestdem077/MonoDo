package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.data.model.TaskEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Clean Swiss Paper Monthly Calendar uniting Tasks and Friends/Family Birthdays on one grid.
 */
@Composable
fun MonthlyCalendarView(
    allTasks: List<TaskEntity>,
    allBirthdays: List<BirthdayEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onAddTaskForDate: (Long) -> Unit,
    onOpenBirthdays: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val todayCal = remember { Calendar.getInstance() }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) // 0-indexed
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var displayedYear by remember { mutableIntStateOf(todayYear) }
    var displayedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    val monthTitle = remember(displayedYear, displayedMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.time)
    }

    val daysInMonth = remember(displayedYear, displayedMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        c.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeekOffset = remember(displayedYear, displayedMonth) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6
    }

    // Map day-of-month -> tasks due on that day in displayedYear/displayedMonth
    val tasksByDay = remember(allTasks, displayedYear, displayedMonth) {
        val map = mutableMapOf<Int, MutableList<TaskEntity>>()
        allTasks.forEach { task ->
            val due = task.dueDate ?: return@forEach
            val c = Calendar.getInstance().apply { timeInMillis = due }
            if (c.get(Calendar.YEAR) == displayedYear && c.get(Calendar.MONTH) == displayedMonth) {
                val d = c.get(Calendar.DAY_OF_MONTH)
                map.getOrPut(d) { mutableListOf() }.add(task)
            }
        }
        map
    }

    // Map day-of-month -> birthdays in displayedMonth (1-indexed in BirthdayEntity!)
    val birthdaysByDay = remember(allBirthdays, displayedMonth) {
        val monthOneBased = displayedMonth + 1
        allBirthdays
            .filter { it.month == monthOneBased }
            .groupBy { it.day }
    }

    val clampedSelectedDay = selectedDay.coerceIn(1, daysInMonth)
    val selectedDateMillis = remember(displayedYear, displayedMonth, clampedSelectedDay) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, clampedSelectedDay)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val selectedDateLabel = remember(displayedYear, displayedMonth, clampedSelectedDay) {
        val c = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, clampedSelectedDay)
        }
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(c.time)
    }

    val dayTasks = tasksByDay[clampedSelectedDay].orEmpty()
        .sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenByDescending { it.priority.ordinal })
    val dayBirthdays = birthdaysByDay[clampedSelectedDay].orEmpty()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_calendar_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Calendar Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            // Month Header Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = monthTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    if (displayedYear != todayYear || displayedMonth != todayMonth) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(colors.accentSoft)
                                .border(1.dp, colors.accentBorder, RoundedCornerShape(999.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    displayedYear = todayYear
                                    displayedMonth = todayMonth
                                    selectedDay = todayDay
                                }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.canvas)
                            .border(1.dp, colors.border, CircleShape)
                            .testTag("calendar_prev_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = colors.primaryInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                            selectedDay = 1
                        },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.canvas)
                            .border(1.dp, colors.border, CircleShape)
                            .testTag("calendar_next_month")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = colors.primaryInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday Header Row
            val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekdays.forEach { dayLabel ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Calendar Grid Rows
            val totalCells = firstDayOfWeekOffset + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstDayOfWeekOffset + 1
                            if (dayNum < 1 || dayNum > daysInMonth) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isToday = displayedYear == todayYear &&
                                    displayedMonth == todayMonth &&
                                    dayNum == todayDay
                                val isSelected = dayNum == clampedSelectedDay
                                val cellTasks = tasksByDay[dayNum].orEmpty()
                                val hasActiveTask = cellTasks.any { !it.isCompleted }
                                val hasOverdueTask = cellTasks.any { !it.isCompleted && LocalDayUtils.isOverdue(it.dueDate) }
                                val hasCompletedOnly = cellTasks.isNotEmpty() && !hasActiveTask
                                val hasBirthday = birthdaysByDay[dayNum].orEmpty().isNotEmpty()

                                val cellBg = when {
                                    isSelected -> colors.accent
                                    isToday -> colors.accentSoft
                                    else -> Color.Transparent
                                }
                                val cellBorder = when {
                                    isSelected -> colors.accent
                                    isToday -> colors.accentBorder
                                    else -> Color.Transparent
                                }
                                val textColor = when {
                                    isSelected -> Color.White
                                    isToday -> colors.accent
                                    else -> colors.primaryInk
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cellBg)
                                        .border(1.dp, cellBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                            selectedDay = dayNum
                                        }
                                        .testTag("calendar_day_$dayNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = JetBrainsMonoFontFamily
                                            ),
                                            color = textColor,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp
                                        )
                                        if (hasActiveTask || hasCompletedOnly || hasBirthday) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                if (hasActiveTask || hasCompletedOnly) {
                                                    val dotColor = when {
                                                        isSelected -> Color.White
                                                        hasOverdueTask -> colors.danger
                                                        hasActiveTask -> colors.accent
                                                        else -> colors.mutedInk
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.5.dp)
                                                            .clip(CircleShape)
                                                            .background(dotColor)
                                                    )
                                                }
                                                if (hasBirthday) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(4.5.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) Color.White.copy(alpha = 0.85f) else colors.success)
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

            Spacer(modifier = Modifier.height(10.dp))

            // Legend Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendDot(color = colors.accent, label = "Tasks")
                LegendDot(color = colors.danger, label = "Overdue")
                LegendDot(color = colors.success, label = "Birthdays")
            }
        }

        // Selected Day Agenda Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = selectedDateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dayTasks.size} ${if (dayTasks.size == 1) "task" else "tasks"}" +
                            if (dayBirthdays.isNotEmpty()) " • ${dayBirthdays.size} ${if (dayBirthdays.size == 1) "birthday" else "birthdays"}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accent)
                        .clickable {
                            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                            onAddTaskForDate(selectedDateMillis)
                        }
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                        .testTag("calendar_add_task_for_day"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add task for day",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add Task",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Birthdays on Selected Day
            if (dayBirthdays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                dayBirthdays.forEach { bday ->
                    val turningAge = BirthdayDateCalculator.turningAge(bday.month, bday.day, bday.birthYear)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.successSoft)
                            .border(1.dp, colors.successBorder, RoundedCornerShape(12.dp))
                            .clickable { onOpenBirthdays() }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cake,
                            contentDescription = "Birthday",
                            tint = colors.success,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${bday.name}'s Birthday",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = buildString {
                                    append(bday.relationship)
                                    if (turningAge != null) append(" • Turns $turningAge")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.secondaryInk
                            )
                        }
                        Text(
                            text = "Wish →",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.success,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Tasks on Selected Day
            if (dayTasks.isEmpty() && dayBirthdays.isEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "No tasks or birthdays scheduled on this day.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryInk,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else if (dayTasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    dayTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable { onEditTask(task) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (task.isCompleted) colors.primaryInk else colors.surface)
                                    .border(
                                        1.2.dp,
                                        if (task.isCompleted) colors.primaryInk else colors.softBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable {
                                        HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
                                        onToggleTask(task)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (task.isCompleted) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Done",
                                        tint = if (colors.isDark) colors.canvas else Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (task.isCompleted) colors.mutedInk else colors.primaryInk,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${task.category} • ${task.priority.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.secondaryInk,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    val colors = MonoDoThemeTokens.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.secondaryInk,
            fontSize = 10.sp
        )
    }
}
