package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local Room Entity representing a distinct note created by the user.
 * Supports title, rich multiline content, color category, pin status, and timestamps.
 */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val category: String = "General",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
