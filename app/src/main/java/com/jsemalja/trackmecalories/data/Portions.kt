package com.jsemalja.trackmecalories.data

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A natural serving of a food: "medium" = 51 g of egg. [key] is one of [PORTION_KEYS]; it's
 * stored (not the label) so a log stays readable after switching the app language.
 */
data class Portion(val key: String, val grams: Double)

/** Every portion key the app has a label for (strings.xml: portion_<key>). */
val PORTION_KEYS = listOf("small", "medium", "large", "extra_large", "piece", "slice", "clove", "tbsp", "tsp", "glass", "cup")

/** "small=42;medium=51" (as stored in [CustomFood.portions]) -> portions, in order. Unknown keys are dropped. */
fun parsePortions(encoded: String?): List<Portion> =
    encoded.orEmpty().split(';').mapNotNull { part ->
        val pieces = part.split('=', limit = 2)
        if (pieces.size != 2) return@mapNotNull null
        val key = pieces[0].trim()
        val grams = pieces[1].trim().toDoubleOrNull()
        if (key in PORTION_KEYS && grams != null && grams > 0) Portion(key, grams) else null
    }

fun encodePortions(portions: List<Portion>): String? =
    portions.takeIf { it.isNotEmpty() }?.joinToString(";") { "${it.key}=${it.grams}" }

val CustomFood.portionList: List<Portion> get() = parsePortions(portions)

/**
 * The portion and count to preselect when logging [food], or null to start in grams.
 * A food that's never been logged starts at one of its usual portion. After that it starts
 * from last time, but only if the remembered amount is a whole or half number of that portion
 * (a suggested "typical amount" of 137 g shouldn't turn into "2.7 medium").
 */
fun initialPortion(food: CustomFood): Pair<Portion, Double>? {
    val portion = food.portionList.firstOrNull { it.key == food.defaultPortion } ?: return null
    val amount = food.defaultAmount ?: return portion to 1.0
    val count = amount / portion.grams
    val halves = (count * 2).roundToLong() / 2.0
    return if (halves > 0 && abs(count - halves) < 0.05) portion to halves else null
}
