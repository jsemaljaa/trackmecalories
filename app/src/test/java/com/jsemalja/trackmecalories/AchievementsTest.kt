package com.jsemalja.trackmecalories

import com.jsemalja.trackmecalories.data.AchievementStats
import com.jsemalja.trackmecalories.data.Badge
import com.jsemalja.trackmecalories.data.DailyTotal
import com.jsemalja.trackmecalories.data.GamificationCounts
import com.jsemalja.trackmecalories.data.StreakHistory
import com.jsemalja.trackmecalories.data.StreakKind
import com.jsemalja.trackmecalories.data.UserGoal
import com.jsemalja.trackmecalories.data.badgeProgress
import com.jsemalja.trackmecalories.data.gamificationState
import com.jsemalja.trackmecalories.data.newBadges
import com.jsemalja.trackmecalories.data.weekStartOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.datetime.LocalDate

class AchievementsTest {
    @Test
    fun `a new user has no badges`() {
        assertTrue(badgeProgress(AchievementStats()).none { it.unlocked })
    }

    @Test
    fun `streak badges follow the best logging streak`() {
        val progress = badgeProgress(AchievementStats(daysLogged = 40, bestLoggingStreak = 30)).associateBy { it.badge }
        assertTrue(progress.getValue(Badge.STREAK_7).unlocked)
        assertTrue(progress.getValue(Badge.STREAK_30).unlocked)
        assertFalse(progress.getValue(Badge.STREAK_100).unlocked)
        assertEquals(0.3f, progress.getValue(Badge.STREAK_100).fraction, 0.001f)
    }

    @Test
    fun `progress past the target stays full`() {
        val weighed = Badge.WEIGHED_50.progress(AchievementStats(weighedEntries = 80))
        assertTrue(weighed.unlocked)
        assertEquals(80, weighed.current)
        assertEquals(1f, weighed.fraction, 0f)
    }

    private val today = LocalDate(2026, 10, 21)

    private fun state(tracked: Set<StreakKind>, waterOn: Boolean, fastingOn: Boolean, counts: GamificationCounts = GamificationCounts()) =
        gamificationState(
            tracked,
            StreakHistory(logged = (0L..2L).associate { today.minusDays(it) to DailyTotal(today.minusDays(it).toString(), 2000, 0.0, 0.0, 0.0) }),
            counts, UserGoal(), today, ::weekStartOf, weeklyBudget = false, waterOn = waterOn, fastingOn = fastingOn
        )

    @Test
    fun `water and fasting streaks only while those features are on`() {
        val all = StreakKind.entries.toSet()
        assertEquals(all - StreakKind.WATER - StreakKind.FASTING, state(all, waterOn = false, fastingOn = false).streaks.keys)
        assertEquals(all, state(all, waterOn = true, fastingOn = true).streaks.keys)
        assertEquals(3, state(all, waterOn = false, fastingOn = false).streaks.getValue(StreakKind.LOGGING).current)
    }

    @Test
    fun `badges of features that are off are hidden unless reached`() {
        val off = state(setOf(StreakKind.LOGGING), waterOn = false, fastingOn = false).badges.map { it.badge }
        assertFalse(Badge.FASTS_10 in off)
        assertFalse(Badge.WATER_30 in off)
        val reached = state(setOf(StreakKind.LOGGING), waterOn = false, fastingOn = false, GamificationCounts(fastsReachingGoal = 10))
        assertTrue(reached.badges.first { it.badge == Badge.FASTS_10 }.unlocked)
    }

    @Test
    fun `new badges leave out the ones already shown`() {
        val progress = badgeProgress(AchievementStats(daysLogged = 3, scannedFoods = 1, recipes = 1))
        assertEquals(listOf(Badge.FIRST_DAY, Badge.FIRST_SCAN, Badge.FIRST_RECIPE), newBadges(progress, emptySet()))
        assertEquals(listOf(Badge.FIRST_RECIPE), newBadges(progress, setOf("FIRST_DAY", "FIRST_SCAN")))
    }
}
