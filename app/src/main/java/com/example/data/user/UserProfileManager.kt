package com.example.data.user

import android.content.Context
import android.content.SharedPreferences

data class UserProfile(
    val userName: String,
    val workspaceTitle: String = "MonoDo",
    val userPurpose: String = "Deep Focus & Productivity",
    val isOnboarded: Boolean
)

class UserProfileManager(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("monodo_user_profile", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_USER_NAME = "key_user_name"
        private const val KEY_WORKSPACE_TITLE = "key_workspace_title"
        private const val KEY_USER_PURPOSE = "key_user_purpose"
        private const val KEY_IS_ONBOARDED = "key_is_onboarded"

        const val DEFAULT_USER_NAME = ""
        const val DEFAULT_WORKSPACE_TITLE = "MonoDo"
        const val DEFAULT_USER_PURPOSE = "Deep Focus & Productivity"
    }

    fun getProfile(): UserProfile {
        val savedName = prefs.getString(KEY_USER_NAME, DEFAULT_USER_NAME) ?: DEFAULT_USER_NAME
        val savedTitle = prefs.getString(KEY_WORKSPACE_TITLE, DEFAULT_WORKSPACE_TITLE) ?: DEFAULT_WORKSPACE_TITLE
        val savedPurpose = prefs.getString(KEY_USER_PURPOSE, DEFAULT_USER_PURPOSE) ?: DEFAULT_USER_PURPOSE

        // Double safeguard: A user is considered onboarded if KEY_IS_ONBOARDED is true OR if they already have a saved non-blank name or custom title.
        // This ensures the welcome screen is shown only once on initial launch and never repeats on subsequent launches.
        val flagValue = prefs.getBoolean(KEY_IS_ONBOARDED, false)
        val hasCompletedOnboarding = flagValue || savedName.isNotBlank() || (prefs.contains(KEY_IS_ONBOARDED) && prefs.getBoolean(KEY_IS_ONBOARDED, false))

        return UserProfile(
            userName = savedName,
            workspaceTitle = savedTitle,
            userPurpose = savedPurpose,
            isOnboarded = hasCompletedOnboarding
        )
    }

    fun saveProfile(
        userName: String,
        workspaceTitle: String = DEFAULT_WORKSPACE_TITLE,
        userPurpose: String = DEFAULT_USER_PURPOSE
    ): Boolean {
        val cleanName = userName.trim()
        val cleanTitle = workspaceTitle.trim().ifBlank { DEFAULT_WORKSPACE_TITLE }
        val cleanPurpose = userPurpose.trim().ifBlank { DEFAULT_USER_PURPOSE }

        // Must use .commit() so that data is synchronously persisted to physical disk immediately,
        // preventing lost state on process restart or app exit.
        return prefs.edit()
            .putString(KEY_USER_NAME, cleanName)
            .putString(KEY_WORKSPACE_TITLE, cleanTitle)
            .putString(KEY_USER_PURPOSE, cleanPurpose)
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
