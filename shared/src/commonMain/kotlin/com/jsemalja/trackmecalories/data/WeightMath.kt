package com.jsemalja.trackmecalories.data

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToInt

/** A weigh-in with its smoothed trend value. */
data class WeightPoint(val date: LocalDate, val weight: Double, val trend: Double)

/**
 * Trend weight: an exponential moving average of the scale readings, so day-to-day water
 * and salt swings (easily ±1 kg) don't read as real gains or losses. Each day pulls the
 * trend 10% of the way toward the new reading; a gap of n days counts as n steps, so a
 * reading after a week away moves the trend more.
 */
fun weightTrend(entries: List<WeightEntry>, smoothing: Double = 0.1): List<WeightPoint> {
    val sorted = entries.sortedBy { it.date }
    var trend: Double? = null
    var lastDate: LocalDate? = null
    return sorted.map { e ->
        val date = LocalDate.parse(e.date)
        val t = trend
        val prev = lastDate
        val next = if (t == null || prev == null) {
            e.weightKg
        } else {
            val days = prev.daysUntil(date).coerceAtLeast(1)
            val alpha = 1 - (1 - smoothing).pow(days.toDouble())
            t + alpha * (e.weightKg - t)
        }
        trend = next
        lastDate = date
        WeightPoint(date, e.weightKg, next)
    }
}

/** Summary of weight history for the Trends > Weight view. */
data class WeightStats(
    val latest: Double,
    val latestDate: LocalDate,
    val trend: Double,
    val start: Double,
    val startDate: LocalDate,
    // Change of the trend since the first weigh-in.
    val change: Double,
    val toGoal: Double?,
    // kg per week of the trend over the last 4 weeks; null with too little data.
    val weeklyRate: Double?,
    // When the goal is reached at the current rate; null if moving away or no goal.
    val projectedGoalDate: LocalDate?
)

fun weightStats(points: List<WeightPoint>, goalKg: Double?, today: LocalDate = currentDate()): WeightStats? {
    if (points.isEmpty()) return null
    val last = points.last()
    val first = points.first()

    val window = points.filter { it.date >= today.minus(28, DateTimeUnit.DAY) }
    val rate = trendSlopePerDay(window)?.let { it * 7 }

    val toGoal = goalKg?.let { it - last.trend }
    val projected = if (toGoal != null && rate != null && rate != 0.0 && toGoal / rate > 0) {
        today.plus(ceil(toGoal / rate * 7).toLong(), DateTimeUnit.DAY)
    } else null

    return WeightStats(
        latest = last.weight,
        latestDate = last.date,
        trend = last.trend,
        start = first.trend,
        startDate = first.date,
        change = last.trend - first.trend,
        toGoal = toGoal,
        weeklyRate = rate,
        projectedGoalDate = projected
    )
}

/**
 * Rate of change (kg/day): a least-squares line through the actual readings; needs 2+
 * points spanning a week. Fitted on the raw readings rather than the smoothed trend — the
 * moving average lags behind a steady change and would under-report it (and with it the
 * measured maintenance) by a good margin; the regression already averages out the noise.
 */
private fun trendSlopePerDay(points: List<WeightPoint>): Double? {
    if (points.size < 2) return null
    val origin = points.first().date
    val x = points.map { origin.daysUntil(it.date).toDouble() }
    if (x.last() - x.first() < 7) return null
    val y = points.map { it.weight }
    val mx = x.average()
    val my = y.average()
    val den = x.sumOf { (it - mx) * (it - mx) }
    return if (den > 0) x.indices.sumOf { (x[it] - mx) * (y[it] - my) } / den else null
}

/**
 * Measured maintenance (TDEE): what you actually burn, from what you ate and how your
 * weight trend moved over the same weeks — energy in minus energy stored (≈7700 kcal per
 * kg). Unlike the formula estimate it reflects your real metabolism and logging habits.
 */
data class AdaptiveMaintenance(val kcal: Int, val loggedDays: Int, val spanDays: Int)

fun adaptiveMaintenance(
    points: List<WeightPoint>,
    dailyIntake: Map<LocalDate, Int>,
    today: LocalDate = currentDate(),
    windowDays: Long = 28
): AdaptiveMaintenance? {
    val from = today.minus(windowDays, DateTimeUnit.DAY)
    val window = points.filter { it.date in from..today }
    val slope = trendSlopePerDay(window) ?: return null
    val span = window.first().date.daysUntil(window.last().date)
    // Only days that were actually logged; today is still in progress.
    val intake = dailyIntake.filterKeys { it >= from && it < today }.values.filter { it > 0 }
    if (span < 14 || window.size < 4 || intake.size < 10) return null
    val tdee = intake.average() - slope * 7700
    return AdaptiveMaintenance(tdee.roundToInt(), intake.size, span)
}
