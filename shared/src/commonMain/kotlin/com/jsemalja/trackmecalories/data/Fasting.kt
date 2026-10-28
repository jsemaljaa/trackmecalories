package com.jsemalja.trackmecalories.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus

const val HOUR_MS = 3_600_000L
private const val MINUTE_MS = 60_000L

/** Fasting hours of the common daily plans (16 = 16:8: fast 16 hours, eat within 8). */
val FASTING_PLANS = listOf(12, 14, 16, 18, 20, 23)
const val DEFAULT_FASTING_HOURS = 16

/** When the fast reaches its goal. */
val Fast.goalTime: Long get() = startTime + targetHours * HOUR_MS

/** A finished fast that lasted its planned hours. */
val Fast.reachedGoal: Boolean get() = endTime.let { it != null && it >= goalTime }

/** How far a running fast is at [now]. */
data class FastProgress(val elapsedMs: Long, val targetMs: Long) {
    val remainingMs: Long get() = (targetMs - elapsedMs).coerceAtLeast(0)
    val fraction: Float get() = if (targetMs <= 0) 1f else (elapsedMs.toFloat() / targetMs).coerceIn(0f, 1f)
    val reached: Boolean get() = elapsedMs >= targetMs
}

fun fastProgress(fast: Fast, now: Long): FastProgress =
    FastProgress(((fast.endTime ?: now) - fast.startTime).coerceAtLeast(0), fast.targetHours * HOUR_MS)

/**
 * When the next fast is due: the eating window after the last fast is 24 h minus the plan's
 * fasting hours, so a 16:8 fast ended at noon makes the next one due at 20:00.
 */
fun nextFastDue(lastFastEnd: Long, planHours: Int): Long =
    lastFastEnd + (24 - planHours).coerceAtLeast(0) * HOUR_MS

/**
 * A fast's start picked as a clock time ("I stopped eating at 20:30"): today, or yesterday when
 * that time hasn't come yet today. Never in the future.
 */
fun fastStartFromClock(time: LocalTime, now: LocalDateTime): LocalDateTime {
    val clock = LocalTime(time.hour, time.minute)
    val today = LocalDateTime(now.date, clock)
    return if (today > now) LocalDateTime(now.date.minus(1, DateTimeUnit.DAY), clock) else today
}

/** "16 h", "12 h 5 min" or "45 min"; the unit words are passed in so they can be localized. */
fun formatFastDuration(ms: Long, hourUnit: String, minuteUnit: String): String {
    val totalMinutes = ms.coerceAtLeast(0) / MINUTE_MS
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "$minutes $minuteUnit"
        minutes == 0L -> "$hours $hourUnit"
        else -> "$hours $hourUnit $minutes $minuteUnit"
    }
}

/** A stopwatch reading, "12:04:09"; hours keep counting past 24. */
fun formatStopwatch(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    return "${totalSeconds / 3600}:${(totalSeconds / 60 % 60).twoDigits()}:${(totalSeconds % 60).twoDigits()}"
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')

/** Whether fasting alerts can be shown at all, and on time. */
data class FastingAccess(val notifications: Boolean, val exactAlarms: Boolean)

/**
 * Starting and ending fasts, and keeping the platform's notifications and alarms in step
 * with the running fast and [FastingSettings] (FastingTimer on Android). Every change ends
 * with [sync].
 */
interface FastingControls {
    suspend fun start(at: Long = currentTimeMillis())
    suspend fun end(at: Long = currentTimeMillis())

    /** "I actually stopped eating at 20:30": moves the running fast's start (never into the future). */
    suspend fun moveStart(at: Long)

    /** A new plan applies to the running fast too; finished fasts keep theirs. */
    suspend fun retarget(planHours: Int)

    fun access(): FastingAccess

    /** Shows, updates or removes the notifications and (re)schedules the alarms; safe to call any time. */
    suspend fun sync()
}
