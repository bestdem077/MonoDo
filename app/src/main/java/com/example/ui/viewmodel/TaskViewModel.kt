package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.CustomCategoryEntity
import com.example.data.model.HabitEntity
import com.example.data.model.HabitLogEntity
import com.example.data.model.NoteEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.model.TaskSortOrder
import com.example.data.reminder.ReminderPreferences
import com.example.data.reminder.ReminderScheduler
import com.example.data.user.UserProfile
import com.example.data.user.UserProfileManager
import com.example.ui.theme.CategoryTheme
import com.example.widget.MonoDoWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val taskDao = database.taskDao()
    private val noteDao = database.noteDao()
    private val habitDao = database.habitDao()
    private val customCategoryDao = database.customCategoryDao()
    private val userProfileManager = UserProfileManager(application)
    private val reminderPrefs = ReminderPreferences(application)

    // Raw streams from DB
    val allTasks: StateFlow<List<TaskEntity>> = taskDao.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = habitDao.getAllHabits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 7-day habit logs range
    private val startDay = (System.currentTimeMillis() - 7L * 86400000L) / 86400000L
    private val endDay = (System.currentTimeMillis() + 86400000L) / 86400000L
    val habitLogs: StateFlow<List<HabitLogEntity>> = habitDao.getLogsForDateRange(startDay, endDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customCategoriesFromDb: StateFlow<List<CustomCategoryEntity>> = customCategoryDao.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Filter and Sort states for tasks
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _priorityFilter = MutableStateFlow<TaskPriority?>(null)
    val priorityFilter: StateFlow<TaskPriority?> = _priorityFilter.asStateFlow()

    private val _sortOrder = MutableStateFlow(TaskSortOrder.DEFAULT)
    val sortOrder: StateFlow<TaskSortOrder> = _sortOrder.asStateFlow()

    // Zen Focus Mode state (null if inactive)
    private val _zenActiveTask = MutableStateFlow<TaskEntity?>(null)
    val zenActiveTask: StateFlow<TaskEntity?> = _zenActiveTask.asStateFlow()

    // Notes filter state
    private val _noteSearchQuery = MutableStateFlow("")
    val noteSearchQuery: StateFlow<String> = _noteSearchQuery.asStateFlow()

    // User profile state
    private val _userProfile = MutableStateFlow(userProfileManager.getProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    init {
        // Register custom categories with CategoryTheme
        viewModelScope.launch {
            customCategoriesFromDb.collect { list ->
                list.forEach { cat ->
                    CategoryTheme.registerCustomStyle(cat.name, cat.colorHex)
                }
            }
        }
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
                    task.description.contains(query, ignoreCase = true)
            val matchesPriority = priority == null || task.priority == priority
            matchesCategory && matchesQuery && matchesPriority
        }

        when (order) {
            TaskSortOrder.DEFAULT -> filtered.sortedByDescending { it.createdAt }
            TaskSortOrder.PRIORITY -> filtered.sortedWith(
                compareByDescending<TaskEntity> { it.priority.ordinal }
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All", "Work", "Personal", "Health", "Focus", "Finance"))

    val categoryCounts: StateFlow<Map<String, Int>> = allTasks.combine(_selectedCategory) { tasks, _ ->
        val map = mutableMapOf<String, Int>()
        val activeTasks = tasks.filter { !it.isCompleted }
        map["All"] = activeTasks.size
        activeTasks.forEach { task ->
            map[task.category] = (map[task.category] ?: 0) + 1
        }
        map
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered notes
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _noteSearchQuery
    ) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            notes.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    // Custom Category with color palette
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

    // Zen Focus Mode Controls
    fun enterZenFocus(task: TaskEntity) {
        _zenActiveTask.value = task
    }

    fun exitZenFocus() {
        _zenActiveTask.value = null
    }

    fun setNoteSearchQuery(query: String) {
        _noteSearchQuery.value = query
    }

    // Task CRUD operations
    fun createTask(
        title: String,
        description: String = "",
        category: String = "General",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Long? = null,
        subtaskTitles: List<String> = emptyList()
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
                createdAt = System.currentTimeMillis()
            )
            taskDao.insertTask(task)
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
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
            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = updated
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            taskDao.deleteTask(task)
            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = null
            }
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun rescheduleTask(task: TaskEntity, newDueDate: Long?) {
        viewModelScope.launch {
            val updated = task.copy(dueDate = newDueDate)
            taskDao.updateTask(updated)
            if (_zenActiveTask.value?.id == task.id) {
                _zenActiveTask.value = updated
            }
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
        }
    }

    // Habits Operations
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

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            habitDao.deleteHabitById(habitId)
            habitDao.deleteLogsForHabit(habitId)
        }
    }

    fun toggleHabitDay(habitId: Long, epochDay: Long) {
        viewModelScope.launch {
            val existing = habitDao.getLog(habitId, epochDay)
            if (existing != null) {
                habitDao.deleteLog(habitId, epochDay)
            } else {
                habitDao.insertOrUpdateLog(
                    HabitLogEntity(habitId = habitId, dateEpochDay = epochDay, isCompleted = true)
                )
            }
        }
    }

    // Notes CRUD operations
    fun createNote(title: String, content: String, category: String = "General") {
        viewModelScope.launch {
            val note = NoteEntity(
                title = title.trim(),
                content = content.trim(),
                category = category.trim().ifBlank { "General" },
                isPinned = false,
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
        }
    }

    // Profile & Reset
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
            taskDao.deleteAllTasks()
            noteDao.deleteAllNotes()
            habitDao.deleteAllHabits()
            habitDao.deleteAllHabitLogs()
            customCategoryDao.deleteAllCategories()
            userProfileManager.resetProfile()
            ReminderScheduler.cancelDailyReminder(getApplication())
            refreshProfile()
            MonoDoWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    // 1-Tap Offline Backup & Restore (JSON)
    fun exportJsonBackup(): String {
        val root = JSONObject()
        root.put("app", "MonoDo")
        root.put("version", 3)
        root.put("timestamp", System.currentTimeMillis())
        root.put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))

        // Tasks
        val tasksArray = JSONArray()
        allTasks.value.forEach { t ->
            val tObj = JSONObject().apply {
                put("id", t.id)
                put("title", t.title)
                put("description", t.description)
                put("category", t.category)
                put("priority", t.priority.name)
                if (t.dueDate != null) put("dueDate", t.dueDate)
                put("isCompleted", t.isCompleted)
                put("createdAt", t.createdAt)
                if (t.completedAt != null) put("completedAt", t.completedAt)

                val subArray = JSONArray()
                t.subtasks.forEach { s ->
                    val sObj = JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("isCompleted", s.isCompleted)
                    }
                    subArray.put(sObj)
                }
                put("subtasks", subArray)
            }
            tasksArray.put(tObj)
        }
        root.put("tasks", tasksArray)

        // Notes
        val notesArray = JSONArray()
        allNotes.value.forEach { n ->
            val nObj = JSONObject().apply {
                put("id", n.id)
                put("title", n.title)
                put("content", n.content)
                put("category", n.category)
                put("isPinned", n.isPinned)
                put("createdAt", n.createdAt)
                put("updatedAt", n.updatedAt)
            }
            notesArray.put(nObj)
        }
        root.put("notes", notesArray)

        // Habits
        val habitsArray = JSONArray()
        allHabits.value.forEach { h ->
            val hObj = JSONObject().apply {
                put("id", h.id)
                put("title", h.title)
                put("frequency", h.frequency)
                put("colorHex", h.colorHex)
                put("createdAt", h.createdAt)
            }
            habitsArray.put(hObj)
        }
        root.put("habits", habitsArray)

        // Habit Logs
        val habitLogsArray = JSONArray()
        habitLogs.value.forEach { l ->
            val lObj = JSONObject().apply {
                put("habitId", l.habitId)
                put("dateEpochDay", l.dateEpochDay)
                put("isCompleted", l.isCompleted)
                put("completedAt", l.completedAt)
            }
            habitLogsArray.put(lObj)
        }
        root.put("habitLogs", habitLogsArray)

        // Custom Categories
        val categoriesArray = JSONArray()
        customCategoriesFromDb.value.forEach { c ->
            val cObj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("colorHex", c.colorHex)
                put("createdAt", c.createdAt)
            }
            categoriesArray.put(cObj)
        }
        root.put("customCategories", categoriesArray)

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

            viewModelScope.launch {
                // Restore Custom Categories
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

                // Restore Tasks
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

                        val priorityStr = tObj.optString("priority", "MEDIUM")
                        val priority = try {
                            TaskPriority.valueOf(priorityStr)
                        } catch (e: Exception) {
                            TaskPriority.MEDIUM
                        }

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
                            completedAt = if (tObj.has("completedAt") && !tObj.isNull("completedAt")) tObj.getLong("completedAt") else null
                        )
                        taskDao.insertTask(task)
                    }
                }

                // Restore Notes
                val notesArray = root.optJSONArray("notes")
                if (notesArray != null) {
                    for (i in 0 until notesArray.length()) {
                        val nObj = notesArray.getJSONObject(i)
                        val note = NoteEntity(
                            id = nObj.optLong("id", 0L),
                            title = nObj.optString("title", "Imported Note"),
                            content = nObj.optString("content", ""),
                            category = nObj.optString("category", "General"),
                            isPinned = nObj.optBoolean("isPinned", false),
                            createdAt = nObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = nObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                        noteDao.insertNote(note)
                    }
                }

                // Restore Habits
                val habitsArray = root.optJSONArray("habits")
                if (habitsArray != null) {
                    for (i in 0 until habitsArray.length()) {
                        val hObj = habitsArray.getJSONObject(i)
                        val habit = HabitEntity(
                            id = hObj.optLong("id", 0L),
                            title = hObj.optString("title", "Imported Habit"),
                            frequency = hObj.optString("frequency", "DAILY"),
                            colorHex = hObj.optString("colorHex", "#2563EB"),
                            createdAt = hObj.optLong("createdAt", System.currentTimeMillis())
                        )
                        habitDao.insertHabit(habit)
                    }
                }

                // Restore Habit Logs
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

                MonoDoWidgetProvider.updateAllWidgets(getApplication())
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Analytics calculations for 7 days
    fun getCompletionStatsForLast7Days(): List<Pair<String, Int>> {
        val tasks = allTasks.value
        val result = mutableListOf<Pair<String, Int>>()
        val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayStart = (dayCal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L

            val completedOnDay = tasks.count {
                it.isCompleted && it.completedAt != null && it.completedAt in dayStart until dayEnd
            }
            val dayLabel = days[dayCal.get(Calendar.DAY_OF_WEEK) - 1]
            result.add(dayLabel to completedOnDay)
        }
        return result
    }

    fun calculateStreakDays(): Int {
        val tasks = allTasks.value
        val completedTasks = tasks.filter { it.isCompleted && it.completedAt != null }
            .sortedByDescending { it.completedAt }
        if (completedTasks.isEmpty()) return 0

        var streak = 0
        val cal = Calendar.getInstance()
        for (i in 0..30) {
            val dayStart = (cal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, -i)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val dayEnd = dayStart + 24 * 60 * 60 * 1000L

            val hasCompleted = completedTasks.any { it.completedAt!! in dayStart until dayEnd }
            if (hasCompleted) {
                streak++
            } else if (i > 0) {
                break
            }
        }
        return streak
    }
}
