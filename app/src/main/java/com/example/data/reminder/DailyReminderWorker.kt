package com.example.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.firstOrNull

class DailyReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "monodo_daily_reminders"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_NAME = "Daily Task Reminders"
    }

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getDatabase(context)
            val allTasks = database.taskDao().getAllTasks().firstOrNull() ?: emptyList()
            val activeTasks = allTasks.filter { !it.isCompleted }

            showReminderNotification(activeTasksCount = activeTasks.size, topTaskTitle = activeTasks.firstOrNull()?.title)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun showReminderNotification(activeTasksCount: Int, topTaskTitle: String?) {
        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "MonoDo • Daily Task Reminder"
        val message = when {
            activeTasksCount == 0 -> "All clear! Plan your tasks for today with MonoDo."
            activeTasksCount == 1 && !topTaskTitle.isNullOrBlank() -> "1 task waiting: \"$topTaskTitle\""
            !topTaskTitle.isNullOrBlank() -> "$activeTasksCount tasks waiting today, starting with \"$topTaskTitle\""
            else -> "You have $activeTasksCount active tasks to tackle today."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS permission
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily reminders for pending and scheduled tasks in MonoDo"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }
}
