package com.jsemalja.trackmecalories.data

/**
 * Data class representing food nutritional information per 100g.
 */
data class FoodInfo(
    val name: String,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double,
    val saltPer100: Double,
    val barcode: String? = null,
    val unit: String = "g",
    // The amount kcalPer100 etc. are relative to: 100 for g/ml, 1 for "pcs".
    val referenceAmount: Double = 100.0,
    val brand: String? = null,
    val quantity: String? = null,
    // Sold in Czechia per Open Food Facts; ranked higher, but nothing else is hidden.
    val isCzech: Boolean = false,
    // For the nutrition rating; null when the source doesn't say.
    val sugarPer100: Double? = null,
    val saturatedFatPer100: Double? = null,
    // Official Nutri-Score grade (a-e) from Open Food Facts.
    val nutriScore: String? = null,
    // Nutri-Score food group ([FoodCategory.key]) and % fruit/vegetables/legumes, if known.
    val category: String? = null,
    val fvlPercent: Double? = null
)

/**
 * A search hit from kaloricketabulky.cz. The search endpoint only returns kcal/100; macros
 * come from [KalorickeTabulkyRepository.macros] and the serving size from
 * [KalorickeTabulkyRepository.resolve].
 */
data class KtHit(
    val id: String,
    val title: String,
    val brand: String?,
    val kcalPer100: Double,
    val unit: String
)

/** Per-100 g/ml macros of a KT food. */
data class KtMacros(
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val salt: Double,
    val sugar: Double?,
    val saturatedFat: Double?,
    // KT's food type, e.g. "vareni-a-peceni/oleje-olivove" (see [ktCategory]).
    val typeUrl: String? = null
)
