package com.jsemalja.trackmecalories.data

/**
 * How full (and energetic) the meals a food was part of left you.
 *
 * @property meals rated meals the food was in: the sample size behind the averages.
 * @property avgEnergy null when none of those meals had energy rated.
 * @property avgMealKcal the whole meal's calories, not just this food's portion.
 * @property fullnessPer100Kcal [avgFullness] per 100 kcal of meal: high = filling for little.
 */
data class FoodSatiety(
    val name: String,
    val foodId: Int?,
    val meals: Int,
    val avgFullness: Double,
    val avgEnergy: Double?,
    val avgMealKcal: Double,
    val fullnessPer100Kcal: Double?
)

/** @property ratedMeals every meal with a fullness rating, for "N rated so far". */
data class SatietyInsights(val ratedMeals: Int, val foods: List<FoodSatiety>)

const val MIN_RATED_MEALS = 3

/**
 * Ranks foods by the average fullness of the meals they were eaten in, most filling first.
 * Ratings are per meal (date + meal type), so a food shares the credit with whatever it was
 * eaten with; that's why only foods seen in at least [minMeals] rated meals are listed.
 * Foods are matched by [FoodEntry.foodId], falling back to the saved food with the same
 * name ([foodIdByName]) and finally to the name itself for older entries. Quick Add isn't a
 * food and is left out.
 */
fun satietyInsights(
    entries: List<FoodEntry>,
    foodIdByName: Map<String, Int> = emptyMap(),
    minMeals: Int = MIN_RATED_MEALS
): SatietyInsights {
    class Meal(val fullness: Int, val energy: Int?, val kcal: Int)
    class Acc(var name: String, var latest: Long, val foodId: Int?, val meals: MutableList<Meal> = mutableListOf())

    val byFood = mutableMapOf<String, Acc>()
    var ratedMeals = 0
    entries.filter { it.mealType != QUICK_ADD }
        .groupBy { it.date to it.mealType }
        .values
        .forEach { mealEntries ->
            // Entries added after rating have no rating of their own; the meal's still applies.
            val fullness = mealEntries.firstNotNullOfOrNull { it.satiety } ?: return@forEach
            ratedMeals++
            val meal = Meal(fullness, mealEntries.firstNotNullOfOrNull { it.energyLevel }, mealEntries.sumOf { it.kcal })
            mealEntries.groupBy { e ->
                val id = e.foodId ?: foodIdByName[e.name]
                if (id != null) "id:$id" else "name:${e.name.trim().lowercase()}"
            }.forEach { (key, sameFood) ->
                // The same food twice in one meal still counts as one meal.
                val newest = sameFood.maxBy { it.time }
                val acc = byFood.getOrPut(key) {
                    Acc(newest.name, newest.time, newest.foodId ?: foodIdByName[newest.name])
                }
                if (newest.time > acc.latest) {
                    acc.name = newest.name
                    acc.latest = newest.time
                }
                acc.meals += meal
            }
        }

    val foods = byFood.values
        .filter { it.meals.size >= minMeals }
        .map { acc ->
            val avgFullness = acc.meals.map { it.fullness }.average()
            val avgKcal = acc.meals.map { it.kcal }.average()
            FoodSatiety(
                name = acc.name,
                foodId = acc.foodId,
                meals = acc.meals.size,
                avgFullness = avgFullness,
                avgEnergy = acc.meals.mapNotNull { it.energy }.takeIf { it.isNotEmpty() }?.average(),
                avgMealKcal = avgKcal,
                // Ratio of averages: one tiny "meal" (a coffee) can't blow it up.
                fullnessPer100Kcal = if (avgKcal > 0) avgFullness / avgKcal * 100 else null
            )
        }
        .sortedWith(compareByDescending<FoodSatiety> { it.avgFullness }.thenByDescending { it.meals })
    return SatietyInsights(ratedMeals, foods)
}

private const val QUICK_ADD = "Quick Add"
