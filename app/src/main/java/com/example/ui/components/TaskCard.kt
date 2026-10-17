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
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.CompletedText
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.FocusedBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: TaskEntity,
    onToggleCompletion: () -> Unit,
    onToggleSubtask: (subtaskId: String) -> Unit,
    onAddSubtask: (title: String) -> Unit,
    onDeleteSubtask: (subtaskId: String) -> Unit,
    onDeleteTask: () -> Unit,
    modifier: Modifier = Modifier,
    onCategoryClick: ((String) -> Unit)? = null,
    onStartFocus: (() -> Unit)? = null,
    onRescheduleTask: ((Long?) -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    var isQuickActionsVisible by remember { mutableStateOf(false) }
    var newSubtaskText by remember { mutableStateOf("") }
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val swipeOffsetX = remember(task.id) { Animatable(0f) }
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    val completeThresholdPx = with(density) { 96.dp.toPx() }
    val actionRevealThresholdPx = with(density) { 72.dp.toPx() }
    val maxRightSwipePx = with(density) { 160.dp.toPx() }
    val maxLeftSwipePx = with(density) { if (onRescheduleTask != null) 216.dp.toPx() else 96.dp.toPx() }

    val isSlidOpenLeft = swipeOffsetX.value <= -actionRevealThresholdPx * 0.5f

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
        targetValue = when {
            isQuickActionsVisible || isSlidOpenLeft -> RoyalBlue
            task.isCompleted -> HairlineBorder
            else -> Color(0xFFDCDCD7)
        },
        label = "cardBorder"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background Layer: Revealed during Horizontal Swipe (Right = Complete, Left = Inline Actions)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        swipeOffsetX.value > 0f -> Color(0xFFEFF6FF)
                        swipeOffsetX.value < 0f -> Color(0xFFF3F4F6)
                        else -> Color.Transparent
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        swipeOffsetX.value >= completeThresholdPx -> RoyalBlue
                        swipeOffsetX.value < 0f -> HairlineBorder
                        else -> Color.Transparent
                    },
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            if (swipeOffsetX.value > 0f) {
                val progressScale = (swipeOffsetX.value / completeThresholdPx).coerceIn(0.75f, 1.15f)
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .scale(progressScale)
                            .clip(CircleShape)
                            .background(if (swipeOffsetX.value >= completeThresholdPx) RoyalBlue else Color.White)
                            .border(1.5.dp, RoyalBlue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Complete Task",
                            tint = if (swipeOffsetX.value >= completeThresholdPx) Color.White else RoyalBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = if (task.isCompleted) "Reopen" else "Complete",
                        style = MaterialTheme.typography.labelMedium,
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (swipeOffsetX.value < 0f) {
                // Inline Action Buttons Revealed Behind the Slid Card
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .testTag("swipe_inline_actions_${task.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onRescheduleTask != null) {
                        // Today Inline Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val todayEnd = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY)
                                    onRescheduleTask(todayEnd)
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(0f, spring())
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("inline_swipe_action_today_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }

                        // Tomorrow Inline Action Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val tomorrowEnd = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TOMORROW)
                                    onRescheduleTask(tomorrowEnd)
                                    coroutineScope.launch {
                                        swipeOffsetX.animateTo(0f, spring())
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("inline_swipe_action_tomorrow_${task.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tomorrow",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Delete Inline Action Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDC2626))
                            .clickable {
                                HapticFeedbackHelper.performDeleteHaptic(view, hapticFeedback)
                                coroutineScope.launch {
                                    swipeOffsetX.snapTo(0f)
                                }
                                onDeleteTask()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("inline_swipe_action_delete_${task.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete Task",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Foreground Slidable Task Card with Modifier.pointerInput
        Column(
            modifier = Modifier
                .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp))
                .pointerInput(task.id, task.isCompleted) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            hasTriggeredThresholdHaptic = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val nextOffset = (swipeOffsetX.value + dragAmount)
                                .coerceIn(-maxLeftSwipePx, maxRightSwipePx)
                            val crossedRight = nextOffset >= completeThresholdPx &&
                                    swipeOffsetX.value < completeThresholdPx
                            val crossedLeft = nextOffset <= -actionRevealThresholdPx &&
                                    swipeOffsetX.value > -actionRevealThresholdPx
                            if ((crossedRight || crossedLeft) && !hasTriggeredThresholdHaptic) {
                                hasTriggeredThresholdHaptic = true
                                HapticFeedbackHelper.performSwipeActionHaptic(view, hapticFeedback)
                            } else if (abs(nextOffset) < minOf(completeThresholdPx, actionRevealThresholdPx) * 0.7f) {
                                hasTriggeredThresholdHaptic = false
                            }
                            coroutineScope.launch {
                                swipeOffsetX.snapTo(nextOffset)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                when {
                                    swipeOffsetX.value >= completeThresholdPx -> {
                                        HapticFeedbackHelper.performTaskCompletionHaptic(view, hapticFeedback)
                                        onToggleCompletion()
                                        swipeOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    swipeOffsetX.value <= -actionRevealThresholdPx -> {
                                        swipeOffsetX.animateTo(
                                            targetValue = -maxLeftSwipePx,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    else -> {
                                        swipeOffsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                swipeOffsetX.animateTo(0f, spring())
                            }
                        }
                    )
                }
                .combinedClickable(
                    onClick = {
                        if (swipeOffsetX.value < -10f) {
                            coroutineScope.launch {
                                swipeOffsetX.animateTo(0f, spring())
                            }
                        } else {
                            isExpanded = !isExpanded
                        }
                    },
                    onLongClick = {
                        if (onStartFocus != null && !task.isCompleted) {
                            HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                            onStartFocus()
                        } else {
                            HapticFeedbackHelper.performSwipeActionHaptic(view, hapticFeedback)
                            isQuickActionsVisible = !isQuickActionsVisible
                        }
                    }
                )
                .padding(16.dp)
                .testTag("task_card_${task.id}")
        ) {
        // Main task row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox
            MonochromeCheckbox(
                checked = task.isCompleted,
                onCheckedChange = {
                    HapticFeedbackHelper.performTaskCompletionHaptic(view, hapticFeedback)
                    onToggleCompletion()
                },
                modifier = Modifier.testTag("task_checkbox_${task.id}")
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Subtle monochrome priority indicator bar (dots) next to task item
            SubtleMonochromeLevelIndicator(
                priority = task.priority,
                isCompleted = task.isCompleted,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Title, description and details
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (task.isCompleted) CompletedText else ObsidianBlack,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (task.isCompleted) CompletedText else SlateGraphite,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Metadata row: Category pill, Monochrome Priority Badge, Due Date, Subtask Count
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category pill
                    val style = CategoryTheme.getStyle(task.category)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(style.backgroundColor)
                            .border(1.dp, style.borderColor, RoundedCornerShape(6.dp))
                            .clickable(enabled = onCategoryClick != null) {
                                onCategoryClick?.invoke(task.category)
                            }
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = style.textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }

                    // Subtle Monochrome Priority Badge
                    MonochromePriorityBadge(
                        priority = task.priority,
                        isCompleted = task.isCompleted
                    )

                    // Due Date if present
                    if (task.dueDate != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Due Date",
                                tint = if (task.isCompleted) CompletedText else SlateGraphite,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                            Text(
                                text = sdf.format(Date(task.dueDate)),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (task.isCompleted) CompletedText else SlateGraphite,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Subtask counter pill if subtasks exist
                    if (totalSubtasksCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DateTileBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "$completedSubtasksCount/$totalSubtasksCount",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Expand / Collapse Chevron, Focus & Quick Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (onStartFocus != null && !task.isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(RoyalBlueSoft)
                            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(999.dp))
                            .clickable {
                                HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                onStartFocus()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("task_focus_button_${task.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Adjust,
                                contentDescription = "Zen Focus Mode",
                                tint = RoyalBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Focus",
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        HapticFeedbackHelper.performSwipeActionHaptic(view, hapticFeedback)
                        coroutineScope.launch {
                            if (swipeOffsetX.value < -10f) {
                                swipeOffsetX.animateTo(0f, spring())
                            } else {
                                swipeOffsetX.animateTo(
                                    targetValue = -maxLeftSwipePx,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .testTag("task_quick_actions_toggle_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreHoriz,
                        contentDescription = "Quick Actions",
                        tint = if (isSlidOpenLeft || isQuickActionsVisible) RoyalBlue else SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Task Details",
                        tint = SlateGraphite,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Right-to-Left Swipe / Quick Actions Bar: Today, Tomorrow, Delete
        AnimatedVisibility(
            visible = isQuickActionsVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
                    .testTag("task_quick_actions_bar_${task.id}"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "QUICK ACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.8.sp
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onRescheduleTask != null) {
                        // Today Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val todayEnd = Calendar.getInstance().apply {
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                    }.timeInMillis
                                    onRescheduleTask(todayEnd)
                                    isQuickActionsVisible = false
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("quick_action_today_${task.id}")
                        ) {
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }

                        // Tomorrow Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val tomorrowEnd = Calendar.getInstance().apply {
                                        add(Calendar.DAY_OF_YEAR, 1)
                                        set(Calendar.HOUR_OF_DAY, 23)
                                        set(Calendar.MINUTE, 59)
                                        set(Calendar.SECOND, 59)
                                    }.timeInMillis
                                    onRescheduleTask(tomorrowEnd)
                                    isQuickActionsVisible = false
                                }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                .testTag("quick_action_tomorrow_${task.id}")
                        ) {
                            Text(
                                text = "Tomorrow",
                                style = MaterialTheme.typography.labelSmall,
                                color = ObsidianBlack,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Delete Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFECACA), RoundedCornerShape(8.dp))
                            .clickable {
                                HapticFeedbackHelper.performDeleteHaptic(view, hapticFeedback)
                                isQuickActionsVisible = false
                                onDeleteTask()
                            }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                            .testTag("quick_action_delete_${task.id}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Delete",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Subtasks Progress Bar (visible if subtasks present)
        if (totalSubtasksCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp)),
                color = if (task.isCompleted) CompletedText else RoyalBlue,
                trackColor = Color(0xFFF0EFEA),
                strokeCap = StrokeCap.Round
            )
        }

        // Expanded Subtasks checklist & quick-add
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HairlineBorder)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "SUBTASKS (${completedSubtasksCount}/${totalSubtasksCount})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // List of subtasks
                task.subtasks.forEach { subtask ->
                    SubtaskItemRow(
                        subtask = subtask,
                        onToggle = {
                            HapticFeedbackHelper.performSubtaskToggleHaptic(view, hapticFeedback)
                            onToggleSubtask(subtask.id)
                        },
                        onDelete = {
                            HapticFeedbackHelper.performDeleteHaptic(view, hapticFeedback)
                            onDeleteSubtask(subtask.id)
                        }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // Add subtask input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompactFormTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = "Add subtask...",
                        singleLine = true,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            if (newSubtaskText.isNotBlank()) {
                                HapticFeedbackHelper.performTaskCreationHaptic(view, hapticFeedback)
                                onAddSubtask(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                HapticFeedbackHelper.performTaskCreationHaptic(view, hapticFeedback)
                                onAddSubtask(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (newSubtaskText.isNotBlank()) RoyalBlue else PaperCanvas)
                            .border(1.dp, if (newSubtaskText.isNotBlank()) RoyalBlue else HairlineBorder, RoundedCornerShape(10.dp))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subtask",
                            tint = if (newSubtaskText.isNotBlank()) Color.White else SlateGraphite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
}

/**
 * Subtle Monochrome Indicator next to the task item:
 * - Low: 1 subtle dot (–)
 * - Medium: 2 subtle dots (••)
 * - High: 3 bold obsidian dots (•••)
 */
@Composable
fun SubtleMonochromeLevelIndicator(
    priority: TaskPriority,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val baseColor = if (isCompleted) CompletedText else priority.monochromeIndicatorColor

    Column(
        modifier = modifier.width(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.5.dp)
    ) {
        repeat(priority.level) {
            Box(
                modifier = Modifier
                    .size(if (priority == TaskPriority.HIGH) 4.5.dp else 4.dp)
                    .clip(CircleShape)
                    .background(baseColor)
            )
        }
    }
}

/**
 * Subtle Monochrome Priority Badge with outline and clean typography
 */
@Composable
fun MonochromePriorityBadge(
    priority: TaskPriority,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (isCompleted) DateTileBackground else priority.monochromeBackgroundColor
    val textColor = if (isCompleted) CompletedText else priority.monochromeTextColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, HairlineBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.indicatorSymbol,
            style = MaterialTheme.typography.labelSmall,
            color = if (isCompleted) CompletedText else priority.monochromeIndicatorColor,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
        Spacer(modifier = Modifier.width(3.dp))
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

@Composable
fun MonochromeCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedBg by animateColorAsState(
        targetValue = if (checked) ObsidianBlack else PaperSurface,
        label = "checkboxBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (checked) ObsidianBlack else Color(0xFFC0C0BA),
        label = "checkboxBorder"
    )

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(animatedBg)
            .border(1.5.dp, animatedBorder, RoundedCornerShape(7.dp))
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(PaperCanvas)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .clickable { onToggle() }
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (subtask.isCompleted) ObsidianBlack else PaperSurface)
                    .border(1.dp, if (subtask.isCompleted) ObsidianBlack else Color(0xFFC0C0BA), RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (subtask.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = subtask.title,
                style = MaterialTheme.typography.bodySmall.copy(
                    textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (subtask.isCompleted) CompletedText else ObsidianBlack
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove subtask",
                tint = SlateGraphite,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
