package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "Personal",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val subtaskTitles: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

object DefaultRoutineTemplates {
    val starters: List<RoutineTemplateEntity> = listOf(
        RoutineTemplateEntity(
            id = -101L,
            title = "Morning Routine",
            category = "Health",
            priority = TaskPriority.MEDIUM,
            subtaskTitles = listOf(
                "Drink a glass of water",
                "10-min stretch or walk",
                "Review today's top 3 priorities"
            ),
            createdAt = 0L
        ),
        RoutineTemplateEntity(
            id = -102L,
            title = "Deep Study Session",
            category = "Study",
            priority = TaskPriority.HIGH,
            subtaskTitles = listOf(
                "Silence phone & clear desk",
                "Review notes for 10 mins",
                "Complete 2 focused Pomodoro blocks",
                "Write 3 key takeaways"
            ),
            createdAt = 0L
        ),
        RoutineTemplateEntity(
            id = -103L,
            title = "Travel Packing Checklist",
            category = "Personal",
            priority = TaskPriority.MEDIUM,
            subtaskTitles = listOf(
                "ID, wallet & tickets",
                "Phone charger & power bank",
                "Clothes & toiletries",
                "Medications & water bottle"
            ),
            createdAt = 0L
        )
    )
}
