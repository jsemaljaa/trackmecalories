package com.jsemalja.trackmecalories.data

/*
 * Badges for the optional gamification: milestones of keeping track (streaks, weighing food,
 * marking days complete), never of eating less. Like streaks they're worked out from the
 * history, so the only thing stored is which ones have been shown (UserPrefs.seenBadges).
 */

/**
 * Counts over the whole history that badges are measured against.
 *
 * @property bestLoggingStreak the best [StreakKind.LOGGING] streak, in days.
 * @property bestCompleteStreak the best [StreakKind.COMPLETE_DAYS] streak, in days.
 * @property weighedEntries entries logged as [Confidence.WEIGHED].
 * @property scannedFoods saved foods with a barcode.
 * @property fastsReachingGoal finished fasts that reached their goal.
 * @property proteinDays / [waterDays] days the protein / water goal was reached (any days, not a streak).
 */
data class AchievementStats(
    val daysLogged: Int = 0,
    val bestLoggingStreak: Int = 0,
    val bestCompleteStreak: Int = 0,
    val weighedEntries: Int = 0,
    val scannedFoods: Int = 0,
    val recipes: Int = 0,
    val fastsReachingGoal: Int = 0,
    val proteinDays: Int = 0,
    val waterDays: Int = 0,
    val weighIns: Int = 0
)

/**
 * The badges, each reached when its measure of [AchievementStats] gets to [target]. The name
 * is the stored id, so don't rename one.
 */
enum class Badge(val target: Int, private val measure: (AchievementStats) -> Int) {
    FIRST_DAY(1, { it.daysLogged }),
    STREAK_7(7, { it.bestLoggingStreak }),
    STREAK_30(30, { it.bestLoggingStreak }),
    STREAK_100(100, { it.bestLoggingStreak }),
    STREAK_365(365, { it.bestLoggingStreak }),
    COMPLETE_WEEK(7, { it.bestCompleteStreak }),
    WEIGHED_50(50, { it.weighedEntries }),
    WEIGHED_250(250, { it.weighedEntries }),
    FIRST_SCAN(1, { it.scannedFoods }),
    FIRST_RECIPE(1, { it.recipes }),
    FASTS_10(10, { it.fastsReachingGoal }),
    PROTEIN_30(30, { it.proteinDays }),
    WATER_30(30, { it.waterDays }),
    WEIGH_INS_10(10, { it.weighIns });

    fun progress(stats: AchievementStats) = BadgeProgress(this, measure(stats))
}

/** How far [badge] is: [current] of its target (current can go past it). */
data class BadgeProgress(val badge: Badge, val current: Int) {
    val unlocked: Boolean get() = current >= badge.target
    val fraction: Float get() = (current.toFloat() / badge.target).coerceIn(0f, 1f)
}

/** Every badge's progress, in the order they're listed. */
fun badgeProgress(stats: AchievementStats): List<BadgeProgress> = Badge.entries.map { it.progress(stats) }

/** Badges reached but not shown yet (not in [seen], which holds [Badge] names). */
fun newBadges(progress: List<BadgeProgress>, seen: Set<String>): List<Badge> =
    progress.filter { it.unlocked && it.badge.name !in seen }.map { it.badge }
