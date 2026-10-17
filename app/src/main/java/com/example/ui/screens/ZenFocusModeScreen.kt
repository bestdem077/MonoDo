package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.components.MonochromeCheckbox
import com.example.ui.components.MonochromePriorityBadge
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import kotlinx.coroutines.delay

@Composable
fun ZenFocusModeScreen(
    task: TaskEntity,
    onExit: () -> Unit,
    onCompleteTask: () -> Unit,
    onToggleSubtask: (subtaskId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onExit() }

    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Timer state (in seconds)
    var initialDuration by remember { mutableIntStateOf(25 * 60) }
    var secondsRemaining by remember { mutableIntStateOf(initialDuration) }
    var isTimerRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isTimerRunning) {
        while (isTimerRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        }
        if (secondsRemaining == 0 && isTimerRunning) {
            isTimerRunning = false
            HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
        }
    }

    val progress = if (initialDuration > 0) {
        secondsRemaining.toFloat() / initialDuration.toFloat()
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "zenProgress")

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .testTag("zen_focus_mode_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, CircleShape)
                    .testTag("zen_exit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Exit Zen Focus",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "ZEN FOCUS",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )

            // Reset Timer button
            IconButton(
                onClick = {
                    isTimerRunning = false
                    secondsRemaining = initialDuration
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset Timer",
                    tint = SlateGraphite,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Task Header Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val catStyle = CategoryTheme.getStyle(task.category)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(catStyle.backgroundColor)
                        .border(1.dp, catStyle.borderColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = task.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = catStyle.textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                MonochromePriorityBadge(priority = task.priority, isCompleted = false)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = task.title,
                style = MaterialTheme.typography.titleLarge,
                color = ObsidianBlack,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Minimalist Circular Focus Timer
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawCircle(
                    color = Color(0xFFE5E5DF),
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active Arc
                drawArc(
                    color = RoyalBlue,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (isTimerRunning) "IN DEEP FOCUS" else "PAUSED",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isTimerRunning) RoyalBlue else SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Timer Controls (+5m, Play/Pause, -5m)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // -5 min
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        if (secondsRemaining > 300) {
                            secondsRemaining -= 300
                            initialDuration = maxOf(300, initialDuration - 300)
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("-5m", style = MaterialTheme.typography.labelMedium, color = SlateGraphite)
            }

            // Play / Pause Primary Pill
            IconButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    isTimerRunning = !isTimerRunning
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(RoyalBlue)
                    .testTag("zen_play_pause_button")
            ) {
                Icon(
                    imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isTimerRunning) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // +5 min
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        secondsRemaining += 300
                        initialDuration += 300
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("+5m", style = MaterialTheme.typography.labelMedium, color = SlateGraphite)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Subtasks Checklist Section
        if (task.subtasks.isNotEmpty()) {
            Text(
                text = "SUBTASKS (${task.subtasks.count { it.isCompleted }}/${task.subtasks.size})",
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(task.subtasks, key = { it.id }) { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PaperSurface)
                            .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                                onToggleSubtask(subtask.id)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MonochromeCheckbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = {
                                HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                                onToggleSubtask(subtask.id)
                            }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = subtask.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            color = if (subtask.isCompleted) SlateGraphite else ObsidianBlack
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Complete Task and Exit CTA
        Button(
            onClick = {
                HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
                onCompleteTask()
                onExit()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("zen_complete_task_button"),
            colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete Task",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Complete Task & Exit Zen",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
