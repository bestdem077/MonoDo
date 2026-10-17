package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.model.SubTask
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.user.UserProfileManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app_name from strings resources`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("MonoDo", appName)
    }

    @Test
    fun `task priority properties test`() {
        assertEquals(1, TaskPriority.LOW.level)
        assertEquals(2, TaskPriority.MEDIUM.level)
        assertEquals(3, TaskPriority.HIGH.level)

        assertEquals("•", TaskPriority.LOW.indicatorSymbol)
        assertEquals("••", TaskPriority.MEDIUM.indicatorSymbol)
        assertEquals("•••", TaskPriority.HIGH.indicatorSymbol)
    }

    @Test
    fun `user profile manager persistence test`() {
        val manager = UserProfileManager(context)
        manager.resetProfile()

        val initialProfile = manager.getProfile()
        assertFalse(initialProfile.isOnboarded)

        // Save profile with personal details
        val success = manager.saveProfile(
            userName = "Vishal",
            workspaceTitle = "MonoDo Workspace",
            birthDate = "15 August 2000",
            birthTimestamp = 966297600000L,
            reminderHour = 9,
            reminderMinute = 30
        )
        assertTrue(success)

        val savedProfile = manager.getProfile()
        assertEquals("Vishal", savedProfile.userName)
        assertEquals("MonoDo Workspace", savedProfile.workspaceTitle)
        assertEquals("15 August 2000", savedProfile.birthDate)
        assertEquals(966297600000L, savedProfile.birthTimestamp)
        assertEquals(9, savedProfile.reminderHour)
        assertEquals(30, savedProfile.reminderMinute)
        assertTrue(savedProfile.isOnboarded)
    }

    @Test
    fun `room database task and subtasks CRUD test`() = runBlocking {
        val taskDao = db.taskDao()
        val subtask = SubTask(id = "sub-1", title = "Write specs", isCompleted = false)
        val task = TaskEntity(
            title = "Build MonoDo App",
            description = "High focus task",
            category = "Work",
            priority = TaskPriority.HIGH,
            subtasks = listOf(subtask)
        )

        val id = taskDao.insertTask(task)
        assertTrue(id > 0)

        val retrieved = taskDao.getTaskById(id)
        assertNotNull(retrieved)
        assertEquals("Build MonoDo App", retrieved?.title)
        assertEquals(TaskPriority.HIGH, retrieved?.priority)
        assertEquals(1, retrieved?.subtasks?.size)

        // Update task completion
        val updated = retrieved!!.copy(isCompleted = true)
        taskDao.updateTask(updated)

        val completedTask = taskDao.getTaskById(id)
        assertTrue(completedTask!!.isCompleted)
    }

    @Test
    fun `room database note CRUD test`() = runBlocking {
        val noteDao = db.noteDao()
        val note = NoteEntity(
            title = "Minimalist Thought",
            content = "Keep only essential features.",
            category = "General",
            isPinned = true
        )

        val id = noteDao.insertNote(note)
        assertTrue(id > 0)

        val notesList = noteDao.getAllNotes().first()
        assertEquals(1, notesList.size)
        assertEquals("Minimalist Thought", notesList[0].title)
        assertTrue(notesList[0].isPinned)
    }

    @Test
    fun `room database habit and logs CRUD test`() = runBlocking {
        val habitDao = db.habitDao()
        val habit = com.example.data.model.HabitEntity(
            title = "Deep Work 2h",
            colorHex = "#2563EB"
        )
        val habitId = habitDao.insertHabit(habit)
        assertTrue(habitId > 0)

        val habits = habitDao.getAllHabits().first()
        assertEquals(1, habits.size)
        assertEquals("Deep Work 2h", habits[0].title)

        // Log completion for today
        val todayEpochDay = 20000L
        habitDao.insertOrUpdateLog(
            com.example.data.model.HabitLogEntity(
                habitId = habitId,
                dateEpochDay = todayEpochDay,
                isCompleted = true
            )
        )

        val log = habitDao.getLog(habitId, todayEpochDay)
        assertNotNull(log)
        assertTrue(log!!.isCompleted)

        // Toggle delete log
        habitDao.deleteLog(habitId, todayEpochDay)
        val logAfterDelete = habitDao.getLog(habitId, todayEpochDay)
        org.junit.Assert.assertNull(logAfterDelete)
    }

    @Test
    fun `room database custom category test`() = runBlocking {
        val catDao = db.customCategoryDao()
        val cat = com.example.data.model.CustomCategoryEntity(
            name = "Reading",
            colorHex = "#16A34A"
        )
        val id = catDao.insertCategory(cat)
        assertTrue(id > 0)

        val categories = catDao.getAllCategories().first()
        assertEquals(1, categories.size)
        assertEquals("Reading", categories[0].name)
        assertEquals("#16A34A", categories[0].colorHex)
    }

    @Test
    fun `task sort order enum verification`() {
        assertEquals("Default", com.example.data.model.TaskSortOrder.DEFAULT.displayName)
        assertEquals("Priority: High → Low", com.example.data.model.TaskSortOrder.PRIORITY.displayName)
        assertEquals("Due Date: Soonest", com.example.data.model.TaskSortOrder.DUE_DATE.displayName)
        assertEquals("Newest First", com.example.data.model.TaskSortOrder.CREATION_DATE.displayName)
        assertEquals("Alphabetical: A → Z", com.example.data.model.TaskSortOrder.ALPHABETICAL.displayName)
    }

    @Test
    fun `quick date calculator presets verification`() {
        val baseTime = 1760698800000L
        val todayTs = com.example.ui.components.QuickDateCalculator.getDeadlineTimestamp(
            com.example.ui.components.QuickDeadlinePreset.TODAY,
            baseTime
        )
        val tomorrowTs = com.example.ui.components.QuickDateCalculator.getDeadlineTimestamp(
            com.example.ui.components.QuickDeadlinePreset.TOMORROW,
            baseTime
        )
        val weekendTs = com.example.ui.components.QuickDateCalculator.getDeadlineTimestamp(
            com.example.ui.components.QuickDeadlinePreset.THIS_WEEKEND,
            baseTime
        )
        val nextWeekTs = com.example.ui.components.QuickDateCalculator.getDeadlineTimestamp(
            com.example.ui.components.QuickDeadlinePreset.NEXT_WEEK,
            baseTime
        )

        assertTrue(com.example.ui.components.QuickDateCalculator.isSameCalendarDay(todayTs, baseTime))
        assertTrue(tomorrowTs > todayTs)
        assertTrue(weekendTs > todayTs)
        assertTrue(nextWeekTs > todayTs)
    }
}
