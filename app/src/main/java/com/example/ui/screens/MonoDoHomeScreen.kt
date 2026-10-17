package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.ui.components.CreateTaskSheet
import com.example.ui.components.MonoDoBottomBar
import com.example.ui.navigation.NavigationTab
import com.example.ui.theme.PaperCanvas
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonoDoHomeScreen(
    viewModel: TaskViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()
    val categoryList by viewModel.categoryList.collectAsState()

    var currentTab by remember { mutableStateOf(NavigationTab.TASKS) }
    var isCreateTaskSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val zenActiveTask by viewModel.zenActiveTask.collectAsState()

    // Fullscreen Zen Focus Mode
    if (zenActiveTask != null) {
        ZenFocusModeScreen(
            task = zenActiveTask!!,
            onExit = { viewModel.exitZenFocus() },
            onCompleteTask = {
                viewModel.toggleTaskCompletion(zenActiveTask!!)
            },
            onToggleSubtask = { subtaskId ->
                viewModel.toggleSubtask(zenActiveTask!!.id, subtaskId)
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
            .background(PaperCanvas)
            .windowInsetsPadding(WindowInsets.statusBars)
            .testTag("mono_do_home_scaffold"),
        bottomBar = {
            MonoDoBottomBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onAddClick = { isCreateTaskSheetOpen = true }
            )
        },
        containerColor = PaperCanvas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                NavigationTab.TASKS -> MonoDoTasksScreen(viewModel = viewModel)
                NavigationTab.STATS -> MonoDoStatsScreen(viewModel = viewModel)
                NavigationTab.TOOLS -> MonoDoToolsScreen(viewModel = viewModel)
                NavigationTab.SETTINGS -> MonoDoSettingsScreen(
                    viewModel = viewModel,
                    onResetAll = {
                        currentTab = NavigationTab.TASKS
                    }
                )
            }
        }

        // Create Task Bottom Sheet
        if (isCreateTaskSheetOpen) {
            CreateTaskSheet(
                sheetState = sheetState,
                onDismiss = {
                    coroutineScope.launch {
                        sheetState.hide()
                        isCreateTaskSheetOpen = false
                    }
                },
                availableCategories = categoryList,
                onAddCustomCategory = { name, colorHex ->
                    viewModel.addCustomCategory(name, colorHex)
                },
                onCreateTask = { title, description, category, priority, dueDate, subtasks ->
                    viewModel.createTask(
                        title = title,
                        description = description,
                        category = category,
                        priority = priority,
                        dueDate = dueDate,
                        subtaskTitles = subtasks
                    )
                }
            )
        }
    }
}
