package com.example.data.user

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val userName: String,
    val workspaceTitle: String = "MonoDo",
    val userPurpose: String = "Deep Focus & Productivity",
    val birthDate: String = "",
    val birthTimestamp: Long = 0L,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isOnboarded: Boolean
)

class UserProfileManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("monodo_user_profile", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_WORKSPACE_TITLE = "key_workspace_title"
        private const val KEY_USER_PURPOSE = "key_user_purpose"
        private const val KEY_BIRTH_DATE = "key_birth_date"
        private const val KEY_BIRTH_TIMESTAMP = "key_birth_timestamp"
        private const val KEY_REMINDER_HOUR = "key_reminder_hour"
        private const val KEY_REMINDER_MINUTE = "key_reminder_minute"
        private const val KEY_IS_ONBOARDED = "key_is_onboarded"

        const val DEFAULT_USER_NAME = ""
        const val DEFAULT_WORKSPACE_TITLE = "MonoDo"
        const val DEFAULT_USER_PURPOSE = "Deep Focus & Productivity"
        const val DEFAULT_BIRTH_DATE = ""
        const val DEFAULT_BIRTH_TIMESTAMP = 0L
        const val DEFAULT_REMINDER_HOUR = 9
        const val DEFAULT_REMINDER_MINUTE = 0
    }

    fun getProfile(): UserProfile {
        val savedName = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        val savedTitle = prefs.getString(KEY_WORKSPACE_TITLE, DEFAULT_WORKSPACE_TITLE) ?: DEFAULT_WORKSPACE_TITLE
        val savedPurpose = prefs.getString(KEY_USER_PURPOSE, DEFAULT_USER_PURPOSE) ?: DEFAULT_USER_PURPOSE
        val savedBirthDate = prefs.getString(KEY_BIRTH_DATE, DEFAULT_BIRTH_DATE) ?: DEFAULT_BIRTH_DATE
        val savedBirthTimestamp = prefs.getLong(KEY_BIRTH_TIMESTAMP, DEFAULT_BIRTH_TIMESTAMP)
        val savedReminderHour = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
        val savedReminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)

        val flagValue = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        val hasCompletedOnboarding = flagValue || savedName.isNotBlank() || (prefs.contains(KEY_IS_ONBOARDED) && prefs.getBoolean(KEY_IS_ONBOARDED, false))

        return UserProfile(
            userName = savedName,
            workspaceTitle = savedTitle,
            userPurpose = savedPurpose,
            birthDate = savedBirthDate,
            birthTimestamp = savedBirthTimestamp,
            reminderHour = savedReminderHour,
            reminderMinute = savedReminderMinute,
            isOnboarded = hasCompletedOnboarding
        )
    }

    fun saveProfile(
        userName: String,
        workspaceTitle: String = DEFAULT_WORKSPACE_TITLE,
        userPurpose: String = DEFAULT_USER_PURPOSE,
        birthDate: String = DEFAULT_BIRTH_DATE,
        birthTimestamp: Long = DEFAULT_BIRTH_TIMESTAMP,
        reminderHour: Int = DEFAULT_REMINDER_HOUR,
        reminderMinute: Int = DEFAULT_REMINDER_MINUTE
    ): Boolean {
        val cleanName = userName.trim()
        val cleanTitle = workspaceTitle.trim().ifBlank { DEFAULT_WORKSPACE_TITLE }
        val cleanPurpose = userPurpose.trim().ifBlank { DEFAULT_USER_PURPOSE }

        return prefs.edit()
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_WORKSPACE_TITLE, cleanTitle)
            .putString(KEY_USER_PURPOSE, cleanPurpose)
            .putString(KEY_BIRTH_DATE, birthDate.trim())
            .putLong(KEY_BIRTH_TIMESTAMP, birthTimestamp)
            .putInt(KEY_REMINDER_HOUR, reminderHour)
            .putInt(KEY_REMINDER_MINUTE, reminderMinute)
            .putBoolean(KEY_IS_ONBOARDED, true)
            .commit()
    }

    fun markOnboarded(): Boolean {
        return prefs.edit()
            .putBoolean(KEY_IS_ONBOARDED, true)
            .commit()
    }

    fun getThemeMode(): AppThemeMode {
        val raw = prefs.getString(KEY_THEME_MODE, AppThemeMode.LIGHT_PAPER.name)
        return try {
            AppThemeMode.valueOf(raw ?: AppThemeMode.LIGHT_PAPER.name)
        } catch (_: Exception) {
            AppThemeMode.LIGHT_PAPER
        }
    }

    fun saveThemeMode(mode: AppThemeMode): Boolean {
        return prefs.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .commit()
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)

    fun setAppLockEnabled(enabled: Boolean): Boolean {
        return prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, enabled)
            .commit()
    }

    fun isPinnedTodayNotificationEnabled(): Boolean =
        prefs.getBoolean(KEY_PINNED_TODAY_NOTIFICATION, false)

    fun setPinnedTodayNotificationEnabled(enabled: Boolean): Boolean {
        return prefs.edit()
            .putBoolean(KEY_PINNED_TODAY_NOTIFICATION, enabled)
            .commit()
    }

    fun getDismissedSelfBirthdayYear(): Int = prefs.getInt(KEY_DISMISSED_SELF_BIRTHDAY_YEAR, -1)

    fun setDismissedSelfBirthdayYear(year: Int): Boolean {
        return prefs.edit()
            .putInt(KEY_DISMISSED_SELF_BIRTHDAY_YEAR, year)
            .commit()
    }

    fun getDismissedUpdateTag(): String = prefs.getString(KEY_DISMISSED_UPDATE_TAG, "") ?: ""

    fun setDismissedUpdateTag(tag: String): Boolean {
        return prefs.edit()
            .putString(KEY_DISMISSED_UPDATE_TAG, tag)
            .commit()
    }

    fun getLastUpdateCheckMillis(): Long = prefs.getLong(KEY_LAST_UPDATE_CHECK_MILLIS, 0L)

    fun setLastUpdateCheckMillis(timestampMillis: Long): Boolean {
        return prefs.edit()
            .putLong(KEY_LAST_UPDATE_CHECK_MILLIS, timestampMillis)
            .commit()
    }

    fun savePomodoroState(
        isRunning: Boolean,
        isBreak: Boolean,
        focusMinutes: Int,
        breakMinutes: Int,
        endTimeMillis: Long,
        remainingSeconds: Int,
        sessionCompletedLogged: Boolean = false
    ) {
        prefs.edit()
            .putBoolean(KEY_POMO_RUNNING, isRunning)
            .putBoolean(KEY_POMO_IS_BREAK, isBreak)
            .putInt(KEY_POMO_FOCUS_MIN, focusMinutes)
            .putInt(KEY_POMO_BREAK_MIN, breakMinutes)
            .putLong(KEY_POMO_END_TIME, endTimeMillis)
            .putInt(KEY_POMO_REMAINING_SEC, remainingSeconds)
            .putBoolean(KEY_POMO_LOGGED, sessionCompletedLogged)
            .commit()
    }

    fun getPersistedPomodoroState(): PersistedPomodoroState {
        return PersistedPomodoroState(
            isRunning = prefs.getBoolean(KEY_POMO_RUNNING, false),
            isBreak = prefs.getBoolean(KEY_POMO_IS_BREAK, false),
            focusMinutes = prefs.getInt(KEY_POMO_FOCUS_MIN, 25),
            breakMinutes = prefs.getInt(KEY_POMO_BREAK_MIN, 5),
            endTimeMillis = prefs.getLong(KEY_POMO_END_TIME, 0L),
            remainingSeconds = prefs.getInt(KEY_POMO_REMAINING_SEC, 25 * 60),
            sessionCompletedLogged = prefs.getBoolean(KEY_POMO_LOGGED, false)
        )
    }

    fun clearPomodoroRunning(resetRemainingSeconds: Int? = null, isBreak: Boolean? = null) {
        val editor = prefs.edit()
            .putBoolean(KEY_POMO_RUNNING, false)
            .putLong(KEY_POMO_END_TIME, 0L)
        if (resetRemainingSeconds != null) {
            editor.putInt(KEY_POMO_REMAINING_SEC, resetRemainingSeconds)
        }
        if (isBreak != null) {
            editor.putBoolean(KEY_POMO_IS_BREAK, isBreak)
        }
        editor.commit()
    }

    fun resetProfile(): Boolean {
        return prefs.edit()
            .clear()
            .commit()
    }
}

data class PersistedPomodoroState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val endTimeMillis: Long = 0L,
    val remainingSeconds: Int = 25 * 60,
    val sessionCompletedLogged: Boolean = false
)

enum class AppThemeMode(val title: String, val subtitle: String) {
    LIGHT_PAPER("Light Paper", "Warm Swiss paper & ink"),
    DARK_SLATE("Dark Slate", "Low-glare charcoal workspace"),
    AMOLED_BLACK("Pure Black", "True OLED battery-saving black")
}

private const val KEY_THEME_MODE = "key_theme_mode"
private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
private const val KEY_PINNED_TODAY_NOTIFICATION = "key_pinned_today_notification"
private const val KEY_DISMISSED_SELF_BIRTHDAY_YEAR = "key_dismissed_self_birthday_year"
private const val KEY_POMO_RUNNING = "key_pomo_running"
private const val KEY_POMO_IS_BREAK = "key_pomo_is_break"
private const val KEY_POMO_FOCUS_MIN = "key_pomo_focus_min"
private const val KEY_POMO_BREAK_MIN = "key_pomo_break_min"
private const val KEY_POMO_END_TIME = "key_pomo_end_time"
private const val KEY_POMO_REMAINING_SEC = "key_pomo_remaining_sec"
private const val KEY_POMO_LOGGED = "key_pomo_logged"
private const val KEY_DISMISSED_UPDATE_TAG = "key_dismissed_update_tag"
private const val KEY_LAST_UPDATE_CHECK_MILLIS = "key_last_update_check_millis"
