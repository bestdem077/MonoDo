package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val frequency: String = "DAILY",
    val colorHex: String = "#2563EB",
    val createdAt: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "dateEpochDay"]
)
data class HabitLogEntity(
    val habitId: Long,
    val dateEpochDay: Long, // Epoch day (e.g. System.currentTimeMillis() / 86400000L)
    val isCompleted: Boolean = true,
    val completedAt: Long = System.currentTimeMillis()
)
