package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.WeeklyMatrixView
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.MonoDoUiState
import kotlin.math.roundToInt

@Composable
fun MonoDoStatsScreen(
    uiState: MonoDoUiState,
    modifier: Modifier = Modifier
) {
    val totalAll = uiState.completedTasks.size + uiState.activeTasks.size
    val completionRatio = if (totalAll > 0) {
        uiState.completedTasks.size.toFloat() / totalAll.toFloat()
    } else 0f

    val animatedCompletionRatio by animateFloatAsState(
        targetValue = completionRatio,
        animationSpec = tween(700),
        label = "completionGauge"
    )

    // Compute category distribution breakdown
    val categoryCounts = (uiState.activeTasks + uiState.completedTasks)
        .groupBy { it.category }
        .mapValues { it.value.size }
        .toList()
        .sortedByDescending { it.second }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Page Title
        item {
            Column {
                Text(
                    text = "ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Productivity & Stats",
                    style = MaterialTheme.typography.headlineMedium,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Hero Streak & Milestone Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(ObsidianBlack)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = PaperSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CURRENT STREAK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFD4D4CE),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.streakDays} Day${if (uiState.streakDays != 1) "s" else ""}",
                            style = MaterialTheme.typography.displaySmall,
                            color = PaperSurface,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (uiState.streakDays > 0) "Consistency builds mastery. Keep your momentum going!" else "Complete a task today to start your streak!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFAAAAA0)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF222222))
                            .border(1.dp, Color(0xFF333333), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${uiState.completedTodayCount}",
                                style = MaterialTheme.typography.titleLarge,
                                color = PaperSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF999990),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Weekly Matrix Section
        item {
            WeeklyMatrixView(
                weeklyActivity = uiState.weeklyActivity,
                modifier = Modifier.padding(horizontal = 0.dp)
            )
        }

        // Circular Completion Rate Gauge Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completion Gauge",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "COMPLETION RATE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${(animatedCompletionRatio * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineLarge,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${uiState.completedTasks.size} completed out of $totalAll total tasks",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGraphite
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(80.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedCompletionRatio },
                            modifier = Modifier.size(76.dp),
                            color = ObsidianBlack,
                            trackColor = DateTileBackground,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // Category Breakdown Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(PaperSurface)
                    .border(1.dp, HairlineBorder, RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PieChart,
                        contentDescription = "Categories",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CATEGORY WORKLOAD",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (categoryCounts.isEmpty()) {
                    Text(
                        text = "No categorized tasks yet. Add tasks in categories to see workload analytics.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateGraphite
                    )
                } else {
                    categoryCounts.forEach { (cat, count) ->
                        val ratio = if (totalAll > 0) count.toFloat() / totalAll.toFloat() else 0f
                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "$count tasks (${(ratio * 100).roundToInt()}%)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateGraphite,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = ObsidianBlack,
                                trackColor = DateTileBackground,
                                strokeCap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}
