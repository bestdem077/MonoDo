package com.example.data.model

import java.util.Calendar

enum class TaskRecurrence(val label: String, val shortLabel: String) {
    NONE("No Repeat", ""),
    DAILY("Daily", "Daily"),
    WEEKLY("Weekly", "Weekly"),
    MONTHLY("Monthly", "Monthly");

    fun nextDueDate(baseDueDate: Long?, nowMillis: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        // If baseDueDate is in the future, advance from it; otherwise advance from today
        val anchor = if (baseDueDate != null && baseDueDate > nowMillis) baseDueDate else nowMillis
        cal.timeInMillis = anchor
        when (this) {
            NONE -> return anchor
            DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
            MONTHLY -> cal.add(Calendar.MONTH, 1)
        }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
