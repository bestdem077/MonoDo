package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.TaskRecurrence
import com.example.data.reminder.ReminderScheduler
import com.example.data.user.AppThemeMode
import com.example.data.user.UserProfileManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonoDoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (appWidgetId in appWidgetIds) {
                    updateAppWidgetSync(context, appWidgetManager, appWidgetId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_LIST_ITEM_CLICK -> {
                val actionType = intent.getStringExtra(EXTRA_ACTION_TYPE) ?: ACTION_TYPE_OPEN
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (actionType == ACTION_TYPE_COMPLETE && taskId > 0L) {
                    completeTaskFromWidget(context, taskId)
                } else {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    context.startActivity(launchIntent)
                }
            }

            ACTION_COMPLETE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                if (taskId > 0L) {
                    completeTaskFromWidget(context, taskId)
                }
            }

            ACTION_REFRESH -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        updateAllWidgetsSync(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun completeTaskFromWidget(context: Context, taskId: Long) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                completeTaskInDb(context, taskId)
                updateAllWidgetsSync(context)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.monodo.ACTION_COMPLETE_TASK"
        const val ACTION_LIST_ITEM_CLICK = "com.example.monodo.ACTION_LIST_ITEM_CLICK"
        const val ACTION_REFRESH = "com.example.monodo.ACTION_REFRESH"
        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_ACTION_TYPE = "extra_action_type"
        const val ACTION_TYPE_COMPLETE = "complete"
        const val ACTION_TYPE_OPEN = "open"

        suspend fun completeTaskInDb(context: Context, taskId: Long) {
            val db = AppDatabase.getDatabase(context)
            val task = db.taskDao().getTaskById(taskId)
            if (task != null && !task.isCompleted) {
                db.taskDao().updateTask(
                    task.copy(
                        isCompleted = true,
                        completedAt = System.currentTimeMillis(),
                        subtasks = task.subtasks.map { it.copy(isCompleted = true) }
                    )
                )
                ReminderScheduler.cancelTaskReminder(context, task.id)

                if (task.recurrence != TaskRecurrence.NONE) {
                    val nextDue = task.recurrence.nextDueDate(task.dueDate)
                    val nextReminder = task.reminderAt?.let { oldRem ->
                        val delta = nextDue - (task.dueDate ?: nextDue)
                        if (delta > 0) oldRem + delta else task.recurrence.nextDueDate(oldRem)
                    }
                    val newId = db.taskDao().insertTask(
                        task.copy(
                            id = 0,
                            dueDate = nextDue,
                            reminderAt = nextReminder,
                            isCompleted = false,
                            completedAt = null,
                            createdAt = System.currentTimeMillis(),
                            subtasks = task.subtasks.map { it.copy(isCompleted = false) }
                        )
                    )
                    if (nextReminder != null) {
                        ReminderScheduler.scheduleTaskReminderAt(context, newId, nextReminder)
                    }
                }
            }
        }

        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                updateAllWidgetsSync(context)
            }
        }

        suspend fun updateAllWidgetsSync(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val agendaIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MonoDoWidgetProvider::class.java)
            )
            for (id in agendaIds) {
                updateAppWidgetSync(context, appWidgetManager, id)
            }
            MonoDoQuickWidgetProvider.updateAllQuickWidgetsSync(context)
        }

        @Suppress("DEPRECATION")
        private suspend fun updateAppWidgetSync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.monodo_widget_layout)

            // Apply theme-synced widget styling
            val themeMode = UserProfileManager(context).getThemeMode()
            val isDark = themeMode != AppThemeMode.LIGHT_PAPER
            val bgDrawable = when (themeMode) {
                AppThemeMode.LIGHT_PAPER -> R.drawable.widget_background
                AppThemeMode.DARK_SLATE -> R.drawable.widget_background_dark
                AppThemeMode.AMOLED_BLACK -> R.drawable.widget_background_amoled
            }
            val primaryColor = if (isDark) Color.parseColor("#F4F4F0") else Color.parseColor("#111111")
            val secondaryColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B6B6B")

            views.setInt(R.id.widget_root, "setBackgroundResource", bgDrawable)
            views.setTextColor(R.id.widget_title, primaryColor)
            views.setTextColor(R.id.widget_subtitle, secondaryColor)
            views.setTextColor(R.id.widget_empty_title, primaryColor)
            views.setTextColor(R.id.widget_empty_sub, secondaryColor)

            // Header opens app
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                100 + appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header, openPending)

            // + Task pill opens CreateTaskSheet directly!
            val addTaskIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_CREATE_TASK, true)
            }
            val addTaskPending = PendingIntent.getActivity(
                context,
                200 + appWidgetId,
                addTaskIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_add_btn, addTaskPending)

            // Refresh button syncs widget immediately
            val refreshIntent = Intent(context, MonoDoWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                300 + appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

            // Bind scrollable RemoteViewsService to ListView
            val serviceIntent = Intent(context, MonoDoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_task_list, serviceIntent)
            views.setEmptyView(R.id.widget_task_list, R.id.widget_empty_view)

            // Template PendingIntent for list item clicks (MUTABLE required on Android 12+ for fill-in extras!)
            val itemTemplateIntent = Intent(context, MonoDoWidgetProvider::class.java).apply {
                action = ACTION_LIST_ITEM_CLICK
            }
            val templateFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val itemTemplatePending = PendingIntent.getBroadcast(
                context,
                400 + appWidgetId,
                itemTemplateIntent,
                templateFlags
            )
            views.setPendingIntentTemplate(R.id.widget_task_list, itemTemplatePending)

            try {
                val db = AppDatabase.getDatabase(context)
                val allTasks = db.taskDao().getAllTasks().first()
                val activeCount = allTasks.count { !it.isCompleted }
                val completedCount = allTasks.count { it.isCompleted }
                val totalCount = allTasks.size
                val progressPct = if (totalCount > 0) ((completedCount * 100f) / totalCount).toInt() else 0

                val todayStr = SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date()).uppercase()
                views.setTextViewText(
                    R.id.widget_subtitle,
                    "$todayStr • $activeCount pending • $completedCount done"
                )
                views.setProgressBar(R.id.widget_progress, 100, progressPct, false)
            } catch (_: Exception) {
                views.setTextViewText(R.id.widget_subtitle, "TODAY")
                views.setProgressBar(R.id.widget_progress, 100, 0, false)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_task_list)
        }
    }
}
