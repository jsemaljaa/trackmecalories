package com.jsemalja.trackmecalories.data

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min

/** A saved food with how often and how much of it you usually have. */
data class FoodHabit(val food: CustomFood, val count: Int, val typicalAmount: Double, val score: Double)

/** A food that fits what's left of today's budget, at your usual portion. */
data class FitSuggestion(
    val food: CustomFood,
    val amount: Double,
    val kcal: Double,
    val protein: Double,
    val fiber: Double,
    val score: Double
)

/** The saved food an entry came from: by its link, else by name and unit (older entries). */
private fun matchFood(entry: FoodEntry, byId: Map<Int, CustomFood>, byName: Map<Pair<String, String>, CustomFood>): CustomFood? =
    entry.foodId?.let(byId::get) ?: byName[entry.name.lowercase() to entry.unit]

private fun FoodEntry.localTime(zone: ZoneId): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(time), zone)

private fun isWeekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY

/**
 * What you usually eat around now: entries within ±[windowMinutes] of the current time of
 * day, weighted toward the same kind of day (weekday vs weekend) and recent weeks. Foods
 * logged in the last 3 hours are skipped — you probably don't want the same thing again.
 */
fun timeSuggestions(
    entries: List<FoodEntry>,
    foods: List<CustomFood>,
    now: LocalDateTime,
    zone: ZoneId = ZoneId.systemDefault(),
    windowMinutes: Int = 90,
    limit: Int = 4
): List<FoodHabit> {
    val byId = foods.associateBy { it.id }
    val byName = foods.associateBy { it.name.lowercase() to it.unit }
    val nowMinute = now.hour * 60 + now.minute
    val recentCutoff = now.minusHours(3)

    val justHad = entries.filter { it.localTime(zone).isAfter(recentCutoff) }
        .mapNotNull { matchFood(it, byId, byName)?.id }.toSet()

    val scores = HashMap<Int, Double>()
    val counts = HashMap<Int, Int>()
    val amounts = HashMap<Int, MutableList<Double>>()
    for (e in entries) {
        if (e.mealType == "Quick Add") continue
        val food = matchFood(e, byId, byName) ?: continue
        val t = e.localTime(zone)
        val minute = t.hour * 60 + t.minute
        val diff = abs(minute - nowMinute).let { min(it, 1440 - it) }
        if (diff > windowMinutes) continue
        val daysAgo = ChronoUnit.DAYS.between(t.toLocalDate(), now.toLocalDate()).coerceAtLeast(0)
        val dayType = if (isWeekend(t.dayOfWeek) == isWeekend(now.dayOfWeek)) 1.0 else 0.4
        val closeness = 1.0 - diff / (windowMinutes * 2.0)
        scores.merge(food.id, dayType * closeness * exp(-daysAgo / 30.0), Double::plus)
        counts.merge(food.id, 1, Int::plus)
        amounts.getOrPut(food.id) { mutableListOf() }.add(e.amount)
    }

    return scores.entries
        .filter { (id, _) -> id !in justHad && (counts[id] ?: 0) >= 2 }
        .sortedByDescending { it.value }
        .take(limit)
        .map { (id, score) ->
            val food = byId.getValue(id)
            FoodHabit(food, counts.getValue(id), food.defaultAmount ?: median(amounts.getValue(id)), score)
        }
}

/**
 * "What fits?": foods from your own history, at your usual portion, that fit the calories
 * left today — ranked by how much of the remaining protein they cover (protein is what's
 * usually short), then fiber, then how often you eat them.
 */
fun whatFits(
    entries: List<FoodEntry>,
    foods: List<CustomFood>,
    remainingKcal: Double,
    remainingProtein: Double,
    limit: Int = 8
): List<FitSuggestion> {
    if (remainingKcal < 50) return emptyList()
    val byId = foods.associateBy { it.id }
    val byName = foods.associateBy { it.name.lowercase() to it.unit }
    val counts = HashMap<Int, Int>()
    val amounts = HashMap<Int, MutableList<Double>>()
    for (e in entries) {
        if (e.mealType == "Quick Add") continue
        val food = matchFood(e, byId, byName) ?: continue
        counts.merge(food.id, 1, Int::plus)
        amounts.getOrPut(food.id) { mutableListOf() }.add(e.amount)
    }

    return counts.keys.mapNotNull { id ->
        val food = byId.getValue(id)
        val amount = food.defaultAmount ?: median(amounts.getValue(id))
        val ratio = amount / food.referenceAmount
        val kcal = food.kcalPer100 * ratio
        if (kcal < 20 || kcal > remainingKcal * 1.05) return@mapNotNull null
        val protein = food.proteinPer100 * ratio
        val fiber = food.fiberPer100 * ratio
        val proteinCover = if (remainingProtein > 1) min(protein, remainingProtein) / remainingProtein else 0.0
        val proteinDensity = protein / kcal * 100 // g per 100 kcal
        val score = proteinCover * 2 + proteinDensity * 0.1 + fiber / kcal * 100 * 0.15 + ln(1.0 + counts.getValue(id)) * 0.3
        FitSuggestion(food, amount, kcal, protein, fiber, score)
    }.sortedByDescending { it.score }.take(limit)
}

private fun median(values: List<Double>): Double {
    val s = values.sorted()
    return if (s.size % 2 == 1) s[s.size / 2] else (s[s.size / 2 - 1] + s[s.size / 2]) / 2
}

/** Start of the history window used for suggestions. */
fun suggestionHistoryStart(today: LocalDate = LocalDate.now()): LocalDate = today.minusDays(90)
