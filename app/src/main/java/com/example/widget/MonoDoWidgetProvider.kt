package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MonoDoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_COMPLETE_TASK) {
            val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
            if (taskId > 0L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppDatabase.getDatabase(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        db.taskDao().updateTask(
                            task.copy(
                                isCompleted = true,
                                completedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    // Refresh widgets
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val ids = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, MonoDoWidgetProvider::class.java)
                    )
                    for (id in ids) {
                        updateAppWidget(context, appWidgetManager, id)
                    }
                }
            }
        }
    }

    companion object {
        const val ACTION_COMPLETE_TASK = "com.example.monodo.ACTION_COMPLETE_TASK"
        const val EXTRA_TASK_ID = "extra_task_id"

        fun updateAllWidgets(context: Context) {
            CoroutineScope(Dispatchers.IO).launch {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val ids = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, MonoDoWidgetProvider::class.java)
                )
                for (id in ids) {
                    updateAppWidget(context, appWidgetManager, id)
                }
            }
        }

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.monodo_widget_layout)

            // Intent to launch MainActivity when clicking header/widget
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingLaunchIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingLaunchIntent)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val tasks = db.taskDao().getAllTasks().first()
                    val activeTasks = tasks.filter { !it.isCompleted }

                    views.setTextViewText(
                        R.id.widget_status,
                        "${activeTasks.size} ${if (activeTasks.size == 1) "task" else "tasks"}"
                    )

                    // Task 1
                    if (activeTasks.isNotEmpty()) {
                        val t1 = activeTasks[0]
                        views.setViewVisibility(R.id.widget_task_1_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_1_title, t1.title)
                        val completeIntent1 = Intent(context, MonoDoWidgetProvider::class.java).apply {
                            action = ACTION_COMPLETE_TASK
                            putExtra(EXTRA_TASK_ID, t1.id)
                        }
                        val pendingComplete1 = PendingIntent.getBroadcast(
                            context,
                            t1.id.toInt() * 10 + 1,
                            completeIntent1,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_task_1_check, pendingComplete1)
                    } else {
                        views.setViewVisibility(R.id.widget_task_1_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_1_title, "No pending tasks • Enjoy!")
                    }

                    // Task 2
                    if (activeTasks.size > 1) {
                        val t2 = activeTasks[1]
                        views.setViewVisibility(R.id.widget_task_2_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_2_title, t2.title)
                        val completeIntent2 = Intent(context, MonoDoWidgetProvider::class.java).apply {
                            action = ACTION_COMPLETE_TASK
                            putExtra(EXTRA_TASK_ID, t2.id)
                        }
                        val pendingComplete2 = PendingIntent.getBroadcast(
                            context,
                            t2.id.toInt() * 10 + 2,
                            completeIntent2,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_task_2_check, pendingComplete2)
                    } else {
                        views.setViewVisibility(R.id.widget_task_2_container, View.GONE)
                    }

                    // Task 3
                    if (activeTasks.size > 2) {
                        val t3 = activeTasks[2]
                        views.setViewVisibility(R.id.widget_task_3_container, View.VISIBLE)
                        views.setTextViewText(R.id.widget_task_3_title, t3.title)
                        val completeIntent3 = Intent(context, MonoDoWidgetProvider::class.java).apply {
                            action = ACTION_COMPLETE_TASK
                            putExtra(EXTRA_TASK_ID, t3.id)
                        }
                        val pendingComplete3 = PendingIntent.getBroadcast(
                            context,
                            t3.id.toInt() * 10 + 3,
                            completeIntent3,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_task_3_check, pendingComplete3)
                    } else {
                        views.setViewVisibility(R.id.widget_task_3_container, View.GONE)
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    // Fallback
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}
