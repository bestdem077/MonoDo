package com.example.ui.components

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.DateTileBorder
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.viewmodel.DayActivity
import java.util.Locale

/**
 * WeeklyMatrixView serves as the primary Weekly Productivity Analytics section.
 * Features a Recharts-inspired animated Bar Chart of completed tasks for each day
 * of the current week, with interactive inspection tooltips, scale guidelines,
 * and view toggle between Bar Chart and Calendar Matrix tiles.
 */
@Composable
fun WeeklyMatrixView(
    weeklyActivity: List<DayActivity>,
    modifier: Modifier = Modifier
) {
    var isChartView by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf<DayActivity?>(null) }
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    val totalWeekly = weeklyActivity.sumOf { it.completedCount }
    val maxCount = maxOf(weeklyActivity.maxOfOrNull { it.completedCount } ?: 0, 4)
    val bestDay = weeklyActivity.maxByOrNull { it.completedCount }?.takeIf { it.completedCount > 0 }
    val dailyAvg = if (weeklyActivity.isNotEmpty()) totalWeekly.toFloat() / weeklyActivity.size else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(PaperSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("weekly_analytics_section")
    ) {
        // Section Header with Title, Stats, and Mode Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = "Analytics Icon",
                        tint = ObsidianBlack,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEEKLY ANALYTICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalWeekly tasks completed this week",
                    style = MaterialTheme.typography.titleSmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold
                )
            }

            // Clean toggle pill between Chart and Tiles
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF2F1ED))
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnalyticsTabPill(
                    label = "Chart",
                    isSelected = isChartView,
                    onClick = {
                        isChartView = true
                        HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                    }
                )
                AnalyticsTabPill(
                    label = "Tiles",
                    isSelected = !isChartView,
                    onClick = {
                        isChartView = false
                        HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Display Area: Recharts-inspired Bar Chart or Calendar Tiles
        if (isChartView) {
            WeeklyBarChart(
                weeklyActivity = weeklyActivity,
                maxCount = maxCount,
                selectedDay = selectedDay,
                onDayClick = { day ->
                    selectedDay = if (selectedDay?.dateString == day.dateString) null else day
                    HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                }
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                weeklyActivity.forEach { day ->
                    CalendarDayTile(
                        day = day,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Interactive Tooltip / Inspector Callout
        AnimatedVisibility(
            visible = selectedDay != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            selectedDay?.let { day ->
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF6F5F2))
                        .border(1.dp, Color(0xFFE2E1DC), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val fullDay = day.dayFullName.ifEmpty { day.dayLabel }
                        Text(
                            text = "$fullDay, Day ${day.dayOfMonth} ${if (day.isToday) "• Today" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${day.completedCount} ${if (day.completedCount == 1) "task" else "tasks"} completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { selectedDay = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss tooltip",
                            tint = SlateGraphite,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Summary Metric Badges (Total, Best Day, Daily Average)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnalyticsStatItem(
                label = "AVG / DAY",
                value = String.format(Locale.getDefault(), "%.1f", dailyAvg)
            )
            AnalyticsStatItem(
                label = "PEAK DAY",
                value = bestDay?.let { "${it.dayLabel} (${it.completedCount})" } ?: "None"
            )
            AnalyticsStatItem(
                label = "DAYS ACTIVE",
                value = "${weeklyActivity.count { it.completedCount > 0 }}/7"
            )
        }
    }
}

/**
 * Recharts-style Bar Chart composable with Y-axis guidelines,
 * dynamic bar heights, today indicator, and tap interaction.
 */
@Composable
private fun WeeklyBarChart(
    weeklyActivity: List<DayActivity>,
    maxCount: Int,
    selectedDay: DayActivity?,
    onDayClick: (DayActivity) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analytics_bar_chart")
    ) {
        // Chart Area: Height 110.dp with grid lines and columns
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            // Horizontal background guidelines (Top, Middle, Base)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 22.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top line (maxCount)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFEBEAE5))
                )
                // Mid line (maxCount / 2)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFF0EFEA))
                )
                // Base line (0)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color(0xFFDFDED9))
                )
            }

            // Y-Axis tick labels on the far left
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$maxCount",
                    fontSize = 9.sp,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${maxCount / 2}",
                    fontSize = 9.sp,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "0",
                    fontSize = 9.sp,
                    color = SlateGraphite,
                    fontWeight = FontWeight.Medium
                )
            }

            // 7 Bar Columns (Mon through Sun)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyActivity.forEach { day ->
                    val isSelected = selectedDay?.dateString == day.dateString
                    val barFillRatio = (day.completedCount.toFloat() / maxCount).coerceIn(0f, 1f)
                    val animatedFill by animateFloatAsState(
                        targetValue = barFillRatio,
                        animationSpec = tween(450),
                        label = "barFill_${day.dateString}"
                    )

                    WeeklyBarColumn(
                        day = day,
                        fillRatio = animatedFill,
                        isSelected = isSelected,
                        onClick = { onDayClick(day) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // X-Axis Labels row (Weekday name & day of month)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            weeklyActivity.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) ObsidianBlack else SlateGraphite,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${day.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) ObsidianBlack else Color(0xFF888880),
                        fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                    // Indicator mark for Today
                    if (day.isToday) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(ObsidianBlack)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single bar column in the bar chart.
 */
@Composable
private fun WeeklyBarColumn(
    day: DayActivity,
    fillRatio: Float,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalBarHeight = 90.dp
    val barHeight by animateDpAsState(
        targetValue = when {
            day.completedCount == 0 -> 4.dp // subtle ground tick
            else -> maxOf(totalBarHeight * fillRatio, 8.dp)
        },
        animationSpec = tween(400),
        label = "barHeight"
    )

    val barColor by animateColorAsState(
        targetValue = when {
            isSelected -> Color(0xFF000000)
            day.isToday && day.completedCount > 0 -> ObsidianBlack
            day.completedCount > 0 -> Color(0xFF262626)
            else -> Color(0xFFD6D5CE)
        },
        animationSpec = tween(200),
        label = "barColor"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .testTag("bar_chart_day_${day.dayLabel}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Value label above bar
        if (day.completedCount > 0) {
            Text(
                text = "${day.completedCount}",
                style = MaterialTheme.typography.labelSmall,
                color = if (day.isToday) ObsidianBlack else SlateGraphite,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Track & Fill bar
        Box(
            modifier = Modifier
                .width(20.dp)
                .height(totalBarHeight)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                .background(Color(0xFFF3F2ED)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(barColor)
                    .then(
                        if (isSelected) {
                            Modifier.border(1.5.dp, ObsidianBlack, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        } else Modifier
                    )
            )
        }
    }
}

/**
 * Calendar day tile for matrix view.
 */
@Composable
private fun CalendarDayTile(
    day: DayActivity,
    modifier: Modifier = Modifier
) {
    val tileBgColor by animateColorAsState(
        targetValue = when {
            day.isToday -> Color(0xFFF0EFEA)
            day.completedCount > 0 -> Color(0xFFECEBE5)
            else -> DateTileBackground
        },
        animationSpec = tween(250),
        label = "tileBg"
    )

    val borderColor = if (day.isToday) ObsidianBlack else DateTileBorder
    val borderWidth = if (day.isToday) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(tileBgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelSmall,
            color = if (day.isToday) ObsidianBlack else SlateGraphite,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = "${day.dayOfMonth}",
            style = MaterialTheme.typography.titleMedium,
            color = ObsidianBlack,
            fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (day.completedCount > 0) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ObsidianBlack)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCDCD5))
            )
        }
    }
}

@Composable
private fun AnalyticsTabPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) ObsidianBlack else Color.Transparent,
        animationSpec = tween(200),
        label = "tabBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else SlateGraphite,
        animationSpec = tween(200),
        label = "tabText"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun AnalyticsStatItem(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = SlateGraphite,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = ObsidianBlack,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
