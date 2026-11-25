package com.example.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Self-healing daily watchdog worker.
 * Even if an aggressive OEM battery manager clears AlarmManager alarms while the user
 * hasn't opened MonoDo for months, this lightweight WorkManager job quietly re-arms
 * all active birthday, task, and daily briefing alarms with AlarmManager.
 */
class BirthdayWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            ReminderScheduler.rescheduleAllActiveReminders(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WATCHDOG_WORK_NAME = "monodo_alarm_reliability_watchdog"

        fun ensureScheduled(context: Context) {
            val request = PeriodicWorkRequestBuilder<BirthdayWatchdogWorker>(
                24,
                TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WATCHDOG_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
