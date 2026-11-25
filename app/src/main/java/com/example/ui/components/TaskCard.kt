package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrence
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    onRescheduleTask: ((Long?) -> Unit)? = null,
    onEditTask: (() -> Unit)? = null,
    isSwipeOpen: Boolean = false,
    onSwipeOpenChanged: ((Boolean) -> Unit)? = null
) {
    val colors = MonoDoThemeTokens.colors
    var isExpanded by remember(task.id) { mutableStateOf(false) }
    var newSubtaskText by remember(task.id) { mutableStateOf("") }
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val swipeOffsetX = remember(task.id) { Animatable(0f) }
    var hasTriggeredThresholdHaptic by remember { mutableStateOf(false) }

    val completeThresholdPx = with(density) { 88.dp.toPx() }
    val actionRevealThresholdPx = with(density) { 68.dp.toPx() }
    val maxRightSwipePx = with(density) { 150.dp.toPx() }
    val actionWidthDp = when {
        onEditTask != null && onRescheduleTask != null -> 236.dp
        onRescheduleTask != null -> 196.dp
        onEditTask != null -> 144.dp
        else -> 92.dp
    }
    val maxLeftSwipePx = with(density) { actionWidthDp.toPx() }

    // Single-open swipe coordination: auto-close when another card becomes active
    LaunchedEffect(isSwipeOpen) {
        if (!isSwipeOpen && swipeOffsetX.value < -1f) {
            swipeOffsetX.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

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

    // Header-scoped press highlight (eliminates clipped circular ripple during expansion!)
    val headerInteractionSource = remember { MutableInteractionSource() }
    val isHeaderPressed by headerInteractionSource.collectIsPressedAsState()
    val headerBackgroundColor by animateColorAsState(
        targetValue = if (isHeaderPressed) colors.cardHighlight else colors.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "headerHighlight"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isSlidOpenLeft -> colors.accent
            isExpanded -> colors.softBorder
            task.isCompleted -> colors.border
            else -> colors.softBorder
        },
        label = "cardBorder"
    )

    val isOverdue = remember(task.dueDate, task.isCompleted) {
        !task.isCompleted && LocalDayUtils.isOverdue(task.dueDate)
    }

    val dueDateLabel = remember(task.dueDate) {
        task.dueDate?.let { ts ->
            when {
                LocalDayUtils.isToday(ts) -> "Today"
                LocalDayUtils.isTomorrow(ts) -> "Tomorrow"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Background Layer: Rendered only when actively sliding
        if (abs(swipeOffsetX.value) > 0.5f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            swipeOffsetX.value > 0f -> colors.accentSoft
                            swipeOffsetX.value < 0f -> colors.surfaceVariant
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = when {
                            swipeOffsetX.value >= completeThresholdPx -> colors.accent
                            swipeOffsetX.value < 0f -> colors.border
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
                            .padding(start = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .scale(progressScale)
                                .clip(CircleShape)
                                .background(if (swipeOffsetX.value >= completeThresholdPx) colors.accent else colors.surface)
                                .border(1.5.dp, colors.accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Complete Task",
                                tint = if (swipeOffsetX.value >= completeThresholdPx) Color.White else colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (task.isCompleted) "Reopen" else "Complete",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (swipeOffsetX.value < 0f) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp)
                            .testTag("swipe_inline_actions_${task.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (onEditTask != null) {
                            SwipeActionPill(
                                label = "Edit",
                                icon = Icons.Default.Edit,
                                containerColor = colors.accentSoft,
                                contentColor = colors.accent,
                                borderColor = colors.accentBorder,
                                testTag = "inline_swipe_action_edit_${task.id}",
                                onClick = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    onSwipeOpenChanged?.invoke(false)
                                    coroutineScope.launch { swipeOffsetX.animateTo(0f, spring()) }
                                    onEditTask()
                                }
                            )
                        }

                        if (onRescheduleTask != null) {
                            SwipeActionPill(
                                label = "Today",
                                containerColor = colors.surface,
                                contentColor = colors.primaryInk,
                                borderColor = colors.border,
                                testTag = "inline_swipe_action_today_${task.id}",
                                onClick = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val todayEnd = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY)
                                    onRescheduleTask(todayEnd)
                                    onSwipeOpenChanged?.invoke(false)
                                    coroutineScope.launch { swipeOffsetX.animateTo(0f, spring()) }
                                }
                            )

                            SwipeActionPill(
                                label = "Tmrw",
                                containerColor = colors.surface,
                                contentColor = colors.primaryInk,
                                borderColor = colors.border,
                                testTag = "inline_swipe_action_tomorrow_${task.id}",
                                onClick = {
                                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                    val tomorrowEnd = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TOMORROW)
                                    onRescheduleTask(tomorrowEnd)
                                    onSwipeOpenChanged?.invoke(false)
                                    coroutineScope.launch { swipeOffsetX.animateTo(0f, spring()) }
                                }
                            )
                        }

                        SwipeActionPill(
                            label = "Delete",
                            icon = Icons.Default.DeleteOutline,
                            containerColor = colors.danger,
                            contentColor = Color.White,
                            borderColor = colors.danger,
                            testTag = "inline_swipe_action_delete_${task.id}",
                            onClick = {
                                HapticFeedbackHelper.performDeleteHaptic(view, hapticFeedback)
                                onSwipeOpenChanged?.invoke(false)
                                coroutineScope.launch { swipeOffsetX.snapTo(0f) }
                                onDeleteTask()
                            }
                        )
                    }
                }
            }
        }

        // Foreground Slidable Task Card
        Column(
            modifier = Modifier
                .offset { IntOffset(swipeOffsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
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
                                        onSwipeOpenChanged?.invoke(false)
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
                                        onSwipeOpenChanged?.invoke(true)
                                        swipeOffsetX.animateTo(
                                            targetValue = -maxLeftSwipePx,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                    else -> {
                                        onSwipeOpenChanged?.invoke(false)
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
                            onSwipeOpenChanged?.invoke(false)
                            coroutineScope.launch {
                                swipeOffsetX.animateTo(0f, spring())
                            }
                        }
                    )
                }
                .testTag("task_card_${task.id}")
        ) {
            // Header Region: Subtle animated surface highlight on press (NO clipped expanding ripple!)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBackgroundColor)
                    .combinedClickable(
                        interactionSource = headerInteractionSource,
                        indication = null,
                        onClick = {
                            if (swipeOffsetX.value < -10f) {
                                onSwipeOpenChanged?.invoke(false)
                                coroutineScope.launch { swipeOffsetX.animateTo(0f, spring()) }
                            } else {
                                isExpanded = !isExpanded
                            }
                        },
                        onLongClick = {
                            if (onStartFocus != null && !task.isCompleted) {
                                HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                onStartFocus()
                            } else if (onEditTask != null) {
                                HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                onEditTask()
                            }
                        }
                    )
                    .padding(start = 6.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    // Accessible 48dp touch wrapper around crisp 22dp checkbox
                    MonochromeCheckbox(
                        checked = task.isCompleted,
                        onCheckedChange = {
                            HapticFeedbackHelper.performTaskCompletionHaptic(view, hapticFeedback)
                            onToggleCompletion()
                        },
                        modifier = Modifier.testTag("task_checkbox_${task.id}")
                    )

                    // Subtle monochrome priority indicator dots
                    SubtleMonochromeLevelIndicator(
                        priority = task.priority,
                        isCompleted = task.isCompleted,
                        modifier = Modifier.padding(top = 15.dp, end = 8.dp)
                    )

                    // Title, description and metadata
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 10.dp)
                    ) {
                        val strikeProgress by animateFloatAsState(
                            targetValue = if (task.isCompleted) 1f else 0f,
                            animationSpec = tween(durationMillis = 260),
                            label = "taskStrikeProgress"
                        )
                        val animatedTitleColor by animateColorAsState(
                            targetValue = if (task.isCompleted) colors.mutedInk else colors.primaryInk,
                            animationSpec = tween(durationMillis = 220),
                            label = "taskTitleColor"
                        )
                        val strikeInkColor = colors.mutedInk
                        var titleLineCount by remember(task.id) { mutableStateOf(1) }

                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.SemiBold,
                                textDecoration = if (task.isCompleted && titleLineCount > 1) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = animatedTitleColor,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            onTextLayout = { result -> titleLineCount = result.lineCount },
                            modifier = Modifier.drawWithContent {
                                drawContent()
                                // Crisp ink-pen animated horizontal sweep for single-line titles
                                if (strikeProgress > 0.01f && titleLineCount == 1) {
                                    val y = size.height * 0.54f
                                    drawLine(
                                        color = strikeInkColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width * strikeProgress, y),
                                        strokeWidth = 1.6.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (task.isCompleted) colors.mutedInk else colors.secondaryInk,
                                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Metadata FlowRow so badges wrap gracefully on compact screens
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            val style = CategoryTheme.getStyle(task.category, colors.isDark)
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

                            MonochromePriorityBadge(
                                priority = task.priority,
                                isCompleted = task.isCompleted
                            )

                            if (dueDateLabel != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isOverdue) colors.dangerSoft else Color.Transparent)
                                        .then(
                                            if (isOverdue) Modifier.border(1.dp, colors.dangerBorder, RoundedCornerShape(6.dp))
                                            else Modifier
                                        )
                                        .padding(horizontal = if (isOverdue) 6.dp else 2.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Due Date",
                                        tint = when {
                                            task.isCompleted -> colors.mutedInk
                                            isOverdue -> colors.danger
                                            else -> colors.secondaryInk
                                        },
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isOverdue) "Overdue • $dueDateLabel" else dueDateLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = when {
                                            task.isCompleted -> colors.mutedInk
                                            isOverdue -> colors.danger
                                            else -> colors.secondaryInk
                                        },
                                        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (task.reminderAt != null && !task.isCompleted) {
                                val remTimeText = remember(task.reminderAt) {
                                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(task.reminderAt))
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accentSoft)
                                        .border(1.dp, colors.accentBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Reminder set",
                                        tint = colors.accent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = remTimeText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = colors.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (task.recurrence != TaskRecurrence.NONE) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.accentSoft)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = "Recurring",
                                        tint = colors.accent,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = task.recurrence.shortLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.accent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            if (totalSubtasksCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colors.tileBg)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$completedSubtasksCount/$totalSubtasksCount",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily
                                        ),
                                        color = colors.secondaryInk,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Action Controls Column / Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        if (onStartFocus != null && !task.isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(colors.accentSoft)
                                    .border(1.dp, colors.accentBorder, RoundedCornerShape(999.dp))
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                        onStartFocus()
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                                    .testTag("task_focus_button_${task.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Adjust,
                                        contentDescription = "Zen Focus Mode",
                                        tint = colors.accent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Focus",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.accent,
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
                                        onSwipeOpenChanged?.invoke(false)
                                        swipeOffsetX.animateTo(0f, spring())
                                    } else {
                                        onSwipeOpenChanged?.invoke(true)
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
                                .size(38.dp)
                                .testTag("task_quick_actions_toggle_${task.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Task Actions",
                                tint = if (isSlidOpenLeft) colors.accent else colors.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = !isExpanded },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Task Details",
                                tint = colors.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Subtasks Progress Bar (visible when subtasks exist)
                if (totalSubtasksCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(1.5.dp)),
                        color = if (task.isCompleted) colors.mutedInk else colors.accent,
                        trackColor = colors.tileBg,
                        strokeCap = StrokeCap.Round
                    )
                }
            }

            // Decoupled Expanded Subtasks Section (smooth spring expand, zero ripple interference!)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMedium)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SUBTASKS ($completedSubtasksCount/$totalSubtasksCount)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily
                            ),
                            color = colors.secondaryInk,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            fontSize = 10.sp
                        )

                        if (onEditTask != null) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                                        onEditTask()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("task_expanded_edit_${task.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Task",
                                    tint = colors.accent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit Task",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.accent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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

                    // Add subtask input row
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
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (newSubtaskText.isNotBlank()) colors.accent else colors.canvas)
                                .border(
                                    1.dp,
                                    if (newSubtaskText.isNotBlank()) colors.accent else colors.border,
                                    RoundedCornerShape(10.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Subtask",
                                tint = if (newSubtaskText.isNotBlank()) Color.White else colors.secondaryInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeActionPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color,
    testTag: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 9.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun SubtleMonochromeLevelIndicator(
    priority: TaskPriority,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val baseColor = if (isCompleted) colors.mutedInk else priority.indicatorColor(colors)

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

@Composable
fun MonochromePriorityBadge(
    priority: TaskPriority,
    isCompleted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val bg = if (isCompleted) colors.tileBg else priority.badgeBackground(colors)
    val textColor = if (isCompleted) colors.mutedInk else priority.badgeTextColor(colors)
    val dotColor = if (isCompleted) colors.mutedInk else {
        if (priority == TaskPriority.HIGH && !colors.isDark) Color.White
        else if (priority == TaskPriority.HIGH && colors.isDark) colors.canvas
        else priority.indicatorColor(colors)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, colors.border, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = priority.indicatorSymbol,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
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
    val colors = MonoDoThemeTokens.colors
    val bounceScale = remember { Animatable(1f) }
    var previousChecked by remember { mutableStateOf(checked) }

    LaunchedEffect(checked) {
        if (checked != previousChecked) {
            previousChecked = checked
            bounceScale.snapTo(if (checked) 0.78f else 0.90f)
            bounceScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val animatedBg by animateColorAsState(
        targetValue = if (checked) colors.primaryInk else colors.surface,
        animationSpec = tween(durationMillis = 180),
        label = "checkboxBg"
    )
    val animatedBorder by animateColorAsState(
        targetValue = if (checked) colors.primaryInk else colors.softBorder,
        animationSpec = tween(durationMillis = 180),
        label = "checkboxBorder"
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "checkIconScale"
    )
    val checkTint = if (colors.isDark) colors.canvas else Color.White

    // 48dp accessible hit target wrapping the crisp 22dp monochrome checkbox with spring bounce
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .scale(bounceScale.value)
                .clip(RoundedCornerShape(7.dp))
                .background(animatedBg)
                .border(1.5.dp, animatedBorder, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checkScale > 0.05f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Completed",
                    tint = checkTint,
                    modifier = Modifier
                        .size(14.dp)
                        .scale(checkScale)
                )
            }
        }
    }
}

@Composable
private fun SubtaskItemRow(
    subtask: SubTask,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val checkTint = if (colors.isDark) colors.canvas else Color.White

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.canvas)
            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable { onToggle() }
            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (subtask.isCompleted) colors.primaryInk else colors.surface)
                    .border(
                        1.dp,
                        if (subtask.isCompleted) colors.primaryInk else colors.softBorder,
                        RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (subtask.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        tint = checkTint,
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
                color = if (subtask.isCompleted) colors.mutedInk else colors.primaryInk
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(38.dp)
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
