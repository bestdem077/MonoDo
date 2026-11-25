package com.example

import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayWishTemplates
import com.example.data.model.DefaultRoutineTemplates
import com.example.data.model.TaskPriority
import com.example.data.model.TaskRecurrence
import com.example.data.model.WishCategory
import com.example.data.reminder.ReminderScheduler
import com.example.data.update.AppUpdateManager
import com.example.data.update.VersionComparator
import com.example.data.user.AppThemeMode
import com.example.ui.components.QuickDateCalculator
import com.example.ui.components.QuickDeadlinePreset
import com.example.ui.util.LocalDayUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun `recurrence advances due dates accurately`() {
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val baseCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 15, 23, 59, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = nowCal.timeInMillis
        val baseMillis = baseCal.timeInMillis

        val nextDaily = TaskRecurrence.DAILY.nextDueDate(baseMillis, nowMillis)
        val nextWeekly = TaskRecurrence.WEEKLY.nextDueDate(baseMillis, nowMillis)
        val nextMonthly = TaskRecurrence.MONTHLY.nextDueDate(baseMillis, nowMillis)

        assertEquals(
            LocalDayUtils.toLocalEpochDay(baseMillis) + 1L,
            LocalDayUtils.toLocalEpochDay(nextDaily)
        )
        assertEquals(
            LocalDayUtils.toLocalEpochDay(baseMillis) + 7L,
            LocalDayUtils.toLocalEpochDay(nextWeekly)
        )
        assertTrue(nextMonthly > nextWeekly)

        // Overdue task should advance from today rather than spawning another overdue task
        val overdueCal = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -5) }
        val nextFromOverdue = TaskRecurrence.DAILY.nextDueDate(overdueCal.timeInMillis, nowMillis)
        assertEquals(
            LocalDayUtils.toLocalEpochDay(nowMillis) + 1L,
            LocalDayUtils.toLocalEpochDay(nextFromOverdue)
        )
    }

    @Test
    fun `local day utils classifies overdue today and upcoming accurately`() {
        val nowCal = Calendar.getInstance()
        val nowMillis = nowCal.timeInMillis

        val yesterday = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
        val tomorrow = (nowCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis

        assertTrue(LocalDayUtils.isOverdue(yesterday, nowMillis))
        assertFalse(LocalDayUtils.isOverdue(nowMillis, nowMillis))
        assertFalse(LocalDayUtils.isOverdue(tomorrow, nowMillis))
        assertFalse(LocalDayUtils.isOverdue(null, nowMillis))

        assertTrue(LocalDayUtils.isToday(nowMillis, nowMillis))
        assertFalse(LocalDayUtils.isToday(yesterday, nowMillis))
        assertTrue(LocalDayUtils.isTomorrow(tomorrow, nowMillis))
    }

    @Test
    fun `quick date calculator presets order correctly`() {
        val baseTime = 1760698800000L
        val today = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TODAY, baseTime)
        val tomorrow = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.TOMORROW, baseTime)
        val weekend = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.THIS_WEEKEND, baseTime)
        val nextWeek = QuickDateCalculator.getDeadlineTimestamp(QuickDeadlinePreset.NEXT_WEEK, baseTime)

        assertTrue(QuickDateCalculator.isSameCalendarDay(today, baseTime))
        assertTrue(tomorrow > today)
        assertTrue(weekend > today)
        assertTrue(nextWeek > today)
    }

    @Test
    fun `daily briefing trigger schedules same day when future and rolls over when past`() {
        val morningNow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 8, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val triggerToday = ReminderScheduler.calculateNextDailyTriggerMillis(9, 0, morningNow)
        assertEquals(
            LocalDayUtils.toLocalEpochDay(morningNow),
            LocalDayUtils.toLocalEpochDay(triggerToday)
        )
        assertTrue(triggerToday > morningNow)

        val afternoonNow = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 20, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val triggerTomorrow = ReminderScheduler.calculateNextDailyTriggerMillis(9, 0, afternoonNow)
        assertEquals(
            LocalDayUtils.toLocalEpochDay(afternoonNow) + 1L,
            LocalDayUtils.toLocalEpochDay(triggerTomorrow)
        )
        assertTrue(triggerTomorrow > afternoonNow)
    }

    @Test
    fun `birthday calculator computes 3h and 4h pre-midnight reminders and next-year rollover accurately`() {
        // Suppose today is Nov 25, 2026 at 2:00 PM, and friend's birthday is Nov 26 (tonight at 12:00 AM)
        val afternoonNov25 = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        assertEquals(1, BirthdayDateCalculator.daysUntilBirthday(11, 26, afternoonNov25))
        assertEquals("TONIGHT 12 AM", BirthdayDateCalculator.formatCountdownLabel(1))
        assertEquals(26, BirthdayDateCalculator.turningAge(11, 26, 2000, afternoonNov25))

        // 3 hours before midnight -> Nov 25 at 21:00 (9:00 PM)
        val pre3h = BirthdayDateCalculator.nextPreReminderTriggerMillis(11, 26, 3, afternoonNov25)
        val pre3hCal = Calendar.getInstance().apply { timeInMillis = pre3h }
        assertEquals(2026, pre3hCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, pre3hCal.get(Calendar.MONTH))
        assertEquals(25, pre3hCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(21, pre3hCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, pre3hCal.get(Calendar.MINUTE))

        // 4 hours before midnight -> Nov 25 at 20:00 (8:00 PM)
        val pre4h = BirthdayDateCalculator.nextPreReminderTriggerMillis(11, 26, 4, afternoonNov25)
        val pre4hCal = Calendar.getInstance().apply { timeInMillis = pre4h }
        assertEquals(20, pre4hCal.get(Calendar.HOUR_OF_DAY))

        // Right after 9:00 PM fires (9:01 PM Nov 25):
        // Pre-reminder rolls over to NEXT YEAR (2027), while midnight alarm stays TONIGHT (Nov 26, 2026 00:00)!
        val after9pm = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 25, 21, 1, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val nextYearPre = BirthdayDateCalculator.nextPreReminderTriggerMillis(11, 26, 3, after9pm)
        val tonightMidnight = BirthdayDateCalculator.nextMidnightTriggerMillis(11, 26, after9pm)

        assertEquals(2027, Calendar.getInstance().apply { timeInMillis = nextYearPre }.get(Calendar.YEAR))
        val midnightCal = Calendar.getInstance().apply { timeInMillis = tonightMidnight }
        assertEquals(2026, midnightCal.get(Calendar.YEAR))
        assertEquals(26, midnightCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, midnightCal.get(Calendar.HOUR_OF_DAY))

        // Feb 29 leap-year birthday safely clamps to Feb 28 in non-leap year 2027
        val feb1_2027 = Calendar.getInstance().apply {
            set(2027, Calendar.FEBRUARY, 1, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val leapMidnight = BirthdayDateCalculator.nextMidnightTriggerMillis(2, 29, feb1_2027)
        val leapCal = Calendar.getInstance().apply { timeInMillis = leapMidnight }
        assertEquals(2027, leapCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, leapCal.get(Calendar.MONTH))
        assertEquals(28, leapCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `theme modes and priorities are properly defined`() {
        assertEquals(3, AppThemeMode.entries.size)
        assertEquals("Light Paper", AppThemeMode.LIGHT_PAPER.title)
        assertEquals("Dark Slate", AppThemeMode.DARK_SLATE.title)
        assertEquals("Pure Black", AppThemeMode.AMOLED_BLACK.title)
        assertEquals(3, TaskPriority.HIGH.level)
    }

    @Test
    fun `birthday wish templates personalize name relationship and turning age across categories`() {
        val friendWish = BirthdayWishTemplates.defaultWishFor("Aarav", "Friend", 24)
        assertTrue(friendWish.contains("Aarav"))
        assertTrue(friendWish.contains("24"))

        val familyWish = BirthdayWishTemplates.defaultWishFor("Maa", "Family", null)
        assertTrue(familyWish.contains("Maa"))
        assertTrue(familyWish.contains("grateful"))

        val colleagueWish = BirthdayWishTemplates.defaultWishFor("Rohan", "Colleague", null)
        assertTrue(colleagueWish.contains("Rohan"))
        assertTrue(colleagueWish.contains("success"))

        val allOptions = BirthdayWishTemplates.wishesFor("Priya", 27)
        assertTrue(allOptions.any { it.category == WishCategory.FRIEND && it.message.contains("Priya") })
        assertTrue(allOptions.any { it.category == WishCategory.FAMILY && it.message.contains("27") })
        assertTrue(allOptions.any { it.category == WishCategory.FORMAL && it.message.contains("Priya") })
        assertTrue(allOptions.all { it.message.isNotBlank() && it.title.isNotBlank() })
    }

    @Test
    fun `default routine templates provide clean reusable checklists`() {
        val starters = DefaultRoutineTemplates.starters
        assertEquals(3, starters.size)
        assertTrue(starters.all { it.title.isNotBlank() && it.subtaskTitles.size >= 3 })
        assertTrue(starters.all { it.id < 0L })
    }

    @Test
    fun `version comparator detects newer GitHub release tags accurately`() {
        assertTrue(VersionComparator.isRemoteNewer("v2.0.1.8", "v2.0.1.7"))
        assertTrue(VersionComparator.isRemoteNewer("v2.0.1.7", "v2.0.1"))
        assertTrue(VersionComparator.isRemoteNewer("v2.1.0", "v2.0.1.99"))
        assertTrue(VersionComparator.isRemoteNewer("v3.0.0", "2.9.9.9"))
        assertFalse(VersionComparator.isRemoteNewer("v2.0.1.7", "v2.0.1.7"))
        assertFalse(VersionComparator.isRemoteNewer("v2.0.1.6", "v2.0.1.7"))
        assertFalse(VersionComparator.isRemoteNewer("v2.0.1", "v2.0.1.7"))
        assertFalse(VersionComparator.isRemoteNewer("v2.0.1.7", "v2.1.0"))
        assertTrue(VersionComparator.isRemoteNewer("v2.1.0.8", "v2.1.0"))
        assertEquals("v2.0.1.8", VersionComparator.displayTag("2.0.1.8"))
        assertEquals("v2.0.1.8", VersionComparator.displayTag("V2.0.1.8"))
    }

    @Test
    fun `app update manager sanitizes filenames formats sizes and cleans release notes`() {
        assertEquals("MonoDo-v2.0.1.8.apk", AppUpdateManager.sanitizeApkFileName("../evil/MonoDo-v2.0.1.8.apk"))
        assertEquals("14.5 MB", AppUpdateManager.formatFileSize((14.5 * 1024 * 1024).toLong()))
        assertEquals("512 KB", AppUpdateManager.formatFileSize(512L * 1024L))

        val rawNotes = """
            ## MonoDo — Minimalist Workspace
            ### What's New in v2.0.1.8
            * Added in-app GitHub updater
            * Improved birthday notifications
            | Build | Details |
            SHA-256: abcdef123456
        """.trimIndent()
        val cleaned = AppUpdateManager.cleanReleaseNotes(rawNotes)
        assertTrue(cleaned.contains("Added in-app GitHub updater"))
        assertTrue(cleaned.contains("Improved birthday notifications"))
        assertFalse(cleaned.contains("SHA-256"))
    }
}
