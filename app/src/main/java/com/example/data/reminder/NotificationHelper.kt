package com.example.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.BirthdayDateCalculator
import com.example.data.model.BirthdayEntity
import com.example.data.model.BirthdayWishTemplates
import com.example.data.model.TaskEntity
import com.example.data.model.TaskPriority
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_TASK_REMINDERS = "monodo_task_reminders_v2"
    const val CHANNEL_DAILY_BRIEFING = "monodo_daily_briefing_v2"
    const val CHANNEL_BIRTHDAYS = "monodo_birthdays_v1"
    const val CHANNEL_PINNED_TODAY = "monodo_pinned_today_v1"
    const val CHANNEL_POMODORO_ONGOING = "monodo_pomodoro_ongoing_v1"
    const val CHANNEL_POMODORO_DONE = "monodo_pomodoro_done_v1"

    const val DAILY_BRIEFING_NOTIFICATION_ID = 1001
    const val PINNED_TODAY_NOTIFICATION_ID = 1002
    const val POMODORO_ONGOING_NOTIFICATION_ID = 1003
    const val POMODORO_DONE_NOTIFICATION_ID = 1004

    private const val TASK_NOTIFICATION_ID_BASE = 20000
    private const val BIRTHDAY_PRE_NOTIFICATION_ID_BASE = 80000
    private const val BIRTHDAY_MIDNIGHT_NOTIFICATION_ID_BASE = 140000

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val taskChannel = NotificationChannel(
                CHANNEL_TASK_REMINDERS,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Exact-time alerts for your scheduled tasks with quick Done & Snooze actions"
                enableVibration(true)
            }

            val briefingChannel = NotificationChannel(
                CHANNEL_DAILY_BRIEFING,
                "Daily Morning Briefing",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily agenda summary showing your overdue, today's tasks, and upcoming birthdays"
            }

            val birthdayChannel = NotificationChannel(
                CHANNEL_BIRTHDAYS,
                "Birthday Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Advance eve alerts (3–4 hrs before 12 AM) and midnight wishes for friends & family birthdays"
                enableVibration(true)
            }

            val pinnedTodayChannel = NotificationChannel(
                CHANNEL_PINNED_TODAY,
                "Pinned Today's Tasks",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Optional silent persistent card displaying today's pending tasks in your shade"
                setShowBadge(false)
            }

            val pomoOngoingChannel = NotificationChannel(
                CHANNEL_POMODORO_ONGOING,
                "Active Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Live silent countdown timer while a Pomodoro focus or break session is running"
                setShowBadge(false)
            }

            val pomoDoneChannel = NotificationChannel(
                CHANNEL_POMODORO_DONE,
                "Focus Session Complete",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority sound and vibration alert when a Pomodoro focus or break session finishes"
                enableVibration(true)
            }

            nm.createNotificationChannel(taskChannel)
            nm.createNotificationChannel(briefingChannel)
            nm.createNotificationChannel(birthdayChannel)
            nm.createNotificationChannel(pinnedTodayChannel)
            nm.createNotificationChannel(pomoOngoingChannel)
            nm.createNotificationChannel(pomoDoneChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun taskNotificationId(taskId: Long): Int {
        return TASK_NOTIFICATION_ID_BASE + (taskId % 50000L).toInt()
    }

    fun cancelTaskNotification(context: Context, taskId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(taskNotificationId(taskId))
    }

    fun birthdayNotificationId(birthdayId: Long, isMidnight: Boolean): Int {
        val base = if (isMidnight) BIRTHDAY_MIDNIGHT_NOTIFICATION_ID_BASE else BIRTHDAY_PRE_NOTIFICATION_ID_BASE
        return base + (birthdayId % 50000L).toInt()
    }

    fun cancelBirthdayNotifications(context: Context, birthdayId: Long) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(birthdayNotificationId(birthdayId, isMidnight = false))
        nm.cancel(birthdayNotificationId(birthdayId, isMidnight = true))
    }

    /**
     * Fires a high-priority notification for a friend or family member's birthday,
     * including 1-tap "Copy Wish" and "Share Wish" actions directly in the shade.
     */
    fun showBirthdayNotification(
        context: Context,
        birthday: BirthdayEntity,
        isMidnightAlert: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = birthdayNotificationId(birthday.id, isMidnightAlert)

        val openBirthdaysIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_BIRTHDAYS, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openBirthdaysIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val age = BirthdayDateCalculator.turningAge(birthday.month, birthday.day, birthday.birthYear)
        val dateText = BirthdayDateCalculator.formatShortDate(birthday.month, birthday.day)
        val agePhrase = if (age != null) " turns $age" else "'s birthday"

        val defaultWish = BirthdayWishTemplates.defaultWishFor(
            name = birthday.name,
            relationship = birthday.relationship,
            turningAge = age
        )

        // Action 1: Copy Wish to Clipboard
        val copyIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_COPY_BIRTHDAY_WISH
            putExtra(ReminderBroadcastReceiver.EXTRA_WISH_TEXT, defaultWish)
            putExtra(ReminderBroadcastReceiver.EXTRA_BIRTHDAY_NAME, birthday.name)
        }
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 3 + 1,
            copyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Share Wish via WhatsApp / SMS / Share Sheet
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, defaultWish)
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Send Birthday Wish").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            notifId * 3 + 2,
            chooserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isMidnightAlert) {
            "Happy Birthday, ${birthday.name}!"
        } else if (birthday.hoursBeforeMidnight >= 12) {
            "Tomorrow's Birthday: ${birthday.name}"
        } else {
            "Birthday in ${birthday.hoursBeforeMidnight}h: ${birthday.name}"
        }

        val leadMessage = if (isMidnightAlert) {
            if (age != null) {
                "It's 12:00 AM — ${birthday.name} turns $age today ($dateText). Send your wish!"
            } else {
                "It's 12:00 AM — it's ${birthday.name}'s birthday today ($dateText). Send your wish!"
            }
        } else if (birthday.hoursBeforeMidnight >= 12) {
            "${birthday.name}$agePhrase tomorrow ($dateText). Get your wish or gift ready!"
        } else {
            "${birthday.name}$agePhrase at 12:00 AM tonight ($dateText) — ${birthday.hoursBeforeMidnight} hours to go!"
        }

        val bigBody = buildString {
            append(leadMessage)
            if (birthday.note.isNotBlank()) {
                append("\nNote: ${birthday.note}")
            }
            append("\n${birthday.relationship} • $dateText")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_BIRTHDAYS)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(title)
            .setContentText(leadMessage)
            .setSubText(if (isMidnightAlert) "12:00 AM Birthday" else "Upcoming Birthday")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigBody))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Copy Wish", copyPendingIntent)
            .addAction(0, "Share Wish", sharePendingIntent)
            .build()

        nm.notify(notifId, notification)
    }

    /**
     * Silent ongoing summary notification for "Pinned Today's Tasks" (opt-in via Settings).
     */
    fun updatePinnedTodayNotification(context: Context, activeTasks: List<TaskEntity>) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            PINNED_TODAY_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addTaskIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CREATE_TASK, true)
        }
        val addTaskPendingIntent = PendingIntent.getActivity(
            context,
            PINNED_TODAY_NOTIFICATION_ID + 1,
            addTaskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val overdue = activeTasks.filter { LocalDayUtils.isOverdue(it.dueDate) }
        val dueToday = activeTasks.filter { LocalDayUtils.isToday(it.dueDate) }
        val focusList = (overdue + dueToday).ifEmpty { activeTasks }.sortedWith(
            compareBy<TaskEntity> {
                when {
                    LocalDayUtils.isOverdue(it.dueDate) -> 0
                    LocalDayUtils.isToday(it.dueDate) -> 1
                    else -> 2
                }
            }.thenByDescending { it.priority.ordinal }
        )

        val title = when {
            overdue.isNotEmpty() && dueToday.isNotEmpty() ->
                "Today: ${dueToday.size} due • ${overdue.size} overdue"
            overdue.isNotEmpty() ->
                "Overdue: ${overdue.size} ${if (overdue.size == 1) "task" else "tasks"} pending"
            dueToday.isNotEmpty() ->
                "Today's Tasks (${dueToday.size} pending)"
            activeTasks.isNotEmpty() ->
                "All caught up for today • ${activeTasks.size} upcoming"
            else ->
                "Today's Tasks • All clear"
        }

        val summaryLine = if (focusList.isEmpty()) {
            "No pending tasks. Enjoy a distraction-free day."
        } else {
            focusList.take(2).joinToString(" • ") { it.title }
        }

        val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
        if (focusList.isEmpty()) {
            inboxStyle.addLine("Tap + Add Task to capture a new priority.")
        } else {
            focusList.take(5).forEach { task ->
                val prefix = when {
                    LocalDayUtils.isOverdue(task.dueDate) -> "[Overdue] "
                    LocalDayUtils.isToday(task.dueDate) -> "[Today] "
                    else -> ""
                }
                inboxStyle.addLine("${task.priority.indicatorSymbol} $prefix${task.title}")
            }
            if (focusList.size > 5) {
                inboxStyle.setSummaryText("+${focusList.size - 5} more in MonoDo")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_PINNED_TODAY)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(title)
            .setContentText(summaryLine)
            .setSubText("Pinned Agenda")
            .setStyle(inboxStyle)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "+ Add Task", addTaskPendingIntent)
            .build()

        nm.notify(PINNED_TODAY_NOTIFICATION_ID, notification)
    }

    fun cancelPinnedTodayNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(PINNED_TODAY_NOTIFICATION_ID)
    }

    /**
     * Live silent countdown notification for active Pomodoro Focus / Break sessions.
     * Uses Android's native chronometer countdown so it ticks accurately with zero battery drain.
     */
    fun showPomodoroOngoingNotification(
        context: Context,
        endTimeMillis: Long,
        isBreak: Boolean,
        durationMinutes: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            POMODORO_ONGOING_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_POMODORO_PAUSE
        }
        val pausePi = PendingIntent.getBroadcast(
            context,
            POMODORO_ONGOING_NOTIFICATION_ID + 1,
            pauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_POMODORO_STOP
        }
        val stopPi = PendingIntent.getBroadcast(
            context,
            POMODORO_ONGOING_NOTIFICATION_ID + 2,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBreak) "Break Timer • ${durationMinutes}m" else "Focus Session • ${durationMinutes}m"
        val subtitle = if (isBreak) "Rest & recharge — MonoDo will alert when ready" else "Deep focus in progress — stays running in background"

        val builder = NotificationCompat.Builder(context, CHANNEL_POMODORO_ONGOING)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSubText(if (isBreak) "Pomodoro Break" else "Pomodoro Focus")
            .setWhen(endTimeMillis)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Pause", pausePi)
            .addAction(0, "Stop", stopPi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setChronometerCountDown(true)
        }

        nm.notify(POMODORO_ONGOING_NOTIFICATION_ID, builder.build())
    }

    fun cancelPomodoroOngoingNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(POMODORO_ONGOING_NOTIFICATION_ID)
    }

    /**
     * High-priority alert with vibration/sound when Pomodoro Focus or Break completes in background.
     */
    fun showPomodoroFinishedNotification(
        context: Context,
        isBreak: Boolean,
        durationMinutes: Int
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            POMODORO_DONE_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isBreak) {
            "Break Complete!"
        } else {
            "Focus Session Complete (${durationMinutes}m)"
        }
        val body = if (isBreak) {
            "Your ${durationMinutes}-minute recharge break is over. Ready for the next focus block?"
        } else {
            "Great work! ${durationMinutes} focused minutes logged to your MonoDo stats. Time for a short break."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_POMODORO_DONE)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(title)
            .setContentText(body)
            .setSubText("Pomodoro Complete")
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .build()

        nm.notify(POMODORO_DONE_NOTIFICATION_ID, notification)
    }

    /**
     * Fires a high-priority actionable notification for an individual scheduled task.
     * Includes "Mark Done" and "Snooze 15m" buttons right inside the Android notification panel.
     */
    fun showTaskReminderNotification(context: Context, task: TaskEntity) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifId = taskNotificationId(task.id)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Mark Done directly from notification shade
        val doneIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_MARK_TASK_DONE
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id)
        }
        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 3 + 1,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Snooze 15 minutes
        val snoozeIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_SNOOZE_TASK
            putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, task.id)
            putExtra(ReminderBroadcastReceiver.EXTRA_SNOOZE_MINUTES, 15)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notifId * 3 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val priorityTag = when (task.priority) {
            TaskPriority.HIGH -> "High Priority"
            TaskPriority.MEDIUM -> "Medium Priority"
            TaskPriority.LOW -> "Low Priority"
        }
        val timeText = task.reminderAt?.let {
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))
        } ?: ""

        val subLine = listOfNotNull(
            task.category.takeIf { it.isNotBlank() },
            priorityTag,
            timeText.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        val bodyText = if (task.description.isNotBlank()) {
            "${task.description}\n$subLine"
        } else {
            subLine
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(task.title)
            .setContentText(if (task.description.isNotBlank()) task.description else subLine)
            .setSubText(task.category.ifBlank { "Task Reminder" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Mark Done", donePendingIntent)
            .addAction(0, "Snooze 15m", snoozePendingIntent)

        nm.notify(notifId, builder.build())
    }

    /**
     * Fires an expandable InboxStyle Daily Morning Briefing showing actual overdue & today's tasks,
     * plus a 1-tap "+ Add Task" action button.
     */
    fun showDailyBriefingNotification(
        context: Context,
        activeTasks: List<TaskEntity>,
        birthdays: List<BirthdayEntity> = emptyList()
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannels(context)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            DAILY_BRIEFING_NOTIFICATION_ID,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val addTaskIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_OPEN_CREATE_TASK, true)
        }
        val addTaskPendingIntent = PendingIntent.getActivity(
            context,
            DAILY_BRIEFING_NOTIFICATION_ID + 1,
            addTaskIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val todayBirthdays = birthdays.filter { BirthdayDateCalculator.isBirthdayToday(it.month, it.day) }
        val tonightBirthdays = birthdays.filter { BirthdayDateCalculator.isBirthdayTomorrow(it.month, it.day) }

        val overdue = activeTasks.filter { LocalDayUtils.isOverdue(it.dueDate) }
        val dueToday = activeTasks.filter { LocalDayUtils.isToday(it.dueDate) }
        val prioritized = activeTasks.sortedWith(
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

        val title = when {
            todayBirthdays.isNotEmpty() -> {
                val names = todayBirthdays.joinToString(", ") { it.name }
                "Birthday Today: $names"
            }
            activeTasks.isEmpty() && tonightBirthdays.isNotEmpty() -> {
                "Tonight 12 AM: ${tonightBirthdays.first().name}'s Birthday"
            }
            activeTasks.isEmpty() -> "Fresh slate for today"
            overdue.isNotEmpty() && dueToday.isNotEmpty() ->
                "${overdue.size} overdue • ${dueToday.size} due today"
            overdue.isNotEmpty() ->
                "${overdue.size} overdue ${if (overdue.size == 1) "task needs" else "tasks need"} attention"
            dueToday.isNotEmpty() ->
                "Today's Agenda • ${dueToday.size} ${if (dueToday.size == 1) "task" else "tasks"}"
            else ->
                "Daily Briefing • ${activeTasks.size} active ${if (activeTasks.size == 1) "task" else "tasks"}"
        }

        val summary = when {
            todayBirthdays.isNotEmpty() && activeTasks.isNotEmpty() ->
                "Wish ${todayBirthdays.first().name} • ${activeTasks.size} pending tasks"
            tonightBirthdays.isNotEmpty() ->
                "${tonightBirthdays.first().name}'s birthday at 12 AM tonight"
            activeTasks.isEmpty() ->
                "Plan your top 3 priorities for a calm, focused day."
            else ->
                prioritized.take(2).joinToString(" • ") { it.title }
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)

        todayBirthdays.take(2).forEach { bday ->
            val age = BirthdayDateCalculator.turningAge(bday.month, bday.day, bday.birthYear)
            val ageSuffix = if (age != null) " (Turns $age)" else ""
            inboxStyle.addLine("[Birthday Today] ${bday.name}$ageSuffix")
        }
        tonightBirthdays.take(2).forEach { bday ->
            inboxStyle.addLine("[Tonight 12 AM] ${bday.name}'s Birthday")
        }

        if (activeTasks.isEmpty() && todayBirthdays.isEmpty() && tonightBirthdays.isEmpty()) {
            inboxStyle.addLine("No pending tasks yet — tap + Add Task to plan your day.")
        } else {
            prioritized.take(5).forEach { task ->
                val tag = when {
                    LocalDayUtils.isOverdue(task.dueDate) -> "[Overdue] "
                    LocalDayUtils.isToday(task.dueDate) -> "[Today] "
                    else -> ""
                }
                val timeHint = task.reminderAt?.let {
                    " (${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(it))})"
                } ?: ""
                inboxStyle.addLine("${task.priority.indicatorSymbol} $tag${task.title}$timeHint")
            }
            if (prioritized.size > 5) {
                inboxStyle.setSummaryText("+${prioritized.size - 5} more in MonoDo")
            } else {
                inboxStyle.setSummaryText("${activeTasks.size} pending in MonoDo")
            }
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_DAILY_BRIEFING)
            .setSmallIcon(R.drawable.ic_monodo_notification)
            .setColor(0xFF2563EB.toInt())
            .setContentTitle(title)
            .setContentText(summary)
            .setSubText("Daily Briefing")
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "+ Add Task", addTaskPendingIntent)
            .build()

        nm.notify(DAILY_BRIEFING_NOTIFICATION_ID, notification)
    }
}
