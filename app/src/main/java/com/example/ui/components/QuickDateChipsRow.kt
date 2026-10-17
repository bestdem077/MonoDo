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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
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
            QuickDeadlinePreset.TODAY -> {
                // Keep today's date
            }
            QuickDeadlinePreset.TOMORROW -> {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
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
        val c1 = Calendar.getInstance().apply { timeInMillis = timestampA }
        val c2 = Calendar.getInstance().apply { timeInMillis = timestampB }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
}

/**
 * Row of 'Today', 'Tomorrow', 'This Weekend', and 'Next Week' clickable chips
 * placed directly below the task input field to quickly set task deadlines.
 */
@Composable
fun QuickDateChipsRow(
    selectedDueDate: Long?,
    onDueDateChange: (Long?) -> Unit,
    onCustomDateClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .testTag("quick_date_chips_row"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickDeadlinePreset.entries.forEach { preset ->
            val targetTimestamp = remember(preset) {
                QuickDateCalculator.getDeadlineTimestamp(preset)
            }
            val isSelected = QuickDateCalculator.isSameCalendarDay(selectedDueDate, targetTimestamp)

            val bgColor by animateColorAsState(
                targetValue = if (isSelected) RoyalBlue else PaperCanvas,
                animationSpec = spring(),
                label = "quickChipBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) RoyalBlue else HairlineBorder,
                animationSpec = spring(),
                label = "quickChipBorder"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else ObsidianBlack,
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
                        if (isSelected) {
                            onDueDateChange(null)
                        } else {
                            onDueDateChange(freshTimestamp)
                        }
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
                    .background(if (isCustomSelected) RoyalBlueSoft else PaperCanvas)
                    .border(
                        1.dp,
                        if (isCustomSelected) RoyalBlue else HairlineBorder,
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
                        tint = if (selectedDueDate != null) RoyalBlue else SlateGraphite,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (selectedDueDate != null) sdf.format(Date(selectedDueDate)) else "Custom",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedDueDate != null) RoyalBlue else SlateGraphite,
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
                    .size(26.dp)
                    .testTag("clear_quick_date_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Date",
                    tint = SlateGraphite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Inline Quick-Add Task Card for the main Tasks screen featuring the task input field
 * and the row of 'Today', 'Tomorrow', 'This Weekend', and 'Next Week' clickable chips
 * directly below the input field.
 */
@Composable
fun InlineQuickAddTaskBar(
    selectedCategory: String,
    onQuickCreateTask: (title: String, dueDate: Long?, category: String) -> Unit,
    modifier: Modifier = Modifier
) {
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
            .background(PaperSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("inline_quick_add_task_bar")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = quickTitle,
                onValueChange = { quickTitle = it },
                placeholder = {
                    Text(
                        text = "Quick add a task...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGraphite
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("inline_task_input_field"),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = ObsidianBlack,
                    fontWeight = FontWeight.SemiBold
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = ObsidianBlack,
                    unfocusedTextColor = ObsidianBlack,
                    cursorColor = RoyalBlue,
                    focusedBorderColor = RoyalBlue,
                    unfocusedBorderColor = HairlineBorder,
                    focusedContainerColor = FocusedBackground,
                    unfocusedContainerColor = PaperCanvas
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submitQuickTask() })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = submitQuickTask,
                enabled = quickTitle.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (quickTitle.isNotBlank()) RoyalBlue else PaperCanvas)
                    .border(
                        1.dp,
                        if (quickTitle.isNotBlank()) RoyalBlue else HairlineBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("inline_quick_add_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    tint = if (quickTitle.isNotBlank()) Color.White else SlateGraphite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Row of 'Today', 'Tomorrow', 'This Weekend', and 'Next Week' clickable chips below the task input field
        QuickDateChipsRow(
            selectedDueDate = quickDueDate,
            onDueDateChange = { quickDueDate = it }
        )
    }
}
