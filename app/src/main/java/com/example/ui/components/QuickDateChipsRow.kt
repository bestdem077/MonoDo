package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class QuickDeadlinePreset(val label: String, val tagSuffix: String) {
    TODAY("Today", "today"),
    TOMORROW("Tomorrow", "tomorrow"),
    THIS_WEEKEND("This Weekend", "this_weekend"),
    NEXT_WEEK("Next Week", "next_week")
}

object QuickDateCalculator {
    fun getDeadlineTimestamp(preset: QuickDeadlinePreset, baseTimeMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = baseTimeMillis
        }
        when (preset) {
            QuickDeadlinePreset.TODAY -> Unit
            QuickDeadlinePreset.TOMORROW -> cal.add(Calendar.DAY_OF_YEAR, 1)
            QuickDeadlinePreset.THIS_WEEKEND -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysUntilSaturday = (Calendar.SATURDAY - dayOfWeek + 7) % 7
                cal.add(Calendar.DAY_OF_YEAR, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
            }
            QuickDeadlinePreset.NEXT_WEEK -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysUntilMonday = ((Calendar.MONDAY - dayOfWeek + 7) % 7).let { if (it == 0) 7 else it }
                cal.add(Calendar.DAY_OF_YEAR, daysUntilMonday)
            }
        }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isSameCalendarDay(timestampA: Long?, timestampB: Long?): Boolean {
        if (timestampA == null || timestampB == null) return false
        return LocalDayUtils.toLocalEpochDay(timestampA) == LocalDayUtils.toLocalEpochDay(timestampB)
    }
}

@Composable
fun QuickDateChipsRow(
    selectedDueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onCustomDateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()
    val todayKey = LocalDayUtils.toLocalEpochDay()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .testTag("quick_date_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickDeadlinePreset.entries.forEach { preset ->
            val targetTimestamp = remember(preset, todayKey) {
                QuickDateCalculator.getDeadlineTimestamp(preset)
            }
            val isSelected = QuickDateCalculator.isSameCalendarDay(selectedDueDate, targetTimestamp)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) colors.accent else colors.canvas,
                animationSpec = spring(),
                label = "quickChipBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) colors.accent else colors.border,
                animationSpec = spring(),
                label = "quickChipBorder"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else colors.primaryInk,
                animationSpec = spring(),
                label = "quickChipText"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(999.dp))
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        val freshTimestamp = QuickDateCalculator.getDeadlineTimestamp(preset)
                        if (isSelected) onDueDateChange(null) else onDueDateChange(freshTimestamp)
                    }
                    .padding(horizontal = 13.dp, vertical = 7.dp)
                    .testTag("quick_date_chip_${preset.tagSuffix}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = preset.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }

        if (onCustomDateClick != null) {
            val isCustomSelected = selectedDueDate != null && QuickDeadlinePreset.entries.none { preset ->
                QuickDateCalculator.isSameCalendarDay(
                    selectedDueDate,
                    QuickDateCalculator.getDeadlineTimestamp(preset)
                )
            }
            val sdf = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isCustomSelected) colors.accentSoft else colors.canvas)
                    .border(
                        1.dp,
                        if (isCustomSelected) colors.accent else colors.border,
                        RoundedCornerShape(999.dp)
                    )
                    .clickable {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        onCustomDateClick()
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
                    .testTag("custom_calendar_picker_chip"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Pick Custom Date",
                        tint = if (selectedDueDate != null) colors.accent else colors.secondaryInk,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (selectedDueDate != null) sdf.format(Date(selectedDueDate)) else "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedDueDate != null) colors.accent else colors.secondaryInk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (selectedDueDate != null) {
            IconButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    onDueDateChange(null)
                },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("clear_quick_date_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Date",
                    tint = colors.secondaryInk,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun InlineQuickAddTaskBar(
    selectedCategory: String,
    onQuickCreateTask: (title: String, dueDate: Long?, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var quickTitle by remember { mutableStateOf("") }
    var quickDueDate by remember {
        mutableStateOf<Long?>(QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY))
    }

    val submitQuickTask = {
        val trimmed = quickTitle.trim()
        if (trimmed.isNotEmpty()) {
            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
            val targetCategory = if (selectedCategory.equals("All", ignoreCase = true)) "Work" else selectedCategory
            onQuickCreateTask(trimmed, quickDueDate, targetCategory)
            quickTitle = ""
            focusManager.clearFocus()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("inline_quick_add_task_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompactFormTextField(
                value = quickTitle,
                onValueChange = { quickTitle = it },
                placeholder = "Quick add a task...",
                singleLine = true,
                fontWeight = FontWeight.SemiBold,
                imeAction = ImeAction.Done,
                onImeAction = { submitQuickTask() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("inline_task_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = submitQuickTask,
                enabled = quickTitle.isNotBlank(),
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (quickTitle.isNotBlank()) colors.accent else colors.canvas)
                    .border(
                        1.dp,
                        if (quickTitle.isNotBlank()) colors.accent else colors.border,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("inline_quick_add_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = if (quickTitle.isNotBlank()) Color.White else colors.secondaryInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        QuickDateChipsRow(
            selectedDueDate = quickDueDate,
            onDueDateChange = { quickDueDate = it }
        )
    }
}
