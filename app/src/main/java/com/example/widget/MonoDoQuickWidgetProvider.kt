package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.TaskEntity
import com.example.data.user.AppThemeMode
import com.example.data.user.UserProfileManager
import com.example.ui.util.LocalDayUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonoDoQuickWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (id in appWidgetIds) {
                    updateQuickWidgetSync(context, appWidgetManager, id)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == MonoDoWidgetProvider.ACTION_COMPLETE_TASK) {
            val taskId = intent.getLongExtra(MonoDoWidgetProvider.EXTRA_TASK_ID, -1L)
            if (taskId > 0L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        MonoDoWidgetProvider.completeTaskInDb(context, taskId)
                        MonoDoWidgetProvider.updateAllWidgetsSync(context)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    companion object {
        suspend fun updateAllQuickWidgetsSync(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetManager.getAppWidgetIds(
                ComponentName(context, MonoDoQuickWidgetProvider::class.java)
            )
            for (id in ids) {
                updateQuickWidgetSync(context, appWidgetManager, id)
            }
        }

        private suspend fun updateQuickWidgetSync(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.monodo_quick_widget_layout)

            val themeMode = UserProfileManager(context).getThemeMode()
            val isDark = themeMode != AppThemeMode.LIGHT_PAPER
            val bgDrawable = when (themeMode) {
                AppThemeMode.LIGHT_PAPER -> R.drawable.widget_background
                AppThemeMode.DARK_SLATE -> R.drawable.widget_background_dark
                AppThemeMode.AMOLED_BLACK -> R.drawable.widget_background_amoled
            }
            val itemBg = when (themeMode) {
                AppThemeMode.LIGHT_PAPER -> R.drawable.widget_item_bg_light
                AppThemeMode.DARK_SLATE -> R.drawable.widget_item_bg_dark
                AppThemeMode.AMOLED_BLACK -> R.drawable.widget_item_bg_amoled
            }
            val primaryColor = if (isDark) Color.parseColor("#F4F4F0") else Color.parseColor("#111111")
            val secondaryColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B6B6B")
            val accentColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")

            views.setInt(R.id.quick_widget_root, "setBackgroundResource", bgDrawable)
            views.setInt(R.id.quick_widget_task_card, "setBackgroundResource", itemBg)
            views.setInt(R.id.quick_widget_open_btn, "setBackgroundResource", itemBg)
            views.setTextColor(R.id.quick_widget_title, primaryColor)
            views.setTextColor(R.id.quick_widget_stats, accentColor)
            views.setTextColor(R.id.quick_widget_task_title, primaryColor)
            views.setTextColor(R.id.quick_widget_task_meta, secondaryColor)
            views.setTextColor(R.id.quick_widget_open_btn, primaryColor)

            // Open App button
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                600 + appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.quick_widget_open_btn, openPending)
            views.setOnClickPendingIntent(R.id.quick_widget_root, openPending)

            // + New Task button
            val addIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_CREATE_TASK, true)
            }
            val addPending = PendingIntent.getActivity(
                context,
                700 + appWidgetId,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.quick_widget_add_btn, addPending)

            try {
                val db = AppDatabase.getDatabase(context)
                val allTasks = db.taskDao().getAllTasks().first()
                val sessions = db.focusSessionDao().getAllSessions().first()
                val todayDay = LocalDayUtils.toLocalEpochDay()
                val todayFocusMins = sessions.filter { it.localEpochDay == todayDay }.sumOf { it.durationMinutes }

                val completedCount = allTasks.count { it.isCompleted }
                val totalCount = allTasks.size
                views.setTextViewText(
                    R.id.quick_widget_stats,
                    "$completedCount/$totalCount done • ${todayFocusMins}m"
                )

                val nextTask = allTasks
                    .filter { !it.isCompleted }
                    .sortedWith(
                        compareBy<TaskEntity> {
                            when {
                                LocalDayUtils.isOverdue(it.dueDate) -> 0
                                LocalDayUtils.isToday(it.dueDate) -> 1
                                else -> 2
                            }
                        }
                            .thenByDescending { it.priority.ordinal }
                            .thenBy { it.dueDate ?: Long.MAX_VALUE }
                    )
                    .firstOrNull()

                if (nextTask == null) {
                    views.setViewVisibility(R.id.quick_widget_task_check, View.GONE)
                    views.setTextViewText(R.id.quick_widget_task_title, "All clear • Ready for new goals")
                    views.setTextViewText(R.id.quick_widget_task_meta, "Tap + New Task to start")
                } else {
                    views.setViewVisibility(R.id.quick_widget_task_check, View.VISIBLE)
                    views.setTextViewText(R.id.quick_widget_task_title, nextTask.title)
                    val timeHint = nextTask.reminderAt?.let {
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
                    }
                    val dueLabel = when {
                        LocalDayUtils.isOverdue(nextTask.dueDate) -> "Overdue"
                        LocalDayUtils.isToday(nextTask.dueDate) -> "Today"
                        LocalDayUtils.isTomorrow(nextTask.dueDate) -> "Tomorrow"
                        else -> "Up Next"
                    }
                    val meta = listOfNotNull(dueLabel, timeHint, nextTask.category).joinToString(" • ")
                    views.setTextViewText(R.id.quick_widget_task_meta, meta)

                    val completeIntent = Intent(context, MonoDoQuickWidgetProvider::class.java).apply {
                        action = MonoDoWidgetProvider.ACTION_COMPLETE_TASK
                        putExtra(MonoDoWidgetProvider.EXTRA_TASK_ID, nextTask.id)
                    }
                    val completePending = PendingIntent.getBroadcast(
                        context,
                        800 + appWidgetId,
                        completeIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.quick_widget_task_check, completePending)
                }
            } catch (_: Exception) {
                // Keep defaults
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
