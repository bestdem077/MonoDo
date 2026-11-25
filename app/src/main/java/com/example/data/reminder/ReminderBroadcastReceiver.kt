package com.example.data.reminder

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.db.AppDatabase
import com.example.data.model.FocusSessionEntity
import com.example.data.model.TaskRecurrence
import com.example.data.user.UserProfileManager
import com.example.ui.util.LocalDayUtils
import com.example.widget.MonoDoWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TASK_REMINDER = "com.example.monodo.ACTION_TASK_REMINDER"
        const val ACTION_DAILY_BRIEFING = "com.example.monodo.ACTION_DAILY_BRIEFING"
        const val ACTION_MARK_TASK_DONE = "com.example.monodo.ACTION_MARK_TASK_DONE"
        const val ACTION_SNOOZE_TASK = "com.example.monodo.ACTION_SNOOZE_TASK"
        const val ACTION_BIRTHDAY_PRE_REMINDER = "com.example.monodo.ACTION_BIRTHDAY_PRE_REMINDER"
        const val ACTION_BIRTHDAY_MIDNIGHT = "com.example.monodo.ACTION_BIRTHDAY_MIDNIGHT"
        const val ACTION_COPY_BIRTHDAY_WISH = "com.example.monodo.ACTION_COPY_BIRTHDAY_WISH"
        const val ACTION_POMODORO_FINISHED = "com.example.monodo.ACTION_POMODORO_FINISHED"
        const val ACTION_POMODORO_PAUSE = "com.example.monodo.ACTION_POMODORO_PAUSE"
        const val ACTION_POMODORO_STOP = "com.example.monodo.ACTION_POMODORO_STOP"

        const val EXTRA_TASK_ID = "extra_task_id"
        const val EXTRA_BIRTHDAY_ID = "extra_birthday_id"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
        const val EXTRA_WISH_TEXT = "extra_wish_text"
        const val EXTRA_BIRTHDAY_NAME = "extra_birthday_name"
        const val EXTRA_POMODORO_IS_BREAK = "extra_pomodoro_is_break"
        const val EXTRA_POMODORO_DURATION_MIN = "extra_pomodoro_duration_min"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_TASK_REMINDER -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        if (taskId > 0L) {
                            val task = AppDatabase.getDatabase(context).taskDao().getTaskById(taskId)
                            if (task != null && !task.isCompleted) {
                                NotificationHelper.showTaskReminderNotification(context, task)
                            }
                        }
                    }

                    ACTION_BIRTHDAY_PRE_REMINDER -> {
                        val birthdayId = intent.getLongExtra(EXTRA_BIRTHDAY_ID, -1L)
                        if (birthdayId > 0L) {
                            val birthday = AppDatabase.getDatabase(context).birthdayDao().getBirthdayById(birthdayId)
                            if (birthday != null) {
                                NotificationHelper.showBirthdayNotification(
                                    context = context,
                                    birthday = birthday,
                                    isMidnightAlert = false
                                )
                                // Immediately re-arm for next year (+60s offset avoids firing twice on same minute)
                                ReminderScheduler.scheduleBirthdayReminders(
                                    context = context,
                                    birthday = birthday,
                                    nowMillis = System.currentTimeMillis() + 60_000L
                                )
                            }
                        }
                    }

                    ACTION_BIRTHDAY_MIDNIGHT -> {
                        val birthdayId = intent.getLongExtra(EXTRA_BIRTHDAY_ID, -1L)
                        if (birthdayId > 0L) {
                            val birthday = AppDatabase.getDatabase(context).birthdayDao().getBirthdayById(birthdayId)
                            if (birthday != null) {
                                if (birthday.notifyAtMidnight) {
                                    NotificationHelper.showBirthdayNotification(
                                        context = context,
                                        birthday = birthday,
                                        isMidnightAlert = true
                                    )
                                }
                                // Immediately re-arm for next year so it works even if app is never opened
                                ReminderScheduler.scheduleBirthdayReminders(
                                    context = context,
                                    birthday = birthday,
                                    nowMillis = System.currentTimeMillis() + 60_000L
                                )
                            }
                        }
                    }

                    ACTION_COPY_BIRTHDAY_WISH -> {
                        val wish = intent.getStringExtra(EXTRA_WISH_TEXT)
                        val name = intent.getStringExtra(EXTRA_BIRTHDAY_NAME) ?: "Birthday"
                        if (!wish.isNullOrBlank()) {
                            withContext(Dispatchers.Main) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Birthday Wish", wish))
                                Toast.makeText(
                                    context.applicationContext,
                                    "Copied wish for $name",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

                    ACTION_POMODORO_FINISHED -> {
                        val profileMgr = UserProfileManager(context)
                        val persisted = profileMgr.getPersistedPomodoroState()
                        val isBreak = intent.getBooleanExtra(EXTRA_POMODORO_IS_BREAK, persisted.isBreak)
                        val durationMins = intent.getIntExtra(
                            EXTRA_POMODORO_DURATION_MIN,
                            if (isBreak) persisted.breakMinutes else persisted.focusMinutes
                        )

                        NotificationHelper.cancelPomodoroOngoingNotification(context)

                        // Log focus session to Room if not already logged by foreground timer
                        if (!isBreak && persisted.isRunning && !persisted.sessionCompletedLogged) {
                            val db = AppDatabase.getDatabase(context)
                            db.focusSessionDao().insertSession(
                                FocusSessionEntity(
                                    durationMinutes = durationMins,
                                    taskTitle = "Focus Session",
                                    localEpochDay = LocalDayUtils.toLocalEpochDay()
                                )
                            )
                        }

                        // Switch persisted timer cleanly to next phase (paused, ready to start)
                        val nextIsBreak = !isBreak
                        val nextSeconds = (if (nextIsBreak) persisted.breakMinutes else persisted.focusMinutes) * 60
                        profileMgr.savePomodoroState(
                            isRunning = false,
                            isBreak = nextIsBreak,
                            focusMinutes = persisted.focusMinutes,
                            breakMinutes = persisted.breakMinutes,
                            endTimeMillis = 0L,
                            remainingSeconds = nextSeconds,
                            sessionCompletedLogged = true
                        )

                        NotificationHelper.showPomodoroFinishedNotification(
                            context = context,
                            isBreak = isBreak,
                            durationMinutes = durationMins
                        )
                    }

                    ACTION_POMODORO_PAUSE -> {
                        val profileMgr = UserProfileManager(context)
                        val persisted = profileMgr.getPersistedPomodoroState()
                        if (persisted.isRunning) {
                            val remainingSec = ((persisted.endTimeMillis - System.currentTimeMillis()) / 1000L)
                                .toInt()
                                .coerceAtLeast(1)
                            profileMgr.clearPomodoroRunning(resetRemainingSeconds = remainingSec)
                        }
                        ReminderScheduler.cancelPomodoroFinishAlarm(context)
                        NotificationHelper.cancelPomodoroOngoingNotification(context)
                    }

                    ACTION_POMODORO_STOP -> {
                        val profileMgr = UserProfileManager(context)
                        val persisted = profileMgr.getPersistedPomodoroState()
                        val resetSec = (if (persisted.isBreak) persisted.breakMinutes else persisted.focusMinutes) * 60
                        profileMgr.clearPomodoroRunning(resetRemainingSeconds = resetSec)
                        ReminderScheduler.cancelPomodoroFinishAlarm(context)
                        NotificationHelper.cancelPomodoroOngoingNotification(context)
                    }

                    ACTION_DAILY_BRIEFING -> {
                        val prefs = ReminderPreferences(context).getSettings()
                        if (prefs.isEnabled) {
                            val db = AppDatabase.getDatabase(context)
                            val activeTasks = db.taskDao().getActiveTasksSync()
                            val birthdays = db.birthdayDao().getAllBirthdaysSync()
                            NotificationHelper.showDailyBriefingNotification(context, activeTasks, birthdays)
                            // Re-schedule next day's alarm & self-heal all birthday alarms
                            ReminderScheduler.scheduleDailyReminder(context, prefs.hour, prefs.minute)
                            val now = System.currentTimeMillis()
                            birthdays.forEach { ReminderScheduler.scheduleBirthdayReminders(context, it, now) }
                        }
                    }

                    ACTION_MARK_TASK_DONE -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        if (taskId > 0L) {
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
                                // Spawn next occurrence if recurring (and preserve reminder time of day!)
                                if (task.recurrence != TaskRecurrence.NONE) {
                                    val nextDue = task.recurrence.nextDueDate(task.dueDate)
                                    val nextReminder = task.reminderAt?.let { oldRem ->
                                        val delta = nextDue - (task.dueDate ?: nextDue)
                                        if (delta > 0) oldRem + delta else task.recurrence.nextDueDate(oldRem)
                                    }
                                    val nextTask = task.copy(
                                        id = 0,
                                        dueDate = nextDue,
                                        reminderAt = nextReminder,
                                        isCompleted = false,
                                        completedAt = null,
                                        createdAt = System.currentTimeMillis(),
                                        subtasks = task.subtasks.map { it.copy(isCompleted = false) }
                                    )
                                    val newId = db.taskDao().insertTask(nextTask)
                                    if (nextReminder != null) {
                                        ReminderScheduler.scheduleTaskReminderAt(context, newId, nextReminder)
                                    }
                                }
                            }
                            NotificationHelper.cancelTaskNotification(context, taskId)
                            val profileMgr = UserProfileManager(context)
                            if (profileMgr.isPinnedTodayNotificationEnabled()) {
                                NotificationHelper.updatePinnedTodayNotification(context, db.taskDao().getActiveTasksSync())
                            }
                            MonoDoWidgetProvider.updateAllWidgets(context)
                        }
                    }

                    ACTION_SNOOZE_TASK -> {
                        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                        val snoozeMins = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 15)
                        if (taskId > 0L) {
                            val newTrigger = System.currentTimeMillis() + snoozeMins * 60_000L
                            val db = AppDatabase.getDatabase(context)
                            val task = db.taskDao().getTaskById(taskId)
                            if (task != null && !task.isCompleted) {
                                db.taskDao().updateTask(task.copy(reminderAt = newTrigger))
                                ReminderScheduler.scheduleTaskReminderAt(context, taskId, newTrigger)
                            }
                            NotificationHelper.cancelTaskNotification(context, taskId)
                            MonoDoWidgetProvider.updateAllWidgets(context)
                        }
                    }

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_LOCKED_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    "android.intent.action.QUICKBOOT_POWERON",
                    "com.htc.intent.action.QUICKBOOT_POWERON",
                    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> {
                        ReminderScheduler.rescheduleAllActiveReminders(context)
                        MonoDoWidgetProvider.updateAllWidgets(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
