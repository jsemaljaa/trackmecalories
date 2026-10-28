package com.jsemalja.trackmecalories.data

import kotlinx.datetime.LocalDate
import kotlin.math.abs

/*
 * Streaks for the optional gamification. They reward keeping track honestly, never eating
 * less: no streak counts days under a calorie goal, and the calorie one counts days near it.
 *
 * Everything is worked out from the logged history each time, so nothing is stored: turning
 * streaks on shows the past straight away, and turning them off leaves nothing behind.
 */

/** One missed day is forgiven per this many days, so a forgotten day doesn't end a long streak. */
const val STREAK_GRACE_DAYS = 7

/** A day counts for [StreakKind.CALORIES_IN_RANGE] within this share of its target, either side. */
const val CALORIE_RANGE = 0.10

/**
 * What a streak counts. [weekly] ones count weeks with at least one hit day, so weighing in
 * every day isn't pushed.
 */
enum class StreakKind(val weekly: Boolean = false) {
    /** Any food logged. */
    LOGGING,
    /** Food logged and the day not marked incomplete. */
    COMPLETE_DAYS,
    PROTEIN,
    WATER,
    /** Within ±[CALORIE_RANGE] of the day's target (the weekly budget's when that's on). */
    CALORIES_IN_RANGE,
    /** A fast reached its goal (counted on the day it ended). */
    FASTING,
    WEIGH_IN(weekly = true);

    companion object {
        val DEFAULT = setOf(LOGGING, COMPLETE_DAYS)
    }
}

/**
 * @property current days (weeks) done in the running streak, 0 when there's none. Forgiven
 *   misses aren't counted.
 * @property best the longest streak so far, the current one included.
 * @property pending today (this week) isn't done yet, which doesn't break the streak.
 * @property atRisk pending, and a miss today couldn't be forgiven: the streak ends unless today
 *   is done.
 */
data class Streak(val current: Int, val best: Int, val pending: Boolean, val atRisk: Boolean) {
    companion object {
        val NONE = Streak(0, 0, pending = false, atRisk = false)
    }
}

/**
 * The logged history streaks are worked out from.
 *
 * @property logged food totals per day, only for days with something logged.
 * @property fastGoalDays days a fast that reached its goal ended on.
 */
data class StreakHistory(
    val logged: Map<LocalDate, DailyTotal> = emptyMap(),
    val waterMl: Map<LocalDate, Double> = emptyMap(),
    val incompleteDays: Set<LocalDate> = emptySet(),
    val fastGoalDays: Set<LocalDate> = emptySet(),
    val weighInDays: Set<LocalDate> = emptySet()
)

/**
 * The streaks of [kinds] at [today]. Past days are judged by today's [goals]; old goals
 * aren't kept.
 *
 * @param weekStart the first day of a date's week, for weekly streaks and the weekly budget.
 * @param weeklyBudget whether the calorie target flexes across the week (see [weeklyBudget]).
 */
fun streaks(
    kinds: Set<StreakKind>,
    history: StreakHistory,
    goals: UserGoal,
    today: LocalDate,
    weekStart: (LocalDate) -> LocalDate,
    weeklyBudget: Boolean = false
): Map<StreakKind, Streak> = kinds.associateWith { kind ->
    val days = streakDays(kind, history, goals, today, weekStart, weeklyBudget)
    if (kind.weekly) weeklyStreak(days, today, weekStart) else dailyStreak(days, today)
}

/** The days that count for [kind]. */
fun streakDays(
    kind: StreakKind,
    history: StreakHistory,
    goals: UserGoal,
    today: LocalDate,
    weekStart: (LocalDate) -> LocalDate,
    weeklyBudget: Boolean = false
): Set<LocalDate> = when (kind) {
    StreakKind.LOGGING -> history.logged.keys
    StreakKind.COMPLETE_DAYS -> history.logged.keys - history.incompleteDays
    StreakKind.PROTEIN -> if (goals.proteinGoal <= 0) emptySet()
        else history.logged.filterValues { it.totalProtein >= goals.proteinGoal }.keys
    StreakKind.WATER -> if (goals.waterGoalMl <= 0) emptySet()
        else history.waterMl.filterValues { it >= goals.waterGoalMl }.keys
    StreakKind.CALORIES_IN_RANGE -> {
        val intake = history.logged.mapValues { it.value.totalKcal }
        history.logged.keys.filterTo(mutableSetOf()) { day ->
            // Today isn't over, so it can't be judged yet; an incomplete day would read as low.
            if (day >= today || day in history.incompleteDays) return@filterTo false
            val target = if (weeklyBudget) {
                weeklyBudget(goals.calorieGoal, weekStart(day), day, intake, history.incompleteDays).todayTarget
            } else goals.calorieGoal
            target > 0 && abs(intake.getValue(day) - target) <= target * CALORIE_RANGE
        }
    }
    StreakKind.FASTING -> history.fastGoalDays
    StreakKind.WEIGH_IN -> history.weighInDays
}

/** A daily streak, with one miss forgiven per [STREAK_GRACE_DAYS] days. */
fun dailyStreak(days: Set<LocalDate>, today: LocalDate): Streak =
    streakOver(days.mapTo(mutableSetOf()) { it.toEpochDays() }, today.toEpochDays(), STREAK_GRACE_DAYS)

/** A weekly streak (no misses forgiven: a week is already lenient). */
fun weeklyStreak(days: Set<LocalDate>, today: LocalDate, weekStart: (LocalDate) -> LocalDate): Streak =
    streakOver(days.mapTo(mutableSetOf()) { weekIndex(weekStart(it)) }, weekIndex(weekStart(today)), graceGap = null)

// Week starts all fall on the same weekday, so they're 7 epoch days apart.
private fun weekIndex(weekStart: LocalDate): Long = weekStart.toEpochDays().floorDiv(7L)

/**
 * The streak over consecutively numbered periods (days or weeks) up to [now]. A missed period
 * is forgiven when it's at least [graceGap] periods after the last one forgiven in the same
 * streak (null: never); a streak can't start with a forgiven miss. Hits after [now] are ignored.
 */
internal fun streakOver(hits: Set<Long>, now: Long, graceGap: Int?): Streak {
    val first = hits.filter { it <= now }.minOrNull() ?: return Streak.NONE
    var run = 0
    var best = 0
    var lastForgiven: Long? = null
    fun forgivable(period: Long): Boolean {
        val last = lastForgiven
        return run > 0 && graceGap != null && (last == null || period - last >= graceGap)
    }

    for (period in first until now) {
        when {
            period in hits -> run++
            forgivable(period) -> lastForgiven = period
            else -> {
                run = 0
                lastForgiven = null
            }
        }
        best = maxOf(best, run)
    }
    val doneNow = now in hits
    if (doneNow) run++
    best = maxOf(best, run)
    val pending = !doneNow && run > 0
    return Streak(run, best, pending, atRisk = pending && !forgivable(now))
}
