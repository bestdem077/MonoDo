package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.util.LocalDayUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

@Entity(tableName = "birthdays")
data class BirthdayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    /** 1-based month: 1 = January .. 12 = December */
    val month: Int,
    /** Day of month: 1..31 */
    val day: Int,
    /** Optional birth year (e.g. 1999) for displaying turning age */
    val birthYear: Int? = null,
    val relationship: String = "Friends",
    val note: String = "",
    /**
     * How many hours before 12:00 AM (midnight) of the birthday to fire the advance alert.
     * Default is 3 hours (9:00 PM on the eve). Supports 1, 2, 3, 4, or 15 (9:00 AM day before).
     */
    val hoursBeforeMidnight: Int = 3,
    /** Whether to also fire a celebratory notification right at 12:00 AM when the birthday starts */
    val notifyAtMidnight: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

object BirthdayDateCalculator {

    private fun clampDayForYear(year: Int, month1Based: Int, day: Int): Int {
        val cal = GregorianCalendar(year, (month1Based - 1).coerceIn(0, 11), 1)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return day.coerceIn(1, maxDay)
    }

    private fun midnightForYear(year: Int, month1Based: Int, day: Int): Calendar {
        val safeMonth = (month1Based - 1).coerceIn(0, 11)
        val safeDay = clampDayForYear(year, month1Based, day)
        return Calendar.getInstance().apply {
            clear()
            set(year, safeMonth, safeDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * Returns calendar days until the next birthday (0 = Today, 1 = Tomorrow / Tonight 12 AM, etc.).
     * Uses LocalDayUtils so DST transitions never cause off-by-one errors.
     */
    fun daysUntilBirthday(
        month: Int,
        day: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Int {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val todayEpoch = LocalDayUtils.toLocalEpochDay(nowMillis)

        val thisYearBday = midnightForYear(currentYear, month, day)
        val thisYearEpoch = LocalDayUtils.toLocalEpochDay(thisYearBday.timeInMillis)

        return if (thisYearEpoch >= todayEpoch) {
            (thisYearEpoch - todayEpoch).toInt()
        } else {
            val nextYearBday = midnightForYear(currentYear + 1, month, day)
            val nextYearEpoch = LocalDayUtils.toLocalEpochDay(nextYearBday.timeInMillis)
            (nextYearEpoch - todayEpoch).toInt()
        }
    }

    fun isBirthdayToday(month: Int, day: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return daysUntilBirthday(month, day, nowMillis) == 0
    }

    fun isBirthdayTomorrow(month: Int, day: Int, nowMillis: Long = System.currentTimeMillis()): Boolean {
        return daysUntilBirthday(month, day, nowMillis) == 1
    }

    /**
     * Computes the exact epoch millis for the next advance reminder
     * (e.g., 3 hours before 12:00 AM = 9:00 PM on the evening before the birthday).
     * Always returns a strictly future timestamp (> nowMillis) for AlarmManager.
     */
    fun nextPreReminderTriggerMillis(
        month: Int,
        day: Int,
        hoursBeforeMidnight: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val safeHours = hoursBeforeMidnight.coerceIn(1, 48)
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)

        for (yearOffset in 0..2) {
            val bdayMidnight = midnightForYear(currentYear + yearOffset, month, day)
            bdayMidnight.add(Calendar.HOUR_OF_DAY, -safeHours)
            if (bdayMidnight.timeInMillis > nowMillis) {
                return bdayMidnight.timeInMillis
            }
        }
        val fallback = midnightForYear(currentYear + 1, month, day)
        fallback.add(Calendar.HOUR_OF_DAY, -safeHours)
        return fallback.timeInMillis
    }

    /**
     * Computes the exact epoch millis for the next 12:00 AM midnight birthday start notification.
     * Always returns a strictly future timestamp (> nowMillis) for AlarmManager.
     */
    fun nextMidnightTriggerMillis(
        month: Int,
        day: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)

        for (yearOffset in 0..2) {
            val bdayMidnight = midnightForYear(currentYear + yearOffset, month, day)
            if (bdayMidnight.timeInMillis > nowMillis) {
                return bdayMidnight.timeInMillis
            }
        }
        return midnightForYear(currentYear + 1, month, day).timeInMillis
    }

    /**
     * Returns the age the person turns on today's (if today) or upcoming birthday.
     */
    fun turningAge(
        month: Int,
        day: Int,
        birthYear: Int?,
        nowMillis: Long = System.currentTimeMillis()
    ): Int? {
        if (birthYear == null || birthYear < 1900) return null
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val currentYear = nowCal.get(Calendar.YEAR)
        val todayEpoch = LocalDayUtils.toLocalEpochDay(nowMillis)
        val thisYearEpoch = LocalDayUtils.toLocalEpochDay(midnightForYear(currentYear, month, day).timeInMillis)
        val targetYear = if (thisYearEpoch >= todayEpoch) currentYear else currentYear + 1
        val age = targetYear - birthYear
        return age.takeIf { it in 1..150 }
    }

    fun formatShortDate(month: Int, day: Int): String {
        val cal = midnightForYear(2024, month, day) // 2024 is leap year so Feb 29 formats cleanly
        return SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(cal.timeInMillis))
    }

    fun formatReminderLeadLabel(hoursBeforeMidnight: Int): String {
        return when (hoursBeforeMidnight) {
            1 -> "1h before (11 PM)"
            2 -> "2h before (10 PM)"
            3 -> "3h before (9 PM)"
            4 -> "4h before (8 PM)"
            15 -> "Day before (9 AM)"
            else -> "${hoursBeforeMidnight}h before"
        }
    }

    fun formatCountdownLabel(daysUntil: Int): String {
        return when (daysUntil) {
            0 -> "TODAY"
            1 -> "TONIGHT 12 AM"
            else -> "IN $daysUntil DAYS"
        }
    }
}
