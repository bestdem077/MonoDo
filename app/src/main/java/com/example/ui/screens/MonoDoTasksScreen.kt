package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryChipsRow
import com.example.ui.components.EmptyTasksView
import com.example.ui.components.MonochromeHeader
import com.example.ui.components.SearchBarAndFilter
import com.example.ui.components.SwissInkStampBadge
import com.example.ui.components.TaskCard
import com.example.ui.theme.DateTileBackground
import com.example.ui.theme.PaperCanvas
import com.example.ui.theme.SlateGraphite
import com.example.ui.viewmodel.TaskViewModel

@Composable
fun MonoDoTasksScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val priorityFilter by viewModel.priorityFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val categoryList by viewModel.categoryList.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val activeTasks = remember(filteredTasks) { filteredTasks.filter { !it.isCompleted } }
    val completedTasks = remember(filteredTasks) { filteredTasks.filter { it.isCompleted } }

    val todayCompletedCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val totalTodayCount = remember(allTasks) { allTasks.size }
    val streakDays = remember(allTasks) { viewModel.calculateStreakDays() }

    var isCompletedSectionExpanded by remember { mutableStateOf(false) }
    var isCelebrationDismissed by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(activeTasks.isEmpty(), totalTodayCount) {
        if (activeTasks.isNotEmpty()) {
            isCelebrationDismissed = false
        }
    }

    // Show celebration when user has tasks and all active tasks are completed
    val showCelebration = totalTodayCount > 0 && activeTasks.isEmpty() && completedTasks.isNotEmpty() && !isCelebrationDismissed

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas)
            .testTag("mono_do_tasks_screen")
    ) {
        // Minimalist Swiss Header (Live Time Greeting & Date)
        MonochromeHeader(
            userName = userProfile.userName,
            streakDays = streakDays,
            completedTodayCount = todayCompletedCount,
            totalTodayCount = totalTodayCount
        )

        // Search Bar and Smart Sorting Toolbar
        SearchBarAndFilter(
            query = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            selectedPriority = priorityFilter,
            onPrioritySelected = { viewModel.setPriorityFilter(it) },
            selectedSortOrder = sortOrder,
            onSortOrderSelected = { viewModel.setSortOrder(it) }
        )

        // Category Filter Chips with Semantic Palette Picker
        CategoryChipsRow(
            categories = categoryList,
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.setSelectedCategory(it) },
            categoryCounts = categoryCounts,
            onAddCustomCategory = { name, colorHex -> viewModel.addCustomCategory(name, colorHex) }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Task List Area (Maximized Vertical Space for Tasks)
        val hasFilters = searchQuery.isNotBlank() || !selectedCategory.equals("All", ignoreCase = true) || priorityFilter != null

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
        ) {
            // 1. Swiss Ink-Stamp Celebration Badge
            if (showCelebration) {
                item(key = "celebration_stamp") {
                    SwissInkStampBadge(
                        visible = true,
                        onDismiss = { isCelebrationDismissed = true }
                    )
                }
            }

            // 2. Active Tasks or Empty State
            if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                item(key = "empty_tasks_view") {
                    EmptyTasksView(hasActiveFilters = hasFilters)
                }
            } else if (activeTasks.isNotEmpty()) {
                items(activeTasks, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                        onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                        onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                        onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task.id, subtaskId) },
                        onDeleteTask = { viewModel.deleteTask(task) },
                        onCategoryClick = { category -> viewModel.setSelectedCategory(category) },
                        onStartFocus = { viewModel.enterZenFocus(task) },
                        onRescheduleTask = { newDate -> viewModel.rescheduleTask(task, newDate) }
                    )
                }
            }

            // 4. Completed Collapsible Section
            if (completedTasks.isNotEmpty()) {
                item(key = "completed_header") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isCompletedSectionExpanded = !isCompletedSectionExpanded }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Completed (${completedTasks.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = SlateGraphite,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Icon(
                            imageVector = if (isCompletedSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Toggle Completed",
                            tint = SlateGraphite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isCompletedSectionExpanded) {
                    items(completedTasks, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                            onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                            onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task.id, subtaskId) },
                            onDeleteTask = { viewModel.deleteTask(task) },
                            onCategoryClick = { category -> viewModel.setSelectedCategory(category) },
                            onStartFocus = { viewModel.enterZenFocus(task) },
                            onRescheduleTask = { newDate -> viewModel.rescheduleTask(task, newDate) }
                        )
                    }
                }
            }
        }
    }
}
