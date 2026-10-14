package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.reminder.ReminderPreferences
import com.example.ui.util.HapticFeedbackHelper
import com.example.ui.components.CategoryChipsRow
import com.example.ui.components.CreateTaskSheet
import com.example.ui.components.EmptyTasksView
import com.example.ui.components.MonochromeHeader
import com.example.ui.components.ReminderSettingsSheet
import com.example.ui.components.SearchBarAndFilter
import com.example.ui.components.TaskCard
import com.example.ui.components.WeeklyMatrixView
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.PaperSurface
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoHomeScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val hapticFeedback = LocalHapticFeedback.current
    val reminderPrefs = remember { ReminderPreferences(context) }
    var reminderSettings by remember { mutableStateOf(reminderPrefs.getSettings()) }

    var isCreateSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isReminderSheetOpen by remember { mutableStateOf(false) }
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fabRotation by animateFloatAsState(
        targetValue = if (isCreateSheetOpen) 45f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "fabRotate"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = PaperCanvas,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    HapticFeedbackHelper.performSelectionHaptic(view, hapticFeedback)
                    isCreateSheetOpen = true
                },
                containerColor = ObsidianBlack,
                contentColor = PaperSurface,
                shape = CircleShape,
                modifier = Modifier
                    .size(58.dp)
                    .testTag("fab_add_task")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Task",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(fabRotation)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Header: Title, Streak Counter, Today's stats & Reminders
            item(key = "header") {
                MonochromeHeader(
                    streakDays = uiState.streakDays,
                    completedToday = uiState.completedTodayCount,
                    totalActive = uiState.totalActiveCount,
                    isReminderEnabled = reminderSettings.isEnabled,
                    onOpenReminderSettings = { isReminderSheetOpen = true }
                )
            }

            // Weekly Productivity Matrix
            item(key = "weekly_matrix") {
                WeeklyMatrixView(weeklyActivity = uiState.weeklyActivity)
            }

            // Category Filter Pills
            item(key = "category_chips") {
                CategoryChipsRow(
                    categories = uiState.categories,
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )
            }

            // Search Bar & Priority Filter
            item(key = "search_and_filter") {
                SearchBarAndFilter(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.setSearchQuery(it) },
                    selectedPriority = uiState.selectedPriority,
                    onPrioritySelected = { viewModel.setPriorityFilter(it) }
                )
            }

            item(key = "active_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TASKS (${uiState.activeTasks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (uiState.selectedCategory != "All" || uiState.selectedPriority != null || uiState.searchQuery.isNotBlank()) {
                        Text(
                            text = "Clear Filters",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                viewModel.selectCategory("All")
                                viewModel.setPriorityFilter(null)
                                viewModel.setSearchQuery("")
                            }
                        )
                    }
                }
            }

            // Active tasks list
            if (uiState.activeTasks.isEmpty()) {
                item(key = "empty_active_tasks") {
                    val emptyMsg = when {
                        uiState.searchQuery.isNotBlank() -> "No tasks match \"${uiState.searchQuery}\""
                        uiState.selectedCategory != "All" -> "No active tasks in ${uiState.selectedCategory}"
                        uiState.completedTasks.isNotEmpty() -> "All priorities accomplished."
                        else -> "No tasks yet."
                    }
                    val subMsg = when {
                        uiState.searchQuery.isNotBlank() || uiState.selectedCategory != "All" -> "Try resetting your search or category filter."
                        uiState.completedTasks.isNotEmpty() -> "Great work! Tap the + button to add more."
                        else -> "Tap the + button below to add your first task."
                    }
                    EmptyTasksView(
                        message = emptyMsg,
                        subMessage = subMsg
                    )
                }
            } else {
                items(
                    items = uiState.activeTasks,
                    key = { it.id }
                ) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                        onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) },
                        onAddSubtask = { subtaskTitle -> viewModel.addSubtaskToTask(task, subtaskTitle) },
                        onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task, subtaskId) },
                        onDeleteTask = { viewModel.deleteTask(task) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }

            // Completed tasks collapsible section
            if (uiState.completedTasks.isNotEmpty()) {
                item(key = "completed_section_header") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                            .clickable { viewModel.toggleShowCompleted() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPLETED (${uiState.completedTasks.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SlateGraphite,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = if (uiState.showCompletedSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Completed Section",
                            tint = SlateGraphite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (uiState.showCompletedSection) {
                    items(
                        items = uiState.completedTasks,
                        key = { "completed_${it.id}" }
                    ) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task, subtaskId) },
                            onAddSubtask = { subtaskTitle -> viewModel.addSubtaskToTask(task, subtaskTitle) },
                            onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task, subtaskId) },
                            onDeleteTask = { viewModel.deleteTask(task) },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Task Creation
    if (isCreateSheetOpen) {
        CreateTaskSheet(
            sheetState = sheetState,
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    isCreateSheetOpen = false
                }
            },
            onSaveTask = { title, description, category, tags, priority, dueDate, subtasks ->
                viewModel.createTask(title, description, category, tags, priority, dueDate, subtasks)
                scope.launch {
                    sheetState.hide()
                    isCreateSheetOpen = false
                }
            }
        )
    }

    // Modal Sheet for Daily Reminder Settings
    if (isReminderSheetOpen) {
        ReminderSettingsSheet(
            sheetState = reminderSheetState,
            onDismiss = {
                scope.launch {
                    reminderSheetState.hide()
                    isReminderSheetOpen = false
                }
            },
            onSettingsChanged = {
                reminderSettings = reminderPrefs.getSettings()
            }
        )
    }
}
