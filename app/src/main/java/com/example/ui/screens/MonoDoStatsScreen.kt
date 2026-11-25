package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HabitDotMatrixView
import com.example.ui.theme.CategoryTheme
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.util.LocalDayUtils
import com.example.ui.viewmodel.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Locale

private data class DayStatsBreakdown(
    val epochDay: Long,
    val tasksDone: Int,
    val habitsDone: Int,
    val focusMinutes: Int
) {
    val totalActivity: Int get() = tasksDone + habitsDone + if (focusMinutes > 0) 1 else 0
}

@Composable
fun MonoDoStatsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val allTasks by viewModel.allTasks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val habitLogs by viewModel.habitLogs.collectAsState()
    val focusSessions by viewModel.focusSessions.collectAsState()

    val completedCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val activeCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val totalCount = remember(allTasks) { allTasks.size }
    val streakDays = remember(allTasks) { viewModel.calculateStreakDays() }
    val weeklyData = remember(allTasks) { viewModel.getCompletionStatsForLast7Days() }
    val maxWeeklyCount = remember(weeklyData) {
        val max = weeklyData.maxOfOrNull { it.second } ?: 1
        if (max > 0) max else 1
    }

    val todayFocusMinutes = remember(focusSessions) { viewModel.getTodayFocusMinutes(focusSessions) }
    val weeklyFocusMinutes = remember(focusSessions) { viewModel.getWeeklyFocusMinutes(focusSessions) }

    // 28-Day detailed breakdown for Tap-to-Inspect on Heatmap & Weekly Bar Chart
    val todayEpochDay = LocalDayUtils.toLocalEpochDay()
    val dayBreakdownMap = remember(allTasks, habitLogs, focusSessions, todayEpochDay) {
        val tasksByDay = mutableMapOf<Long, Int>()
        val habitsByDay = mutableMapOf<Long, Int>()
        val focusByDay = mutableMapOf<Long, Int>()

        allTasks.forEach { task ->
            val completedAt = task.completedAt
            if (task.isCompleted && completedAt != null) {
                val day = LocalDayUtils.toLocalEpochDay(completedAt)
                if (day in (todayEpochDay - 27L)..todayEpochDay) {
                    tasksByDay[day] = (tasksByDay[day] ?: 0) + 1
                }
            }
        }
        habitLogs.forEach { log ->
            if (log.isCompleted && log.dateEpochDay in (todayEpochDay - 27L)..todayEpochDay) {
                habitsByDay[log.dateEpochDay] = (habitsByDay[log.dateEpochDay] ?: 0) + 1
            }
        }
        focusSessions.forEach { session ->
            if (session.localEpochDay in (todayEpochDay - 27L)..todayEpochDay) {
                focusByDay[session.localEpochDay] = (focusByDay[session.localEpochDay] ?: 0) + session.durationMinutes
            }
        }

        (27 downTo 0).associate { offset ->
            val day = todayEpochDay - offset
            day to DayStatsBreakdown(
                epochDay = day,
                tasksDone = tasksByDay[day] ?: 0,
                habitsDone = habitsByDay[day] ?: 0,
                focusMinutes = focusByDay[day] ?: 0
            )
        }
    }

    val heatmapCounts = remember(dayBreakdownMap, todayEpochDay) {
        (27 downTo 0).map { offset ->
            val day = todayEpochDay - offset
            day to (dayBreakdownMap[day]?.totalActivity ?: 0)
        }
    }

    var inspectedHeatmapDay by remember { mutableStateOf<Long?>(null) }
    var inspectedWeeklyIndex by remember { mutableStateOf<Int?>(null) }
    val inspectDateFmt = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    val categoryBreakdown = remember(allTasks) {
        allTasks.groupBy { it.category.ifBlank { "General" } }
            .map { (cat, list) ->
                val done = list.count { it.isCompleted }
                Triple(cat, done, list.size)
            }
            .sortedByDescending { it.third }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mono_do_stats_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Productivity Stats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primaryInk,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Consistent daily focus, habits & execution",
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondaryInk
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2x2 KPI Metric Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Task Streak",
                value = "$streakDays ${if (streakDays == 1) "day" else "days"}",
                subtitle = "Consecutive days",
                icon = Icons.Default.Bolt,
                iconColor = colors.accent,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = "Focus Today",
                value = formatFocusDuration(todayFocusMinutes),
                subtitle = "7d: ${formatFocusDuration(weeklyFocusMinutes)}",
                icon = Icons.Default.Timer,
                iconColor = colors.accent,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Completed",
                value = "$completedCount",
                subtitle = if (totalCount > 0) "${((completedCount * 100f) / totalCount).toInt()}% of all tasks" else "No tasks yet",
                icon = Icons.Default.CheckCircle,
                iconColor = colors.success,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = "Active Queue",
                value = "$activeCount",
                subtitle = "Pending tasks",
                icon = Icons.Default.TrendingUp,
                iconColor = colors.primaryInk,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Habit Dot Matrix Tracker with Streaks & Manage Dialog
        HabitDotMatrixView(
            habits = allHabits,
            habitLogs = habitLogs,
            onToggleHabitDay = { habitId, epochDay ->
                viewModel.toggleHabitDay(habitId, epochDay)
            },
            onAddHabit = { title, colorHex ->
                viewModel.addHabit(title, colorHex)
            },
            onUpdateHabit = { habit, title, colorHex ->
                viewModel.updateHabit(habit, title, colorHex)
            },
            onDeleteHabit = { habitId ->
                viewModel.deleteHabit(habitId)
            },
            getHabitStreaks = { habitId ->
                viewModel.calculateHabitStreaks(habitId, habitLogs)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4-Week Consistency Heatmap (28 Days)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(18.dp)
                .testTag("weekly_heatmap_card")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "28-Day Consistency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "Tasks, habits & focus blocks",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk
                        )
                    }

                    val activeDaysIn28 = heatmapCounts.count { it.second > 0 }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$activeDaysIn28/28 days",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4 rows x 7 columns (tap any cell to inspect exact metrics)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (week in 0 until 4) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (col in 0 until 7) {
                                val index = week * 7 + col
                                val (dayKey, count) = heatmapCounts[index]
                                val isToday = dayKey == todayEpochDay
                                val isSelected = inspectedHeatmapDay == dayKey
                                val cellColor = when {
                                    count <= 0 -> colors.tileBg
                                    count == 1 -> colors.accent.copy(alpha = 0.28f)
                                    count in 2..3 -> colors.accent.copy(alpha = 0.62f)
                                    else -> colors.accent
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.35f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(cellColor)
                                        .border(
                                            width = if (isSelected) 2.dp else if (isToday) 1.5.dp else 0.5.dp,
                                            color = when {
                                                isSelected -> colors.primaryInk
                                                isToday -> colors.primaryInk.copy(alpha = 0.7f)
                                                else -> colors.border.copy(alpha = 0.6f)
                                            },
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                            inspectedHeatmapDay = if (inspectedHeatmapDay == dayKey) null else dayKey
                                        }
                                        .testTag("heatmap_cell_$index")
                                )
                            }
                        }
                    }
                }

                // Inline Tap-to-Inspect Tooltip Pill for Heatmap
                AnimatedVisibility(visible = inspectedHeatmapDay != null) {
                    inspectedHeatmapDay?.let { dayKey ->
                        val stats = dayBreakdownMap[dayKey] ?: DayStatsBreakdown(dayKey, 0, 0, 0)
                        val cal = LocalDayUtils.calendarForLocalEpochDay(dayKey)
                        val dateLabel = inspectDateFmt.format(cal.time)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.accentBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("heatmap_inspect_tooltip"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stats.tasksDone} tasks • ${stats.habitsDone} habits • ${formatFocusDuration(stats.focusMinutes)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = colors.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap any square to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.mutedInk,
                        fontSize = 10.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Less",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        listOf(
                            colors.tileBg,
                            colors.accent.copy(alpha = 0.28f),
                            colors.accent.copy(alpha = 0.62f),
                            colors.accent
                        ).forEach { swatch ->
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .size(11.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(swatch)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "More",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Activity Bar Chart (fixed conflicting width modifiers)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Last 7 Days Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryInk
                        )
                        Text(
                            text = "Tasks completed per day",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondaryInk
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Weekly",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyData.forEachIndexed { index, (day, count) ->
                        val isTodayCol = index == weeklyData.lastIndex
                        val isSelectedBar = inspectedWeeklyIndex == index
                        val targetHeight = (100f * (count.toFloat() / maxWeeklyCount.toFloat()).coerceIn(0.08f, 1f)).dp
                        val animatedHeight by animateDpAsState(
                            targetValue = targetHeight,
                            animationSpec = tween(durationMillis = 350),
                            label = "barHeight_$day"
                        )

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                                    inspectedWeeklyIndex = if (inspectedWeeklyIndex == index) null else index
                                }
                                .padding(vertical = 2.dp)
                                .testTag("weekly_bar_$index")
                        ) {
                            if (count > 0 || isSelectedBar) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (isSelectedBar) colors.accent else colors.primaryInk,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(animatedHeight)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(
                                        when {
                                            isSelectedBar -> colors.primaryInk
                                            count > 0 && isTodayCol -> colors.accent
                                            count > 0 -> colors.accent.copy(alpha = 0.72f)
                                            else -> colors.tileBg
                                        }
                                    )
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isSelectedBar -> colors.primaryInk
                                    isTodayCol -> colors.accent
                                    else -> colors.secondaryInk
                                },
                                fontWeight = if (isTodayCol || isSelectedBar) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Inline Tap-to-Inspect Tooltip for 7-Day Bar Chart
                AnimatedVisibility(visible = inspectedWeeklyIndex != null) {
                    inspectedWeeklyIndex?.let { idx ->
                        val offsetFromToday = (weeklyData.lastIndex - idx).toLong()
                        val targetEpoch = todayEpochDay - offsetFromToday
                        val stats = dayBreakdownMap[targetEpoch] ?: DayStatsBreakdown(targetEpoch, weeklyData[idx].second, 0, 0)
                        val cal = LocalDayUtils.calendarForLocalEpochDay(targetEpoch)
                        val dateLabel = inspectDateFmt.format(cal.time)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.canvas)
                                .border(1.dp, colors.accentBorder, RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("weekly_bar_inspect_tooltip"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.primaryInk,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stats.tasksDone} tasks • ${formatFocusDuration(stats.focusMinutes)} focus",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = colors.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Category Breakdown Card
        if (categoryBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                    .padding(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Category Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.primaryInk
                    )
                    Text(
                        text = "Completion progress across workspaces",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.secondaryInk
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        categoryBreakdown.forEach { (cat, done, total) ->
                            val style = CategoryTheme.getStyle(cat, colors.isDark)
                            val fraction = if (total > 0) done.toFloat() / total.toFloat() else 0f

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(style.accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = cat,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.primaryInk,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Text(
                                        text = "$done / $total",
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = colors.secondaryInk,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(colors.tileBg)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(style.accentColor)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

private fun formatFocusDuration(totalMinutes: Int): String {
    if (totalMinutes <= 0) return "0m"
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(colors.tileBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.secondaryInk,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                fontWeight = FontWeight.Bold,
                color = colors.primaryInk,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = colors.secondaryInk,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
