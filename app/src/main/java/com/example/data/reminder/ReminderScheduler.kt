package com.example.data.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    const val UNIQUE_PERIODIC_WORK = "monodo_daily_reminder_work"
    const val UNIQUE_TEST_WORK = "monodo_test_reminder_work"

    /**
     * Calculates the millisecond delay until the next occurrence of [hour]:[minute].
     * If the time for today has already passed, schedules for tomorrow.
     */
    fun calculateInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    /**
     * Schedules a recurring 24-hour reminder via WorkManager at the user-specified [hour] and [minute].
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val initialDelay = calculateInitialDelayMillis(hour, minute)

        val periodicRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest
        )
    }

    /**
     * Cancels the scheduled daily reminder.
     */
    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_PERIODIC_WORK)
    }

    /**
     * Fires an immediate test notification via WorkManager so the user can verify notifications.
     */
    fun triggerInstantTestReminder(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<DailyReminderWorker>()
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_TEST_WORK,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
