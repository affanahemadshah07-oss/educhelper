package com.example.domain

import com.example.data.TimetableSlotEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DayOption(
    val dayOfWeek: Int, // 1 = Mon .. 7 = Sun
    val shortLabel: String,
    val fullName: String
)

val WeekDays = listOf(
    DayOption(1, "Mon", "Monday"),
    DayOption(2, "Tue", "Tuesday"),
    DayOption(3, "Wed", "Wednesday"),
    DayOption(4, "Thu", "Thursday"),
    DayOption(5, "Fri", "Friday"),
    DayOption(6, "Sat", "Saturday"),
    DayOption(7, "Sun", "Sunday")
)

sealed interface LiveClassState {
    data class Ongoing(
        val slot: TimetableSlotEntity,
        val minutesRemaining: Int,
        val progress: Float
    ) : LiveClassState

    data class UpNext(
        val slot: TimetableSlotEntity,
        val minutesUntilStart: Int
    ) : LiveClassState

    data object CompletedForToday : LiveClassState
    data object NoClassesToday : LiveClassState
}

object TimeUtils {

    /** Returns 1 = Monday .. 7 = Sunday matching our Room entity convention. */
    fun currentDayOfWeek(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun currentMinutesFromMidnight(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    fun currentHour(nowMillis: Long = System.currentTimeMillis()): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    fun formatMinutes(minutesFromMidnight: Int): String {
        val clamped = minutesFromMidnight.coerceIn(0, 23 * 60 + 59)
        val hour24 = clamped / 60
        val minute = clamped % 60
        val amPm = if (hour24 < 12) "AM" else "PM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, amPm)
    }

    fun formatDurationMinutes(totalMinutes: Int): String {
        val safe = totalMinutes.coerceAtLeast(0)
        val hours = safe / 60
        val mins = safe % 60
        return when {
            hours > 0 && mins > 0 -> "${hours}h ${mins}m"
            hours > 0 -> "${hours}h"
            else -> "${mins}m"
        }
    }

    fun formatTodayHeader(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
        return formatter.format(Date(nowMillis))
    }

    fun formatDueDateTime(epochMillis: Long): String {
        val formatter = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    fun formatDueCountdown(dueEpochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Pair<String, Boolean> {
        val diffMillis = dueEpochMillis - nowMillis
        val isOverdue = diffMillis < 0
        val absMinutes = (kotlin.math.abs(diffMillis) / 60_000L).toInt()
        val days = absMinutes / (60 * 24)
        val hours = (absMinutes % (60 * 24)) / 60
        val mins = absMinutes % 60

        val label = when {
            isOverdue && days > 0 -> "Overdue by ${days}d"
            isOverdue && hours > 0 -> "Overdue by ${hours}h"
            isOverdue -> "Overdue"
            days >= 2 -> "In $days days"
            days == 1 -> "Tomorrow"
            hours >= 1 -> "In ${hours}h ${mins}m"
            mins > 0 -> "In ${mins}m"
            else -> "Due now"
        }
        return label to isOverdue
    }

    fun computeLiveClassState(
        todaySlots: List<TimetableSlotEntity>,
        nowMinutes: Int
    ): LiveClassState {
        if (todaySlots.isEmpty()) return LiveClassState.NoClassesToday
        val sorted = todaySlots.sortedBy { it.startMinutes }

        val ongoing = sorted.firstOrNull { nowMinutes in it.startMinutes until it.endMinutes }
        if (ongoing != null) {
            val duration = (ongoing.endMinutes - ongoing.startMinutes).coerceAtLeast(1)
            val elapsed = (nowMinutes - ongoing.startMinutes).coerceIn(0, duration)
            return LiveClassState.Ongoing(
                slot = ongoing,
                minutesRemaining = (ongoing.endMinutes - nowMinutes).coerceAtLeast(1),
                progress = elapsed.toFloat() / duration.toFloat()
            )
        }

        val next = sorted.firstOrNull { it.startMinutes > nowMinutes }
        if (next != null) {
            return LiveClassState.UpNext(
                slot = next,
                minutesUntilStart = (next.startMinutes - nowMinutes).coerceAtLeast(1)
            )
        }

        return LiveClassState.CompletedForToday
    }
}
