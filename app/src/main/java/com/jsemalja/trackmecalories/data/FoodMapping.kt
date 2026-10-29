package com.jsemalja.trackmecalories.data

import kotlin.math.roundToInt

/**
 * Builds a logged [FoodEntry] from a saved food template, scaling its per-reference-amount
 * macros by how much was actually consumed. Centralizes the amount/referenceAmount ratio so
 * every add-flow (scan, search, recents, manual) handles "pcs" foods the same way grams/ml do.
 */
fun CustomFood.toEntry(
    amount: Double,
    mealType: String,
    date: String,
    confidence: Confidence = Confidence.ESTIMATED,
    // Set when [amount] was entered as a number of this portion.
    portion: Portion? = null
): FoodEntry {
    val ratio = amount / referenceAmount
    return FoodEntry(
        name = name,
        kcal = (kcalPer100 * ratio).roundToInt(),
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
        foodId = id.takeIf { it != 0 },
        portion = portion?.key,
        portionGrams = portion?.grams
    )
}

/**
 * [this] saved food with the rating data it's missing filled in from [bundled], the same food
 * in the app's own lists (saved before those lists had it). Null when nothing changes.
 * Only empty fields are filled, so nothing the user edited is overwritten.
 */
fun CustomFood.withRatingDataFrom(bundled: CustomFood): CustomFood? {
    val filled = copy(
        category = category ?: bundled.category,
        fvlPercent = fvlPercent ?: bundled.fvlPercent,
        // Totals per the same reference amount (both per 100 g, or both per piece).
        sugarPer100 = sugarPer100 ?: bundled.sugarPer100?.takeIf { unit == bundled.unit && referenceAmount == bundled.referenceAmount },
        saturatedFatPer100 = saturatedFatPer100
            ?: bundled.saturatedFatPer100?.takeIf { unit == bundled.unit && referenceAmount == bundled.referenceAmount }
    )
    return filled.takeIf { it != this }
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
    referenceAmount = referenceAmount,
    // People usually eat the whole package, not "100 g".
    defaultAmount = packageAmount(quantity, unit),
    // A per-piece food's quantity is its serving, e.g. "kus (215 g)" from Kalorické tabulky.
    pieceGrams = if (unit == "pcs") quantityGrams(quantity) else null,
    sugarPer100 = sugarPer100,
    saturatedFatPer100 = saturatedFatPer100,
    nutriScore = nutriScore,
    category = category,
    fvlPercent = fvlPercent
)

/**
 * The amount in one package, in [unit], from Open Food Facts' free-text quantity:
 * "350 g" -> 350, "0,5 l" -> 500 (ml), "4 x 125 g" -> 125 (one pot of a multipack).
 * Null when there's nothing usable (or the food is counted in pieces).
 */
fun packageAmount(quantity: String?, unit: String): Double? =
    if (unit == "pcs") null else quantityGrams(quantity)

/**
 * Grams (or ml) in a free-text quantity or serving: "350 g" -> 350, "0,5 l" -> 500,
 * "4 x 125 g" -> 125, "kus (215 g)" -> 215. Null when there's no usable amount.
 */
fun quantityGrams(quantity: String?): Double? {
    if (quantity.isNullOrBlank()) return null
    val m = PACKAGE_REGEX.find(quantity.lowercase()) ?: return null
    val value = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
    val grams = when (m.groupValues[3]) {
        "kg", "l" -> value * 1000
        "cl" -> value * 10
        "dl" -> value * 100
        else -> value // g, ml
    }
    return grams.takeIf { it in 1.0..5000.0 }
}

// Optional "4 x " multipack prefix, then the per-item amount and its unit.
private val PACKAGE_REGEX = Regex("""(?:(\d+)\s*[x×]\s*)?(\d+(?:[.,]\d+)?)\s*(kg|g|ml|cl|dl|l)\b""")

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
    referenceAmount = referenceAmount,
    quantity = quantity,
    sugarPer100 = sugarPer100,
    saturatedFatPer100 = saturatedFatPer100,
    nutriScore = nutriScore,
    category = category,
    fvlPercent = fvlPercent
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
        referenceAmount = referenceAmount,
        quantity = quantity,
        sugarPer100 = sugarPer100,
        saturatedFatPer100 = saturatedFatPer100,
        nutriScore = nutriScore,
        category = category,
        fvlPercent = fvlPercent
    )
}
