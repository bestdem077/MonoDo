package com.example.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskEntity
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import com.example.ui.viewmodel.TaskViewModel
import com.example.widget.MonoDoQuickWidgetProvider
import com.example.widget.MonoDoWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ToolSubTab(val label: String) {
    NOTES("Notes"),
    BIRTHDAYS("Birthdays"),
    POMODORO("Focus"),
    WIDGET("Widgets")
}

private enum class HomeWidgetStyle(val title: String, val sizeLabel: String) {
    AGENDA_LIST("Agenda List", "4×3 Scrollable"),
    FOCUS_GLANCE("Focus Glance", "4×2 Compact")
}

@Composable
fun MonoDoToolsScreen(
    viewModel: TaskViewModel,
    selectedTool: ToolSubTab = ToolSubTab.NOTES,
    onSelectTool: (ToolSubTab) -> Unit = {},
    onQuickAddTask: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val pomodoroState by viewModel.pomodoroState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .testTag("mono_do_tools_screen")
    ) {
        // Top Tool Selector Tabs (4 compact tools: Notes, Birthdays, Focus, Widgets)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ToolSubTab.entries.forEach { tool ->
                val isSelected = selectedTool == tool
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.primaryInk else Color.Transparent)
                        .clickable { onSelectTool(tool) }
                        .padding(vertical = 8.dp, horizontal = 2.dp)
                        .testTag("tool_subtab_${tool.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = tool.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.canvas else colors.secondaryInk,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (tool == ToolSubTab.POMODORO && pomodoroState.isRunning) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(colors.accent)
                            )
                        }
                    }
                }
            }
        }

        when (selectedTool) {
            ToolSubTab.NOTES -> MonoDoNotesScreen(viewModel = viewModel)
            ToolSubTab.BIRTHDAYS -> MonoDoBirthdaysScreen(viewModel = viewModel)
            ToolSubTab.POMODORO -> PomodoroFocusScreen(viewModel = viewModel)
            ToolSubTab.WIDGET -> HomeWidgetPreviewScreen(
                viewModel = viewModel,
                onQuickAddTask = onQuickAddTask
            )
        }
    }
}

@Composable
private fun HomeWidgetPreviewScreen(
    viewModel: TaskViewModel,
    onQuickAddTask: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val allTasks by viewModel.allTasks.collectAsState()
    val focusSessions by viewModel.focusSessions.collectAsState()

    var selectedWidget by remember { mutableStateOf(HomeWidgetStyle.AGENDA_LIST) }
    val nowMillis = remember(allTasks) { System.currentTimeMillis() }

    val sortedActiveTasks = remember(allTasks) {
        allTasks.filter { !it.isCompleted }
            .sortedWith(
                compareBy<TaskEntity> { task ->
                    val due = task.dueDate
                    when {
                        due != null && LocalDayUtils.isOverdue(due, nowMillis) -> 0
                        due != null && LocalDayUtils.isToday(due, nowMillis) -> 1
                        due != null -> 2
                        else -> 3
                    }
                }
                    .thenByDescending { it.priority.level }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
            )
    }

    val activeCount = sortedActiveTasks.size
    val completedCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val totalCount = allTasks.size
    val progressRatio = if (totalCount > 0) (completedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f) else 0f
    val todayFocusMins = remember(focusSessions) { viewModel.getTodayFocusMinutes(focusSessions) }
    val todayLabel = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()).uppercase(Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .testTag("home_widget_preview_screen"),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "Home Screen Widgets",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primaryInk,
            letterSpacing = (-0.4).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Choose between a scrollable live task agenda or a compact focus glance widget. Both sync with your theme and support 1-tap check-off.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondaryInk,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Widget Style Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HomeWidgetStyle.entries.forEach { style ->
                val isSelected = selectedWidget == style
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.accentSoft else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) colors.accentBorder else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            selectedWidget = style
                        }
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                        .testTag("widget_style_${style.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (style == HomeWidgetStyle.AGENDA_LIST) Icons.Default.FormatListBulleted else Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isSelected) colors.accent else colors.secondaryInk,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = style.title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) colors.accent else colors.primaryInk,
                            maxLines = 1
                        )
                        Text(
                            text = style.sizeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontSize = 9.5.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Live Interactive Widget Preview
        if (selectedWidget == HomeWidgetStyle.AGENDA_LIST) {
            AgendaListWidgetLivePreview(
                tasks = sortedActiveTasks,
                todayLabel = todayLabel,
                activeCount = activeCount,
                completedCount = completedCount,
                totalCount = totalCount,
                progressRatio = progressRatio,
                nowMillis = nowMillis,
                onToggleTask = { task ->
                    HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
                    viewModel.toggleTaskCompletion(task)
                },
                onQuickAdd = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    onQuickAddTask()
                },
                onRefresh = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    MonoDoWidgetProvider.updateAllWidgets(context)
                    Toast.makeText(context, "Home screen widgets synced", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            FocusGlanceWidgetLivePreview(
                topTask = sortedActiveTasks.firstOrNull(),
                completedCount = completedCount,
                totalCount = totalCount,
                todayFocusMins = todayFocusMins,
                todayLabel = todayLabel,
                onToggleTask = { task ->
                    HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
                    viewModel.toggleTaskCompletion(task)
                },
                onQuickAdd = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    onQuickAddTask()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compact Feature Highlights Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val pills = if (selectedWidget == HomeWidgetStyle.AGENDA_LIST) {
                listOf("Scrollable 25+", "1-Tap Done", "+ Quick Add")
            } else {
                listOf("Next Priority", "Focus Stats", "1-Tap Capture")
            }
            pills.forEach { label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.5.sp,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Pin Selected Widget Button
        Button(
            onClick = {
                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                MonoDoWidgetProvider.updateAllWidgets(context)
                val widgetClass = if (selectedWidget == HomeWidgetStyle.AGENDA_LIST) {
                    MonoDoWidgetProvider::class.java
                } else {
                    MonoDoQuickWidgetProvider::class.java
                }
                val widgetName = if (selectedWidget == HomeWidgetStyle.AGENDA_LIST) {
                    "MonoDo Agenda"
                } else {
                    "MonoDo Focus Glance"
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                    val provider = ComponentName(context, widgetClass)
                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                    } else {
                        Toast.makeText(
                            context,
                            "Synced! Long-press your home screen -> Widgets -> $widgetName.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Synced! Add $widgetName from your home screen widgets menu.",
                        Toast.LENGTH_LONG
                    ).show()
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
                .testTag("pin_home_widget_button")
        ) {
            Icon(imageVector = Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Pin ${selectedWidget.title} to Home Screen",
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(92.dp))
    }
}

@Composable
private fun AgendaListWidgetLivePreview(
    tasks: List<TaskEntity>,
    todayLabel: String,
    activeCount: Int,
    completedCount: Int,
    totalCount: Int,
    progressRatio: Float,
    nowMillis: Long,
    onToggleTask: (TaskEntity) -> Unit,
    onQuickAdd: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val dateFmt = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.primaryInk.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("live_widget_card")
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MONODO AGENDA",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (totalCount == 0) "$todayLabel • Ready" else "$todayLabel • $completedCount/$totalCount done • $activeCount left",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.secondaryInk,
                    fontSize = 10.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(colors.tileBg)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable { onRefresh() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Widget",
                        tint = colors.secondaryInk,
                        modifier = Modifier.size(15.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(colors.accent)
                        .clickable { onQuickAdd() }
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                        .testTag("widget_preview_quick_add"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Task",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Task",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(colors.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressRatio)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Scrollable Task List Preview
        if (tasks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.canvas)
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = colors.success,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "All clear for today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap + Task above to capture a new item",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tasks.take(8).forEach { task ->
                    val isOverdue = task.dueDate != null && LocalDayUtils.isOverdue(task.dueDate, nowMillis)
                    val metaParts = buildList {
                        add(task.category)
                        task.dueDate?.let { due ->
                            when {
                                LocalDayUtils.isOverdue(due, nowMillis) -> add("Overdue")
                                LocalDayUtils.isToday(due, nowMillis) -> add("Today")
                                LocalDayUtils.isTomorrow(due, nowMillis) -> add("Tomorrow")
                                else -> add(dateFmt.format(Date(due)))
                            }
                        }
                        task.reminderAt?.let { rem ->
                            if (rem > 0L) add("Bell ${timeFmt.format(Date(rem))}")
                        }
                        if (task.subtasks.isNotEmpty()) {
                            val doneSubs = task.subtasks.count { it.isCompleted }
                            add("$doneSubs/${task.subtasks.size} steps")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.canvas)
                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                            .clickable { onToggleTask(task) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .border(1.75.dp, colors.accent, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = metaParts.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isOverdue) colors.danger else colors.secondaryInk,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = task.priority.indicatorSymbol,
                            style = MaterialTheme.typography.labelSmall,
                            color = task.priority.indicatorColor(colors),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FocusGlanceWidgetLivePreview(
    topTask: TaskEntity?,
    completedCount: Int,
    totalCount: Int,
    todayFocusMins: Int,
    todayLabel: String,
    onToggleTask: (TaskEntity) -> Unit,
    onQuickAdd: () -> Unit
) {
    val colors = MonoDoThemeTokens.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surface)
            .border(1.5.dp, colors.primaryInk.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(16.dp)
            .testTag("live_quick_widget_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "FOCUS GLANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = todayLabel,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                color = colors.secondaryInk,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Up Next Highlight Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.canvas)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable(enabled = topTask != null) {
                    topTask?.let { onToggleTask(it) }
                }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(
                        1.75.dp,
                        if (topTask != null) colors.accent else colors.success,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (topTask == null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (topTask != null) "UP NEXT • ${topTask.category.uppercase(Locale.getDefault())}" else "ALL CLEAR",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.5.sp,
                    letterSpacing = 0.6.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = topTask?.title ?: "No pending tasks — enjoy your focus!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primaryInk,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Bottom Stats & Quick Add Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.tileBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$completedCount/$totalCount done",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentSoft)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "${todayFocusMins}m focus",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = colors.accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.accent)
                    .clickable { onQuickAdd() }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Task",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New Task",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PomodoroFocusScreen(viewModel: TaskViewModel) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val focusSessions by viewModel.focusSessions.collectAsState()

    val todayFocusMins = remember(focusSessions) { viewModel.getTodayFocusMinutes(focusSessions) }
    val weeklyFocusMins = remember(focusSessions) { viewModel.getWeeklyFocusMinutes(focusSessions) }

    val progress = if (pomodoroState.totalSeconds > 0) {
        (pomodoroState.secondsRemaining.toFloat() / pomodoroState.totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "pomodoroProgress")

    val minutes = pomodoroState.secondsRemaining / 60
    val secs = pomodoroState.secondsRemaining % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .testTag("pomodoro_focus_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Focus / Break Mode Switcher
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(false to "Focus (${pomodoroState.focusMinutes}m)", true to "Break (${pomodoroState.breakMinutes}m)").forEach { (isBreakMode, label) ->
                val isSelected = pomodoroState.isBreak == isBreakMode
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.accentSoft else Color.Transparent)
                        .clickable {
                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                            viewModel.switchPomodoroMode(isBreakMode)
                        }
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .testTag(if (isBreakMode) "pomodoro_mode_break" else "pomodoro_mode_focus")
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) colors.accent else colors.secondaryInk
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Circular Timer Display
        Box(
            modifier = Modifier.size(236.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = colors.border,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (pomodoroState.isBreak) colors.success else colors.accent,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    fontWeight = FontWeight.Black,
                    color = colors.primaryInk,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        pomodoroState.isRunning && pomodoroState.isBreak -> "ON BREAK"
                        pomodoroState.isRunning -> "FOCUSING..."
                        else -> "READY"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (pomodoroState.isRunning) colors.accent else colors.secondaryInk,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Duration Adjusters (-5m / +5m when paused)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val step = if (pomodoroState.isBreak) 1 else 5
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .clickable(enabled = !pomodoroState.isRunning) {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        viewModel.adjustPomodoroDuration(-step)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "-${step}m",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (pomodoroState.isRunning) colors.mutedInk else colors.primaryInk,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .clickable(enabled = !pomodoroState.isRunning) {
                        HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                        viewModel.adjustPomodoroDuration(step)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "+${step}m",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = if (pomodoroState.isRunning) colors.mutedInk else colors.primaryInk,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Timer Controls
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    viewModel.togglePomodoro()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (pomodoroState.isRunning) colors.primaryInk else colors.accent,
                    contentColor = if (pomodoroState.isRunning) colors.canvas else Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("pomodoro_toggle_button")
            ) {
                Icon(
                    imageVector = if (pomodoroState.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (pomodoroState.isRunning) "Pause" else "Start"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (pomodoroState.isRunning) "Pause" else "Start Focus",
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    viewModel.resetPomodoro()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.surface,
                    contentColor = colors.primaryInk
                ),
                border = BorderStroke(1.dp, colors.border),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("pomodoro_reset_button")
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Logged Focus Summary Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
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
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Deep Focus Logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.primaryInk,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Persists across tabs automatically",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk,
                        fontSize = 10.5.sp
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${todayFocusMins}m today",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.accent,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${weeklyFocusMins}m / 7d",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.secondaryInk,
                    fontSize = 10.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
