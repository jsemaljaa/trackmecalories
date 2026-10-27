package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.data.Streak
import com.jsemalja.trackmecalories.data.StreakHistory
import com.jsemalja.trackmecalories.data.StreakKind
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.dailyStreak
import com.jsemalja.trackmecalories.data.streakDays
import com.jsemalja.trackmecalories.data.streaks
import com.jsemalja.trackmecalories.data.weeklyStreak
import com.jsemalja.trackmecalories.ui.viewmodel.weekStartOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreaksTest {
    private val today = LocalDate.of(2026, 10, 21) // a Wednesday

    /** Days counted back from [today]: 0 is today, 1 yesterday. */
    private fun days(vararg ago: Long) = ago.mapTo(mutableSetOf()) { today.minusDays(it) }

    private fun total(day: LocalDate, kcal: Int, protein: Double = 0.0) = DailyTotal(day.toString(), kcal, protein, 0.0, 0.0)

    @Test
    fun `nothing logged is no streak`() {
        assertEquals(Streak.NONE, dailyStreak(emptySet(), today))
    }

    @Test
    fun `consecutive days up to today`() {
        val s = dailyStreak(days(0, 1, 2, 3), today)
        assertEquals(4, s.current)
        assertEquals(4, s.best)
        assertFalse(s.pending)
    }

    @Test
    fun `today not logged yet keeps the streak, pending`() {
        val s = dailyStreak(days(1, 2, 3), today)
        assertEquals(3, s.current)
        assertTrue(s.pending)
        // Missing today could still be forgiven.
        assertFalse(s.atRisk)
    }

    @Test
    fun `one missed day is forgiven and not counted`() {
        val s = dailyStreak(days(0, 1, 3, 4, 5), today)
        assertEquals(5, s.current)
    }

    @Test
    fun `a second miss within a week ends the streak`() {
        // Misses 4 and 1 days ago are 3 days apart.
        val s = dailyStreak(days(0, 2, 3, 5, 6), today)
        assertEquals(1, s.current)
        assertEquals(4, s.best)
    }

    @Test
    fun `misses a week apart are both forgiven`() {
        val logged = (0L..15L).filter { it != 2L && it != 9L }.toLongArray()
        assertEquals(14, dailyStreak(days(*logged), today).current)
    }

    @Test
    fun `two missed days in a row end it`() {
        val s = dailyStreak(days(0, 3, 4, 5), today)
        assertEquals(1, s.current)
        assertEquals(3, s.best)
    }

    @Test
    fun `missed yesterday and nothing yet today is at risk`() {
        val s = dailyStreak(days(2, 3, 4), today)
        assertEquals(3, s.current)
        assertTrue(s.pending)
        assertTrue(s.atRisk)
    }

    @Test
    fun `a streak can't start with a miss`() {
        // Logged only today after a long gap: the gap isn't bridged.
        val s = dailyStreak(days(0, 10, 11), today)
        assertEquals(1, s.current)
        assertEquals(2, s.best)
    }

    @Test
    fun `days after today are ignored`() {
        val s = dailyStreak(days(0, 1) + today.plusDays(1), today)
        assertEquals(2, s.current)
    }

    @Test
    fun `weekly streak counts weeks with any hit`() {
        // Weighed in twice last week (Sat 17th, Tue 13th) and once the week before (Fri 9th);
        // nothing yet this week.
        val s = weeklyStreak(days(4, 8, 12), today, ::weekStartOf)
        assertEquals(2, s.current)
        assertTrue(s.pending)
        // No forgiven weeks, so skipping this one ends it.
        assertTrue(s.atRisk)
    }

    @Test
    fun `complete days leave out days marked incomplete`() {
        val history = StreakHistory(
            logged = days(0, 1, 2).associateWith { total(it, 2000) },
            incompleteDays = days(1)
        )
        assertEquals(days(0, 2), streakDays(StreakKind.COMPLETE_DAYS, history, UserGoal(), today, ::weekStartOf))
    }

    @Test
    fun `protein counts days reaching the goal`() {
        val history = StreakHistory(logged = mapOf(today to total(today, 2000, 150.0), today.minusDays(1) to total(today.minusDays(1), 2000, 149.0)))
        val goals = UserGoal(proteinGoal = 150.0)
        assertEquals(days(0), streakDays(StreakKind.PROTEIN, history, goals, today, ::weekStartOf))
    }

    @Test
    fun `calories count near the goal either side, not under it`() {
        val kcal = mapOf(1L to 1790, 2L to 2200, 3L to 1500, 4L to 2300)
        val history = StreakHistory(logged = kcal.entries.associate { (ago, k) -> today.minusDays(ago) to total(today.minusDays(ago), k) })
        // 1790 is 10.5% under: eating well below the goal doesn't count.
        assertEquals(days(2), streakDays(StreakKind.CALORIES_IN_RANGE, history, UserGoal(calorieGoal = 2000), today, ::weekStartOf))
    }

    @Test
    fun `calories skip today and incomplete days`() {
        val history = StreakHistory(
            logged = days(0, 1).associateWith { total(it, 2000) },
            incompleteDays = days(1)
        )
        assertEquals(emptySet<LocalDate>(), streakDays(StreakKind.CALORIES_IN_RANGE, history, UserGoal(calorieGoal = 2000), today, ::weekStartOf))
    }

    @Test
    fun `calories follow the weekly budget's target`() {
        val monday = today.minusDays(2)
        val tuesday = today.minusDays(1)
        // 500 saved on Monday lifts Tuesday's target to 2083, so 2250 is in range.
        val history = StreakHistory(logged = mapOf(monday to total(monday, 1500), tuesday to total(tuesday, 2250)))
        val goals = UserGoal(calorieGoal = 2000)
        assertEquals(setOf(tuesday), streakDays(StreakKind.CALORIES_IN_RANGE, history, goals, today, ::weekStartOf, weeklyBudget = true))
        assertEquals(emptySet<LocalDate>(), streakDays(StreakKind.CALORIES_IN_RANGE, history, goals, today, ::weekStartOf, weeklyBudget = false))
    }

    @Test
    fun `only the asked streaks are worked out`() {
        val history = StreakHistory(logged = days(0, 1).associateWith { total(it, 2000) }, weighInDays = days(0))
        val result = streaks(setOf(StreakKind.LOGGING, StreakKind.WEIGH_IN), history, UserGoal(), today, ::weekStartOf)
        assertEquals(setOf(StreakKind.LOGGING, StreakKind.WEIGH_IN), result.keys)
        assertEquals(2, result.getValue(StreakKind.LOGGING).current)
        assertEquals(1, result.getValue(StreakKind.WEIGH_IN).current)
    }
}
