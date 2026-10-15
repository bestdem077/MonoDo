package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.TaskDao
import com.example.data.model.NoteEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.user.UserProfileManager
import com.example.ui.viewmodel.TaskViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = db.taskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("MonoDo", appName)
    }

    @Test
    fun `task creation and dao CRUD operations`() = runBlocking {
        val task = TaskEntity(
            title = "Architect Design System",
            description = "Build monochrome components",
            category = "Focus",
            priority = TaskPriority.URGENT,
            subtasks = listOf(
                SubTask(id = "1", title = "Color tokens", isCompleted = true)
            )
        )
        val id = taskDao.insertTask(task)
        assertTrue(id > 0)

        val retrieved = taskDao.getTaskById(id)
        assertNotNull(retrieved)
        assertEquals("Architect Design System", retrieved?.title)
        assertEquals(TaskPriority.URGENT, retrieved?.priority)
        assertEquals(1, retrieved?.subtasks?.size)
        assertTrue(retrieved?.subtasks?.first()?.isCompleted == true)

        // Update
        val updated = retrieved!!.copy(isCompleted = true)
        taskDao.updateTask(updated)
        val afterUpdate = taskDao.getTaskById(id)
        assertTrue(afterUpdate?.isCompleted == true)

        // Delete
        taskDao.deleteById(id)
        val afterDelete = taskDao.getTaskById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun `note creation and dao operations`() = runBlocking {
        val noteDao = db.noteDao()
        val note = NoteEntity(
            title = "Architecture Ideas",
            content = "Keep local database clean and reactive.",
            category = "Work",
            isPinned = true
        )
        val id = noteDao.insertNote(note)
        assertTrue(id > 0)

        val retrieved = noteDao.getNoteById(id)
        assertNotNull(retrieved)
        assertEquals("Architecture Ideas", retrieved?.title)
        assertEquals("Work", retrieved?.category)
        assertTrue(retrieved?.isPinned == true)

        noteDao.deleteNoteById(id)
        val afterDelete = noteDao.getNoteById(id)
        assertEquals(null, afterDelete)
    }

    @Test
    fun `task viewModel provides observable state`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TaskViewModel(app, taskDao)
        val initialUiState = viewModel.uiState.value
        assertNotNull(initialUiState)
        assertEquals("All", initialUiState.selectedCategory)
        assertEquals(0, initialUiState.activeTasks.size)
        assertEquals(0, initialUiState.completedTasks.size)
    }

    @Test
    fun `deleteAllTasks empties database completely`() = runBlocking {
        taskDao.insertTask(TaskEntity(title = "Task 1"))
        taskDao.insertTask(TaskEntity(title = "Task 2"))
        assertEquals(2, taskDao.getTaskCount())

        taskDao.deleteAllTasks()
        assertEquals(0, taskDao.getTaskCount())
    }

    @Test
    fun `viewModel can be created by AndroidViewModelFactory`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(TaskViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun `search filters tasks by title and tag`() = runBlocking {
        taskDao.insertTask(TaskEntity(title = "Design Swiss System", category = "Focus"))
        taskDao.insertTask(TaskEntity(title = "Review Weekly PRs", category = "Work"))

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TaskViewModel(app, taskDao)

        val state = viewModel.uiState.first { it.activeTasks.size == 2 }
        assertEquals(2, state.activeTasks.size)

        // Search by title keyword
        viewModel.searchTasks("Swiss")
        val titleMatch = viewModel.uiState.first { it.searchQuery == "Swiss" }
        assertEquals(1, titleMatch.activeTasks.size)
        assertEquals("Design Swiss System", titleMatch.activeTasks.first().title)

        // Search by tag with hashtag
        viewModel.searchTasks("#Work")
        val tagMatch = viewModel.uiState.first { it.searchQuery == "#Work" }
        assertEquals(1, tagMatch.activeTasks.size)
        assertEquals("Review Weekly PRs", tagMatch.activeTasks.first().title)
    }

    @Test
    fun `weekly activity calculates 7 days of current week and reflects completed tasks`() = runBlocking {
        val now = System.currentTimeMillis()
        val completedTask = TaskEntity(
            title = "Finalize Design Specifications",
            isCompleted = true,
            completedAt = now
        )
        taskDao.insertTask(completedTask)

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TaskViewModel(app, taskDao)

        val state = viewModel.uiState.first { it.completedTasks.size == 1 }
        assertEquals(7, state.weeklyActivity.size)

        // Today must be marked isToday = true
        val todayActivity = state.weeklyActivity.firstOrNull { it.isToday }
        assertNotNull(todayActivity)
        assertEquals(1, todayActivity?.completedCount)

        // Total completions for the week
        val totalWeekCompleted = state.weeklyActivity.sumOf { it.completedCount }
        assertEquals(1, totalWeekCompleted)
    }

    @Test
    fun `category labeling system and filter bar correctly isolates categories`() = runBlocking {
        taskDao.insertTask(TaskEntity(title = "Strategy Deck", category = "Work"))
        taskDao.insertTask(TaskEntity(title = "Morning 5k Run", category = "Health"))
        taskDao.insertTask(TaskEntity(title = "Grocery Restock", category = "Personal"))

        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TaskViewModel(app, taskDao)

        val initialState = viewModel.uiState.first { it.activeTasks.size >= 3 }
        assertTrue(initialState.categories.contains("Work"))
        assertTrue(initialState.categories.contains("Personal"))
        assertTrue(initialState.categories.contains("Health"))

        // Filter by Work
        viewModel.selectCategory("Work")
        val workState = viewModel.uiState.first { it.selectedCategory == "Work" }
        assertEquals(1, workState.activeTasks.size)
        assertEquals("Strategy Deck", workState.activeTasks.first().title)

        // Filter by Health
        viewModel.selectCategory("Health")
        val healthState = viewModel.uiState.first { it.selectedCategory == "Health" }
        assertEquals(1, healthState.activeTasks.size)
        assertEquals("Morning 5k Run", healthState.activeTasks.first().title)

        // Reset to All
        viewModel.selectCategory("All")
        val allState = viewModel.uiState.first { it.selectedCategory == "All" }
        assertTrue(allState.activeTasks.size >= 3)
    }

    @Test
    fun `userProfileManager persists name and ensures onboarding does not loop`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val manager = UserProfileManager(app)

        // Fresh state: should not be onboarded initially
        manager.resetProfile()
        val initialProfile = manager.getProfile()
        assertEquals(false, initialProfile.isOnboarded)

        // Save profile
        manager.saveProfile("Vishal", "MonoDo Studio")
        val savedProfile = manager.getProfile()
        assertEquals("Vishal", savedProfile.userName)
        assertEquals("MonoDo Studio", savedProfile.workspaceTitle)
        assertEquals(true, savedProfile.isOnboarded)

        // Subsequent read (simulating next app launch)
        val nextLaunchManager = UserProfileManager(app)
        val nextLaunchProfile = nextLaunchManager.getProfile()
        assertEquals(true, nextLaunchProfile.isOnboarded)
        assertEquals("Vishal", nextLaunchProfile.userName)

        // After data erase / resetProfile, onboarding should return
        manager.resetProfile()
        val resetProfile = manager.getProfile()
        assertEquals(false, resetProfile.isOnboarded)
    }

    @Test
    fun `task viewModel resetAllData clears both tasks and notes`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = TaskViewModel(app, taskDao)

        // Insert a task and note
        taskDao.insertTask(
            TaskEntity(
                title = "To be erased",
                category = "Work",
                priority = TaskPriority.HIGH,
                createdAt = System.currentTimeMillis()
            )
        )
        db.noteDao().insertNote(
            NoteEntity(
                title = "Note to be erased",
                content = "Erase test",
                category = "General"
            )
        )

        assertTrue(taskDao.getTaskCount() > 0)
        assertTrue(db.noteDao().getNoteCount() > 0)

        viewModel.resetAllData()

        assertEquals(0, taskDao.getTaskCount())
        assertEquals(0, db.noteDao().getNoteCount())
    }
}


