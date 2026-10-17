package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SlateGraphite
import com.example.ui.util.HapticFeedbackHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class DayColumnInfo(
    val epochDay: Long,
    val dayAbbr: String,
    val dayNumber: String,
    val isToday: Boolean
)

@Composable
fun HabitDotMatrixView(
    habits: List<HabitEntity>,
    habitLogs: List<HabitLogEntity>,
    onToggleHabitDay: (habitId: Long, epochDay: Long) -> Unit,
    onAddHabit: (title: String, colorHex: String) -> Unit,
    onDeleteHabit: (habitId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    var isExpanded by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    // Compute the 7 days of the current week (or past 6 days + today)
    val dayColumns = remember {
        val list = mutableListOf<DayColumnInfo>()
        val cal = Calendar.getInstance()
        val todayEpochDay = System.currentTimeMillis() / 86400000L

        // Past 6 days leading up to today
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val epochDay = c.timeInMillis / 86400000L
            val dayAbbr = SimpleDateFormat("EEE", Locale.getDefault()).format(c.time).take(1).uppercase()
            val dayNumber = SimpleDateFormat("d", Locale.getDefault()).format(c.time)
            list.add(
                DayColumnInfo(
                    epochDay = epochDay,
                    dayAbbr = dayAbbr,
                    dayNumber = dayNumber,
                    isToday = (epochDay == todayEpochDay)
                )
            )
        }
        list
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PaperSurface)
            .border(1.dp, HairlineBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
            .testTag("habit_dot_matrix_view")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(RoyalBlue)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "HABIT MATRIX",
                    style = MaterialTheme.typography.labelSmall,
                    color = ObsidianBlack,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${habits.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = SlateGraphite,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Habits",
                    tint = SlateGraphite,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                    showAddDialog = true
                },
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(PaperCanvas)
                    .border(1.dp, HairlineBorder, CircleShape)
                    .testTag("add_habit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Habit",
                    tint = ObsidianBlack,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                // Column headers for days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dayColumns.forEach { day ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(24.dp)
                            ) {
                                Text(
                                    text = day.dayAbbr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) RoyalBlue else SlateGraphite,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = day.dayNumber,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (day.isToday) ObsidianBlack else SlateGraphite,
                                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (habits.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Track daily consistency • Tap + or pick a quick habit:",
                            style = MaterialTheme.typography.bodySmall,
                            color = SlateGraphite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val quickHabits = listOf(
                                "Read 20m" to "#2563EB",
                                "Deep Work 2h" to "#111111",
                                "Gym" to "#16A34A",
                                "Meditate" to "#7C3AED"
                            )
                            quickHabits.forEach { (presetTitle, presetColor) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(PaperCanvas)
                                        .border(1.dp, HairlineBorder, RoundedCornerShape(999.dp))
                                        .clickable {
                                            HapticFeedbackHelper.performTaskCreationHaptic(view, haptic)
                                            onAddHabit(presetTitle, presetColor)
                                        }
                                        .padding(horizontal = 9.dp, vertical = 5.dp)
                                        .testTag("quick_habit_preset_${presetTitle.lowercase().replace(" ", "_")}")
                                ) {
                                    Text(
                                        text = "+ $presetTitle",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ObsidianBlack,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    habits.forEach { habit ->
                        val habitColor = try {
                            Color(android.graphics.Color.parseColor(habit.colorHex))
                        } catch (e: Exception) {
                            RoyalBlue
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Habit Title with color dot
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { habitToDelete = habit },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(habitColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = habit.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 7 Day Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                dayColumns.forEach { day ->
                                    val isCompleted = habitLogs.any {
                                        it.habitId == habit.id && it.dateEpochDay == day.epochDay && it.isCompleted
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) habitColor
                                                else if (day.isToday) Color(0xFFEFF6FF)
                                                else PaperCanvas
                                            )
                                            .border(
                                                1.dp,
                                                if (isCompleted) habitColor
                                                else if (day.isToday) RoyalBlue
                                                else HairlineBorder,
                                                CircleShape
                                            )
                                            .clickable {
                                                HapticFeedbackHelper.performSubtaskToggleHaptic(view, haptic)
                                                onToggleHabitDay(habit.id, day.epochDay)
                                            }
                                            .testTag("habit_dot_${habit.id}_${day.epochDay}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isCompleted) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Habit Dialog
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var selectedColorHex by remember { mutableStateOf("#2563EB") }
        val colorOptions = listOf(
            "#2563EB", // Royal Blue
            "#16A34A", // Emerald
            "#D97706", // Amber
            "#DC2626", // Crimson
            "#7C3AED", // Purple
            "#0D9488", // Teal
            "#E11D48", // Rose
            "#111111"  // Obsidian
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text("New Daily Habit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ObsidianBlack)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        placeholder = { Text("Habit name (e.g. Read 20m, Gym)", color = SlateGraphite) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RoyalBlue,
                            unfocusedBorderColor = HairlineBorder,
                            focusedTextColor = ObsidianBlack,
                            unfocusedTextColor = ObsidianBlack
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Accent Color", style = MaterialTheme.typography.labelSmall, color = SlateGraphite, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        colorOptions.forEach { hex ->
                            val color = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex == hex

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) ObsidianBlack else Color.Transparent,
                                        CircleShape
                                    )
                                    .clickable { selectedColorHex = hex }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onAddHabit(newTitle.trim(), selectedColorHex)
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Habit", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface
        )
    }

    // Delete Habit Confirmation Dialog
    habitToDelete?.let { habit ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("Delete Habit?", color = ObsidianBlack, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete \"${habit.title}\" and its logged history?", color = SlateGraphite) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteHabit(habit.id)
                        habitToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("Cancel", color = SlateGraphite)
                }
            },
            containerColor = PaperSurface
        )
    }
}
