package com.example.data.model

enum class TaskSortOrder(val displayName: String) {
    DEFAULT("Default"),
    PRIORITY("Priority: High → Low"),
    DUE_DATE("Due Date: Soonest"),
    CREATION_DATE("Newest First"),
    ALPHABETICAL("Alphabetical: A → Z")
}
