package com.example.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.db.AppDatabase
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.data.user.AppThemeMode
import com.example.data.user.UserProfileManager
import com.example.ui.util.LocalDayUtils
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MonoDoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return MonoDoTaskRemoteViewsFactory(applicationContext)
    }
}

private class MonoDoTaskRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var activeTasks: List<TaskEntity> = emptyList()
    private var themeMode: AppThemeMode = AppThemeMode.LIGHT_PAPER

    override fun onCreate() {}

    override fun onDataSetChanged() {
        themeMode = UserProfileManager(context).getThemeMode()
        activeTasks = runBlocking {
            try {
                val db = AppDatabase.getDatabase(context)
                db.taskDao().getActiveTasksSync()
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
                            .thenByDescending { it.createdAt }
                    )
                    .take(25)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    override fun onDestroy() {
        activeTasks = emptyList()
    }

    override fun getCount(): Int = activeTasks.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.monodo_widget_task_item)
        val task = activeTasks.getOrNull(position) ?: return views

        val isDark = themeMode != AppThemeMode.LIGHT_PAPER
        val itemBg = when (themeMode) {
            AppThemeMode.LIGHT_PAPER -> R.drawable.widget_item_bg_light
            AppThemeMode.DARK_SLATE -> R.drawable.widget_item_bg_dark
            AppThemeMode.AMOLED_BLACK -> R.drawable.widget_item_bg_amoled
        }
        val titleColor = if (isDark) Color.parseColor("#F4F4F0") else Color.parseColor("#111111")
        val metaColor = if (isDark) Color.parseColor("#9CA3AF") else Color.parseColor("#6B6B6B")
        val overdueColor = if (isDark) Color.parseColor("#F87171") else Color.parseColor("#DC2626")
        val accentColor = if (isDark) Color.parseColor("#60A5FA") else Color.parseColor("#2563EB")

        views.setInt(R.id.widget_item_root, "setBackgroundResource", itemBg)
        views.setTextViewText(R.id.widget_item_title, task.title)
        views.setTextColor(R.id.widget_item_title, titleColor)

        val isOverdue = LocalDayUtils.isOverdue(task.dueDate)
        val datePart = when {
            isOverdue -> "Overdue"
            LocalDayUtils.isToday(task.dueDate) -> "Today"
            LocalDayUtils.isTomorrow(task.dueDate) -> "Tomorrow"
            task.dueDate != null -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(task.dueDate))
            else -> null
        }
        val reminderPart = task.reminderAt?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
        }
        val subtaskPart = if (task.subtasks.isNotEmpty()) {
            "${task.subtasks.count { it.isCompleted }}/${task.subtasks.size} steps"
        } else null

        val metaText = listOfNotNull(
            datePart,
            reminderPart,
            task.category.takeIf { it.isNotBlank() },
            subtaskPart
        ).joinToString(" • ")

        views.setTextViewText(R.id.widget_item_meta, metaText.ifBlank { "General" })
        views.setTextColor(R.id.widget_item_meta, if (isOverdue) overdueColor else metaColor)

        views.setTextViewText(R.id.widget_item_priority, task.priority.indicatorSymbol)
        val priorityColor = when (task.priority) {
            TaskPriority.HIGH -> overdueColor
            TaskPriority.MEDIUM -> accentColor
            TaskPriority.LOW -> metaColor
        }
        views.setTextColor(R.id.widget_item_priority, priorityColor)

        // Fill-in intent for check circle -> completes task right from home screen
        val completeFillIn = Intent().apply {
            putExtra(MonoDoWidgetProvider.EXTRA_ACTION_TYPE, MonoDoWidgetProvider.ACTION_TYPE_COMPLETE)
            putExtra(MonoDoWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_check, completeFillIn)

        // Fill-in intent for row body -> opens MonoDo
        val openFillIn = Intent().apply {
            putExtra(MonoDoWidgetProvider.EXTRA_ACTION_TYPE, MonoDoWidgetProvider.ACTION_TYPE_OPEN)
            putExtra(MonoDoWidgetProvider.EXTRA_TASK_ID, task.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_body, openFillIn)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = activeTasks.getOrNull(position)?.id ?: position.toLong()

    override fun hasStableIds(): Boolean = true
}
