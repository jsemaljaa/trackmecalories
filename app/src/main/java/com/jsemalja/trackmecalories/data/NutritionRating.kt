package com.jsemalja.trackmecalories.data

/**
 * Nutrition rating for a food: the Nutri-Score grade plus simple densities.
 *
 * - [grade]: Open Food Facts' official Nutri-Score when the product has one; otherwise
 *   computed here with the 2023 algorithm when sugar and saturated fat are known
 *   ([estimated] = true — fruit/vegetable/legume content isn't known, so it's taken as 0,
 *   which can only make a grade worse, never better).
 * - [proteinPer100kcal], [fiberPer100kcal]: how much of the nutrients that are usually
 *   short you get per calorie.
 * - [energyDensity]: kcal per gram; lower is more filling per calorie (Rolls, 2009).
 */
data class NutritionRating(
    val grade: Char?,
    val estimated: Boolean,
    val proteinPer100kcal: Double,
    val fiberPer100kcal: Double,
    val energyDensity: Double?
)

fun CustomFood.rating(): NutritionRating {
    val official = nutriScore?.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'E' }
    val computed = if (official == null && unit != "pcs") {
        nutriScore2023(
            kcal = kcalPer100,
            sugar = sugarPer100 ?: Double.NaN,
            saturatedFat = saturatedFatPer100 ?: Double.NaN,
            salt = saltPer100,
            protein = proteinPer100,
            fiber = fiberPer100,
            beverage = unit == "ml"
        )
    } else null
    return NutritionRating(
        grade = official ?: computed,
        estimated = official == null && computed != null,
        proteinPer100kcal = if (kcalPer100 > 0) proteinPer100 / kcalPer100 * 100 else 0.0,
        fiberPer100kcal = if (kcalPer100 > 0) fiberPer100 / kcalPer100 * 100 else 0.0,
        // Per 100 g for g/ml foods; a piece's weight is unknown.
        energyDensity = if (unit == "pcs") null else kcalPer100 / 100
    )
}

/**
 * Nutri-Score (2023 update) from per-100 g (or ml) values. Returns null when sugar or
 * saturated fat is unknown (NaN) — those decide too much of the score to guess.
 * Fruit/vegetable/legume content and sweeteners aren't known and count as none.
 */
fun nutriScore2023(
    kcal: Double,
    sugar: Double,
    saturatedFat: Double,
    salt: Double,
    protein: Double,
    fiber: Double,
    beverage: Boolean
): Char? {
    if (sugar.isNaN() || saturatedFat.isNaN()) return null
    val kj = kcal * 4.184

    fun points(value: Double, thresholds: DoubleArray): Int = thresholds.count { value > it }

    val satFatPts = points(saturatedFat, doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0))
    val saltPts = points(salt, DoubleArray(20) { 0.2 * (it + 1) })
    val fiberPts = points(fiber, doubleArrayOf(3.0, 4.1, 5.2, 6.3, 7.4))

    return if (!beverage) {
        val energyPts = points(kj, DoubleArray(10) { 335.0 * (it + 1) })
        val sugarPts = points(sugar, doubleArrayOf(3.4, 6.8, 10.0, 14.0, 17.0, 20.0, 24.0, 27.0, 31.0, 34.0, 37.0, 41.0, 44.0, 48.0, 51.0))
        val proteinPts = points(protein, doubleArrayOf(2.4, 4.8, 7.2, 9.6, 12.0, 14.0, 17.0))
        val negative = energyPts + sugarPts + satFatPts + saltPts
        // High-"bad" foods don't get credit for protein (2023 rule for general foods).
        val positive = fiberPts + if (negative >= 11) 0 else proteinPts
        when (negative - positive) {
            in Int.MIN_VALUE..0 -> 'A'
            in 1..2 -> 'B'
            in 3..10 -> 'C'
            in 11..18 -> 'D'
            else -> 'E'
        }
    } else {
        val energyPts = points(kj, doubleArrayOf(30.0, 90.0, 150.0, 210.0, 240.0, 270.0, 300.0, 330.0, 360.0, 390.0))
        val sugarPts = points(sugar, doubleArrayOf(0.5, 2.0, 3.5, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0))
        val proteinPts = points(protein, doubleArrayOf(1.2, 1.5, 1.8, 2.1, 2.4, 2.7, 3.0))
        val score = energyPts + sugarPts + satFatPts + saltPts - fiberPts - proteinPts
        // Grade A is reserved for water; any other drink starts at B.
        when {
            kcal <= 0.5 && sugar <= 0.0 && saturatedFat <= 0.0 && salt <= 0.01 -> 'A'
            score <= 2 -> 'B'
            score <= 6 -> 'C'
            score <= 9 -> 'D'
            else -> 'E'
        }
    }
}
