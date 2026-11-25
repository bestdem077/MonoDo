package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BirthdayEntity
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.FocusSessionEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.NoteEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrence
import com.example.data.model.TaskSortOrder
import com.example.data.reminder.NotificationHelper
import com.example.data.reminder.ReminderScheduler
import com.example.data.update.AppUpdateManager
import com.example.data.update.AppUpdateState
import com.example.data.update.GitHubReleaseInfo
import com.example.data.update.VersionComparator
import com.example.data.user.AppThemeMode
import com.example.data.user.UserProfile
import com.example.data.user.UserProfileManager
import com.example.ui.components.QuickDateCalculator
import com.example.ui.components.QuickDeadlinePreset
import com.example.ui.theme.CategoryTheme
import com.example.ui.util.LocalDayUtils
import com.example.widget.MonoDoWidgetProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class SnackbarEvent(
    val message: String,
    val actionLabel: String? = null,
    val onAction: (suspend () -> Unit)? = null
)

data class PomodoroUiState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val secondsRemaining: Int = 25 * 60
) {
    val totalSeconds: Int
        get() = (if (isBreak) breakMinutes else focusMinutes) * 60
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()
    private val habitDao = database.habitDao()
    private val customCategoryDao = database.customCategoryDao()
    private val focusSessionDao = database.focusSessionDao()
    private val birthdayDao = database.birthdayDao()
    private val routineTemplateDao = database.routineTemplateDao()
    private val userProfileManager = UserProfileManager(application)

    // Raw streams from DB
    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = habitDao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Full habit logs stream for accurate streaks, 7-day matrix, and 28-day heatmap across midnight
    val habitLogs: StateFlow<List<HabitLogEntity>> = habitDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val focusSessions: StateFlow<List<FocusSessionEntity>> = focusSessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategoriesFromDb: StateFlow<List<CustomCategoryEntity>> = customCategoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBirthdays: StateFlow<List<BirthdayEntity>> = birthdayDao.getAllBirthdays()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTemplates: StateFlow<List<RoutineTemplateEntity>> = routineTemplateDao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Theme Mode, App Lock, Pinned Today Notification, and Self-Birthday Dismissal state
    private val _themeMode = MutableStateFlow(userProfileManager.getThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _isAppLockEnabled = MutableStateFlow(userProfileManager.isAppLockEnabled())
    val isAppLockEnabled: StateFlow<Boolean> = _isAppLockEnabled.asStateFlow()

    private val _isPinnedTodayEnabled = MutableStateFlow(userProfileManager.isPinnedTodayNotificationEnabled())
    val isPinnedTodayEnabled: StateFlow<Boolean> = _isPinnedTodayEnabled.asStateFlow()

    private val _dismissedSelfBirthdayYear = MutableStateFlow(userProfileManager.getDismissedSelfBirthdayYear())
    val dismissedSelfBirthdayYear: StateFlow<Int> = _dismissedSelfBirthdayYear.asStateFlow()

    // UI Filter and Sort states for tasks
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    val priorityFilter: StateFlow<TaskPriority?> = _priorityFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(TaskSortOrder.DEFAULT)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    // Single-open swipe card coordinator
    private val _activeSwipedTaskId = MutableStateFlow<Long?>(null)
    val activeSwipedTaskId: StateFlow<Long?> = _activeSwipedTaskId.asStateFlow()

    // Snackbar / Undo event bus
    private val _snackbarEvents = MutableSharedFlow<SnackbarEvent>(extraBufferCapacity = 4)
    val snackbarEvents: SharedFlow<SnackbarEvent> = _snackbarEvents.asSharedFlow()

    // Zen Focus Mode state (null if inactive)
    private val _zenActiveTask = MutableStateFlow<TaskEntity?>(null)
    val zenActiveTask: StateFlow<TaskEntity?> = _zenActiveTask.asStateFlow()

    // Persistent Pomodoro Timer State (survives tab switches)
    private val _pomodoroState = MutableStateFlow(PomodoroUiState())
    val pomodoroState: StateFlow<PomodoroUiState> = _pomodoroState.asStateFlow()
    private var pomodoroJob: Job? = null
    private var pomodoroEndTimeMillis: Long = 0L

    // Notes filter states
    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    private val _noteSelectedCategory = MutableStateFlow("All")
    val noteSelectedCategory: StateFlow<String> = _noteSelectedCategory.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow(userProfileManager.getProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    // In-App GitHub Release Updater State
    private val _appUpdateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val appUpdateState: StateFlow<AppUpdateState> = _appUpdateState.asStateFlow()

    private val _isUpdateSheetVisible = MutableStateFlow(false)
    val isUpdateSheetVisible: StateFlow<Boolean> = _isUpdateSheetVisible.asStateFlow()

    private val _dismissedUpdateTag = MutableStateFlow(userProfileManager.getDismissedUpdateTag())
    val dismissedUpdateTag: StateFlow<String> = _dismissedUpdateTag.asStateFlow()

    private var updateDownloadJob: Job? = null

    init {
        viewModelScope.launch {
            customCategoriesFromDb.collect { list ->
                list.forEach { cat ->
                    CategoryTheme.registerCustomStyle(cat.name, cat.colorHex)
                }
            }
        }
        // Keep optional Pinned Today's Tasks notification automatically synchronized with active tasks
        viewModelScope.launch {
            combine(allTasks, _isPinnedTodayEnabled) { tasks, pinnedEnabled ->
                tasks to pinnedEnabled
            }.collect { (tasks, pinnedEnabled) ->
                if (pinnedEnabled) {
                    NotificationHelper.updatePinnedTodayNotification(
                        getApplication(),
                        tasks.filter { !it.isCompleted }
                    )
                }
            }
        }
        syncPomodoroFromPersistence()
        // Silent, offline-safe GitHub release check (throttled to once every 6 hours)
        checkForAppUpdates(userInitiated = false)
    }

    // Filtered & Sorted tasks state
    val filteredTasks: StateFlow<List<TaskEntity>> = combine(
        allTasks,
        _searchQuery,
        _selectedCategory,
        _priorityFilter,
        _sortOrder
    ) { tasks, query, category, priority, order ->
        val filtered = tasks.filter { task ->
            val matchesCategory = category.equals("All", ignoreCase = true) ||
                    task.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.subtasks.any { it.title.contains(query, ignoreCase = true) }
            val matchesPriority = priority == null || task.priority == priority
            matchesCategory && matchesQuery && matchesPriority
        }

        when (order) {
            TaskSortOrder.DEFAULT -> filtered.sortedWith(
                compareBy<TaskEntity> { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.priority.ordinal }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOrder.PRIORITY -> filtered.sortedWith(
                compareByDescending<TaskEntity> { it.priority.ordinal }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOrder.DUE_DATE -> filtered.sortedWith(
                compareBy<TaskEntity> { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )
            TaskSortOrder.CREATION_DATE -> filtered.sortedByDescending { it.createdAt }
            TaskSortOrder.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase(Locale.getDefault()) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categoryList: StateFlow<List<String>> = combine(
        allTasks,
        customCategoriesFromDb
    ) { tasks, customEntities ->
        val defaults = listOf("All", "Work", "Personal", "Health", "Focus", "Finance")
        val fromCustom = customEntities.map { it.name }
        val fromTasks = tasks.map { it.category }.filter { it.isNotBlank() }
        (defaults + fromCustom + fromTasks).distinct()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        listOf("All", "Work", "Personal", "Health", "Focus", "Finance")
    )

    val categoryCounts: StateFlow<Map<String, Int>> = allTasks.combine(_selectedCategory) { tasks, _ ->
        val map = mutableMapOf<String, Int>()
        val activeTasks = tasks.filter { !it.isCompleted }
        map["All"] = activeTasks.size
        activeTasks.forEach { task ->
            map[task.category] = (map[task.category] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered notes (by category + search query)
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _noteSearchQuery,
        _noteSelectedCategory
    ) { notes, query, category ->
        notes.filter { note ->
            val matchesCat = category.equals("All", ignoreCase = true) ||
                    note.category.equals(category, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    note.title.contains(query, ignoreCase = true) ||
                    note.content.contains(query, ignoreCase = true)
            matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setThemeMode(mode: AppThemeMode) {
        userProfileManager.saveThemeMode(mode)
        _themeMode.value = mode
        MonoDoWidgetProvider.updateAllWidgets(getApplication())
    }

    fun setAppLockEnabled(enabled: Boolean) {
        userProfileManager.setAppLockEnabled(enabled)
        _isAppLockEnabled.value = enabled
        _snackbarEvents.tryEmit(
            SnackbarEvent(if (enabled) "App Lock enabled (Fingerprint / PIN)" else "App Lock disabled")
        )
    }

    fun setPinnedTodayEnabled(enabled: Boolean) {
        userProfileManager.setPinnedTodayNotificationEnabled(enabled)
        _isPinnedTodayEnabled.value = enabled
        if (enabled) {
            NotificationHelper.updatePinnedTodayNotification(
                getApplication(),
                allTasks.value.filter { !it.isCompleted }
            )
            _snackbarEvents.tryEmit(SnackbarEvent("Today's Tasks pinned to notification panel"))
        } else {
            NotificationHelper.cancelPinnedTodayNotification(getApplication())
            _snackbarEvents.tryEmit(SnackbarEvent("Pinned Today's Tasks notification turned off"))
        }
    }

    fun dismissSelfBirthdayBanner() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        userProfileManager.setDismissedSelfBirthdayYear(currentYear)
        _dismissedSelfBirthdayYear.value = currentYear
    }

    fun saveRoutineTemplate(
        title: String,
        category: String,
        priority: TaskPriority,
        subtaskTitles: List<String>
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return
        viewModelScope.launch {
            routineTemplateDao.insertTemplate(
                RoutineTemplateEntity(
                    title = cleanTitle,
                    category = category.trim().ifBlank { "Personal" },
                    priority = priority,
                    subtaskTitles = subtaskTitles.map { it.trim() }.filter { it.isNotBlank() },
                    createdAt = System.currentTimeMillis()
                )
            )
            _snackbarEvents.tryEmit(SnackbarEvent("Saved \"$cleanTitle\" as a reusable template"))
        }
    }

    fun deleteRoutineTemplate(template: RoutineTemplateEntity) {
        if (template.id <= 0L) return
        viewModelScope.launch {
            routineTemplateDao.deleteTemplate(template.id)
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = "Template \"${template.title}\" removed",
                    actionLabel = "Undo",
                    onAction = { routineTemplateDao.insertTemplate(template) }
                )
            )
        }
    }

    fun spawnTaskFromTemplate(template: RoutineTemplateEntity, dueDate: Long? = null) {
        createTask(
            title = template.title,
            category = template.category,
            priority = template.priority,
            dueDate = dueDate ?: QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY),
            subtaskTitles = template.subtaskTitles
        )
        _snackbarEvents.tryEmit(SnackbarEvent("Created \"${template.title}\" from template"))
    }

    fun sendTestBriefingNotification() {
        viewModelScope.launch {
            val activeTasks = taskDao.getActiveTasksSync()
            val birthdays = birthdayDao.getAllBirthdaysSync()
            NotificationHelper.showDailyBriefingNotification(
                getApplication(),
                activeTasks,
                birthdays
            )
            _snackbarEvents.tryEmit(SnackbarEvent("Morning Briefing preview sent to notifications"))
        }
    }

    // ==================== BIRTHDAYS CRUD + LONG-TERM ALARM SCHEDULING ====================

    fun addBirthday(
        name: String,
        month: Int,
        day: Int,
        birthYear: Int?,
        relationship: String,
        note: String,
        hoursBeforeMidnight: Int = 3,
        notifyAtMidnight: Boolean = true
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val entity = BirthdayEntity(
                name = cleanName,
                month = month.coerceIn(1, 12),
                day = day.coerceIn(1, 31),
                birthYear = birthYear?.takeIf { it in 1900..2100 },
                relationship = relationship.trim().ifBlank { "Friends" },
                note = note.trim(),
                hoursBeforeMidnight = hoursBeforeMidnight.coerceIn(1, 48),
                notifyAtMidnight = notifyAtMidnight,
                createdAt = System.currentTimeMillis()
            )
            val newId = birthdayDao.insertBirthday(entity)
            ReminderScheduler.scheduleBirthdayReminders(getApplication(), entity.copy(id = newId))
            _snackbarEvents.tryEmit(SnackbarEvent("Birthday saved & reminder armed for $cleanName"))
        }
    }

    fun updateBirthday(
        existing: BirthdayEntity,
        name: String,
        month: Int,
        day: Int,
        birthYear: Int?,
        relationship: String,
        note: String,
        hoursBeforeMidnight: Int,
        notifyAtMidnight: Boolean
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            val updated = existing.copy(
                name = cleanName,
                month = month.coerceIn(1, 12),
                day = day.coerceIn(1, 31),
                birthYear = birthYear?.takeIf { it in 1900..2100 },
                relationship = relationship.trim().ifBlank { "Friends" },
                note = note.trim(),
                hoursBeforeMidnight = hoursBeforeMidnight.coerceIn(1, 48),
                notifyAtMidnight = notifyAtMidnight
            )
            birthdayDao.updateBirthday(updated)
            ReminderScheduler.scheduleBirthdayReminders(getApplication(), updated)
            _snackbarEvents.tryEmit(SnackbarEvent("Updated birthday reminder for $cleanName"))
        }
    }

    fun deleteBirthday(birthday: BirthdayEntity) {
        viewModelScope.launch {
            birthdayDao.deleteBirthday(birthday)
            ReminderScheduler.cancelBirthdayReminders(getApplication(), birthday.id)
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = "Removed ${birthday.name}'s birthday",
                    actionLabel = "Undo",
                    onAction = {
                        birthdayDao.insertBirthday(birthday)
                        ReminderScheduler.scheduleBirthdayReminders(getApplication(), birthday)
                    }
                )
            )
        }
    }

    fun sendTestBirthdayNotification(birthday: BirthdayEntity) {
        NotificationHelper.showBirthdayNotification(
            context = getApplication(),
            birthday = birthday,
            isMidnightAlert = false
        )
        _snackbarEvents.tryEmit(SnackbarEvent("Preview alert sent for ${birthday.name}'s birthday"))
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _priorityFilter.value = priority
    }

    fun setSortOrder(order: TaskSortOrder) {
        _sortOrder.value = order
    }

    fun setActiveSwipedTask(taskId: Long?) {
        _activeSwipedTaskId.value = taskId
    }

    fun addCustomCategory(name: String, colorHex: String = "#2563EB") {
        val trimmed = name.trim()
        if (trimmed.isNotBlank()) {
            CategoryTheme.registerCustomStyle(trimmed, colorHex)
            viewModelScope.launch {
                customCategoryDao.insertCategory(
                    CustomCategoryEntity(name = trimmed, colorHex = colorHex)
                )
            }
        }
    }

    fun enterZenFocus(task: TaskEntity) {
        _zenActiveTask.value = task
    }

    fun exitZenFocus() {
        _zenActiveTask.value = null
    }

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    fun setNoteSelectedCategory(category: String) {
        _noteSelectedCategory.value = category
    }

    // ==================== PERSISTENT POMODORO CONTROLLER (BACKGROUND-RELIABLE) ====================

    /**
     * Reconciles UI timer state with SharedPreferences so background alarms,
     * notification Pause/Stop clicks, and app restarts stay 100% in sync.
     */
    fun syncPomodoroFromPersistence() {
        val persisted = userProfileManager.getPersistedPomodoroState()
        val now = System.currentTimeMillis()
        if (persisted.isRunning) {
            val remaining = ((persisted.endTimeMillis - now + 500L) / 1000L).toInt()
            if (remaining > 0) {
                pomodoroEndTimeMillis = persisted.endTimeMillis
                _pomodoroState.value = PomodoroUiState(
                    isRunning = true,
                    isBreak = persisted.isBreak,
                    focusMinutes = persisted.focusMinutes,
                    breakMinutes = persisted.breakMinutes,
                    secondsRemaining = remaining
                )
                startPomodoroLoop()
            } else {
                // Finished while app was closed or alarm was delayed
                pomodoroJob?.cancel()
                pomodoroJob = null
                NotificationHelper.cancelPomodoroOngoingNotification(getApplication())
                if (!persisted.isBreak && !persisted.sessionCompletedLogged) {
                    logFocusSession(
                        durationMinutes = persisted.focusMinutes,
                        taskId = null,
                        taskTitle = "Pomodoro Focus"
                    )
                }
                val nextIsBreak = !persisted.isBreak
                val nextSec = (if (nextIsBreak) persisted.breakMinutes else persisted.focusMinutes) * 60
                userProfileManager.savePomodoroState(
                    isRunning = false,
                    isBreak = nextIsBreak,
                    focusMinutes = persisted.focusMinutes,
                    breakMinutes = persisted.breakMinutes,
                    endTimeMillis = 0L,
                    remainingSeconds = nextSec,
                    sessionCompletedLogged = true
                )
                _pomodoroState.value = PomodoroUiState(
                    isRunning = false,
                    isBreak = nextIsBreak,
                    focusMinutes = persisted.focusMinutes,
                    breakMinutes = persisted.breakMinutes,
                    secondsRemaining = nextSec
                )
            }
        } else {
            pomodoroJob?.cancel()
            pomodoroJob = null
            _pomodoroState.value = PomodoroUiState(
                isRunning = false,
                isBreak = persisted.isBreak,
                focusMinutes = persisted.focusMinutes,
                breakMinutes = persisted.breakMinutes,
                secondsRemaining = persisted.remainingSeconds.coerceAtLeast(1)
            )
        }
    }

    fun togglePomodoro() {
        val current = _pomodoroState.value
        val app = getApplication<Application>()
        if (current.isRunning) {
            pomodoroJob?.cancel()
            pomodoroJob = null
            val remaining = ((pomodoroEndTimeMillis - System.currentTimeMillis() + 500L) / 1000L)
                .toInt()
                .coerceAtLeast(1)
            userProfileManager.savePomodoroState(
                isRunning = false,
                isBreak = current.isBreak,
                focusMinutes = current.focusMinutes,
                breakMinutes = current.breakMinutes,
                endTimeMillis = 0L,
                remainingSeconds = remaining,
                sessionCompletedLogged = false
            )
            ReminderScheduler.cancelPomodoroFinishAlarm(app)
            NotificationHelper.cancelPomodoroOngoingNotification(app)
            _pomodoroState.update { it.copy(isRunning = false, secondsRemaining = remaining) }
        } else {
            val remaining = if (current.secondsRemaining <= 0) current.totalSeconds else current.secondsRemaining
            val endTime = System.currentTimeMillis() + remaining * 1000L
            pomodoroEndTimeMillis = endTime
            val durationMinutes = if (current.isBreak) current.breakMinutes else current.focusMinutes
            userProfileManager.savePomodoroState(
                isRunning = true,
                isBreak = current.isBreak,
                focusMinutes = current.focusMinutes,
                breakMinutes = current.breakMinutes,
                endTimeMillis = endTime,
                remainingSeconds = remaining,
                sessionCompletedLogged = false
            )
            ReminderScheduler.schedulePomodoroFinishAlarm(app, endTime, current.isBreak, durationMinutes)
            NotificationHelper.showPomodoroOngoingNotification(app, endTime, current.isBreak, durationMinutes)
            _pomodoroState.update { it.copy(isRunning = true, secondsRemaining = remaining) }
            startPomodoroLoop()
        }
    }

    private fun startPomodoroLoop() {
        pomodoroJob?.cancel()
        pomodoroJob = viewModelScope.launch {
            val app = getApplication<Application>()
            while (true) {
                // Re-check persisted running flag in case user paused/stopped from notification shade
                val persisted = userProfileManager.getPersistedPomodoroState()
                if (!persisted.isRunning) {
                    _pomodoroState.value = PomodoroUiState(
                        isRunning = false,
                        isBreak = persisted.isBreak,
                        focusMinutes = persisted.focusMinutes,
                        breakMinutes = persisted.breakMinutes,
                        secondsRemaining = persisted.remainingSeconds.coerceAtLeast(1)
                    )
                    break
                }

                val remaining = ((pomodoroEndTimeMillis - System.currentTimeMillis() + 500L) / 1000L)
                    .toInt()
                    .coerceAtLeast(0)

                if (remaining <= 0) {
                    val finishedState = _pomodoroState.value
                    val durationMins = if (finishedState.isBreak) finishedState.breakMinutes else finishedState.focusMinutes
                    ReminderScheduler.cancelPomodoroFinishAlarm(app)
                    NotificationHelper.cancelPomodoroOngoingNotification(app)
                    if (!finishedState.isBreak && !persisted.sessionCompletedLogged) {
                        logFocusSession(
                            durationMinutes = finishedState.focusMinutes,
                            taskId = null,
                            taskTitle = "Pomodoro Focus"
                        )
                        _snackbarEvents.tryEmit(
                            SnackbarEvent("Focus session logged (${finishedState.focusMinutes}m) — Time for a break")
                        )
                    } else if (finishedState.isBreak) {
                        _snackbarEvents.tryEmit(
                            SnackbarEvent("Break finished — Ready for your next focus block")
                        )
                    }
                    val nextIsBreak = !finishedState.isBreak
                    val nextSeconds = (if (nextIsBreak) finishedState.breakMinutes else finishedState.focusMinutes) * 60
                    userProfileManager.savePomodoroState(
                        isRunning = false,
                        isBreak = nextIsBreak,
                        focusMinutes = finishedState.focusMinutes,
                        breakMinutes = finishedState.breakMinutes,
                        endTimeMillis = 0L,
                        remainingSeconds = nextSeconds,
                        sessionCompletedLogged = true
                    )
                    NotificationHelper.showPomodoroFinishedNotification(app, finishedState.isBreak, durationMins)
                    _pomodoroState.update {
                        it.copy(
                            isRunning = false,
                            isBreak = nextIsBreak,
                            secondsRemaining = nextSeconds
                        )
                    }
                    break
                } else {
                    _pomodoroState.update { it.copy(secondsRemaining = remaining) }
                    delay(500L)
                }
            }
        }
    }

    fun resetPomodoro() {
        pomodoroJob?.cancel()
        pomodoroJob = null
        val app = getApplication<Application>()
        ReminderScheduler.cancelPomodoroFinishAlarm(app)
        NotificationHelper.cancelPomodoroOngoingNotification(app)
        val current = _pomodoroState.value
        val resetSec = current.totalSeconds
        userProfileManager.savePomodoroState(
            isRunning = false,
            isBreak = current.isBreak,
            focusMinutes = current.focusMinutes,
            breakMinutes = current.breakMinutes,
            endTimeMillis = 0L,
            remainingSeconds = resetSec,
            sessionCompletedLogged = false
        )
        _pomodoroState.update { state ->
            state.copy(
                isRunning = false,
                secondsRemaining = resetSec
            )
        }
    }

    fun switchPomodoroMode(toBreak: Boolean) {
        pomodoroJob?.cancel()
        pomodoroJob = null
        val app = getApplication<Application>()
        ReminderScheduler.cancelPomodoroFinishAlarm(app)
        NotificationHelper.cancelPomodoroOngoingNotification(app)
        val state = _pomodoroState.value
        val targetSeconds = (if (toBreak) state.breakMinutes else state.focusMinutes) * 60
        userProfileManager.savePomodoroState(
            isRunning = false,
            isBreak = toBreak,
            focusMinutes = state.focusMinutes,
            breakMinutes = state.breakMinutes,
            endTimeMillis = 0L,
            remainingSeconds = targetSeconds,
            sessionCompletedLogged = false
        )
        _pomodoroState.update {
            it.copy(
                isRunning = false,
                isBreak = toBreak,
                secondsRemaining = targetSeconds
            )
        }
    }

    fun adjustPomodoroDuration(deltaMinutes: Int) {
        val state = _pomodoroState.value
        if (state.isRunning) return
        if (state.isBreak) {
            val nextBreak = (state.breakMinutes + deltaMinutes).coerceIn(1, 30)
            userProfileManager.savePomodoroState(
                isRunning = false,
                isBreak = true,
                focusMinutes = state.focusMinutes,
                breakMinutes = nextBreak,
                endTimeMillis = 0L,
                remainingSeconds = nextBreak * 60
            )
            _pomodoroState.update {
                it.copy(breakMinutes = nextBreak, secondsRemaining = nextBreak * 60)
            }
        } else {
            val nextFocus = (state.focusMinutes + deltaMinutes).coerceIn(5, 90)
            userProfileManager.savePomodoroState(
                isRunning = false,
                isBreak = false,
                focusMinutes = nextFocus,
                breakMinutes = state.breakMinutes,
                endTimeMillis = 0L,
                remainingSeconds = nextFocus * 60
            )
            _pomodoroState.update {
                it.copy(focusMinutes = nextFocus, secondsRemaining = nextFocus * 60)
            }
        }
    }

    fun logFocusSession(durationMinutes: Int, taskId: Long? = null, taskTitle: String = "") {
        if (durationMinutes <= 0) return
        viewModelScope.launch {
            focusSessionDao.insertSession(
                FocusSessionEntity(
                    durationMinutes = durationMinutes,
                    taskId = taskId,
                    taskTitle = taskTitle,
                    completedAt = System.currentTimeMillis(),
                    localEpochDay = LocalDayUtils.toLocalEpochDay()
                )
            )
        }
    }

    // ==================== TASK CRUD + EDIT + RECURRENCE + UNDO ====================

    fun createTask(
        title: String,
        description: String = "",
        category: String = "General",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Long? = null,
        subtaskTitles: List<String> = emptyList(),
        recurrence: TaskRecurrence = TaskRecurrence.NONE,
        reminderAt: Long? = null
    ) {
        viewModelScope.launch {
            val subtasks = subtaskTitles.map { subTitle ->
                SubTask(
                    id = UUID.randomUUID().toString(),
                    title = subTitle.trim(),
                    isCompleted = false
                )
            }
            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = category.trim().ifBlank { "General" },
                priority = priority,
                dueDate = dueDate,
                isCompleted = false,
                subtasks = subtasks,
                createdAt = System.currentTimeMillis(),
                recurrence = recurrence,
                reminderAt = reminderAt
            )
            val newId = taskDao.insertTask(task)
            if (reminderAt != null && reminderAt > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminderAt(getApplication(), newId, reminderAt)
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun updateTask(
        existingTask: TaskEntity,
        title: String,
        description: String,
        category: String,
        priority: TaskPriority,
        dueDate: Long?,
        subtasks: List<SubTask>,
        recurrence: TaskRecurrence,
        reminderAt: Long? = existingTask.reminderAt
    ) {
        viewModelScope.launch {
            val updated = existingTask.copy(
                title = title.trim(),
                description = description.trim(),
                category = category.trim().ifBlank { "General" },
                priority = priority,
                dueDate = dueDate,
                subtasks = subtasks,
                recurrence = recurrence,
                reminderAt = reminderAt
            )
            taskDao.updateTask(updated)
            if (reminderAt != null && !updated.isCompleted && reminderAt > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminderAt(getApplication(), updated.id, reminderAt)
            } else {
                ReminderScheduler.cancelTaskReminder(getApplication(), updated.id)
            }
            if (_zenActiveTask.value?.id == existingTask.id) {
                _zenActiveTask.value = updated
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
            _snackbarEvents.tryEmit(SnackbarEvent("Task updated"))
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val willBeCompleted = !task.isCompleted
            val updatedSubtasks = if (willBeCompleted) {
                task.subtasks.map { it.copy(isCompleted = true) }
            } else {
                task.subtasks
            }
            val updated = task.copy(
                isCompleted = willBeCompleted,
                completedAt = if (willBeCompleted) System.currentTimeMillis() else null,
                subtasks = updatedSubtasks
            )
            taskDao.updateTask(updated)

            if (willBeCompleted) {
                ReminderScheduler.cancelTaskReminder(getApplication(), task.id)
            } else if (task.reminderAt != null && task.reminderAt > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminderAt(getApplication(), task.id, task.reminderAt)
            }

            var spawnedRecurringId: Long? = null
            if (willBeCompleted && task.recurrence != TaskRecurrence.NONE) {
                val nextDue = task.recurrence.nextDueDate(task.dueDate)
                val nextReminder = task.reminderAt?.let { oldRem ->
                    val delta = nextDue - (task.dueDate ?: nextDue)
                    if (delta > 0) oldRem + delta else task.recurrence.nextDueDate(oldRem)
                }
                val nextTask = task.copy(
                    id = 0,
                    dueDate = nextDue,
                    reminderAt = nextReminder,
                    isCompleted = false,
                    completedAt = null,
                    createdAt = System.currentTimeMillis(),
                    subtasks = task.subtasks.map { it.copy(isCompleted = false) }
                )
                spawnedRecurringId = taskDao.insertTask(nextTask)
                if (nextReminder != null && nextReminder > System.currentTimeMillis()) {
                    ReminderScheduler.scheduleTaskReminderAt(getApplication(), spawnedRecurringId, nextReminder)
                }
            }

            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = updated
            }
            if (_activeSwipedTaskId.value == task.id) {
                _activeSwipedTaskId.value = null
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())

            val msg = when {
                willBeCompleted && task.recurrence != TaskRecurrence.NONE ->
                    "Completed • Next ${task.recurrence.shortLabel.lowercase()} task scheduled"
                willBeCompleted -> "Task completed"
                else -> "Task reopened"
            }
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = msg,
                    actionLabel = "Undo",
                    onAction = {
                        taskDao.updateTask(task)
                        if (!task.isCompleted && task.reminderAt != null && task.reminderAt > System.currentTimeMillis()) {
                            ReminderScheduler.scheduleTaskReminderAt(getApplication(), task.id, task.reminderAt)
                        }
                        if (spawnedRecurringId != null) {
                            ReminderScheduler.cancelTaskReminder(getApplication(), spawnedRecurringId)
                            taskDao.deleteById(spawnedRecurringId)
                        }
                        MonoDoWidgetProvider.updateAllWidgets(getApplication())
                    }
                )
            )
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            ReminderScheduler.cancelTaskReminder(getApplication(), task.id)
            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = null
            }
            if (_activeSwipedTaskId.value == task.id) {
                _activeSwipedTaskId.value = null
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = "Task deleted",
                    actionLabel = "Undo",
                    onAction = {
                        taskDao.insertTask(task)
                        if (!task.isCompleted && task.reminderAt != null && task.reminderAt > System.currentTimeMillis()) {
                            ReminderScheduler.scheduleTaskReminderAt(getApplication(), task.id, task.reminderAt)
                        }
                        MonoDoWidgetProvider.updateAllWidgets(getApplication())
                    }
                )
            )
        }
    }

    fun rescheduleTask(task: TaskEntity, newDueDate: Long?) {
        viewModelScope.launch {
            val updated = task.copy(dueDate = newDueDate)
            taskDao.updateTask(updated)
            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = updated
            }
            _activeSwipedTaskId.value = null
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteCustomCategory(name: String) {
        viewModelScope.launch {
            customCategoryDao.deleteCategoryByName(name)
            if (_selectedCategory.value.equals(name, ignoreCase = true)) {
                _selectedCategory.value = "All"
            }
        }
    }

    fun toggleSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.map { sub ->
                if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
            }
            val allCompleted = updatedSubtasks.isNotEmpty() && updatedSubtasks.all { it.isCompleted }
            val updated = task.copy(
                subtasks = updatedSubtasks,
                isCompleted = if (allCompleted) true else task.isCompleted,
                completedAt = if (allCompleted && !task.isCompleted) System.currentTimeMillis() else task.completedAt
            )
            taskDao.updateTask(updated)
            if (_zenActiveTask.value?.id == taskId) {
                _zenActiveTask.value = updated
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun addSubtask(taskId: Long, title: String) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val newSub = SubTask(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                isCompleted = false
            )
            val updatedSubtasks = task.subtasks + newSub
            val updated = task.copy(
                subtasks = updatedSubtasks,
                isCompleted = false,
                completedAt = null
            )
            taskDao.updateTask(updated)
            if (_zenActiveTask.value?.id == taskId) {
                _zenActiveTask.value = updated
            }
        }
    }

    fun deleteSubtask(taskId: Long, subtaskId: String) {
        viewModelScope.launch {
            val task = taskDao.getTaskById(taskId) ?: return@launch
            val updatedSubtasks = task.subtasks.filter { it.id != subtaskId }
            val updated = task.copy(subtasks = updatedSubtasks)
            taskDao.updateTask(updated)
            if (_zenActiveTask.value?.id == taskId) {
                _zenActiveTask.value = updated
            }
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val completed = allTasks.value.filter { it.isCompleted }
            completed.forEach { taskDao.deleteTask(it) }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
            if (completed.isNotEmpty()) {
                _snackbarEvents.tryEmit(
                    SnackbarEvent("Cleared ${completed.size} completed ${if (completed.size == 1) "task" else "tasks"}")
                )
            }
        }
    }

    // ==================== HABITS CRUD + STREAKS ====================

    fun addHabit(title: String, colorHex: String) {
        val clean = title.trim()
        if (clean.isNotBlank()) {
            viewModelScope.launch {
                habitDao.insertHabit(
                    HabitEntity(title = clean, colorHex = colorHex)
                )
            }
        }
    }

    fun updateHabit(habit: HabitEntity, newTitle: String, newColorHex: String) {
        val clean = newTitle.trim()
        if (clean.isNotBlank()) {
            viewModelScope.launch {
                habitDao.updateHabit(habit.copy(title = clean, colorHex = newColorHex))
            }
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            val existingHabit = allHabits.value.find { it.id == habitId }
            val existingLogs = habitLogs.value.filter { it.habitId == habitId }
            habitDao.deleteHabitById(habitId)
            habitDao.deleteLogsForHabit(habitId)
            if (existingHabit != null) {
                _snackbarEvents.tryEmit(
                    SnackbarEvent(
                        message = "Habit \"${existingHabit.title}\" deleted",
                        actionLabel = "Undo",
                        onAction = {
                            habitDao.insertHabit(existingHabit)
                            existingLogs.forEach { habitDao.insertOrUpdateLog(it) }
                        }
                    )
                )
            }
        }
    }

    fun toggleHabitDay(habitId: Long, localEpochDay: Long) {
        viewModelScope.launch {
            val existing = habitDao.getLog(habitId, localEpochDay)
            if (existing != null) {
                habitDao.deleteLog(habitId, localEpochDay)
            } else {
                habitDao.insertOrUpdateLog(
                    HabitLogEntity(habitId = habitId, dateEpochDay = localEpochDay, isCompleted = true)
                )
            }
        }
    }

    /**
     * Returns (currentStreak, bestStreak) for a habit using timezone-safe local epoch days.
     */
    fun calculateHabitStreaks(habitId: Long, logs: List<HabitLogEntity> = habitLogs.value): Pair<Int, Int> {
        val days = logs
            .filter { it.habitId == habitId && it.isCompleted }
            .map { it.dateEpochDay }
            .distinct()
            .sorted()
        if (days.isEmpty()) return 0 to 0

        val daySet = days.toHashSet()
        val today = LocalDayUtils.toLocalEpochDay()

        // Current streak: count backwards from today (or yesterday if not yet checked today)
        var current = 0
        var cursor = if (daySet.contains(today)) today else today - 1L
        while (daySet.contains(cursor)) {
            current++
            cursor -= 1L
        }

        // Best streak: longest consecutive run
        var best = 1
        var run = 1
        for (i in 1 until days.size) {
            if (days[i] == days[i - 1] + 1L) {
                run++
                if (run > best) best = run
            } else {
                run = 1
            }
        }
        return current to maxOf(best, current)
    }

    // ==================== NOTES CRUD + NOTE-TO-TASK ====================

    fun createNote(title: String, content: String, category: String = "General", isPinned: Boolean = false) {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title.trim(),
                content = content.trim(),
                category = category.trim().ifBlank { "General" },
                isPinned = isPinned,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            noteDao.insertNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            _snackbarEvents.tryEmit(
                SnackbarEvent(
                    message = "Note deleted",
                    actionLabel = "Undo",
                    onAction = { noteDao.insertNote(note) }
                )
            )
        }
    }

    fun convertNoteToTask(note: NoteEntity) {
        viewModelScope.launch {
            val title = note.title.trim().ifBlank {
                note.content.trim().lineSequence().firstOrNull()?.take(60) ?: "Note Action"
            }
            val desc = if (note.title.isNotBlank()) note.content.trim() else ""
            val task = TaskEntity(
                title = title,
                description = desc,
                category = note.category.ifBlank { "General" },
                priority = TaskPriority.MEDIUM,
                dueDate = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY),
                isCompleted = false,
                createdAt = System.currentTimeMillis()
            )
            taskDao.insertTask(task)
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
            _snackbarEvents.tryEmit(SnackbarEvent("Added \"$title\" to Tasks"))
        }
    }

    // ==================== PROFILE & RESET ====================

    fun refreshProfile() {
        _userProfile.value = userProfileManager.getProfile()
    }

    fun saveUserProfile(
        name: String,
        workspaceTitle: String = "MonoDo",
        birthDate: String = "",
        birthTimestamp: Long = 0L,
        reminderHour: Int = 9,
        reminderMinute: Int = 0
    ) {
        userProfileManager.saveProfile(
            userName = name,
            workspaceTitle = workspaceTitle,
            birthDate = birthDate,
            birthTimestamp = birthTimestamp,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute
        )
        refreshProfile()
    }

    fun resetAllData() {
        viewModelScope.launch {
            pomodoroJob?.cancel()
            pomodoroJob = null
            _pomodoroState.value = PomodoroUiState()
            ReminderScheduler.cancelPomodoroFinishAlarm(getApplication())
            NotificationHelper.cancelPomodoroOngoingNotification(getApplication())
            NotificationHelper.cancelPinnedTodayNotification(getApplication())
            taskDao.deleteAllTasks()
            noteDao.deleteAllNotes()
            habitDao.deleteAllHabits()
            habitDao.deleteAllHabitLogs()
            customCategoryDao.deleteAllCategories()
            allBirthdays.value.forEach { b ->
                ReminderScheduler.cancelBirthdayReminders(getApplication(), b.id)
            }
            birthdayDao.deleteAllBirthdays()
            routineTemplateDao.deleteAllTemplates()
            focusSessionDao.deleteAllSessions()
            userProfileManager.resetProfile()
            _themeMode.value = AppThemeMode.LIGHT_PAPER
            _isAppLockEnabled.value = false
            _isPinnedTodayEnabled.value = false
            _dismissedSelfBirthdayYear.value = -1
            ReminderScheduler.cancelDailyReminder(getApplication())
            refreshProfile()
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // ==================== OFFLINE JSON BACKUP & RESTORE ====================

    fun exportJsonBackup(): String {
        val root = JSONObject()
        root.put("app", "MonoDo")
        root.put("version", 6)
        root.put("themeMode", _themeMode.value.name)
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        val tasksArray = JSONArray()
        allTasks.value.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("category", t.category)
                put("priority", t.priority.name)
                put("recurrence", t.recurrence.name)
                if (t.dueDate != null) put("dueDate", t.dueDate)
                if (t.reminderAt != null) put("reminderAt", t.reminderAt)
                put("isCompleted", t.isCompleted)
                put("createdAt", t.createdAt)
                if (t.completedAt != null) put("completedAt", t.completedAt)

                val subArray = JSONArray()
                t.subtasks.forEach { s ->
                    subArray.put(JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("isCompleted", s.isCompleted)
                    })
                }
                put("subtasks", subArray)
            }
            tasksArray.put(tObj)
        }
        root.put("tasks", tasksArray)

        val notesArray = JSONArray()
        allNotes.value.forEach { n ->
            notesArray.put(JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("isPinned", n.isPinned)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
            })
        }
        root.put("notes", notesArray)

        val habitsArray = JSONArray()
        allHabits.value.forEach { h ->
            habitsArray.put(JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("frequency", h.frequency)
                put("colorHex", h.colorHex)
                put("createdAt", h.createdAt)
            })
        }
        root.put("habits", habitsArray)

        val habitLogsArray = JSONArray()
        habitLogs.value.forEach { l ->
            habitLogsArray.put(JSONObject().apply {
                put("habitId", l.habitId)
                put("dateEpochDay", l.dateEpochDay)
                put("isCompleted", l.isCompleted)
                put("completedAt", l.completedAt)
            })
        }
        root.put("habitLogs", habitLogsArray)

        val categoriesArray = JSONArray()
        customCategoriesFromDb.value.forEach { c ->
            categoriesArray.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("colorHex", c.colorHex)
                put("createdAt", c.createdAt)
            })
        }
        root.put("customCategories", categoriesArray)

        val focusArray = JSONArray()
        focusSessions.value.forEach { f ->
            focusArray.put(JSONObject().apply {
                put("id", f.id)
                put("durationMinutes", f.durationMinutes)
                if (f.taskId != null) put("taskId", f.taskId)
                put("taskTitle", f.taskTitle)
                put("completedAt", f.completedAt)
                put("localEpochDay", f.localEpochDay)
            })
        }
        root.put("focusSessions", focusArray)

        val birthdaysArray = JSONArray()
        allBirthdays.value.forEach { b ->
            birthdaysArray.put(JSONObject().apply {
                put("id", b.id)
                put("name", b.name)
                put("month", b.month)
                put("day", b.day)
                if (b.birthYear != null) put("birthYear", b.birthYear)
                put("relationship", b.relationship)
                put("note", b.note)
                put("hoursBeforeMidnight", b.hoursBeforeMidnight)
                put("notifyAtMidnight", b.notifyAtMidnight)
                put("createdAt", b.createdAt)
            })
        }
        root.put("birthdays", birthdaysArray)

        val templatesArray = JSONArray()
        allTemplates.value.forEach { tpl ->
            templatesArray.put(JSONObject().apply {
                put("id", tpl.id)
                put("title", tpl.title)
                put("category", tpl.category)
                put("priority", tpl.priority.name)
                val subs = JSONArray()
                tpl.subtaskTitles.forEach { subs.put(it) }
                put("subtaskTitles", subs)
                put("createdAt", tpl.createdAt)
            })
        }
        root.put("routineTemplates", templatesArray)

        return root.toString(2)
    }

    fun saveBackupToInternalStorage(): String {
        val json = exportJsonBackup()
        val file = java.io.File(getApplication<Application>().filesDir, "monodo_backup_latest.json")
        file.writeText(json)
        return file.absolutePath
    }

    fun hasInternalBackupFile(): Boolean {
        val file = java.io.File(getApplication<Application>().filesDir, "monodo_backup_latest.json")
        return file.exists() && file.length() > 0
    }

    fun restoreFromInternalStorage(): Boolean {
        val file = java.io.File(getApplication<Application>().filesDir, "monodo_backup_latest.json")
        if (!file.exists()) return false
        return importJsonBackup(file.readText())
    }

    fun importJsonBackup(jsonString: String): Boolean {
        return try {
            val root = JSONObject(jsonString)

            root.optString("themeMode", "").takeIf { it.isNotBlank() }?.let { rawTheme ->
                runCatching { AppThemeMode.valueOf(rawTheme) }.getOrNull()?.let { setThemeMode(it) }
            }

            viewModelScope.launch {
                val categoriesArray = root.optJSONArray("customCategories")
                if (categoriesArray != null) {
                    for (i in 0 until categoriesArray.length()) {
                        val cObj = categoriesArray.getJSONObject(i)
                        val name = cObj.optString("name", "")
                        val colorHex = cObj.optString("colorHex", "#2563EB")
                        if (name.isNotBlank()) {
                            CategoryTheme.registerCustomStyle(name, colorHex)
                            customCategoryDao.insertCategory(
                                CustomCategoryEntity(
                                    id = cObj.optLong("id", 0L),
                                    name = name,
                                    colorHex = colorHex,
                                    createdAt = cObj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                }

                val tasksArray = root.optJSONArray("tasks")
                if (tasksArray != null) {
                    for (i in 0 until tasksArray.length()) {
                        val tObj = tasksArray.getJSONObject(i)
                        val subtasksList = mutableListOf<SubTask>()
                        val subArray = tObj.optJSONArray("subtasks")
                        if (subArray != null) {
                            for (j in 0 until subArray.length()) {
                                val sObj = subArray.getJSONObject(j)
                                subtasksList.add(
                                    SubTask(
                                        id = sObj.optString("id", UUID.randomUUID().toString()),
                                        title = sObj.optString("title", ""),
                                        isCompleted = sObj.optBoolean("isCompleted", false)
                                    )
                                )
                            }
                        }

                        val priority = runCatching {
                            TaskPriority.valueOf(tObj.optString("priority", "MEDIUM"))
                        }.getOrDefault(TaskPriority.MEDIUM)

                        val recurrence = runCatching {
                            TaskRecurrence.valueOf(tObj.optString("recurrence", "NONE"))
                        }.getOrDefault(TaskRecurrence.NONE)

                        val task = TaskEntity(
                            id = tObj.optLong("id", 0L),
                            title = tObj.optString("title", "Imported Task"),
                            description = tObj.optString("description", ""),
                            category = tObj.optString("category", "General"),
                            priority = priority,
                            dueDate = if (tObj.has("dueDate") && !tObj.isNull("dueDate")) tObj.getLong("dueDate") else null,
                            isCompleted = tObj.optBoolean("isCompleted", false),
                            subtasks = subtasksList,
                            createdAt = tObj.optLong("createdAt", System.currentTimeMillis()),
                            completedAt = if (tObj.has("completedAt") && !tObj.isNull("completedAt")) tObj.getLong("completedAt") else null,
                            recurrence = recurrence,
                            reminderAt = if (tObj.has("reminderAt") && !tObj.isNull("reminderAt")) tObj.getLong("reminderAt") else null
                        )
                        val insertedId = taskDao.insertTask(task)
                        if (!task.isCompleted && task.reminderAt != null && task.reminderAt > System.currentTimeMillis()) {
                            ReminderScheduler.scheduleTaskReminderAt(getApplication(), insertedId, task.reminderAt)
                        }
                    }
                }

                val notesArray = root.optJSONArray("notes")
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val nObj = notesArray.getJSONObject(i)
                        noteDao.insertNote(
                            NoteEntity(
                                id = nObj.optLong("id", 0L),
                                title = nObj.optString("title", "Imported Note"),
                                content = nObj.optString("content", ""),
                                category = nObj.optString("category", "General"),
                                isPinned = nObj.optBoolean("isPinned", false),
                                createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val habitsArray = root.optJSONArray("habits")
                if (habitsArray != null) {
                    for (i in 0 until habitsArray.length()) {
                        val hObj = habitsArray.getJSONObject(i)
                        habitDao.insertHabit(
                            HabitEntity(
                                id = hObj.optLong("id", 0L),
                                title = hObj.optString("title", "Imported Habit"),
                                frequency = hObj.optString("frequency", "DAILY"),
                                colorHex = hObj.optString("colorHex", "#2563EB"),
                                createdAt = hObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val habitLogsArray = root.optJSONArray("habitLogs")
                if (habitLogsArray != null) {
                    for (i in 0 until habitLogsArray.length()) {
                        val lObj = habitLogsArray.getJSONObject(i)
                        habitDao.insertOrUpdateLog(
                            HabitLogEntity(
                                habitId = lObj.optLong("habitId", 0L),
                                dateEpochDay = lObj.optLong("dateEpochDay", 0L),
                                isCompleted = lObj.optBoolean("isCompleted", true),
                                completedAt = lObj.optLong("completedAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                val focusArray = root.optJSONArray("focusSessions")
                if (focusArray != null) {
                    for (i in 0 until focusArray.length()) {
                        val fObj = focusArray.getJSONObject(i)
                        focusSessionDao.insertSession(
                            FocusSessionEntity(
                                id = fObj.optLong("id", 0L),
                                durationMinutes = fObj.optInt("durationMinutes", 25),
                                taskId = if (fObj.has("taskId") && !fObj.isNull("taskId")) fObj.getLong("taskId") else null,
                                taskTitle = fObj.optString("taskTitle", ""),
                                completedAt = fObj.optLong("completedAt", System.currentTimeMillis()),
                                localEpochDay = fObj.optLong("localEpochDay", LocalDayUtils.toLocalEpochDay())
                            )
                        )
                    }
                }

                val birthdaysArray = root.optJSONArray("birthdays")
                if (birthdaysArray != null) {
                    for (i in 0 until birthdaysArray.length()) {
                        val bObj = birthdaysArray.getJSONObject(i)
                        val entity = BirthdayEntity(
                            id = bObj.optLong("id", 0L),
                            name = bObj.optString("name", "Friend"),
                            month = bObj.optInt("month", 1).coerceIn(1, 12),
                            day = bObj.optInt("day", 1).coerceIn(1, 31),
                            birthYear = if (bObj.has("birthYear") && !bObj.isNull("birthYear")) bObj.getInt("birthYear") else null,
                            relationship = bObj.optString("relationship", "Friends"),
                            note = bObj.optString("note", ""),
                            hoursBeforeMidnight = bObj.optInt("hoursBeforeMidnight", 3).coerceIn(1, 48),
                            notifyAtMidnight = bObj.optBoolean("notifyAtMidnight", true),
                            createdAt = bObj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val insertedId = birthdayDao.insertBirthday(entity)
                        ReminderScheduler.scheduleBirthdayReminders(getApplication(), entity.copy(id = insertedId))
                    }
                }

                val templatesArray = root.optJSONArray("routineTemplates")
                if (templatesArray != null) {
                    for (i in 0 until templatesArray.length()) {
                        val tplObj = templatesArray.getJSONObject(i)
                        val subs = mutableListOf<String>()
                        val subArr = tplObj.optJSONArray("subtaskTitles")
                        if (subArr != null) {
                            for (j in 0 until subArr.length()) {
                                val s = subArr.optString(j, "")
                                if (s.isNotBlank()) subs.add(s)
                            }
                        }
                        val priority = runCatching {
                            TaskPriority.valueOf(tplObj.optString("priority", "MEDIUM"))
                        }.getOrDefault(TaskPriority.MEDIUM)
                        routineTemplateDao.insertTemplate(
                            RoutineTemplateEntity(
                                id = tplObj.optLong("id", 0L),
                                title = tplObj.optString("title", "Routine"),
                                category = tplObj.optString("category", "Personal"),
                                priority = priority,
                                subtaskTitles = subs,
                                createdAt = tplObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }

                MonoDoWidgetProvider.updateAllWidgets(getApplication())
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    // ==================== ANALYTICS ====================

    fun getCompletionStatsForLast7Days(): List<Pair<String, Int>> {
        val tasks = allTasks.value
        val result = mutableListOf<Pair<String, Int>>()
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val targetLocalDay = LocalDayUtils.toLocalEpochDay(dayCal.timeInMillis)
            val completedOnDay = tasks.count {
                it.isCompleted && it.completedAt != null &&
                        LocalDayUtils.toLocalEpochDay(it.completedAt) == targetLocalDay
            }
            val dayLabel = days[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
            result.add(dayLabel to completedOnDay)
        }
        return result
    }

    fun calculateStreakDays(): Int {
        val tasks = allTasks.value
        val completedDays = tasks
            .filter { it.isCompleted && it.completedAt != null }
            .map { LocalDayUtils.toLocalEpochDay(it.completedAt!!) }
            .toHashSet()
        if (completedDays.isEmpty()) return 0

        var streak = 0
        val today = LocalDayUtils.toLocalEpochDay()
        var cursor = if (completedDays.contains(today)) today else today - 1L
        while (completedDays.contains(cursor)) {
            streak++
            cursor -= 1L
        }
        return streak
    }

    fun getTodayFocusMinutes(sessions: List<FocusSessionEntity> = focusSessions.value): Int {
        val today = LocalDayUtils.toLocalEpochDay()
        return sessions.filter { it.localEpochDay == today }.sumOf { it.durationMinutes }
    }

    fun getWeeklyFocusMinutes(sessions: List<FocusSessionEntity> = focusSessions.value): Int {
        val cutoff = LocalDayUtils.toLocalEpochDay() - 6L
        return sessions.filter { it.localEpochDay >= cutoff }.sumOf { it.durationMinutes }
    }

    // ==================== IN-APP GITHUB RELEASE UPDATER ====================

    fun checkForAppUpdates(userInitiated: Boolean) {
        val currentState = _appUpdateState.value
        if (currentState is AppUpdateState.Downloading || currentState is AppUpdateState.Checking) {
            if (userInitiated) _isUpdateSheetVisible.value = true
            return
        }
        if (currentState is AppUpdateState.ReadyToInstall && !userInitiated) {
            return
        }

        val now = System.currentTimeMillis()
        val sixHoursMillis = 6L * 60L * 60L * 1000L
        if (!userInitiated) {
            val lastCheck = userProfileManager.getLastUpdateCheckMillis()
            if (lastCheck > 0L && (now - lastCheck) < sixHoursMillis) return
            if (!AppUpdateManager.isOnline(getApplication())) return
        }

        if (userInitiated && !AppUpdateManager.isOnline(getApplication())) {
            _appUpdateState.value = AppUpdateState.Error(
                message = "You're offline. Connect to the internet to check GitHub for MonoDo updates."
            )
            _isUpdateSheetVisible.value = true
            return
        }

        viewModelScope.launch {
            if (userInitiated) {
                _appUpdateState.value = AppUpdateState.Checking
                _isUpdateSheetVisible.value = true
            }
            val installedTag = AppUpdateManager.getCurrentInstalledTag()
            val result = AppUpdateManager.fetchLatestRelease()
            result.fold(
                onSuccess = { release ->
                    userProfileManager.setLastUpdateCheckMillis(System.currentTimeMillis())
                    if (VersionComparator.isRemoteNewer(release.tagName, installedTag)) {
                        _appUpdateState.value = AppUpdateState.UpdateAvailable(
                            release = release,
                            currentVersion = installedTag
                        )
                    } else {
                        _appUpdateState.value = AppUpdateState.UpToDate(
                            currentVersion = installedTag,
                            checkedAtMillis = System.currentTimeMillis()
                        )
                    }
                },
                onFailure = { err ->
                    if (userInitiated) {
                        _appUpdateState.value = AppUpdateState.Error(
                            message = err.localizedMessage ?: "Couldn't reach GitHub right now."
                        )
                    }
                }
            )
        }
    }

    fun startUpdateDownload(releaseOverride: GitHubReleaseInfo? = null) {
        val release = releaseOverride ?: when (val state = _appUpdateState.value) {
            is AppUpdateState.UpdateAvailable -> state.release
            is AppUpdateState.Error -> state.release
            is AppUpdateState.ReadyToInstall -> state.release
            else -> null
        } ?: return

        if (!AppUpdateManager.isOnline(getApplication())) {
            _appUpdateState.value = AppUpdateState.Error(
                message = "No internet connection. Connect to Wi-Fi or mobile data and try again.",
                release = release
            )
            return
        }

        updateDownloadJob?.cancel()
        AppUpdateManager.cancelActiveDownload()

        _isUpdateSheetVisible.value = true
        _appUpdateState.value = AppUpdateState.Downloading(
            release = release,
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = release.apkSizeBytes
        )

        updateDownloadJob = viewModelScope.launch {
            val result = AppUpdateManager.downloadApkWithProgress(
                context = getApplication(),
                release = release,
                onProgress = { percent, downloaded, total ->
                    _appUpdateState.value = AppUpdateState.Downloading(
                        release = release,
                        progressPercent = percent,
                        downloadedBytes = downloaded,
                        totalBytes = total
                    )
                }
            )

            result.fold(
                onSuccess = { downloaded ->
                    _appUpdateState.value = AppUpdateState.ReadyToInstall(
                        release = release,
                        cacheFilePath = downloaded.cacheFilePath,
                        savedInDownloadsName = downloaded.savedInDownloadsName,
                        savedInDownloadsSuccess = downloaded.savedInDownloadsSuccess
                    )
                },
                onFailure = { err ->
                    if (err is kotlinx.coroutines.CancellationException) {
                        _appUpdateState.value = AppUpdateState.UpdateAvailable(
                            release = release,
                            currentVersion = AppUpdateManager.getCurrentInstalledTag()
                        )
                    } else {
                        _appUpdateState.value = AppUpdateState.Error(
                            message = err.localizedMessage ?: "Download interrupted. Please retry.",
                            release = release
                        )
                    }
                }
            )
        }
    }

    fun cancelUpdateDownload() {
        val current = _appUpdateState.value
        val release = (current as? AppUpdateState.Downloading)?.release
        AppUpdateManager.cancelActiveDownload()
        updateDownloadJob?.cancel()
        updateDownloadJob = null
        if (release != null) {
            _appUpdateState.value = AppUpdateState.UpdateAvailable(
                release = release,
                currentVersion = AppUpdateManager.getCurrentInstalledTag()
            )
        }
    }

    fun openUpdateSheet() {
        _isUpdateSheetVisible.value = true
    }

    fun closeUpdateSheet() {
        _isUpdateSheetVisible.value = false
    }

    fun dismissUpdateBanner(tag: String) {
        userProfileManager.setDismissedUpdateTag(tag)
        _dismissedUpdateTag.value = tag
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
        updateDownloadJob?.cancel()
        AppUpdateManager.cancelActiveDownload()
    }
}
