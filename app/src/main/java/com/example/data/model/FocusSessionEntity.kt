package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val durationMinutes: Int,
    val taskId: Long? = null,
    val taskTitle: String = "",
    val completedAt: Long = System.currentTimeMillis(),
    val localEpochDay: Long
)
