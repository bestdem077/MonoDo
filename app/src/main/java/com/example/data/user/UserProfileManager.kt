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

    fun resetProfile(): Boolean {
        return prefs.edit()
            .clear()
            .commit()
    }
}
