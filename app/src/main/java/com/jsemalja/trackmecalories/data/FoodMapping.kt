package com.jsemalja.trackmecalories.data

/**
 * Builds a logged [FoodEntry] from a saved food template, scaling its per-reference-amount
 * macros by how much was actually consumed. Centralizes the amount/referenceAmount ratio so
 * every add-flow (scan, search, recents, manual) handles "pcs" foods the same way grams/ml do.
 */
fun CustomFood.toEntry(
    amount: Double,
    mealType: String,
    date: String,
    confidence: Confidence = Confidence.ESTIMATED
): FoodEntry {
    val ratio = amount / referenceAmount
    return FoodEntry(
        name = name,
        kcal = (kcalPer100 * ratio).toInt(),
        protein = proteinPer100 * ratio,
        carbs = carbsPer100 * ratio,
        fat = fatPer100 * ratio,
        fiber = fiberPer100 * ratio,
        salt = saltPer100 * ratio,
        amount = amount,
        unit = unit,
        referenceAmount = referenceAmount,
        mealType = mealType,
        date = date,
        confidence = confidence.name,
        foodId = id.takeIf { it != 0 }
    )
}

/** "Tvaroh · Tatra" — the brand disambiguates otherwise-identical generic names in logs/Recents. */
val FoodInfo.displayName: String
    get() = if (brand.isNullOrBlank() || name.contains(brand, ignoreCase = true)) name else "$name · $brand"

fun FoodInfo.toCustomFood(): CustomFood = CustomFood(
    barcode = barcode,
    name = displayName,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
    saltPer100 = saltPer100,
    unit = unit,
    referenceAmount = referenceAmount
)

fun FoodCache.toFoodInfo(): FoodInfo = FoodInfo(
    name = name,
    kcalPer100 = kcalPer100,
    proteinPer100 = proteinPer100,
    carbsPer100 = carbsPer100,
    fatPer100 = fatPer100,
    fiberPer100 = fiberPer100,
    saltPer100 = saltPer100,
    barcode = barcode,
    unit = unit,
    referenceAmount = referenceAmount
)

/** Only barcoded products are worth caching — search results without a barcode can't be re-looked-up. */
fun FoodInfo.toCache(): FoodCache? {
    val code = barcode ?: return null
    return FoodCache(
        barcode = code,
        // The cache has no brand column, so store the brand-qualified name.
        name = displayName,
        kcalPer100 = kcalPer100,
        proteinPer100 = proteinPer100,
        carbsPer100 = carbsPer100,
        fatPer100 = fatPer100,
        fiberPer100 = fiberPer100,
        saltPer100 = saltPer100,
        unit = unit,
        referenceAmount = referenceAmount
    )
}
