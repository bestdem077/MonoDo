package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.HairlineBorder
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.RoyalBlueSoft
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.delay

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.os.Build
import android.widget.Toast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import com.example.ui.util.HapticFeedbackHelper
import com.example.widget.MonoDoWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ToolSubTab(val label: String) {
    NOTES("Notes"),
    POMODORO("Focus Timer"),
    WIDGET("Home Widget")
}

@Composable
fun MonoDoToolsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTool by remember { mutableStateOf(ToolSubTab.NOTES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .testTag("mono_do_tools_screen")
    ) {
        // Top Tool Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PaperSurface)
                .border(1.dp, HairlineBorder, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolSubTab.entries.forEach { tool ->
                val isSelected = selectedTool == tool
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) ObsidianBlack else Color.Transparent)
                        .clickable { selectedTool = tool }
                        .padding(vertical = 8.dp)
                        .testTag("tool_subtab_${tool.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tool.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else SlateGraphite
                    )
                }
            }
        }

        when (selectedTool) {
            ToolSubTab.NOTES -> MonoDoNotesScreen(viewModel = viewModel)
            ToolSubTab.POMODORO -> PomodoroFocusScreen()
            ToolSubTab.WIDGET -> HomeWidgetPreviewScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun HomeWidgetPreviewScreen(viewModel: TaskViewModel) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current
    val allTasks by viewModel.allTasks.collectAsState()

    val topActiveTasks = remember(allTasks) {
        allTasks.filter { !it.isCompleted }
            .sortedWith(
                compareByDescending<com.example.data.model.TaskEntity> { it.priority.ordinal }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
            )
            .take(3)
    }
    val activeCount = remember(allTasks) { allTasks.count { !it.isCompleted } }
    val todayLabel = remember {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()).uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 12.dp)
            .testTag("home_widget_preview_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Android Home Screen Widget",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = ObsidianBlack
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Live interactive paper widget showing your top 3 priority tasks. Tick tasks right from your home screen.",
            style = MaterialTheme.typography.bodySmall,
            color = SlateGraphite
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Live Interactive Paper Widget Card (Mirrors monodo_widget_layout.xml)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(PaperCanvas)
                .border(1.5.dp, ObsidianBlack, RoundedCornerShape(20.dp))
                .padding(18.dp)
                .testTag("live_widget_card")
        ) {
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
                            .background(RoyalBlue)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "MONODO • TODAY",
                        style = MaterialTheme.typography.labelSmall,
                        color = ObsidianBlack,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(RoyalBlueSoft)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$todayLabel • $activeCount PENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = RoyalBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HairlineBorder)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (topActiveTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All clear for today • 100% Done",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateGraphite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topActiveTasks.forEach { task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(PaperSurface)
                                .border(1.dp, HairlineBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    HapticFeedbackHelper.performTaskCompletionHaptic(view, haptic)
                                    viewModel.toggleTaskCompletion(task)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, RoyalBlue, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {}
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = ObsidianBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                            }

                            Text(
                                text = task.priority.indicatorSymbol,
                                style = MaterialTheme.typography.labelSmall,
                                color = RoyalBlue,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                HapticFeedbackHelper.performSelectionHaptic(view, haptic)
                MonoDoWidgetProvider.updateAllWidgets(context)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
                    val provider = ComponentName(context, MonoDoWidgetProvider::class.java)
                    if (appWidgetManager != null && appWidgetManager.isRequestPinAppWidgetSupported) {
                        appWidgetManager.requestPinAppWidget(provider, null, null)
                    } else {
                        Toast.makeText(
                            context,
                            "Widget refreshed! Long-press your home screen -> Widgets -> MonoDo Today.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        context,
                        "Widget refreshed! Add MonoDo Today from your home screen widgets menu.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = RoyalBlue,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("pin_home_widget_button")
        ) {
            Icon(imageVector = Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Pin Widget to Home Screen", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PomodoroFocusScreen() {
    var isRunning by remember { mutableStateOf(false) }
    var isBreak by remember { mutableStateOf(false) }
    var secondsRemaining by remember { mutableIntStateOf(25 * 60) }
    val totalSeconds = if (isBreak) 5 * 60 else 25 * 60

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        } else if (isRunning && secondsRemaining == 0) {
            isBreak = !isBreak
            secondsRemaining = if (isBreak) 5 * 60 else 25 * 60
            isRunning = false
        }
    }

    val progress = (secondsRemaining.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    val minutes = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val timeFormatted = String.format("%02d:%02d", minutes, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isBreak) RoyalBlueSoft else DateTileBackground)
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isBreak) "5 MIN SHORT BREAK" else "25 MIN DEEP FOCUS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (isBreak) RoyalBlue else ObsidianBlack,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Circular Timer Display
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = DateTileBackground,
                strokeWidth = 10.dp
            )

            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = if (isBreak) RoyalBlue else ObsidianBlack,
                strokeWidth = 10.dp
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = ObsidianBlack,
                    letterSpacing = (-1).sp
                )
                Text(
                    text = if (isRunning) "Focusing..." else "Paused",
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateGraphite
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Timer Controls
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) ObsidianBlack else RoyalBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start"
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isRunning) "Pause" else "Start Focus")
            }

            Button(
                onClick = {
                    isRunning = false
                    secondsRemaining = if (isBreak) 5 * 60 else 25 * 60
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = PaperSurface,
                    contentColor = ObsidianBlack
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, HairlineBorder),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset")
            }
        }
    }
}
