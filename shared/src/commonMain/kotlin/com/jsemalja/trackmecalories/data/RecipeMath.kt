package com.jsemalja.trackmecalories.data

import kotlin.math.abs
import kotlin.math.roundToLong

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

/** Whether any ingredient changes weight when cooked, so the estimate differs from the raw weight. */
fun List<RecipeIngredient>.hasCookFactors(): Boolean = any { kotlin.math.abs(it.cookFactor - 1.0) > 0.001 }

/** Ingredients counted in pieces whose piece weight is unknown, so they're missing from the dish weight. */
fun List<RecipeIngredient>.missingWeights(): List<RecipeIngredient> = filter { it.gramsPerUnit <= 0.0 }

/**
 * Whether a "pcs" ingredient reads best as a count ("6 pcs") rather than in grams: whole or
 * half pieces, or no piece weight to convert with. 6 g of a 500 g tub is "6 g", not "0.01 pcs".
 */
fun RecipeIngredient.inPieces(): Boolean {
    if (unit != "pcs") return false
    if (gramsPerUnit <= 0.0) return true
    val halves = amount * 2
    return amount >= 0.5 && abs(halves - halves.roundToLong()) < 0.01
}

/** Nutrients per 100 g of a dish weighing [weightG]; null while the weight is unknown. */
fun NutrientTotals.per100g(weightG: Double): NutrientTotals? = if (weightG > 0) this * (100.0 / weightG) else null

/** Typical raw -> cooked weight factors (water absorbed or lost in cooking). */
enum class CookFactor(val factor: Double) {
    NONE(1.0),
    RICE(2.5),
    PASTA(2.2),
    GRAINS(2.6),
    LEGUMES(2.4),
    MEAT(0.75),
    FISH(0.8),
    VEGETABLES(0.8)
}

/** Makes an ingredient (snapshot of [food]) of [amount] in the food's unit. */
fun CustomFood.toIngredient(amount: Double) = RecipeIngredient(
    recipeId = 0,
    position = 0,
    name = name,
    amount = amount,
    unit = unit,
    referenceAmount = referenceAmount,
    // ml counts as g (close enough for cooking); 0 when a piece's weight is unknown.
    gramsPerUnit = if (unit == "pcs") pieceGrams ?: 0.0 else 1.0,
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
        totals.per100g(dishWeightG(ingredients)) ?: NutrientTotals()
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
        // A serving's weight, when the dish's is known, so it can also be logged in grams.
        pieceGrams = dishWeightG(ingredients).takeIf { perServing && it > 0 }?.div(servings.coerceAtLeast(0.01)) ?: base.pieceGrams,
        lastUsedAt = base.lastUsedAt.takeIf { it > 0 } ?: currentTimeMillis()
    )
}
