package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.NoteDao
import com.example.data.db.TaskDao
import com.example.data.model.NoteEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class DayActivity(
    val dayLabel: String,
    val dayOfMonth: Int = 1,
    val dateString: String,
    val completedCount: Int,
    val isToday: Boolean,
    val dayFullName: String = ""
)

data class MonoDoUiState(
    val activeTasks: List<TaskEntity> = emptyList(),
    val completedTasks: List<TaskEntity> = emptyList(),
    val categories: List<String> = listOf("All", "Work", "Personal", "Health", "Focus", "Ideas"),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val allTags: List<String> = emptyList(),
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val selectedPriority: TaskPriority? = null,
    val streakDays: Int = 0,
    val completedTodayCount: Int = 0,
    val totalActiveCount: Int = 0,
    val weeklyActivity: List<DayActivity> = emptyList(),
    val showCompletedSection: Boolean = true
)

/**
 * TaskViewModel manages task state, notes, and exposes reactive StateFlow data for the UI layer.
 * Backed by Room's TaskDao and NoteDao to support full CRUD operations.
 */
class TaskViewModel(
    application: Application,
    private val taskDao: TaskDao,
    private val noteDao: NoteDao
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AppDatabase.getDatabase(application).taskDao(),
        AppDatabase.getDatabase(application).noteDao()
    )

    constructor(application: Application, taskDao: TaskDao) : this(
        application,
        taskDao,
        AppDatabase.getDatabase(application).noteDao()
    )

    private val repository: TaskRepository = TaskRepository(taskDao)

    /**
     * Production clean state: We do not inject mock/dummy tasks.
     */
    fun seedInitialTasksIfEmpty() {
        // No-op for production: users start with their own real tasks
    }

    /**
     * Production cleanup: Purges any demo/dummy tasks and notes from early testing,
     * leaving user-created data intact and guaranteeing a clean slate.
     */
    fun removeSampleDataIfPresent() {
        viewModelScope.launch {
            val sampleTaskTitles = setOf(
                "Prepare Q4 Product Strategy & KPI Deck",
                "Morning 5km Run & Mobility Flow",
                "Weekly Grocery Restock & Meal Prep",
                "Deep Focus: Category Tagging & Filter Bar",
                "Explore Minimalist Haptic Gestures"
            )
            val currentTasks = repository.allTasks.first()
            for (task in currentTasks) {
                if (task.title in sampleTaskTitles) {
                    repository.deleteTask(task)
                }
            }

            val sampleNoteTitles = setOf(
                "Q4 Key Strategic Objectives",
                "Daily Recovery & Mobility Protocol",
                "Favorite Coffee Roasters & Grind Sizes"
            )
            val currentNotes = noteDao.getAllNotes().first()
            for (note in currentNotes) {
                if (note.title in sampleNoteTitles) {
                    noteDao.deleteNote(note)
                }
            }
        }
    }

    // Observable raw StateFlow stream of all tasks directly from Room DAO
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Observable raw StateFlow stream of all Google Keep-style notes from Room DAO
    val allNotes: StateFlow<List<NoteEntity>> = noteDao.getAllNotes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter and query state
    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPriority = MutableStateFlow<TaskPriority?>(null)
    val selectedPriority: StateFlow<TaskPriority?> = _selectedPriority.asStateFlow()

    private val _showCompletedSection = MutableStateFlow(true)
    val showCompletedSection: StateFlow<Boolean> = _showCompletedSection.asStateFlow()

    // Consolidated reactive StateFlow for the UI screen
    val uiState: StateFlow<MonoDoUiState> = combine(
        allTasks,
        _selectedCategory,
        _searchQuery,
        _selectedPriority,
        _showCompletedSection
    ) { tasksList, category, query, priority, showCompleted ->
        val trimmed = query.trim()
        val tagQuery = trimmed.removePrefix("#").trim()

        val baseCategories = listOf("All", "Work", "Personal", "Health", "Focus", "Ideas")
        val customCategories = tasksList.map { it.category }.filter { it.isNotBlank() && !baseCategories.contains(it) }.distinct()
        val allTagsList = tasksList.flatMap { it.tags }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val tagFilters = allTagsList.map { if (it.startsWith("#")) it else "#$it" }
        val allDisplayCategories = (baseCategories + customCategories + tagFilters).distinct()

        // Calculate count of active tasks per category for the filter bar
        val activeTasksAll = tasksList.filter { !it.isCompleted }
        val countsMap = mutableMapOf<String, Int>()
        countsMap["All"] = activeTasksAll.size
        for (cat in allDisplayCategories) {
            if (cat != "All") {
                countsMap[cat] = activeTasksAll.count { task ->
                    if (cat.startsWith("#")) {
                        val raw = cat.removePrefix("#")
                        task.tags.any { it.equals(raw, ignoreCase = true) || it.equals(cat, ignoreCase = true) }
                    } else {
                        task.category.equals(cat, ignoreCase = true) ||
                                task.tags.any { it.equals(cat, ignoreCase = true) || it.equals("#$cat", ignoreCase = true) }
                    }
                }
            }
        }

        val filtered = tasksList.filter { task ->
            val matchesCategory = when {
                category == "All" -> true
                category.startsWith("#") -> {
                    val raw = category.removePrefix("#")
                    task.tags.any { it.equals(raw, ignoreCase = true) || it.equals(category, ignoreCase = true) }
                }
                else -> {
                    task.category.equals(category, ignoreCase = true) ||
                            task.tags.any { it.equals(category, ignoreCase = true) || it.equals("#$category", ignoreCase = true) }
                }
            }
            val matchesQuery = if (trimmed.isBlank()) true else {
                task.title.contains(trimmed, ignoreCase = true) ||
                        task.category.contains(tagQuery, ignoreCase = true) ||
                        task.category.contains(trimmed, ignoreCase = true) ||
                        task.tags.any { it.contains(tagQuery, ignoreCase = true) || it.contains(trimmed, ignoreCase = true) } ||
                        task.description.contains(trimmed, ignoreCase = true) ||
                        task.priority.label.contains(trimmed, ignoreCase = true) ||
                        task.subtasks.any { it.title.contains(trimmed, ignoreCase = true) }
            }
            val matchesPriority = if (priority == null) true else task.priority == priority
            matchesCategory && matchesQuery && matchesPriority
        }

        val active = filtered.filter { !it.isCompleted }
            .sortedWith(
                compareByDescending<TaskEntity> { it.priority.ordinal }
                    .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    .thenByDescending { it.createdAt }
            )

        val completed = filtered.filter { it.isCompleted }
            .sortedByDescending { it.completedAt ?: it.createdAt }

        // Dynamic streak & stats calculation
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val completedToday = tasksList.count {
            it.isCompleted && (it.completedAt ?: it.createdAt) >= todayStart
        }

        val weekly = computeWeeklyActivity(tasksList)
        val streak = computeStreak(tasksList)

        MonoDoUiState(
            activeTasks = active,
            completedTasks = completed,
            categories = allDisplayCategories,
            categoryCounts = countsMap,
            allTags = allTagsList,
            selectedCategory = category,
            searchQuery = query,
            selectedPriority = priority,
            streakDays = streak,
            completedTodayCount = completedToday,
            totalActiveCount = tasksList.count { !it.isCompleted },
            weeklyActivity = weekly,
            showCompletedSection = showCompleted
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonoDoUiState()
    )

    // ==========================================
    // CRUD Operations (Room DAO interactions)
    // ==========================================

    /**
     * Create: Insert a task entity directly into Room database.
     */
    fun insertTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    /**
     * Create: Insert a batch of task entities into Room database.
     */
    fun insertTasks(tasks: List<TaskEntity>) {
        viewModelScope.launch {
            repository.insertTasks(tasks)
        }
    }

    /**
     * Create: Convenience builder to create and insert a new task with optional subtasks and tags.
     */
    fun createTask(
        title: String,
        description: String = "",
        category: String = "General",
        tags: List<String> = emptyList(),
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Long? = null,
        subtaskTitles: List<String> = emptyList()
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val subtaskList = subtaskTitles
                .filter { it.isNotBlank() }
                .map { SubTask(id = UUID.randomUUID().toString(), title = it.trim(), isCompleted = false) }

            val cleanTags = tags.map { it.trim().removePrefix("#") }.filter { it.isNotBlank() }.distinct()

            val task = TaskEntity(
                title = title.trim(),
                description = description.trim(),
                category = if (category.isBlank()) "General" else category,
                tags = cleanTags,
                priority = priority,
                dueDate = dueDate,
                isCompleted = false,
                subtasks = subtaskList
            )
            repository.insertTask(task)
        }
    }

    fun createTask(
        title: String,
        description: String = "",
        category: String = "General",
        priority: TaskPriority = TaskPriority.MEDIUM,
        dueDate: Long? = null,
        subtaskTitles: List<String> = emptyList()
    ) = createTask(title, description, category, emptyList(), priority, dueDate, subtaskTitles)

    /**
     * Read: Retrieve a single task by its ID asynchronously.
     */
    fun getTaskById(id: Long, onResult: (TaskEntity?) -> Unit) {
        viewModelScope.launch {
            val task = repository.getTaskById(id)
            onResult(task)
        }
    }

    /**
     * Read: Observe a specific task by ID as a reactive Flow.
     */
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> {
        return repository.getTaskByIdFlow(id)
    }

    /**
     * Update: Update an existing task in Room database.
     */
    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    /**
     * Update: Toggle the completion status of a task with timestamp.
     */
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val updated = task.copy(
                isCompleted = !task.isCompleted,
                completedAt = if (!task.isCompleted) System.currentTimeMillis() else null
            )
            repository.updateTask(updated)
        }
    }

    /**
     * Update: Add a subtask to an existing task.
     */
    fun addSubtaskToTask(task: TaskEntity, subtaskTitle: String) {
        if (subtaskTitle.isBlank()) return
        viewModelScope.launch {
            val newSubtask = SubTask(
                id = UUID.randomUUID().toString(),
                title = subtaskTitle.trim(),
                isCompleted = false
            )
            val updatedList = task.subtasks + newSubtask
            repository.updateTask(task.copy(subtasks = updatedList))
        }
    }

    /**
     * Update: Toggle a subtask's completion status.
     */
    fun toggleSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            val updatedSubtasks = task.subtasks.map {
                if (it.id == subtaskId) it.copy(isCompleted = !it.isCompleted) else it
            }
            repository.updateTask(task.copy(subtasks = updatedSubtasks))
        }
    }

    /**
     * Update: Delete a subtask from an existing task.
     */
    fun deleteSubtask(task: TaskEntity, subtaskId: String) {
        viewModelScope.launch {
            val updatedList = task.subtasks.filter { it.id != subtaskId }
            repository.updateTask(task.copy(subtasks = updatedList))
        }
    }

    /**
     * Delete: Remove a task entity from Room database.
     */
    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    /**
     * Delete: Remove a task by its ID.
     */
    fun deleteTaskById(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    /**
     * Delete: Clear all tasks from the database.
     */
    fun clearAllTasks() {
        viewModelScope.launch {
            repository.deleteAllTasks()
        }
    }

    // ==========================================
    // Notes CRUD Operations (Room DAO)
    // ==========================================

    fun createNote(
        title: String,
        content: String,
        category: String = "General",
        isPinned: Boolean = false
    ) {
        if (title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val note = NoteEntity(
                title = title.trim(),
                content = content.trim(),
                category = category.ifBlank { "General" },
                isPinned = isPinned,
                createdAt = now,
                updatedAt = now
            )
            noteDao.insertNote(note)
        }
    }

    fun updateNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(updatedAt = System.currentTimeMillis())
            noteDao.updateNote(updated)
        }
    }

    fun deleteNote(note: NoteEntity) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            noteDao.deleteAllNotes()
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.deleteAllTasks()
            noteDao.deleteAllNotes()
        }
    }

    fun togglePinNote(note: NoteEntity) {
        viewModelScope.launch {
            val updated = note.copy(
                isPinned = !note.isPinned,
                updatedAt = System.currentTimeMillis()
            )
            noteDao.updateNote(updated)
        }
    }

    /**
     * Production clean state: We do not inject mock/dummy notes.
     */
    fun seedInitialNotesIfEmpty() {
        // No-op for production: users start with their own real notes
    }

    // ==========================================
    // Filter and UI State Controls
    // ==========================================

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Search and filter tasks locally by title, tag, or keyword.
     */
    fun searchTasks(query: String) {
        setSearchQuery(query)
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        _selectedPriority.value = priority
    }

    fun toggleShowCompleted() {
        _showCompletedSection.value = !_showCompletedSection.value
    }

    // ==========================================
    // Internal Analytic Helpers
    // ==========================================

    private fun getStartOfDay(timeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }

    private fun computeWeeklyActivity(allTasks: List<TaskEntity>): List<DayActivity> {
        val days = mutableListOf<DayActivity>()
        val calendar = Calendar.getInstance()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))
        val fullDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val start = getStartOfDay(cal.timeInMillis)
            val end = start + 86_400_000L
            val dateStr = dateFormat.format(cal.time)
            val dayLabel = dayFormat.format(cal.time).uppercase().take(2)
            val dayFullName = fullDayFormat.format(cal.time)

            val count = allTasks.count {
                val completionTime = it.completedAt ?: (if (it.isCompleted) it.createdAt else 0L)
                it.isCompleted && completionTime in start until end
            }

            days.add(
                DayActivity(
                    dayLabel = dayLabel,
                    dayOfMonth = cal.get(Calendar.DAY_OF_MONTH),
                    dateString = dateStr,
                    completedCount = count,
                    isToday = dateStr == todayStr,
                    dayFullName = dayFullName
                )
            )
        }
        return days
    }

    private fun computeStreak(allTasks: List<TaskEntity>): Int {
        val completedTimestamps = allTasks.filter { it.isCompleted }
            .map { it.completedAt ?: it.createdAt }

        if (completedTimestamps.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val completedDates = completedTimestamps.map { dateFormat.format(Date(it)) }.toSet()

        val calendar = Calendar.getInstance()
        val todayStr = dateFormat.format(calendar.time)

        var streak = 0
        if (completedDates.contains(todayStr)) {
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        } else {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayStr = dateFormat.format(calendar.time)
            if (!completedDates.contains(yesterdayStr)) {
                return 0
            }
            streak++
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        while (true) {
            val dateStr = dateFormat.format(calendar.time)
            if (completedDates.contains(dateStr)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    /**
     * Export all active and completed tasks to a clean JSON string for backup.
     */
    fun exportTasksAsJson(): String {
        val root = JSONObject()
        root.put("app", "MonoDo")
        root.put("version", "1.0")
        root.put("exportedAt", System.currentTimeMillis())

        val tasksArray = JSONArray()
        for (task in allTasks.value) {
            val taskObj = JSONObject()
            taskObj.put("id", task.id)
            taskObj.put("title", task.title)
            taskObj.put("description", task.description)
            taskObj.put("category", task.category)
            taskObj.put("isCompleted", task.isCompleted)
            taskObj.put("priority", task.priority.name)
            taskObj.put("dueDate", task.dueDate ?: JSONObject.NULL)
            taskObj.put("createdAt", task.createdAt)
            taskObj.put("completedAt", task.completedAt ?: JSONObject.NULL)

            val tagsArray = JSONArray()
            task.tags.forEach { tagsArray.put(it) }
            taskObj.put("tags", tagsArray)

            val subtasksArray = JSONArray()
            task.subtasks.forEach { sub ->
                val subObj = JSONObject()
                subObj.put("id", sub.id)
                subObj.put("title", sub.title)
                subObj.put("isCompleted", sub.isCompleted)
                subtasksArray.put(subObj)
            }
            taskObj.put("subtasks", subtasksArray)

            tasksArray.put(taskObj)
        }
        root.put("tasks", tasksArray)
        return root.toString(2)
    }

    /**
     * Import tasks from a backup JSON string into the local Room database.
     * Returns the count of successfully restored tasks.
     */
    suspend fun importTasksFromJson(jsonString: String): Int {
        try {
            val root = JSONObject(jsonString)
            val tasksArray = root.getJSONArray("tasks")
            val newTasks = mutableListOf<TaskEntity>()

            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                val title = obj.optString("title", "").trim()
                if (title.isBlank()) continue

                val description = obj.optString("description", "")
                val category = obj.optString("category", "Focus")
                val isCompleted = obj.optBoolean("isCompleted", false)
                val priorityStr = obj.optString("priority", TaskPriority.MEDIUM.name)
                val priority = try { TaskPriority.valueOf(priorityStr) } catch (_: Exception) { TaskPriority.MEDIUM }
                val dueDate = if (obj.isNull("dueDate")) null else obj.optLong("dueDate")
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val completedAt = if (obj.isNull("completedAt")) null else obj.optLong("completedAt")

                val tags = mutableListOf<String>()
                val tagsArr = obj.optJSONArray("tags")
                if (tagsArr != null) {
                    for (t in 0 until tagsArr.length()) {
                        tags.add(tagsArr.getString(t))
                    }
                }

                val subtasks = mutableListOf<SubTask>()
                val subArr = obj.optJSONArray("subtasks")
                if (subArr != null) {
                    for (s in 0 until subArr.length()) {
                        val sObj = subArr.getJSONObject(s)
                        subtasks.add(
                            SubTask(
                                id = sObj.optString("id", UUID.randomUUID().toString()),
                                title = sObj.optString("title", ""),
                                isCompleted = sObj.optBoolean("isCompleted", false)
                            )
                        )
                    }
                }

                newTasks.add(
                    TaskEntity(
                        id = 0, // auto-generate fresh IDs
                        title = title,
                        description = description,
                        category = category,
                        tags = tags,
                        priority = priority,
                        dueDate = dueDate,
                        isCompleted = isCompleted,
                        createdAt = createdAt,
                        completedAt = completedAt,
                        subtasks = subtasks
                    )
                )
            }

            if (newTasks.isNotEmpty()) {
                repository.insertTasks(newTasks)
            }
            return newTasks.size
        } catch (_: Exception) {
            return 0
        }
    }

    /**
     * Delete all completed tasks to declutter the workspace.
     */
    fun clearCompletedTasks() {
        viewModelScope.launch {
            val completed = allTasks.value.filter { it.isCompleted }
            for (task in completed) {
                repository.deleteTask(task)
            }
        }
    }

    /**
     * Seeds initial demonstration tasks spanning Work, Personal, Health, Focus, and Ideas categories.
     */
    private suspend fun seedInitialTasks() {
        val now = System.currentTimeMillis()
        val sampleTasks = listOf(
            TaskEntity(
                title = "Prepare Q4 Product Strategy & KPI Deck",
                description = "Review quarterly metrics, map milestones, and align with design and engineering leads.",
                category = "Work",
                tags = listOf("roadmap", "q4"),
                priority = TaskPriority.HIGH,
                dueDate = now + 86_400_000L * 2,
                isCompleted = false,
                subtasks = listOf(
                    SubTask(id = UUID.randomUUID().toString(), title = "Compile OKR completion rates", isCompleted = true),
                    SubTask(id = UUID.randomUUID().toString(), title = "Draft core initiative slides", isCompleted = false),
                    SubTask(id = UUID.randomUUID().toString(), title = "Collect tech lead feedback", isCompleted = false)
                )
            ),
            TaskEntity(
                title = "Morning 5km Run & Mobility Flow",
                description = "Maintain aerobic base and complete 15 mins of full body hip and shoulder mobility.",
                category = "Health",
                tags = listOf("fitness", "wellness"),
                priority = TaskPriority.HIGH,
                dueDate = now,
                isCompleted = false,
                subtasks = listOf(
                    SubTask(id = UUID.randomUUID().toString(), title = "Dynamic warmup stretches", isCompleted = true),
                    SubTask(id = UUID.randomUUID().toString(), title = "5km continuous run", isCompleted = false),
                    SubTask(id = UUID.randomUUID().toString(), title = "15-min mobility flow", isCompleted = false)
                )
            ),
            TaskEntity(
                title = "Weekly Grocery Restock & Meal Prep",
                description = "Pick up fresh vegetables, sourdough, olive oil, and prepare lunch containers.",
                category = "Personal",
                tags = listOf("home", "nutrition"),
                priority = TaskPriority.MEDIUM,
                dueDate = now + 86_400_000L,
                isCompleted = false,
                subtasks = listOf(
                    SubTask(id = UUID.randomUUID().toString(), title = "Organic greens & berries", isCompleted = false),
                    SubTask(id = UUID.randomUUID().toString(), title = "Batch cook quinoa & roasted veg", isCompleted = false)
                )
            ),
            TaskEntity(
                title = "Deep Focus: Category Tagging & Filter Bar",
                description = "Implement color-coded category labels, filter bar chips, and live counts.",
                category = "Focus",
                tags = listOf("android", "compose"),
                priority = TaskPriority.URGENT,
                dueDate = now,
                isCompleted = true,
                completedAt = now - 3600_000L,
                subtasks = listOf(
                    SubTask(id = UUID.randomUUID().toString(), title = "CategoryTheme color palettes", isCompleted = true),
                    SubTask(id = UUID.randomUUID().toString(), title = "CategoryChipsRow filter bar", isCompleted = true)
                )
            ),
            TaskEntity(
                title = "Explore Minimalist Haptic Gestures",
                description = "Idea to trigger subtle physical clicks on swipe completion and long press.",
                category = "Ideas",
                tags = listOf("design", "ux"),
                priority = TaskPriority.LOW,
                dueDate = null,
                isCompleted = false,
                subtasks = emptyList()
            )
        )
        repository.insertTasks(sampleTasks)
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    return TaskViewModel(application, db.taskDao(), db.noteDao()) as T
                }
            }

        fun provideFactory(application: Application, dao: TaskDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    return TaskViewModel(application, dao, db.noteDao()) as T
                }
            }
    }
}
