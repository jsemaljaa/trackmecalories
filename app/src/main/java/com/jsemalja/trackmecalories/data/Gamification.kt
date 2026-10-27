package com.jsemalja.trackmecalories.data

import java.time.LocalDate

/** Counts for badges that aren't per day. */
data class GamificationCounts(
    val weighedEntries: Int = 0,
    val scannedFoods: Int = 0,
    val recipes: Int = 0,
    val fastsReachingGoal: Int = 0
)

/**
 * Everything the streak and badge screens show.
 *
 * @property streaks the tracked streaks, in [StreakKind] order.
 * @property loggedDays / [completeDays] for the consistency calendar on Trends.
 */
data class GamificationState(
    val streaks: Map<StreakKind, Streak>,
    val badges: List<BadgeProgress>,
    val loggedDays: Set<LocalDate>,
    val completeDays: Set<LocalDate>
)

/**
 * Streaks and badges at [today]. Water and fasting streaks are only tracked while those
 * features are on, and their badges are hidden then unless already reached.
 */
fun gamificationState(
    tracked: Set<StreakKind>,
    history: StreakHistory,
    counts: GamificationCounts,
    goals: UserGoal,
    today: LocalDate,
    weekStart: (LocalDate) -> LocalDate,
    weeklyBudget: Boolean,
    waterOn: Boolean,
    fastingOn: Boolean
): GamificationState {
    fun days(kind: StreakKind) = streakDays(kind, history, goals, today, weekStart, weeklyBudget)
    val shown = StreakKind.entries.filter {
        it in tracked && (it != StreakKind.WATER || waterOn) && (it != StreakKind.FASTING || fastingOn)
    }
    // Logging and complete days feed badges even when not shown as streaks.
    val all = streaks((shown + StreakKind.LOGGING + StreakKind.COMPLETE_DAYS).toSet(), history, goals, today, weekStart, weeklyBudget)
    val completeDays = days(StreakKind.COMPLETE_DAYS)
    val stats = AchievementStats(
        daysLogged = history.logged.size,
        bestLoggingStreak = all.getValue(StreakKind.LOGGING).best,
        bestCompleteStreak = all.getValue(StreakKind.COMPLETE_DAYS).best,
        weighedEntries = counts.weighedEntries,
        scannedFoods = counts.scannedFoods,
        recipes = counts.recipes,
        fastsReachingGoal = counts.fastsReachingGoal,
        proteinDays = days(StreakKind.PROTEIN).size,
        waterDays = days(StreakKind.WATER).size,
        weighIns = history.weighInDays.size
    )
    val badges = badgeProgress(stats).filter {
        it.unlocked || (it.badge != Badge.FASTS_10 || fastingOn) && (it.badge != Badge.WATER_30 || waterOn)
    }
    return GamificationState(
        streaks = shown.associateWith { all.getValue(it) },
        badges = badges,
        loggedDays = history.logged.keys,
        completeDays = completeDays
    )
}
