package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.TaskEntity
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.ui.components.AppUpdateModalSheet
import com.example.ui.components.CreateTaskSheet
import com.example.ui.components.MonoDoBottomBar
import com.example.ui.navigation.NavigationTab
import com.example.ui.theme.MonoDoThemeTokens
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoHomeScreen(
    viewModel: TaskViewModel = viewModel(),
    openCreateTaskExternal: Boolean = false,
    onConsumeOpenCreateTaskExternal: () -> Unit = {},
    openBirthdaysExternal: Boolean = false,
    onConsumeOpenBirthdaysExternal: () -> Unit = {}
) {
    val colors = MonoDoThemeTokens.colors
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val categoryList by viewModel.categoryList.collectAsState()
    val savedTemplates by viewModel.allTemplates.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val appUpdateState by viewModel.appUpdateState.collectAsState()
    val isUpdateSheetVisible by viewModel.isUpdateSheetVisible.collectAsState()

    var currentTab by remember { mutableStateOf(NavigationTab.TASKS) }
    var selectedToolTab by remember { mutableStateOf(ToolSubTab.NOTES) }
    var isCreateTaskSheetOpen by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var preselectedDueDate by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val updateSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    val zenActiveTask by viewModel.zenActiveTask.collectAsState()

    // Re-sync Pomodoro countdown if user paused/stopped from notification or session finished while backgrounded
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.syncPomodoroFromPersistence()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Handle 1-tap "+ Task" launch from Android Home Screen Widget or Notification Action
    LaunchedEffect(openCreateTaskExternal, userProfile.isOnboarded) {
        if (openCreateTaskExternal && userProfile.isOnboarded) {
            currentTab = NavigationTab.TASKS
            editingTask = null
            preselectedDueDate = null
            isCreateTaskSheetOpen = true
            onConsumeOpenCreateTaskExternal()
        }
    }

    // Handle deep link from Birthday Notification -> opens Tools -> Birthdays directly
    LaunchedEffect(openBirthdaysExternal, userProfile.isOnboarded) {
        if (openBirthdaysExternal && userProfile.isOnboarded) {
            selectedToolTab = ToolSubTab.BIRTHDAYS
            currentTab = NavigationTab.TOOLS
            onConsumeOpenBirthdaysExternal()
        }
    }

    // Collect global Undo / Feedback Snackbar events
    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { event ->
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = event.actionLabel,
                withDismissAction = false,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                event.onAction?.invoke()
            }
        }
    }

    // Back press on secondary tabs returns cleanly to Tasks tab instead of immediately exiting
    BackHandler(enabled = userProfile.isOnboarded && zenActiveTask == null && currentTab != NavigationTab.TASKS) {
        currentTab = NavigationTab.TASKS
    }

    // Fullscreen Zen Focus Mode
    if (zenActiveTask != null) {
        val activeZen = zenActiveTask!!
        ZenFocusModeScreen(
            task = activeZen,
            onExit = { viewModel.exitZenFocus() },
            onCompleteTask = {
                viewModel.toggleTaskCompletion(activeZen)
            },
            onToggleSubtask = { subtaskId ->
                viewModel.toggleSubtask(activeZen.id, subtaskId)
            },
            onLogFocusSession = { minutes ->
                viewModel.logFocusSession(
                    durationMinutes = minutes,
                    taskId = activeZen.id,
                    taskTitle = activeZen.title
                )
            }
        )
        return
    }

    // Check if onboarding is completed
    if (!userProfile.isOnboarded) {
        MonoDoOnboardingScreen(
            initialUserName = userProfile.userName,
            initialBirthDate = userProfile.birthDate,
            initialBirthTimestamp = userProfile.birthTimestamp,
            initialReminderHour = userProfile.reminderHour,
            initialReminderMinute = userProfile.reminderMinute,
            onComplete = { name, birthDate, birthTimestamp, isReminderEnabled, reminderHour, reminderMinute ->
                viewModel.saveUserProfile(
                    name = name,
                    workspaceTitle = "MonoDo",
                    birthDate = birthDate,
                    birthTimestamp = birthTimestamp,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute
                )

                // Save reminder preferences and schedule if enabled
                val reminderPrefs = ReminderPreferences(context)
                reminderPrefs.saveSettings(isReminderEnabled, reminderHour, reminderMinute)
                if (isReminderEnabled) {
                    ReminderScheduler.scheduleDailyReminder(context, reminderHour, reminderMinute)
                }
            }
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("mono_do_home_scaffold"),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) { data ->
                val containerBg = if (colors.isDark) colors.surfaceVariant else colors.primaryInk
                val contentFg = if (colors.isDark) colors.primaryInk else Color.White
                Snackbar(
                    modifier = Modifier
                        .border(1.dp, if (colors.isDark) colors.softBorder else Color.Transparent, RoundedCornerShape(14.dp))
                        .testTag("monodo_snackbar"),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = containerBg,
                    contentColor = contentFg,
                    action = data.visuals.actionLabel?.let { label ->
                        {
                            TextButton(
                                onClick = { data.performAction() },
                                modifier = Modifier.testTag("snackbar_undo_button")
                            ) {
                                Text(
                                    text = label.uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (colors.isDark) colors.accentHover else Color(0xFF93C5FD),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                ) {
                    Text(
                        text = data.visuals.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentFg,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        bottomBar = {
            MonoDoBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onAddClick = {
                    editingTask = null
                    preselectedDueDate = null
                    isCreateTaskSheetOpen = true
                },
                isPomodoroRunning = pomodoroState.isRunning
            )
        },
        containerColor = colors.canvas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.TASKS -> MonoDoTasksScreen(
                    viewModel = viewModel,
                    onEditTask = { task ->
                        editingTask = task
                        preselectedDueDate = null
                        isCreateTaskSheetOpen = true
                    },
                    onOpenBirthdays = {
                        selectedToolTab = ToolSubTab.BIRTHDAYS
                        currentTab = NavigationTab.TOOLS
                    },
                    onAddTaskForDate = { dateMillis ->
                        editingTask = null
                        preselectedDueDate = dateMillis
                        isCreateTaskSheetOpen = true
                    }
                )
                NavigationTab.STATS -> MonoDoStatsScreen(viewModel = viewModel)
                NavigationTab.TOOLS -> MonoDoToolsScreen(
                    viewModel = viewModel,
                    selectedTool = selectedToolTab,
                    onSelectTool = { selectedToolTab = it },
                    onQuickAddTask = {
                        editingTask = null
                        preselectedDueDate = null
                        isCreateTaskSheetOpen = true
                    }
                )
                NavigationTab.SETTINGS -> MonoDoSettingsScreen(
                    viewModel = viewModel,
                    onResetAll = {
                        currentTab = NavigationTab.TASKS
                    }
                )
            }
        }

        // Unified Create & Edit Task Bottom Sheet
        if (isCreateTaskSheetOpen || editingTask != null) {
            CreateTaskSheet(
                sheetState = sheetState,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        isCreateTaskSheetOpen = false
                        editingTask = null
                        preselectedDueDate = null
                    }
                },
                availableCategories = categoryList,
                onAddCustomCategory = { name, colorHex ->
                    viewModel.addCustomCategory(name, colorHex)
                },
                taskToEdit = editingTask,
                initialDueDate = preselectedDueDate,
                savedTemplates = savedTemplates,
                onSaveAsTemplate = { title, category, priority, subtasks ->
                    viewModel.saveRoutineTemplate(title, category, priority, subtasks)
                },
                onDeleteTemplate = { tpl ->
                    viewModel.deleteRoutineTemplate(tpl)
                },
                onCreateTask = { title, description, category, priority, dueDate, subtasks, recurrence, reminderAt ->
                    viewModel.createTask(
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        subtaskTitles = subtasks,
                        recurrence = recurrence,
                        reminderAt = reminderAt
                    )
                },
                onUpdateTask = { existing, title, description, category, priority, dueDate, subtasks, recurrence, reminderAt ->
                    viewModel.updateTask(
                        existingTask = existing,
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        subtasks = subtasks,
                        recurrence = recurrence,
                        reminderAt = reminderAt
                    )
                }
            )
        }

        // Global In-App GitHub Release Update Sheet
        if (isUpdateSheetVisible) {
            AppUpdateModalSheet(
                sheetState = updateSheetState,
                state = appUpdateState,
                onDismiss = {
                    coroutineScope.launch {
                        updateSheetState.hide()
                        viewModel.closeUpdateSheet()
                    }
                },
                onCheckNow = { viewModel.checkForAppUpdates(userInitiated = true) },
                onStartDownload = { viewModel.startUpdateDownload() },
                onCancelDownload = { viewModel.cancelUpdateDownload() }
            )
        }
    }
}
