package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryChipsRow
import com.example.ui.components.EmptyTasksView
import com.example.ui.components.MonochromeHeader
import com.example.ui.components.SearchBarAndFilter
import com.example.ui.components.TaskCard
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.MonoDoUiState
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun MonoDoTasksScreen(
    uiState: MonoDoUiState,
    viewModel: TaskViewModel,
    isReminderEnabled: Boolean,
    onOpenReminderSettings: () -> Unit,
    userName: String = "",
    workspaceTitle: String = "MonoDo",
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Header: Title, Streak Counter, Today's stats & Reminders
        item(key = "header") {
            MonochromeHeader(
                streakDays = uiState.streakDays,
                completedToday = uiState.completedTodayCount,
                totalActive = uiState.totalActiveCount,
                userName = userName,
                workspaceTitle = workspaceTitle,
                isReminderEnabled = isReminderEnabled,
                onOpenReminderSettings = onOpenReminderSettings
            )
        }

        // Category Filter Pills & Color-Coded Bar
        item(key = "category_chips") {
            CategoryChipsRow(
                categories = uiState.categories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) },
                categoryCounts = uiState.categoryCounts,
                onAddCustomCategory = { newCat ->
                    viewModel.selectCategory(newCat)
                }
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

        // Section header with active count and active filter indicator
        item(key = "active_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "TASKS (${uiState.activeTasks.size})",
                        style = MaterialTheme.typography.labelSmall,
                        color = SlateGraphite,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    if (uiState.selectedCategory != "All") {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  ${uiState.selectedCategory}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ObsidianBlack,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                    uiState.searchQuery.isNotBlank() || uiState.selectedCategory != "All" -> "Try resetting your search or tapping \"All\" in the category filter bar."
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
                    onCategoryClick = { viewModel.selectCategory(it) },
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
                        onCategoryClick = { viewModel.selectCategory(it) },
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
