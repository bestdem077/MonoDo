package com.example.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import com.example.data.db.AppDatabase
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.data.model.TaskEntity
import java.util.Calendar

object ReminderScheduler {
    private const val DAILY_BRIEFING_REQUEST_CODE = 10001
    private const val TASK_ALARM_REQUEST_CODE_BASE = 30000
    private const val BIRTHDAY_PRE_ALARM_REQUEST_CODE_BASE = 90000
    private const val BIRTHDAY_MIDNIGHT_ALARM_REQUEST_CODE_BASE = 150000

    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun calculateNextDailyTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun calculateInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = System.currentTimeMillis()
        return (calculateNextDailyTriggerMillis(hour, minute, now) - now).coerceAtLeast(0L)
    }

    /**
     * Schedules the Daily Morning Briefing using AlarmManager (setExactAndAllowWhileIdle /
     * setAndAllowWhileIdle) so it fires reliably even after months in Doze mode.
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val triggerAt = calculateNextDailyTriggerMillis(hour, minute)
        val pendingIntent = buildDailyBriefingPendingIntent(context)
        scheduleAlarmSafely(context, triggerAt, pendingIntent)
        BirthdayWatchdogWorker.ensureScheduled(context)
    }

    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildDailyBriefingPendingIntent(context))
    }

    /**
     * Schedules an individual exact-time alarm for a specific task.
     */
    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        val reminderAt = task.reminderAt ?: return
        if (task.isCompleted || reminderAt <= System.currentTimeMillis()) {
            cancelTaskReminder(context, task.id)
            return
        }
        val pendingIntent = buildTaskReminderPendingIntent(context, task.id)
        scheduleAlarmSafely(context, reminderAt, pendingIntent)
    }

    fun scheduleTaskReminderAt(context: Context, taskId: Long, triggerAtMillis: Long) {
        if (triggerAtMillis <= System.currentTimeMillis()) return
        val pendingIntent = buildTaskReminderPendingIntent(context, taskId)
        scheduleAlarmSafely(context, triggerAtMillis, pendingIntent)
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildTaskReminderPendingIntent(context, taskId))
        NotificationHelper.cancelTaskNotification(context, taskId)
    }

    /**
     * Schedules both the advance eve reminder (e.g. 3–4 hrs before 12:00 AM) and optional
     * 12:00 AM midnight alert for a friend/family birthday.
     * Pass a shifted [nowMillis] (e.g. now + 60s) when re-arming for next year inside the receiver.
     */
    fun scheduleBirthdayReminders(
        context: Context,
        birthday: BirthdayEntity,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (birthday.id <= 0L) return

        // 1. Pre-birthday eve reminder (e.g. 9:00 PM / 8:00 PM before 12:00 AM)
        val preTriggerAt = BirthdayDateCalculator.nextPreReminderTriggerMillis(
            month = birthday.month,
            day = birthday.day,
            hoursBeforeMidnight = birthday.hoursBeforeMidnight,
            nowMillis = nowMillis
        )
        scheduleAlarmSafely(
            context,
            preTriggerAt,
            buildBirthdayPendingIntent(context, birthday.id, isMidnight = false)
        )

        // 2. Optional 12:00 AM midnight wish alert
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val midnightPi = buildBirthdayPendingIntent(context, birthday.id, isMidnight = true)
        if (birthday.notifyAtMidnight) {
            val midnightTriggerAt = BirthdayDateCalculator.nextMidnightTriggerMillis(
                month = birthday.month,
                day = birthday.day,
                nowMillis = nowMillis
            )
            scheduleAlarmSafely(context, midnightTriggerAt, midnightPi)
        } else {
            alarmManager.cancel(midnightPi)
        }

        // Ensure the 24h self-healing watchdog is running so alarms stay armed across months
        BirthdayWatchdogWorker.ensureScheduled(context)
    }

    fun cancelBirthdayReminders(context: Context, birthdayId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildBirthdayPendingIntent(context, birthdayId, isMidnight = false))
        alarmManager.cancel(buildBirthdayPendingIntent(context, birthdayId, isMidnight = true))
        NotificationHelper.cancelBirthdayNotifications(context, birthdayId)
    }

    fun schedulePomodoroFinishAlarm(
        context: Context,
        endTimeMillis: Long,
        isBreak: Boolean,
        durationMinutes: Int
    ) {
        if (endTimeMillis <= System.currentTimeMillis()) return
        val pi = buildPomodoroFinishPendingIntent(context, isBreak, durationMinutes)
        scheduleAlarmSafely(context, endTimeMillis, pi)
    }

    fun cancelPomodoroFinishAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPomodoroFinishPendingIntent(context, isBreak = false, durationMinutes = 25))
    }

    /**
     * Re-registers all active task alarms, daily briefing alarm, AND all saved birthdays.
     * Called on boot, app update, time/timezone change, exact alarm permission change,
     * app launch, and daily by BirthdayWatchdogWorker for multi-month reliability.
     */
    suspend fun rescheduleAllActiveReminders(context: Context) {
        val db = AppDatabase.getDatabase(context)
        val prefs = ReminderPreferences(context).getSettings()
        if (prefs.isEnabled) {
            scheduleDailyReminder(context, prefs.hour, prefs.minute)
        } else {
            cancelDailyReminder(context)
        }

        val now = System.currentTimeMillis()
        val tasks = db.taskDao().getTasksWithFutureReminders(now)
        tasks.forEach { task ->
            scheduleTaskReminder(context, task)
        }

        val birthdays = db.birthdayDao().getAllBirthdaysSync()
        birthdays.forEach { birthday ->
            scheduleBirthdayReminders(context, birthday, now)
        }

        // Refresh optional pinned Today's Tasks notification
        val profileMgr = com.example.data.user.UserProfileManager(context)
        if (profileMgr.isPinnedTodayNotificationEnabled()) {
            val activeTasks = db.taskDao().getActiveTasksSync()
            NotificationHelper.updatePinnedTodayNotification(context, activeTasks)
        }

        // Re-arm active Pomodoro countdown if still running
        val pomo = profileMgr.getPersistedPomodoroState()
        if (pomo.isRunning && pomo.endTimeMillis > now) {
            val durationMins = if (pomo.isBreak) pomo.breakMinutes else pomo.focusMinutes
            schedulePomodoroFinishAlarm(context, pomo.endTimeMillis, pomo.isBreak, durationMins)
            NotificationHelper.showPomodoroOngoingNotification(context, pomo.endTimeMillis, pomo.isBreak, durationMins)
        }

        if (prefs.isEnabled || tasks.isNotEmpty() || birthdays.isNotEmpty()) {
            BirthdayWatchdogWorker.ensureScheduled(context)
        }
    }

    private fun buildPomodoroFinishPendingIntent(
        context: Context,
        isBreak: Boolean,
        durationMinutes: Int
    ): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_POMODORO_FINISHED
            putExtra(ReminderBroadcastReceiver.EXTRA_POMODORO_IS_BREAK, isBreak)
            putExtra(ReminderBroadcastReceiver.EXTRA_POMODORO_DURATION_MIN, durationMinutes)
        }
        return PendingIntent.getBroadcast(
            context,
            10099,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleAlarmSafely(
        context: Context,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun buildDailyBriefingPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_DAILY_BRIEFING
        }
        return PendingIntent.getBroadcast(
            context,
            DAILY_BRIEFING_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildTaskReminderPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_TASK_REMINDER
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId)
        }
        val reqCode = TASK_ALARM_REQUEST_CODE_BASE + (taskId % 50000L).toInt()
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildBirthdayPendingIntent(
        context: Context,
        birthdayId: Long,
        isMidnight: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = if (isMidnight) {
                ReminderBroadcastReceiver.ACTION_BIRTHDAY_MIDNIGHT
            } else {
                ReminderBroadcastReceiver.ACTION_BIRTHDAY_PRE_REMINDER
            }
            putExtra(ReminderBroadcastReceiver.EXTRA_BIRTHDAY_ID, birthdayId)
        }
        val base = if (isMidnight) BIRTHDAY_MIDNIGHT_ALARM_REQUEST_CODE_BASE else BIRTHDAY_PRE_ALARM_REQUEST_CODE_BASE
        val reqCode = base + (birthdayId % 50000L).toInt()
        return PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
