package com.jsemalja.trackmecalories.data

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
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
 * The portion (or [unitPortion], e.g. grams of a burger) and count to preselect when logging
 * [food], or null to start in the food's own unit.
 * A food that's never been logged starts at one of its usual portion. After that it starts
 * from last time, but only if the remembered amount is a whole or half number of that portion
 * (a suggested "typical amount" of 137 g shouldn't turn into "2.7 medium").
 */
fun initialPortion(food: CustomFood): Pair<Portion, Double>? {
    val key = food.defaultPortion ?: return null
    val portion = food.portionList.firstOrNull { it.key == key }
        ?: unitPortion(food, key)?.takeIf { key != food.logUnit }
        ?: return null
    val amount = food.defaultAmount ?: return portion to 1.0
    val count = amount / portion.grams
    // Grams and millilitres are weighed, not counted: any amount is fine.
    if (key in WEIGHT_UNITS) return portion to count
    val halves = (count * 2).roundToLong() / 2.0
    return if (halves > 0 && abs(count - halves) < 0.05) portion to halves else null
}

/** The units the log sheet's unit picker offers for every food, in menu order. */
val LOG_UNITS = listOf("piece", "g", "ml", "tsp", "tbsp")

/** Log units that are amounts of weight/volume rather than a count of something. */
val WEIGHT_UNITS = setOf("g", "ml")

/** The food's own unit as a [LOG_UNITS] key. */
val CustomFood.logUnit: String get() = if (unit == "pcs") "piece" else unit

/**
 * Whether logging [food] in [key] needs the weight of one piece: weight units of a food
 * counted in pieces, or pieces of a food counted by weight. A food's own portion of that
 * name ("tbsp=13.5" for oil) is used instead when it has one.
 */
fun usesPieceWeight(food: CustomFood, key: String): Boolean =
    key in LOG_UNITS && key != food.logUnit && food.portionList.none { it.key == key } &&
        (food.unit == "pcs" || key == "piece")

/**
 * [key] (one of [LOG_UNITS]) as a [Portion] of [food]: how much of the food's own unit one
 * of it is. The food's own unit is 1. Null when that needs the weight of one piece
 * ([pieceGrams]) and it isn't known. Millilitres count as grams, and a spoon holds 5 / 15 ml,
 * unless the food has its own spoon portion.
 */
fun unitPortion(food: CustomFood, key: String, pieceGrams: Double? = food.pieceGrams): Portion? {
    if (key == food.logUnit) return Portion(key, 1.0)
    food.portionList.firstOrNull { it.key == key }?.let { return it }
    val piece = pieceGrams?.takeIf { it > 0 }
    val grams = when (key) {
        "g", "ml" -> 1.0
        "tsp" -> TSP_ML
        "tbsp" -> TBSP_ML
        "piece" -> piece
        else -> null
    } ?: return null
    return if (food.unit == "pcs") piece?.let { Portion(key, grams / it) } else Portion(key, grams)
}

/** Grams (ml) in one [key] of [food], when known: "1 tbsp · 15 g", "1 pcs · 215 g". */
fun unitGrams(food: CustomFood, key: String, pieceGrams: Double? = food.pieceGrams): Double? {
    val portion = unitPortion(food, key, pieceGrams) ?: return null
    return if (food.unit == "pcs") pieceGrams?.takeIf { it > 0 }?.times(portion.grams) else portion.grams
}

const val TSP_ML = 5.0
const val TBSP_ML = 15.0

/**
 * [amount] of the food's unit as a number of [portion] (null = the food's own unit), for
 * keeping the same quantity when switching units: whole grams/ml, tenths of anything counted.
 */
fun amountIn(amount: Double, portion: Portion?, counting: Boolean): Double {
    val count = portion?.let { amount / it.grams } ?: amount
    val step = if (counting) 0.1 else 1.0
    return maxOf(step, Math.round(count / step) * step).let { Math.round(it * 10) / 10.0 }
}

/**
 * The amount after one tap of the log sheet's − / +: portions and pieces ([counting]) step by
 * one, grams and millilitres by 10, landing on round numbers (152 g → 160 g or 150 g).
 * Never goes below one step; an amount already smaller than that stays put on −.
 */
fun stepAmount(current: Double, counting: Boolean, up: Boolean): Double {
    val step = if (counting) 1.0 else 10.0
    if (up) return floor(current / step) * step + step
    val next = ceil(current / step) * step - step
    return maxOf(next, minOf(current, step))
}
