package com.jsemalja.trackmecalories.data

/** Nutrient amounts (not per-100). */
data class NutrientTotals(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val salt: Double = 0.0
) {
    operator fun plus(o: NutrientTotals) = NutrientTotals(
        kcal + o.kcal, protein + o.protein, carbs + o.carbs, fat + o.fat, fiber + o.fiber, salt + o.salt
    )

    operator fun times(f: Double) = NutrientTotals(kcal * f, protein * f, carbs * f, fat * f, fiber * f, salt * f)
}

fun RecipeIngredient.totals(): NutrientTotals {
    val ratio = if (referenceAmount > 0) amount / referenceAmount else 0.0
    return NutrientTotals(kcalPer100, proteinPer100, carbsPer100, fatPer100, fiberPer100, saltPer100) * ratio
}

fun List<RecipeIngredient>.totals(): NutrientTotals = fold(NutrientTotals()) { acc, i -> acc + i.totals() }

/** Combined raw weight of the ingredients, counting only those whose weight is known. */
fun List<RecipeIngredient>.rawWeightG(): Double = sumOf { it.amount * it.gramsPerUnit }

/**
 * Estimated weight after cooking: each ingredient's raw weight times its cooking factor
 * (200 g dry rice ×2.5 = 500 g cooked). Weighing the pot beats this, see [dishWeightG].
 */
fun List<RecipeIngredient>.estimatedCookedWeightG(): Double = sumOf { it.amount * it.gramsPerUnit * it.cookFactor }

/** Weight the per-100 g values are based on: the weighed pot if entered, else the estimate. */
fun Recipe.dishWeightG(ingredients: List<RecipeIngredient>): Double =
    cookedWeightG?.takeIf { it > 0 } ?: ingredients.estimatedCookedWeightG()

/** Typical raw -> cooked weight factors (water absorbed or lost in cooking). */
enum class CookFactor(@androidx.annotation.StringRes val label: Int, val factor: Double) {
    NONE(com.jsemalja.trackmecalories.R.string.cook_none, 1.0),
    RICE(com.jsemalja.trackmecalories.R.string.cook_rice, 2.5),
    PASTA(com.jsemalja.trackmecalories.R.string.cook_pasta, 2.2),
    GRAINS(com.jsemalja.trackmecalories.R.string.cook_grains, 2.6),
    LEGUMES(com.jsemalja.trackmecalories.R.string.cook_legumes, 2.4),
    MEAT(com.jsemalja.trackmecalories.R.string.cook_meat, 0.75),
    FISH(com.jsemalja.trackmecalories.R.string.cook_fish, 0.8),
    VEGETABLES(com.jsemalja.trackmecalories.R.string.cook_vegetables, 0.8)
}

/** Makes an ingredient (snapshot of [food]) of [amount] in the food's unit. */
fun CustomFood.toIngredient(amount: Double) = RecipeIngredient(
    recipeId = 0,
    position = 0,
    name = name,
    amount = amount,
    unit = unit,
    referenceAmount = referenceAmount,
    // ml counts as g (close enough for cooking); a piece's weight is unknown.
    gramsPerUnit = if (unit == "pcs") 0.0 else 1.0,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
    saltPer100 = saltPer100
)

/**
 * The loggable food for a recipe: per serving ("pcs") or per 100 g of the finished dish.
 * [existing] keeps the saved food's id, favorite flag and recency.
 */
fun Recipe.toCustomFood(ingredients: List<RecipeIngredient>, existing: CustomFood?): CustomFood {
    val totals = ingredients.totals()
    val perServing = logBy != "g"
    val per = if (perServing) {
        totals * (1.0 / servings.coerceAtLeast(0.01))
    } else {
        val weight = dishWeightG(ingredients)
        if (weight > 0) totals * (100.0 / weight) else NutrientTotals()
    }
    val base = existing ?: CustomFood(name = name, kcalPer100 = 0.0, proteinPer100 = 0.0, carbsPer100 = 0.0, fatPer100 = 0.0, fiberPer100 = 0.0, saltPer100 = 0.0)
    return base.copy(
        name = name,
        kcalPer100 = per.kcal,
        proteinPer100 = per.protein,
        carbsPer100 = per.carbs,
        fatPer100 = per.fat,
        fiberPer100 = per.fiber,
        saltPer100 = per.salt,
        unit = if (perServing) "pcs" else "g",
        referenceAmount = if (perServing) 1.0 else 100.0,
        lastUsedAt = base.lastUsedAt.takeIf { it > 0 } ?: System.currentTimeMillis()
    )
}
