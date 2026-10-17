package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HabitDotMatrixView
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun MonoDoStatsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val habitLogs by viewModel.habitLogs.collectAsState()
    val completedCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val activeCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val totalCount = remember(allTasks) { allTasks.size }
    val streakDays = remember(allTasks) { viewModel.calculateStreakDays() }
    val weeklyData = remember(allTasks) { viewModel.getCompletionStatsForLast7Days() }
    val maxWeeklyCount = remember(weeklyData) {
        val max = weeklyData.maxOfOrNull { it.second } ?: 1
        if (max > 0) max else 1
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
            .testTag("mono_do_stats_screen")
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = "Productivity Stats",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ObsidianBlack,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "Consistent daily focus, habits & execution",
            style = MaterialTheme.typography.bodySmall,
            color = SlateGraphite
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick KPI Metric Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatMetricCard(
                title = "Streak",
                value = "$streakDays ${if (streakDays == 1) "day" else "days"}",
                icon = Icons.Default.Bolt,
                iconColor = RoyalBlue,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = "Completed",
                value = "$completedCount",
                icon = Icons.Default.CheckCircle,
                iconColor = ObsidianBlack,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = "Active",
                value = "$activeCount",
                icon = Icons.Default.TrendingUp,
                iconColor = RoyalBlue,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Habit Dot Matrix Tracker (Moved from Home Screen to Stats for Clean Focus)
        HabitDotMatrixView(
            habits = allHabits,
            habitLogs = habitLogs,
            onToggleHabitDay = { habitId, epochDay ->
                viewModel.toggleHabitDay(habitId, epochDay)
            },
            onAddHabit = { title, colorHex ->
                viewModel.addHabit(title, colorHex)
            },
            onDeleteHabit = { habitId ->
                viewModel.deleteHabit(habitId)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7-Day Activity Bar Chart
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
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
                            color = ObsidianBlack
                        )
                        Text(
                            text = "Tasks completed per day",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RoyalBlueSoft)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Weekly",
                            style = MaterialTheme.typography.labelSmall,
                            color = RoyalBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Bar columns
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weeklyData.forEach { (day, count) ->
                        val barHeightFraction = (count.toFloat() / maxWeeklyCount.toFloat()).coerceIn(0.08f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxWidth(0.55f)
                                    .height(100.dp * barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                    .background(if (count > 0) RoyalBlue else DateTileBackground)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = day,
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Total Productivity Summary Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Efficiency Rate",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ObsidianBlack
                )

                Spacer(modifier = Modifier.height(6.dp))

                val rate = if (totalCount > 0) ((completedCount.toFloat() / totalCount.toFloat()) * 100).toInt() else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "$rate% Completed",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = RoyalBlue
                    )

                    Text(
                        text = "$completedCount of $totalCount tasks done",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun StatMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(PaperSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DateTileBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ObsidianBlack
            )

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = SlateGraphite
            )
        }
    }
}
