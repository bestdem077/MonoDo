package com.example.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.db.AppDatabase

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getDatabase(context)
        val activeTasks = db.taskDao().getActiveTasksSync()
        val birthdays = db.birthdayDao().getAllBirthdaysSync()
        NotificationHelper.showDailyBriefingNotification(context, activeTasks, birthdays)
        return Result.success()
    }
}
