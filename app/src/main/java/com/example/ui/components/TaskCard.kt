package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.theme.CompletedText
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.LightPill
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.theme.SoftBorderGrey
import com.example.ui.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleCompletion: () -> Unit,
    onToggleSubtask: (subtaskId: String) -> Unit,
    onAddSubtask: (title: String) -> Unit,
    onDeleteSubtask: (subtaskId: String) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    val completedSubtasksCount = task.subtasks.count { it.isCompleted }
    val totalSubtasksCount = task.subtasks.size
    val subtaskProgress = if (totalSubtasksCount > 0) {
        completedSubtasksCount.toFloat() / totalSubtasksCount.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = subtaskProgress,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "subtaskProgress"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = if (task.isCompleted) HairlineBorder else Color(0xFFDCDCD7),
        label = "cardBorder"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PaperSurface)
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("task_card_${task.id}")
    ) {
        // Main task row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Spring animated checkbox with tactile haptic confirmation
            MonochromeCheckbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    HapticFeedbackHelper.performTaskCompletionHaptic(view, hapticFeedback)
                    onToggleCompletion()
                },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { isExpanded = !isExpanded }
            ) {
                // Task Title with animated strikethrough & color change
                val titleColor by animateColorAsState(
                    targetValue = if (task.isCompleted) CompletedText else ObsidianBlack,
                    animationSpec = spring(),
                    label = "titleColor"
                )

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.isCompleted) CompletedText else SlateGraphite,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Badges (Priority, Category, Due Date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Pill
                    PriorityBadge(priority = task.priority)

                    // Category Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(LightPill)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    }

                    // Tags Pills
                    task.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaperCanvas)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (tag.startsWith("#")) tag else "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Medium,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Due Date if present
                    if (task.dueDate != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(PaperCanvas)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Due Date",
                                tint = SlateGraphite,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatDueDate(task.dueDate),
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            // Quick actions (expand or delete)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (totalSubtasksCount > 0) {
                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand Subtasks",
                            tint = SlateGraphite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDeleteTask,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_task_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Task",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Subtasks Progress Bar (always visible if subtasks exist)
        if (totalSubtasksCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ObsidianBlack,
                    trackColor = HairlineBorder,
                    strokeCap = StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "$completedSubtasksCount/$totalSubtasksCount subtasks",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Expandable Subtasks List & Inline Subtask Creator
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 4.dp)
            ) {
                // Divider line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HairlineBorder)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Subtask Items
                task.subtasks.forEach { subtask ->
                    SubtaskItemRow(
                        subtask = subtask,
                        onToggle = { onToggleSubtask(subtask.id) },
                        onDelete = { onDeleteSubtask(subtask.id) }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Inline Subtask Add input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = {
                            Text(
                                "Add a subtask...",
                                style = MaterialTheme.typography.bodySmall,
                                color = SlateGraphite
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("new_subtask_input_${task.id}"),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(color = ObsidianBlack),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack,
                            cursorColor = ObsidianBlack,
                            focusedBorderColor = SoftBorderGrey,
                            unfocusedBorderColor = HairlineBorder,
                            focusedContainerColor = FocusedBackground,
                            unfocusedContainerColor = PaperCanvas
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (newSubtaskText.isNotBlank()) {
                                onAddSubtask(newSubtaskText)
                                newSubtaskText = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                onAddSubtask(newSubtaskText)
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ObsidianBlack)
                            .testTag("add_subtask_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subtask",
                            tint = PaperSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MonochromeCheckbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "checkScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (checked) ObsidianBlack else Color.Transparent,
        animationSpec = spring(),
        label = "checkBg"
    )

    val borderColor = if (checked) ObsidianBlack else Color(0xFF404040)

    Box(
        modifier = modifier
            .size(24.dp)
            .scale(scale)
            .clip(RoundedCornerShape(7.dp))
            .background(bgColor)
            .border(2.dp, borderColor, RoundedCornerShape(7.dp))
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Checked",
                tint = PaperSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SubtaskItemRow(
    subtask: SubTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Mini checkbox
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(if (subtask.isCompleted) ObsidianBlack else Color.Transparent)
                .border(
                    1.5.dp,
                    if (subtask.isCompleted) ObsidianBlack else SlateGraphite,
                    RoundedCornerShape(5.dp)
                )
                .clickable {
                    HapticFeedbackHelper.performSubtaskToggleHaptic(view, hapticFeedback)
                    onToggle()
                },
            contentAlignment = Alignment.Center
        ) {
            if (subtask.isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Subtask Completed",
                    tint = PaperSurface,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = subtask.title,
            style = MaterialTheme.typography.bodySmall,
            color = if (subtask.isCompleted) CompletedText else ObsidianBlack,
            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier
                .weight(1f)
                .clickable {
                    HapticFeedbackHelper.performSubtaskToggleHaptic(view, hapticFeedback)
                    onToggle()
                }
        )

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete Subtask",
                tint = SlateGraphite,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun PriorityBadge(priority: TaskPriority) {
    val (bgColor, textColor, border) = when (priority) {
        TaskPriority.URGENT -> Triple(ObsidianBlack, PaperSurface, null)
        TaskPriority.HIGH -> Triple(PaperSurface, ObsidianBlack, HairlineBorder)
        TaskPriority.MEDIUM -> Triple(LightPill, ObsidianBlack, null)
        TaskPriority.LOW -> Triple(PaperCanvas, SlateGraphite, HairlineBorder)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .then(if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(6.dp)) else Modifier)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            letterSpacing = 0.5.sp
        )
    }
}

private fun formatDueDate(timeMillis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
    val today = Calendar.getInstance()
    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

    return when {
        cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"

        cal.get(Calendar.YEAR) == tomorrow.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == tomorrow.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"

        else -> {
            val format = SimpleDateFormat("MMM d", Locale.getDefault())
            format.format(Date(timeMillis))
        }
    }
}
