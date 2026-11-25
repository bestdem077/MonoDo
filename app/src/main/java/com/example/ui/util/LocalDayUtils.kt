package com.example.ui.util

import java.util.Calendar
import java.util.TimeZone

/**
 * Timezone-safe calendar day helpers.
 * Uses local Year/Month/Day projected to UTC midnight so day keys are strictly consecutive integers
 * unaffected by UTC offsets (e.g., IST +5:30 midnight-to-5:30 AM bug) or DST transitions.
 */
object LocalDayUtils {

    fun toLocalEpochDay(timeMillis: Long = System.currentTimeMillis()): Long {
        val localCal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                localCal.get(Calendar.YEAR),
                localCal.get(Calendar.MONTH),
                localCal.get(Calendar.DAY_OF_MONTH),
                0,
                0,
                0
            )
        }
        return utcCal.timeInMillis / 86_400_000L
    }

    fun calendarForLocalEpochDay(localEpochDay: Long): Calendar {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = localEpochDay * 86_400_000L
        }
        return Calendar.getInstance().apply {
            clear()
            set(
                utcCal.get(Calendar.YEAR),
                utcCal.get(Calendar.MONTH),
                utcCal.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0
            )
        }
    }

    fun startOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfTodayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
        return Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun isOverdue(dueDate: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueDate == null) return false
        return toLocalEpochDay(dueDate) < toLocalEpochDay(nowMillis)
    }

    fun isToday(dueDate: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueDate == null) return false
        return toLocalEpochDay(dueDate) == toLocalEpochDay(nowMillis)
    }

    fun isTomorrow(dueDate: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (dueDate == null) return false
        return toLocalEpochDay(dueDate) == toLocalEpochDay(nowMillis) + 1L
    }
}
