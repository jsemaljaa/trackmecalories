package com.jsemalja.trackmecalories.data

import android.content.res.AssetManager
import android.util.Log
import kotlinx.serialization.json.*
import java.util.Locale

/**
 * A raw or basic food ("Egg, raw", "Rice, cooked") from the bundled list in
 * assets/base_foods.json, built by tools/build_base_foods.py from Fineli (THL) and USDA
 * FoodData Central. Values are per 100 g (or ml); [portions] are its natural servings.
 */
data class BaseFood(
    val key: String,
    val names: Map<String, String>, // language ("en", "ru", "cs") -> name
    val aliases: String,
    val unit: String,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val fiber: Double,
    val salt: Double,
    val sugar: Double?,
    val saturatedFat: Double?,
    val portions: List<Portion>,
    val defaultPortion: String?
) {
    /** The name in the app's [language], English otherwise. */
    fun name(language: String): String = names[language] ?: names.getValue("en")

    fun toCustomFood(language: String) = CustomFood(
        // Pseudo-barcode, like restaurant items: logging it again finds the same saved food.
        barcode = "base:$key",
        name = name(language),
        kcalPer100 = kcal,
        proteinPer100 = protein,
        carbsPer100 = carbs,
        fatPer100 = fat,
        fiberPer100 = fiber,
        saltPer100 = salt,
        unit = unit,
        referenceAmount = 100.0,
        sugarPer100 = sugar,
        saturatedFatPer100 = saturatedFat,
        portions = encodePortions(portions),
        defaultPortion = defaultPortion
    )
}

fun parseBaseFoods(json: String): List<BaseFood> =
    Json.parseToJsonElement(json).jsonObject.getValue("foods").jsonArray.map { element ->
        val obj = element.jsonObject
        fun num(key: String) = obj[key]?.jsonPrimitive?.doubleOrNull
        BaseFood(
            key = obj.getValue("key").jsonPrimitive.content,
            names = listOf("en", "ru", "cs").mapNotNull { lang -> obj[lang]?.jsonPrimitive?.contentOrNull?.let { lang to it } }.toMap(),
            aliases = obj["aliases"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            unit = obj["unit"]?.jsonPrimitive?.contentOrNull ?: "g",
            kcal = num("kcal") ?: 0.0,
            protein = num("protein") ?: 0.0,
            carbs = num("carbs") ?: 0.0,
            fat = num("fat") ?: 0.0,
            fiber = num("fiber") ?: 0.0,
            salt = num("salt") ?: 0.0,
            sugar = num("sugar"),
            saturatedFat = num("satFat"),
            portions = obj["portions"]?.jsonObject.orEmpty().mapNotNull { (key, grams) ->
                grams.jsonPrimitive.doubleOrNull?.takeIf { key in PORTION_KEYS }?.let { Portion(key, it) }
            },
            defaultPortion = obj["defaultPortion"]?.jsonPrimitive?.contentOrNull
        )
    }

/**
 * How well [food] matches [tokens] in any of its languages, or null. A Russian, English or
 * Czech query all find the same egg. The preparation after the comma ("Egg, raw") counts as
 * extra words rather than part of the name, so it doesn't make raw foods look less concise
 * than "Egg, hard-boiled" variants; the list order decides between them instead.
 */
fun baseFoodScore(food: BaseFood, tokens: List<String>): Double? =
    food.names.values.mapNotNull { name ->
        SearchMatcher.score(tokens, name.substringBefore(','), null, "${name.substringAfter(',', "")} ${food.aliases}")
    }.maxOrNull()

/**
 * Base foods matching [query], as search results named in [language]. They're boosted above
 * every other source: someone typing "chicken breast" or "морковь" wants the plain food first,
 * not a ready meal that happens to contain it. Within equally good matches the list's own
 * order wins, which puts raw before cooked.
 */
fun searchBaseFoods(foods: List<BaseFood>, query: String, language: String): List<SearchHit> {
    val tokens = SearchMatcher.tokens(query)
    if (tokens.isEmpty()) return emptyList()
    return foods.withIndex().mapNotNull { (i, food) ->
        val score = baseFoodScore(food, tokens) ?: return@mapNotNull null
        SearchHit(
            key = "base:${food.key}",
            name = food.name(language),
            brand = null,
            detail = null,
            kcal = food.kcal,
            protein = food.protein,
            carbs = food.carbs,
            fat = food.fat,
            unit = food.unit,
            source = SearchSource.BASIC,
            score = score + BASE_FOOD_BOOST - 0.0001 * i,
            food = food.toCustomFood(language)
        )
    }
}

/**
 * The Czech name of the base food best matching a Cyrillic [query], for asking the Czech
 * online databases (which don't understand Russian): "куриная грудка" -> "Kuřecí prsa".
 * Null for non-Cyrillic queries or when nothing matches.
 */
fun czechQueryFor(foods: List<BaseFood>, query: String): String? {
    if (!isCyrillic(query)) return null
    val tokens = SearchMatcher.tokens(query)
    if (tokens.isEmpty()) return null
    val best = foods.withIndex()
        .mapNotNull { (i, food) -> baseFoodScore(food, tokens)?.let { Triple(food, it, i) } }
        .maxWithOrNull(compareBy<Triple<BaseFood, Double, Int>> { it.second }.thenByDescending { it.third })
        ?.first ?: return null
    // Drop the preparation ("Kuřecí prsa, syrová" -> "Kuřecí prsa") to get every product.
    return best.names["cs"]?.substringBefore(',')?.trim()?.takeIf { it.isNotEmpty() }
}

private const val BASE_FOOD_BOOST = 2.0

/** The bundled base-food list, parsed on first use (callers run on Dispatchers.Default). */
class BaseFoods(private val assets: AssetManager) {
    private val foods: List<BaseFood> by lazy {
        try {
            parseBaseFoods(assets.open("base_foods.json").bufferedReader().use { it.readText() })
        } catch (e: Exception) {
            Log.e("BaseFoods", "Couldn't load base foods", e)
            emptyList()
        }
    }

    // Names follow the app's language; Czech isn't a UI language, so those users get English.
    private fun language() = Locale.getDefault().language.takeIf { it == "ru" } ?: "en"

    fun search(query: String): List<SearchHit> = searchBaseFoods(foods, query, language())

    fun czechQuery(query: String): String? = czechQueryFor(foods, query)
}
