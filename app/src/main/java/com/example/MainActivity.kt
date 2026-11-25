package com.example

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.reminder.NotificationHelper
import com.example.data.reminder.ReminderScheduler
import com.example.data.user.UserProfileManager
import com.example.ui.components.AppLockOverlay
import com.example.ui.screens.MonoDoHomeScreen
import com.example.ui.theme.MonoDoTheme
import com.example.ui.util.AppLockHelper
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_CREATE_TASK = "extra_open_create_task"
        const val EXTRA_OPEN_BIRTHDAYS = "extra_open_birthdays"
    }

    private val openCreateTaskRequest = mutableStateOf(false)
    private val openBirthdaysRequest = mutableStateOf(false)
    private val isAppLocked = mutableStateOf(false)
    private val lockErrorMessage = mutableStateOf<String?>(null)
    private var isLaunchingSystemCredential = false
    private var hasLeftForeground = false

    private val fallbackCredentialLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLaunchingSystemCredential = false
        if (result.resultCode == Activity.RESULT_OK) {
            isAppLocked.value = false
            lockErrorMessage.value = null
        } else {
            lockErrorMessage.value = "Authentication canceled. Tap Unlock to try again."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val profileManager = UserProfileManager(applicationContext)
        if (profileManager.isAppLockEnabled() && AppLockHelper.isDeviceSecure(this)) {
            isAppLocked.value = true
        }

        if (intent?.getBooleanExtra(EXTRA_OPEN_CREATE_TASK, false) == true) {
            openCreateTaskRequest.value = true
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_BIRTHDAYS, false) == true) {
            openBirthdaysRequest.value = true
        }

        // Ensure notification channels exist & all active AlarmManager schedules are fresh
        NotificationHelper.ensureChannels(applicationContext)
        lifecycleScope.launch(Dispatchers.IO) {
            ReminderScheduler.rescheduleAllActiveReminders(applicationContext)
        }

        setContent {
            val taskViewModel: TaskViewModel = viewModel()
            val themeMode by taskViewModel.themeMode.collectAsState()
            val userProfile by taskViewModel.userProfile.collectAsState()
            val appLockEnabled by taskViewModel.isAppLockEnabled.collectAsState()

            MonoDoTheme(themeMode = themeMode) {
                if (isAppLocked.value && appLockEnabled && userProfile.isOnboarded) {
                    AppLockOverlay(
                        workspaceTitle = userProfile.workspaceTitle,
                        errorMessage = lockErrorMessage.value,
                        onUnlockClick = { triggerAppUnlock() }
                    )
                } else {
                    MonoDoHomeScreen(
                        viewModel = taskViewModel,
                        openCreateTaskExternal = openCreateTaskRequest.value,
                        onConsumeOpenCreateTaskExternal = { openCreateTaskRequest.value = false },
                        openBirthdaysExternal = openBirthdaysRequest.value,
                        onConsumeOpenBirthdaysExternal = { openBirthdaysRequest.value = false }
                    )
                }
            }
        }
    }

    private fun triggerAppUnlock() {
        lockErrorMessage.value = null
        AppLockHelper.authenticate(
            activity = this,
            title = "Unlock MonoDo",
            subtitle = "Use your fingerprint or screen PIN to access your workspace",
            onSuccess = {
                isAppLocked.value = false
                lockErrorMessage.value = null
            },
            onError = { msg ->
                lockErrorMessage.value = msg
            },
            onFallbackCredentialIntent = { intent ->
                isLaunchingSystemCredential = true
                fallbackCredentialLauncher.launch(intent)
            }
        )
    }

    override fun onStop() {
        super.onStop()
        if (!isLaunchingSystemCredential && !isChangingConfigurations) {
            hasLeftForeground = true
        }
    }

    override fun onStart() {
        super.onStart()
        if (hasLeftForeground && !isLaunchingSystemCredential) {
            hasLeftForeground = false
            val profileManager = UserProfileManager(applicationContext)
            if (profileManager.isAppLockEnabled() && AppLockHelper.isDeviceSecure(this)) {
                isAppLocked.value = true
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_CREATE_TASK, false)) {
            openCreateTaskRequest.value = true
        }
        if (intent.getBooleanExtra(EXTRA_OPEN_BIRTHDAYS, false)) {
            openBirthdaysRequest.value = true
        }
    }
}
