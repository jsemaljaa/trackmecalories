package com.jsemalja.trackmecalories.data

import kotlin.math.abs

/**
 * The food groups Nutri-Score 2023 rates with different thresholds. Stored in
 * [CustomFood.category] by [key]; a food without one is guessed from its name ([guessCategory]).
 */
enum class FoodCategory(val key: String) {
    GENERAL("general"),
    // Drinks, including milk and drinkable dairy (moved here in 2023).
    BEVERAGE("beverage"),
    // Plain water: the only drink that can get an A.
    WATER("water"),
    // Fats, oils, nuts and seeds: rated on the share of saturated fat, not its amount.
    FAT("fat"),
    // Cheese always counts its protein.
    CHEESE("cheese"),
    // Red meat and its products: protein counts for at most 2 points.
    RED_MEAT("red_meat"),
    // Nutri-Score doesn't rate alcoholic drinks.
    ALCOHOL("alcohol");

    companion object {
        fun of(key: String?): FoodCategory? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Nutrition rating for a food: the Nutri-Score grade, a 0–100 health score to rank foods by,
 * and simple densities.
 *
 * - [grade]: Open Food Facts' official Nutri-Score when the product has one; otherwise computed
 *   here with the 2023 algorithm ([estimated] = true). Null when the food can't be rated
 *   (alcohol, a piece of unknown weight) or when the unknowns allow more than three grades.
 * - [score]: 0–100, higher is healthier. Each grade owns a band (A 80–100, B 60–79, C 40–59,
 *   D 20–39, E 0–19) and the exact Nutri-Score points place the food inside it, so foods
 *   with the same letter still rank. See [nutriScoreToScore].
 * - [bestGrade]/[worstGrade]: when sugar or saturated fat is unknown the food is rated at
 *   both extremes (none at all, or all the carbs/fat) and [score] is the midpoint. Equal to
 *   [grade] when everything is known.
 * - [proteinPer100kcal], [fiberPer100kcal]: how much of the nutrients that are usually
 *   short you get per calorie.
 * - [energyDensity]: kcal per gram; lower is more filling per calorie (Rolls, 2009).
 */
data class NutritionRating(
    val grade: Char?,
    val estimated: Boolean,
    val score: Double?,
    val bestGrade: Char?,
    val worstGrade: Char?,
    val category: FoodCategory,
    val proteinPer100kcal: Double,
    val fiberPer100kcal: Double,
    val energyDensity: Double?
) {
    /** True when missing sugar or saturated fat leaves more than one possible grade. */
    val uncertain: Boolean get() = bestGrade != worstGrade
}

fun CustomFood.rating(): NutritionRating {
    val category = FoodCategory.of(this.category) ?: guessCategory(name, unit, kcalPer100)
    // Nutri-Score is per 100 g/ml; a per-piece food needs its weight.
    val grams = if (unit == "pcs") pieceGrams?.takeIf { it > 0 } else referenceAmount
    val toPer100 = grams?.let { 100.0 / it }
    val official = nutriScore?.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'E' }

    var best: Double? = null
    var worst: Double? = null
    if (toPer100 != null && category != FoodCategory.ALCOHOL) {
        val kcal = kcalPer100 * toPer100
        fun scoreFor(sugar: Double, saturatedFat: Double): Double {
            val points = nutriScorePoints(
                category = category,
                kcal = kcal,
                sugar = sugar,
                saturatedFat = saturatedFat,
                fat = fatPer100 * toPer100,
                salt = saltPer100 * toPer100,
                protein = proteinPer100 * toPer100,
                fiber = fiberPer100 * toPer100,
                fvlPercent = fvlPercent ?: 0.0
            )
            return nutriScoreToScore(category, kcal, points)
        }
        // Unknown sugar is somewhere between none and all the carbs; same for saturated fat.
        best = scoreFor(sugarPer100?.times(toPer100) ?: 0.0, saturatedFatPer100?.times(toPer100) ?: 0.0)
        worst = scoreFor(
            (sugarPer100 ?: carbsPer100) * toPer100,
            (saturatedFatPer100 ?: fatPer100) * toPer100
        )
    }

    val computedScore = if (best != null && worst != null) (best + worst) / 2 else null
    val score = when {
        official == null -> computedScore
        // Trust the official letter; our own points only order foods inside its band.
        computedScore != null -> computedScore.coerceIn(bandFloor(official), bandFloor(official) + 19.99)
        else -> bandFloor(official) + 10
    }
    val bestGrade = best?.let(::gradeOf)
    val worstGrade = worst?.let(::gradeOf)
    val grade = official ?: computedScore?.takeIf {
        // A–C or B–D still says something; A–E doesn't.
        abs(bestGrade!! - worstGrade!!) <= 2
    }?.let(::gradeOf)

    val kcalPerGram = grams?.let { kcalPer100 / it }
    return NutritionRating(
        grade = grade,
        estimated = official == null && grade != null,
        score = score,
        bestGrade = if (official != null) official else bestGrade,
        worstGrade = if (official != null) official else worstGrade,
        category = category,
        proteinPer100kcal = if (kcalPer100 > 0) proteinPer100 / kcalPer100 * 100 else 0.0,
        fiberPer100kcal = if (kcalPer100 > 0) fiberPer100 / kcalPer100 * 100 else 0.0,
        energyDensity = kcalPerGram
    )
}

/** Healthiest first: by [NutritionRating.score], then protein and fiber per calorie; unrated last. */
val healthOrder: Comparator<NutritionRating?> = compareByDescending<NutritionRating?> { it?.score ?: -1.0 }
    .thenByDescending { (it?.proteinPer100kcal ?: 0.0) + (it?.fiberPer100kcal ?: 0.0) }

/**
 * Nutri-Score 2023 points from per-100 g (or ml) values: unfavourable minus favourable, lower
 * is healthier. [fvlPercent] is the share of fruit, vegetables, legumes and nuts (and olive,
 * rapeseed or walnut oil). Sweeteners aren't known and count as none.
 */
fun nutriScorePoints(
    category: FoodCategory,
    kcal: Double,
    sugar: Double,
    saturatedFat: Double,
    fat: Double,
    salt: Double,
    protein: Double,
    fiber: Double,
    fvlPercent: Double
): Int {
    val kj = kcal * 4.184
    val saltPts = points(salt, SALT)
    val fiberPts = points(fiber, FIBER)

    if (category == FoodCategory.BEVERAGE || category == FoodCategory.WATER) {
        val fvlPts = when {
            fvlPercent > 80 -> 6
            fvlPercent > 60 -> 4
            fvlPercent > 40 -> 2
            else -> 0
        }
        val negative = points(kj, BEVERAGE_ENERGY) + points(sugar, BEVERAGE_SUGAR) + points(saturatedFat, SATURATED_FAT) + saltPts
        return negative - points(protein, BEVERAGE_PROTEIN) - fiberPts - fvlPts
    }

    val fvlPts = when {
        fvlPercent > 80 -> 5
        fvlPercent > 60 -> 2
        fvlPercent > 40 -> 1
        else -> 0
    }
    val sugarPts = points(sugar, SUGAR)
    var proteinPts = points(protein, PROTEIN)
    return if (category == FoodCategory.FAT) {
        // Energy from saturated fat (37 kJ/g) and saturated fat as a share of all fat.
        val satRatio = if (fat > 0) saturatedFat / fat * 100 else 0.0
        val negative = points(saturatedFat * 37, FAT_SAT_ENERGY) + sugarPts + points(satRatio, FAT_SAT_RATIO) + saltPts
        if (negative >= 7) proteinPts = 0
        negative - proteinPts - fiberPts - fvlPts
    } else {
        val negative = points(kj, ENERGY) + sugarPts + points(saturatedFat, SATURATED_FAT) + saltPts
        if (category == FoodCategory.RED_MEAT) proteinPts = minOf(proteinPts, 2)
        // High-"bad" foods don't get credit for protein, except cheese.
        if (negative >= 11 && category != FoodCategory.CHEESE) proteinPts = 0
        negative - proteinPts - fiberPts - fvlPts
    }
}

/** The Nutri-Score letter for [points] in [category]. Only plain water gets an A as a drink. */
fun nutriScoreGrade(category: FoodCategory, kcal: Double, points: Int): Char =
    gradeOf(nutriScoreToScore(category, kcal, points))

/**
 * Nutri-Score points as a 0–100 score: the grade's band (A 80–100 … E 0–19), and inside it
 * evenly spread from the band's best points to its worst. Water scores 100.
 */
fun nutriScoreToScore(category: FoodCategory, kcal: Double, points: Int): Double {
    // The guard keeps flavoured "water" with sugar in it from scoring like water.
    if (category == FoodCategory.WATER && kcal < 1) return 100.0
    val bands = when (category) {
        FoodCategory.BEVERAGE, FoodCategory.WATER -> BEVERAGE_BANDS
        FoodCategory.FAT -> FAT_BANDS
        else -> GENERAL_BANDS
    }
    val (grade, range) = bands.firstOrNull { points <= it.second.last } ?: bands.last()
    val p = points.coerceIn(range.first, range.last)
    val position = (p - range.first + 0.5) / (range.last - range.first + 1)
    return bandFloor(grade) + 20 * (1 - position)
}

/** The letter whose band (A 80–100 … E 0–19) [score] falls in. */
fun gradeOf(score: Double): Char = when {
    score >= 80 -> 'A'
    score >= 60 -> 'B'
    score >= 40 -> 'C'
    score >= 20 -> 'D'
    else -> 'E'
}

private fun bandFloor(grade: Char): Double = (('E' - grade) * 20).toDouble()

/**
 * A category for a food that doesn't have one, from its name (any language) and unit: oils,
 * butter and nuts are fats; drinks are logged in ml. Only the words before "with"/"s"/"с"
 * count, so "toast with cheese" stays a general food.
 */
fun guessCategory(name: String, unit: String, kcalPer100: Double = 0.0): FoodCategory {
    val normalized = name.substringBefore('·').normalizedForSearch()
    val words = normalized.split(' ').takeWhile { it !in JOINING_WORDS }
    fun any(set: Set<String>, stems: List<String> = emptyList()) =
        words.any { w -> w in set || stems.any { w.startsWith(it) } }
    return when {
        any(FAT_WORDS, FAT_STEMS) -> FoodCategory.FAT
        any(CHEESE_WORDS) -> FoodCategory.CHEESE
        any(RED_MEAT_WORDS, RED_MEAT_STEMS) -> FoodCategory.RED_MEAT
        unit != "ml" -> FoodCategory.GENERAL
        NON_ALCOHOLIC.any { it in normalized } -> FoodCategory.BEVERAGE
        any(ALCOHOL_WORDS) -> FoodCategory.ALCOHOL
        any(WATER_WORDS) && kcalPer100 < 1 -> FoodCategory.WATER
        else -> FoodCategory.BEVERAGE
    }
}

/**
 * Category and fruit/vegetable/legume share from a Kalorické tabulky food type, e.g.
 * "vareni-a-peceni/oleje-olivove" -> fat, 100 %. Null category for an unknown type.
 */
fun ktCategory(typeUrl: String?): Pair<FoodCategory?, Double?> {
    if (typeUrl.isNullOrBlank()) return null to null
    val root = typeUrl.substringBefore('/')
    val sub = typeUrl.substringAfter('/', "")
    return when (root) {
        "alkoholicke-napoje" -> FoodCategory.ALCOHOL to null
        "nealkoholicke-napoje" -> (if (sub.startsWith("voda")) FoodCategory.WATER else FoodCategory.BEVERAGE) to null
        "mlecne-vyrobky" -> when {
            sub.startsWith("syry") -> FoodCategory.CHEESE
            sub.startsWith("masla") -> FoodCategory.FAT
            sub.startsWith("mleko") || "napoj" in sub -> FoodCategory.BEVERAGE
            else -> FoodCategory.GENERAL
        } to null
        "vareni-a-peceni" -> when {
            sub.startsWith("oleje") -> FoodCategory.FAT to (if ("olivov" in sub || "repkov" in sub) 100.0 else null)
            "tuk" in sub || "sadl" in sub || "margar" in sub -> FoodCategory.FAT to null
            else -> FoodCategory.GENERAL to null
        }
        // Plain nuts and seeds count as fruit/vegetables/legumes since 2023; coated ones don't.
        "orechy-a-semena" -> FoodCategory.FAT to (if ("ciste" in sub) 100.0 else null)
        "maso" -> (if (RED_MEAT_KT.any { it in sub }) FoodCategory.RED_MEAT else FoodCategory.GENERAL) to null
        "uzeniny-a-lahudky" -> FoodCategory.RED_MEAT to null
        "ovoce", "zelenina", "lusteniny" -> FoodCategory.GENERAL to 100.0
        else -> FoodCategory.GENERAL to null
    }
}

/** Category from Open Food Facts' categories_tags ("en:cheeses", "en:olive-oils", …), or null. */
fun offCategory(tags: List<String>): FoodCategory? {
    fun has(vararg t: String) = t.any { it in tags }
    return when {
        has("en:alcoholic-beverages") -> FoodCategory.ALCOHOL
        has("en:waters") && !has("en:flavored-waters", "en:flavoured-waters") -> FoodCategory.WATER
        has("en:cheeses") -> FoodCategory.CHEESE
        has("en:fats", "en:vegetable-oils", "en:butters", "en:nuts", "en:seeds", "en:nut-butters") -> FoodCategory.FAT
        has("en:beverages", "en:milks") -> FoodCategory.BEVERAGE
        has("en:red-meats", "en:beef", "en:pork", "en:lamb", "en:veal", "en:hams", "en:bacon", "en:salamis") -> FoodCategory.RED_MEAT
        tags.isEmpty() -> null
        else -> FoodCategory.GENERAL
    }
}

private fun points(value: Double, thresholds: DoubleArray): Int = thresholds.count { value > it }

// Points range of each grade. General foods (also cheese and red meat) and fats go from -17
// (5 fruit/veg + 5 fiber + 7 protein) to 55; drinks have no A (that's water) and reach 54.
private val GENERAL_BANDS = listOf('A' to -17..0, 'B' to 1..2, 'C' to 3..10, 'D' to 11..18, 'E' to 19..55)
private val FAT_BANDS = listOf('A' to -17..-6, 'B' to -5..2, 'C' to 3..10, 'D' to 11..18, 'E' to 19..55)
private val BEVERAGE_BANDS = listOf('B' to -18..2, 'C' to 3..6, 'D' to 7..9, 'E' to 10..54)

private val ENERGY = DoubleArray(10) { 335.0 * (it + 1) }
private val SUGAR = doubleArrayOf(3.4, 6.8, 10.0, 14.0, 17.0, 20.0, 24.0, 27.0, 31.0, 34.0, 37.0, 41.0, 44.0, 48.0, 51.0)
private val SATURATED_FAT = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0)
private val SALT = DoubleArray(20) { 0.2 * (it + 1) }
private val PROTEIN = doubleArrayOf(2.4, 4.8, 7.2, 9.6, 12.0, 14.0, 17.0)
private val FIBER = doubleArrayOf(3.0, 4.1, 5.2, 6.3, 7.4)
private val FAT_SAT_ENERGY = DoubleArray(10) { 120.0 * (it + 1) }
private val FAT_SAT_RATIO = doubleArrayOf(10.0, 16.0, 22.0, 28.0, 34.0, 40.0, 46.0, 52.0, 58.0, 64.0)
private val BEVERAGE_ENERGY = doubleArrayOf(30.0, 90.0, 150.0, 210.0, 240.0, 270.0, 300.0, 330.0, 360.0, 390.0)
private val BEVERAGE_SUGAR = doubleArrayOf(0.5, 2.0, 3.5, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0)
private val BEVERAGE_PROTEIN = doubleArrayOf(1.2, 1.5, 1.8, 2.1, 2.4, 2.7, 3.0)

private val RED_MEAT_KT = listOf("hovezi", "veprove", "jehneci", "teleci", "zverina", "kozi", "konske")

// Name keywords, compared after normalizedForSearch (no accents, lowercase; й -> и, ё -> е).
private fun normalizedSet(vararg words: String) = words.map { it.normalizedForSearch() }.toSet()
private fun normalizedList(vararg stems: String) = stems.map { it.normalizedForSearch() }

private val JOINING_WORDS = normalizedSet("s", "se", "with", "in", "v", "ve", "na", "с", "со", "в", "на")
private val FAT_WORDS = normalizedSet(
    "oil", "oils", "butter", "ghee", "lard", "margarine", "nuts", "almonds", "walnuts", "hazelnuts", "cashews",
    "peanuts", "pistachios", "seeds", "máslo", "másla", "sádlo", "ořech", "ořechy", "ořechů", "mandle",
    "arašídy", "kešu", "pistácie", "semínka", "chia", "масло", "масла", "сало", "орех", "орехи", "миндаль",
    "кешью", "фисташки", "арахис", "семечки", "семена"
)
private val FAT_STEMS = normalizedList("olej", "margarín", "margarin", "маргарин")
private val CHEESE_WORDS = normalizedSet(
    "cheese", "sýr", "sýry", "sýru", "sýrem", "cheddar", "gouda", "eidam", "edam", "emmental", "ementál",
    "mozzarella", "parmesan", "parmezán", "feta", "brie", "camembert", "hermelín", "niva", "halloumi",
    "ricotta", "mascarpone", "сыр", "сыры", "сыра", "моцарелла", "пармезан", "фета", "брынза"
)
private val RED_MEAT_WORDS = normalizedSet(
    "beef", "pork", "lamb", "veal", "ham", "bacon", "salami", "sausage", "sausages", "šunka", "slanina",
    "salám", "klobása", "párek", "párky", "ветчина", "бекон", "салями", "сосиски", "сардельки"
)
private val RED_MEAT_STEMS = normalizedList("hověz", "vepř", "jehněč", "telecí", "говя", "свин", "баранин", "телят", "колбас")
// Checked against the whole name: "non alcoholic" is two words once normalized.
private val NON_ALCOHOLIC = normalizedList("nealko", "alkoholfrei", "non alcoholic", "alcohol free", "безалког")
private val ALCOHOL_WORDS = normalizedSet(
    "beer", "wine", "pivo", "víno", "vodka", "rum", "whisky", "whiskey", "gin", "likér", "cider", "prosecco",
    "champagne", "пиво", "вино", "водка", "виски", "коньяк", "ликер", "сидр", "шампанское"
)
private val WATER_WORDS = normalizedSet("water", "voda", "вода", "минералка", "minerálka")
