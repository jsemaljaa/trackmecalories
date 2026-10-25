package com.jsemalja.trackmecalories.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Weekly budgeting: the week has goal × 7 kcal, and today's target flexes with what was
 * saved or overspent earlier in the week, spread over the days left (today included).
 *
 * @property todayTarget today's flexible target, kept within ±[MAX_FLEX] of the daily goal so
 *   one big day nudges the rest of the week instead of breaking it.
 * @property weekLeft what's left of the whole week's budget before today's eating.
 */
data class WeeklyBudget(
    val weekBudget: Int,
    val usedBeforeToday: Int,
    val daysLeft: Int,
    val todayTarget: Int,
    val weekLeft: Int,
    // True when the ±limit kicked in (today's share of the week would be further off).
    val capped: Boolean
) {
    companion object {
        const val MAX_FLEX = 0.25
    }
}

/**
 * @param intakeByDate kcal per day of this week (earlier days); missing = nothing logged.
 * @param incompleteDays days marked incomplete — they count as at least [dailyGoal], so a
 *   forgotten dinner doesn't turn into fake "savings".
 */
fun weeklyBudget(
    dailyGoal: Int,
    weekStart: LocalDate,
    today: LocalDate,
    intakeByDate: Map<LocalDate, Int>,
    incompleteDays: Set<LocalDate> = emptySet()
): WeeklyBudget {
    val weekBudget = dailyGoal * 7
    val earlierDays = (0 until ChronoUnit.DAYS.between(weekStart, today)).map { weekStart.plusDays(it) }
    val used = earlierDays.sumOf { d ->
        val logged = intakeByDate[d] ?: 0
        // A day with nothing logged is unknown, not a fast: count it as on-goal.
        if (logged == 0 || d in incompleteDays) maxOf(logged, dailyGoal) else logged
    }
    val daysLeft = (7 - earlierDays.size).coerceAtLeast(1)
    val weekLeft = weekBudget - used
    val share = weekLeft.toDouble() / daysLeft
    val min = dailyGoal * (1 - WeeklyBudget.MAX_FLEX)
    val max = dailyGoal * (1 + WeeklyBudget.MAX_FLEX)
    val target = share.coerceIn(min, max)
    return WeeklyBudget(weekBudget, used, daysLeft, target.roundToInt(), weekLeft, capped = share < min || share > max)
}
