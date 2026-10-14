package com.example.data.reminder

import android.content.Context
import android.content.SharedPreferences

data class ReminderSettings(
    val isEnabled: Boolean,
    val hour: Int,
    val minute: Int
)

class ReminderPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("monodo_reminder_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ENABLED = "key_reminder_enabled"
        private const val KEY_HOUR = "key_reminder_hour"
        private const val KEY_MINUTE = "key_reminder_minute"
    }

    fun getSettings(): ReminderSettings {
        return ReminderSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 9), // Default 9:00 AM
            minute = prefs.getInt(KEY_MINUTE, 0)
        )
    }

    fun saveSettings(isEnabled: Boolean, hour: Int, minute: Int) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, isEnabled)
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }
}
