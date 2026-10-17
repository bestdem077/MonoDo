package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "General",
    val tags: List<String> = emptyList(),
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val dueDate: Long? = null,
    val isCompleted: Boolean = false,
    val subtasks: List<SubTask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
