package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.Fast
import com.jsemalja.trackmecalories.data.HOUR_MS
import com.jsemalja.trackmecalories.data.fastProgress
import com.jsemalja.trackmecalories.data.fastStartFromClock
import com.jsemalja.trackmecalories.data.formatFastDuration
import com.jsemalja.trackmecalories.data.formatStopwatch
import com.jsemalja.trackmecalories.data.goalTime
import com.jsemalja.trackmecalories.data.nextFastDue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FastingTest {
    private val fast = Fast(startTime = 1_000_000L, targetHours = 16)

    @Test
    fun progressBeforeGoal() {
        val p = fastProgress(fast, fast.startTime + 4 * HOUR_MS)
        assertEquals(4 * HOUR_MS, p.elapsedMs)
        assertEquals(12 * HOUR_MS, p.remainingMs)
        assertEquals(0.25f, p.fraction, 0.0001f)
        assertFalse(p.reached)
    }

    @Test
    fun progressPastGoalIsCappedButKeepsCounting() {
        val p = fastProgress(fast, fast.startTime + 18 * HOUR_MS)
        assertEquals(18 * HOUR_MS, p.elapsedMs)
        assertEquals(0L, p.remainingMs)
        assertEquals(1f, p.fraction, 0f)
        assertTrue(p.reached)
    }

    @Test
    fun endedFastStopsAtItsEnd() {
        val ended = fast.copy(endTime = fast.startTime + 10 * HOUR_MS)
        assertEquals(10 * HOUR_MS, fastProgress(ended, fast.startTime + 30 * HOUR_MS).elapsedMs)
    }

    @Test
    fun startInTheFutureCountsAsZero() {
        assertEquals(0L, fastProgress(fast, fast.startTime - HOUR_MS).elapsedMs)
    }

    @Test
    fun goalTimeIsStartPlusPlan() {
        assertEquals(fast.startTime + 16 * HOUR_MS, fast.goalTime)
    }

    @Test
    fun nextFastIsDueAfterTheEatingWindow() {
        assertEquals(8 * HOUR_MS, nextFastDue(0, 16))
        assertEquals(1 * HOUR_MS, nextFastDue(0, 23))
    }

    @Test
    fun clockTimeEarlierTodayIsToday() {
        val now = ZonedDateTime.of(2026, 10, 27, 9, 15, 30, 0, ZoneId.of("Europe/Prague"))
        val start = fastStartFromClock(LocalTime.of(7, 0), now)
        assertEquals(ZonedDateTime.of(2026, 10, 27, 7, 0, 0, 0, now.zone), start)
    }

    @Test
    fun clockTimeLaterThanNowIsYesterday() {
        val now = ZonedDateTime.of(2026, 10, 27, 9, 15, 30, 0, ZoneId.of("Europe/Prague"))
        val start = fastStartFromClock(LocalTime.of(20, 30), now)
        assertEquals(ZonedDateTime.of(2026, 10, 26, 20, 30, 0, 0, now.zone), start)
    }

    @Test
    fun durations() {
        assertEquals("16 h", formatFastDuration(16 * HOUR_MS, "h", "min"))
        assertEquals("12 h 5 min", formatFastDuration(12 * HOUR_MS + 5 * 60_000 + 59_000, "h", "min"))
        assertEquals("45 min", formatFastDuration(45 * 60_000L, "h", "min"))
        assertEquals("0 min", formatFastDuration(-5_000, "h", "min"))
    }

    @Test
    fun stopwatchKeepsCountingPastADay() {
        assertEquals("0:00:09", formatStopwatch(9_999))
        assertEquals("12:04:09", formatStopwatch(12 * HOUR_MS + 4 * 60_000 + 9_000))
        assertEquals("36:00:00", formatStopwatch(36 * HOUR_MS))
    }
}
