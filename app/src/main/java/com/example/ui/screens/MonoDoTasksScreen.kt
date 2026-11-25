package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.example.data.model.TaskEntity
import com.example.data.model.TaskSortOrder
import com.example.ui.components.CategoryChipsRow
import com.example.ui.components.EmptyTasksView
import com.example.ui.components.MonochromeHeader
import com.example.ui.components.MonthlyCalendarView
import com.example.ui.components.SearchBarAndFilter
import com.example.ui.components.SwissInkStampBadge
import com.example.ui.components.TaskCard
import com.example.ui.components.UpcomingBirthdayBanner
import com.example.ui.components.UpdateAvailableBanner
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.util.LocalDayUtils
import com.example.ui.viewmodel.TaskViewModel

private enum class TaskSmartSection(val key: String, val label: String) {
    OVERDUE("overdue", "OVERDUE"),
    TODAY("today", "TODAY"),
    UPCOMING("upcoming", "UPCOMING"),
    NO_DATE("no_date", "NO DATE")
}

@Composable
fun MonoDoTasksScreen(
    viewModel: TaskViewModel,
    onEditTask: (TaskEntity) -> Unit = {},
    onOpenBirthdays: () -> Unit = {},
    onAddTaskForDate: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = MonoDoThemeTokens.colors
    val allTasks by viewModel.allTasks.collectAsState()
    val allBirthdays by viewModel.allBirthdays.collectAsState()
    val filteredTasks by viewModel.filteredTasks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val priorityFilter by viewModel.priorityFilter.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val categoryList by viewModel.categoryList.collectAsState()
    val categoryCounts by viewModel.categoryCounts.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeSwipedTaskId by viewModel.activeSwipedTaskId.collectAsState()
    val dismissedSelfBirthdayYear by viewModel.dismissedSelfBirthdayYear.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val dismissedUpdateTag by viewModel.dismissedUpdateTag.collectAsState()

    var isCalendarView by remember { mutableStateOf(false) }

    val activeTasks = remember(filteredTasks) { filteredTasks.filter { !it.isCompleted } }
    val completedTasks = remember(filteredTasks) { filteredTasks.filter { it.isCompleted } }

    val todayCompletedCount = remember(allTasks) { allTasks.count { it.isCompleted } }
    val totalTodayCount = remember(allTasks) { allTasks.size }
    val streakDays = remember(allTasks) { viewModel.calculateStreakDays() }

    val hasFilters = searchQuery.isNotBlank() ||
            !selectedCategory.equals("All", ignoreCase = true) ||
            priorityFilter != null

    // Strictly check global tasks so empty filtered categories NEVER trigger false completion banner
    val allGlobalTasksCompleted = remember(allTasks) {
        allTasks.isNotEmpty() && allTasks.none { !it.isCompleted }
    }

    var isCompletedSectionExpanded by remember { mutableStateOf(false) }
    var isCelebrationDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(allGlobalTasksCompleted) {
        if (!allGlobalTasksCompleted) {
            isCelebrationDismissed = false
        }
    }

    val showCelebration = allGlobalTasksCompleted && !hasFilters && !isCelebrationDismissed

    // Smart section grouping for Default / Due Date sort orders
    val useSmartSections = sortOrder == TaskSortOrder.DEFAULT || sortOrder == TaskSortOrder.DUE_DATE
    val collapsedSections = remember { mutableStateMapOf<TaskSmartSection, Boolean>() }

    val sectionedActiveTasks = remember(activeTasks, useSmartSections) {
        if (!useSmartSections) {
            emptyList()
        } else {
            val overdue = mutableListOf<TaskEntity>()
            val today = mutableListOf<TaskEntity>()
            val upcoming = mutableListOf<TaskEntity>()
            val noDate = mutableListOf<TaskEntity>()

            activeTasks.forEach { task ->
                when {
                    LocalDayUtils.isOverdue(task.dueDate) -> overdue.add(task)
                    LocalDayUtils.isToday(task.dueDate) -> today.add(task)
                    task.dueDate != null -> upcoming.add(task)
                    else -> noDate.add(task)
                }
            }

            listOf(
                TaskSmartSection.OVERDUE to overdue,
                TaskSmartSection.TODAY to today,
                TaskSmartSection.UPCOMING to upcoming,
                TaskSmartSection.NO_DATE to noDate
            ).filter { it.second.isNotEmpty() }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.canvas)
            .testTag("mono_do_tasks_screen")
    ) {
        // Minimalist Swiss Header (Live Time Greeting, Calendar Toggle & Self-Birthday Banner)
        MonochromeHeader(
            userName = userProfile.userName,
            streakDays = streakDays,
            completedTodayCount = todayCompletedCount,
            totalTodayCount = totalTodayCount,
            isCalendarView = isCalendarView,
            onToggleCalendarView = { isCalendarView = !isCalendarView },
            userBirthTimestamp = userProfile.birthTimestamp,
            dismissedSelfBirthdayYear = dismissedSelfBirthdayYear,
            onDismissSelfBirthday = { viewModel.dismissSelfBirthdayBanner() }
        )

        UpdateAvailableBanner(
            state = appUpdateState,
            dismissedTag = dismissedUpdateTag,
            onOpenSheet = { viewModel.openUpdateSheet() },
            onDismiss = { tag -> viewModel.dismissUpdateBanner(tag) }
        )

        AnimatedContent(
            targetState = isCalendarView,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tasksCalendarSwitch",
            modifier = Modifier.weight(1f)
        ) { showCalendar ->
            if (showCalendar) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 96.dp)
                ) {
                    MonthlyCalendarView(
                        allTasks = allTasks,
                        allBirthdays = allBirthdays,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onEditTask = onEditTask,
                        onAddTaskForDate = onAddTaskForDate,
                        onOpenBirthdays = onOpenBirthdays
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
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

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
                    ) {
                        // 0. Smart Upcoming Birthday Highlight Banner (auto-hides when no birthday in next 3 days)
                        if (allBirthdays.isNotEmpty() && !hasFilters) {
                            item(key = "upcoming_birthday_banner") {
                                Box(modifier = Modifier.animateItem()) {
                                    UpcomingBirthdayBanner(
                                        birthdays = allBirthdays,
                                        onOpenBirthdays = onOpenBirthdays
                                    )
                                }
                            }
                        }

                        // 1. Sleek Minimalist All-Complete Banner (only when ALL global tasks are done)
                        if (showCelebration) {
                            item(key = "celebration_stamp") {
                                Box(modifier = Modifier.animateItem()) {
                                    SwissInkStampBadge(
                                        visible = true,
                                        completedCount = todayCompletedCount,
                                        onDismiss = { isCelebrationDismissed = true }
                                    )
                                }
                            }
                        }

                        // 2. Active Tasks (Smart Sections or Flat Sorted List) or Empty State
                        if (activeTasks.isEmpty() && completedTasks.isEmpty()) {
                            item(key = "empty_tasks_view") {
                                EmptyTasksView(hasActiveFilters = hasFilters)
                            }
                        } else if (activeTasks.isNotEmpty()) {
                            if (useSmartSections && sectionedActiveTasks.size > 1 || (useSmartSections && sectionedActiveTasks.any { it.first == TaskSmartSection.OVERDUE || it.first == TaskSmartSection.TODAY })) {
                                sectionedActiveTasks.forEach { (section, sectionTasks) ->
                                    val isCollapsed = collapsedSections[section] == true
                                    val dotColor = when (section) {
                                        TaskSmartSection.OVERDUE -> colors.danger
                                        TaskSmartSection.TODAY -> colors.accent
                                        TaskSmartSection.UPCOMING -> colors.primaryInk
                                        TaskSmartSection.NO_DATE -> colors.secondaryInk
                                    }
                                    val labelColor = when (section) {
                                        TaskSmartSection.OVERDUE -> colors.danger
                                        TaskSmartSection.TODAY -> colors.accent
                                        else -> colors.secondaryInk
                                    }

                                    item(key = "section_header_${section.key}") {
                                        Box(modifier = Modifier.animateItem()) {
                                            SmartSectionHeader(
                                                title = section.label,
                                                count = sectionTasks.size,
                                                dotColor = dotColor,
                                                labelColor = labelColor,
                                                isExpanded = !isCollapsed,
                                                onToggle = { collapsedSections[section] = !isCollapsed },
                                                testTag = "smart_section_${section.key}"
                                            )
                                        }
                                    }

                                    if (!isCollapsed) {
                                        items(sectionTasks, key = { it.id }) { task ->
                                            Box(modifier = Modifier.animateItem()) {
                                                TaskCard(
                                                    task = task,
                                                    onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                                    onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                                                    onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                                                    onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task.id, subtaskId) },
                                                    onDeleteTask = { viewModel.deleteTask(task) },
                                                    onCategoryClick = { category -> viewModel.setSelectedCategory(category) },
                                                    onStartFocus = { viewModel.enterZenFocus(task) },
                                                    onRescheduleTask = { newDate -> viewModel.rescheduleTask(task, newDate) },
                                                    onEditTask = { onEditTask(task) },
                                                    isSwipeOpen = activeSwipedTaskId == task.id,
                                                    onSwipeOpenChanged = { open ->
                                                        viewModel.setActiveSwipedTask(
                                                            if (open) task.id else if (activeSwipedTaskId == task.id) null else activeSwipedTaskId
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                items(activeTasks, key = { it.id }) { task ->
                                    Box(modifier = Modifier.animateItem()) {
                                        TaskCard(
                                            task = task,
                                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                                            onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                                            onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task.id, subtaskId) },
                                            onDeleteTask = { viewModel.deleteTask(task) },
                                            onCategoryClick = { category -> viewModel.setSelectedCategory(category) },
                                            onStartFocus = { viewModel.enterZenFocus(task) },
                                            onRescheduleTask = { newDate -> viewModel.rescheduleTask(task, newDate) },
                                            onEditTask = { onEditTask(task) },
                                            isSwipeOpen = activeSwipedTaskId == task.id,
                                            onSwipeOpenChanged = { open ->
                                                viewModel.setActiveSwipedTask(
                                                    if (open) task.id else if (activeSwipedTaskId == task.id) null else activeSwipedTaskId
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Completed Collapsible Section
                        if (completedTasks.isNotEmpty()) {
                            item(key = "completed_header") {
                                Box(modifier = Modifier.animateItem()) {
                                    Column {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { isCompletedSectionExpanded = !isCompletedSectionExpanded }
                                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                                .testTag("completed_section_header"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "COMPLETED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.secondaryInk,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 1.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${completedTasks.size}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                                    color = colors.secondaryInk,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Icon(
                                                imageVector = if (isCompletedSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Toggle Completed",
                                                tint = colors.secondaryInk,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (isCompletedSectionExpanded) {
                                items(completedTasks, key = { it.id }) { task ->
                                    Box(modifier = Modifier.animateItem()) {
                                        TaskCard(
                                            task = task,
                                            onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                            onToggleSubtask = { subtaskId -> viewModel.toggleSubtask(task.id, subtaskId) },
                                            onAddSubtask = { title -> viewModel.addSubtask(task.id, title) },
                                            onDeleteSubtask = { subtaskId -> viewModel.deleteSubtask(task.id, subtaskId) },
                                            onDeleteTask = { viewModel.deleteTask(task) },
                                            onCategoryClick = { category -> viewModel.setSelectedCategory(category) },
                                            onStartFocus = { viewModel.enterZenFocus(task) },
                                            onRescheduleTask = { newDate -> viewModel.rescheduleTask(task, newDate) },
                                            onEditTask = { onEditTask(task) },
                                            isSwipeOpen = activeSwipedTaskId == task.id,
                                            onSwipeOpenChanged = { open ->
                                                viewModel.setActiveSwipedTask(
                                                    if (open) task.id else if (activeSwipedTaskId == task.id) null else activeSwipedTaskId
                                                )
                                            }
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

@Composable
private fun SmartSectionHeader(
    title: String,
    count: Int,
    dotColor: Color,
    labelColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String
) {
    val colors = MonoDoThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp, horizontal = 4.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(colors.tileBg)
                    .padding(horizontal = 7.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                    color = colors.secondaryInk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }

        Icon(
            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = "Toggle $title",
            tint = colors.secondaryInk,
            modifier = Modifier.size(18.dp)
        )
    }
}
