package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.data.user.UserProfileManager
import com.example.ui.components.CreateTaskSheet
import com.example.ui.components.MonoDoBottomBar
import com.example.ui.components.ReminderSettingsSheet
import com.example.ui.navigation.NavigationTab
import com.example.ui.theme.PaperCanvas
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

    // User Profile & Onboarding
    val userProfileManager = remember { UserProfileManager(context.applicationContext) }
    var userProfile by remember { mutableStateOf(userProfileManager.getProfile()) }
    var isOnboardingActive by remember { mutableStateOf(!userProfile.isOnboarded) }

    // Daily Reminder Preferences
    val reminderPrefs = remember { ReminderPreferences(context) }
    var reminderSettings by remember { mutableStateOf(reminderPrefs.getSettings()) }

    var currentTab by remember { mutableStateOf(NavigationTab.TASKS) }

    var isCreateSheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isReminderSheetOpen by remember { mutableStateOf(false) }
    val reminderSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.removeSampleDataIfPresent()
    }

    // First open flow: Onboarding screen to enter name and customize app name
    if (isOnboardingActive) {
        MonoDoOnboardingScreen(
            initialUserName = userProfile.userName,
            initialWorkspaceTitle = userProfile.workspaceTitle,
            onComplete = { enteredName, enteredTitle ->
                userProfileManager.saveProfile(enteredName, enteredTitle)
                userProfile = userProfileManager.getProfile()
                isOnboardingActive = false
            }
        )
        return
    }

    // BackHandler: If user is on Stats, Tools, or Settings tab, return to Tasks tab
    BackHandler(enabled = currentTab != NavigationTab.TASKS) {
        currentTab = NavigationTab.TASKS
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(PaperCanvas),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = PaperCanvas,
        bottomBar = {
            MonoDoBottomBar(
                currentTab = currentTab,
                onTabSelected = { selected ->
                    currentTab = selected
                },
                onAddClick = {
                    isCreateSheetOpen = true
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 200),
                label = "tabTransition"
            ) { tab ->
                when (tab) {
                    NavigationTab.TASKS -> {
                        MonoDoTasksScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            isReminderEnabled = reminderSettings.isEnabled,
                            onOpenReminderSettings = { isReminderSheetOpen = true },
                            userName = userProfile.userName,
                            workspaceTitle = userProfile.workspaceTitle
                        )
                    }

                    NavigationTab.STATS -> {
                        MonoDoStatsScreen(
                            uiState = uiState
                        )
                    }

                    NavigationTab.TOOLS -> {
                        MonoDoToolsScreen(viewModel = viewModel)
                    }

                    NavigationTab.SETTINGS -> {
                        MonoDoSettingsScreen(
                            viewModel = viewModel,
                            userProfile = userProfile,
                            onUpdateProfile = { updatedName, updatedTitle ->
                                userProfileManager.saveProfile(updatedName, updatedTitle)
                                userProfile = userProfileManager.getProfile()
                            },
                            isReminderEnabled = reminderSettings.isEnabled,
                            onOpenReminderSettings = { isReminderSheetOpen = true },
                            onToggleReminder = { enabled ->
                                val current = reminderPrefs.getSettings()
                                reminderPrefs.saveSettings(enabled, current.hour, current.minute)
                                reminderSettings = reminderPrefs.getSettings()
                                if (enabled) {
                                    ReminderScheduler.scheduleDailyReminder(context, current.hour, current.minute)
                                } else {
                                    ReminderScheduler.cancelDailyReminder(context)
                                }
                            },
                            onResetAllData = {
                                viewModel.resetAllData()
                                userProfileManager.resetProfile()
                                userProfile = userProfileManager.getProfile()
                                isOnboardingActive = true
                                currentTab = NavigationTab.TASKS
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Sheet for Task Creation (Triggered by centered '+' FAB)
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
