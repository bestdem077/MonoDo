package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.TaskDao
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    val categories: List<String> = listOf("All", "Work", "Personal", "Focus", "Ideas"),
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
 * TaskViewModel manages task state and exposes reactive StateFlow data for the UI layer.
 * Backed by Room's TaskDao and TaskRepository to support full CRUD operations.
 */
class TaskViewModel(
    application: Application,
    private val taskDao: TaskDao
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        AppDatabase.getDatabase(application).taskDao()
    )

    private val repository: TaskRepository = TaskRepository(taskDao)

    // Observable raw StateFlow stream of all tasks directly from Room DAO
    val allTasks: StateFlow<List<TaskEntity>> = repository.allTasks.stateIn(
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

        val baseCategories = listOf("All", "Focus", "Work", "Personal", "Ideas")
        val customCategories = tasksList.map { it.category }.filter { it.isNotBlank() && !baseCategories.contains(it) }.distinct()
        val allTagsList = tasksList.flatMap { it.tags }.map { it.trim() }.filter { it.isNotBlank() }.distinct().sorted()
        val tagFilters = allTagsList.map { if (it.startsWith("#")) it else "#$it" }
        val allDisplayCategories = (baseCategories + customCategories + tagFilters).distinct()

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
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val fullDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val now = System.currentTimeMillis()
        val todayStr = dateFormat.format(Date(now))

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Determine days to rewind to align with Monday of the current week
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2, ..., Saturday=7
        val daysFromMonday = when (dayOfWeek) {
            Calendar.SUNDAY -> 6
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)

        for (i in 0..6) {
            val start = cal.timeInMillis
            val end = start + 86_400_000L
            val dateStr = dateFormat.format(cal.time)
            val dayLabel = dayFormat.format(cal.time).uppercase().take(3)
            val dayFullName = fullDayFormat.format(cal.time)
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

            val count = allTasks.count {
                val completionTime = it.completedAt ?: (if (it.isCompleted) it.createdAt else 0L)
                it.isCompleted && completionTime in start until end
            }

            days.add(
                DayActivity(
                    dayLabel = dayLabel,
                    dayOfMonth = dayOfMonth,
                    dateString = dateStr,
                    completedCount = count,
                    isToday = dateStr == todayStr,
                    dayFullName = dayFullName
                )
            )
            cal.add(Calendar.DAY_OF_YEAR, 1)
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

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(application)
                    return TaskViewModel(application, db.taskDao()) as T
                }
            }

        fun provideFactory(application: Application, dao: TaskDao): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return TaskViewModel(application, dao) as T
                }
            }
    }
}
