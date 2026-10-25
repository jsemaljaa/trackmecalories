package com.jsemalja.trackmecalories.data

import android.content.res.AssetManager
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import kotlinx.serialization.json.*
import java.text.Normalizer

/**
 * Restaurant menus bundled with the app (currently McDonald's ČR, scraped from the official
 * nutrition tables by tools/scrape_mcdonalds_cz.py). Values are per portion, so every item
 * is a "pcs" food. Searching is offline and accent-insensitive ("kureci" finds "Kuřecí").
 */
class RestaurantMenu(private val assets: AssetManager) {
    private class Item(val info: FoodInfo, val haystack: String, val normalizedName: String)

    // Parsed on first search, off the main thread (the caller runs on Dispatchers.Default).
    private val items: List<Item> by lazy { load() }

    fun search(query: String): List<FoodInfo> {
        val tokens = query.normalizedForSearch().split(' ').filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        val first = tokens.first()
        return items
            .filter { item -> tokens.all { it in item.haystack } }
            .sortedWith(compareBy({ !it.normalizedName.startsWith(first) }, { it.normalizedName }))
            .map { it.info }
    }

    private fun load(): List<Item> = try {
        assets.list("restaurants").orEmpty().filter { it.endsWith(".json") }.flatMap { file ->
            val root = assets.open("restaurants/$file").bufferedReader().use { Json.parseToJsonElement(it.readText()) }.jsonObject
            val chain = root.getValue("chain").jsonPrimitive.content
            val aliases = CHAIN_ALIASES[chain].orEmpty()
            root.getValue("items").jsonArray.map { element ->
                val obj = element.jsonObject
                val name = obj.getValue("name").jsonPrimitive.content
                val grams = obj["portionGrams"]?.jsonPrimitive?.doubleOrNull
                val info = FoodInfo(
                    name = name,
                    kcalPer100 = obj.num("kcal"),
                    proteinPer100 = obj.num("protein"),
                    carbsPer100 = obj.num("carbs"),
                    fatPer100 = obj.num("fat"),
                    fiberPer100 = obj.num("fiber"),
                    saltPer100 = obj.num("salt"),
                    // Pseudo-barcode: lets a logged item be recognised (and not duplicated) in
                    // saved foods, without needing its own column. Never collides with EAN/UPC.
                    barcode = "menu:${chain.normalizedForSearch()}:${name.normalizedForSearch()}".replace(' ', '-'),
                    unit = "pcs",
                    referenceAmount = 1.0,
                    brand = chain,
                    quantity = grams?.let { "portion ${it.toInt()} g" }
                )
                Item(info, "$name $chain $aliases".normalizedForSearch(), name.normalizedForSearch())
            }
        }
    } catch (e: Exception) {
        Log.e("RestaurantMenu", "Couldn't load bundled menus", e)
        emptyList()
    }

    private fun JsonObject.num(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

    companion object {
        private val CHAIN_ALIASES = mapOf("McDonald's" to "mcdonalds mcd mekac mekdonalds")
    }
}

/**
 * A search hit from kaloricketabulky.cz. Only carries kcal/100 — full macros and the
 * portion size need a second request, done lazily via [KalorickeTabulkyRepository.resolve]
 * when the user actually picks the item.
 */
data class KtHit(
    val id: String,
    val title: String,
    val brand: String?,
    val kcalPer100: Double,
    val unit: String
)

/**
 * Live lookups against kaloricketabulky.cz, the largest Czech food database. It covers the
 * menus of Czech chains and restaurants (Bageterie Boulevard, Don Pačínko, Subway, KFC, ...)
 * that Open Food Facts doesn't, since those have no barcodes.
 */
class KalorickeTabulkyRepository {
    private val TAG = "KalorickeTabulky"
    private val base = "https://www.kaloricketabulky.cz"

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    private val searchCache = object : LinkedHashMap<String, List<KtHit>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<KtHit>>) = size > 20
    }
    private val resolved = HashMap<String, FoodInfo>()

    /** Throws on network failure so callers can tell "offline" apart from "no matches". */
    suspend fun search(query: String): List<KtHit> {
        val trimmed = query.trim()
        val key = trimmed.lowercase()
        if (key.length < 2) return emptyList()
        searchCache[key]?.let { return it }

        val response: String = client.get("$base/autocomplete/foodstuff-activity-meal") {
            parameter("query", trimmed)
        }.body()

        val hits = Json.parseToJsonElement(response).jsonArray.mapNotNull { element ->
            val obj = element.jsonObject
            // Recipes, meals and activities use different detail endpoints; foods only.
            if (obj.str("clazz") != "foodstuff") return@mapNotNull null
            KtHit(
                id = obj.str("id") ?: return@mapNotNull null,
                title = obj.str("title") ?: return@mapNotNull null,
                brand = obj.str("brandName"),
                kcalPer100 = obj.str("value")?.czechNumber() ?: return@mapNotNull null,
                unit = if (obj.str("unit") == "ml") "ml" else "g"
            )
        }.distinctBy { it.id } // ids are LazyColumn keys; duplicates would crash the list
        searchCache[key] = hits
        return hits
    }

    /**
     * Fetches full macros for [hit]. If the item has a natural portion (e.g. "kus (215 g)"
     * for a Big Mac, "porce (216 g)" for a pizza slice), it's returned as a per-piece food,
     * which is how restaurant meals are actually logged; otherwise per 100 g/ml.
     */
    suspend fun resolve(hit: KtHit): FoodInfo {
        resolved[hit.id]?.let { return it }

        val detail = Json.parseToJsonElement(
            client.get("$base/foodstuff/detail/${hit.id}/100/${hit.unit}") { parameter("format", "json") }.body<String>()
        ).jsonObject
        val food = detail.getValue("foodstuff").jsonObject
        val energy = food.str("energy")?.czechNumber() ?: hit.kcalPer100
        val kcal100 = if (detail.str("energyUnit").equals("kj", ignoreCase = true)) energy / 4.184 else energy

        val portion = try {
            val form = Json.parseToJsonElement(
                client.get("$base/foodstuff/detail/form/${hit.id}") {
                    parameter("format", "json")
                    parameter("default", "true")
                }.body<String>()
            ).jsonObject
            form["unitOptions"]?.jsonArray
                ?.map { it.jsonObject }
                // "100 g" / "1 ml" are just the base unit; anything else is a real serving.
                ?.firstOrNull { !BASE_UNIT_OPTION.matches(it.str("title").orEmpty()) }
                ?.let { (it.str("title") ?: "") to (it["multiplier"]?.jsonPrimitive?.doubleOrNull ?: 0.0) }
                ?.takeIf { it.second > 0 }
        } catch (e: Exception) {
            Log.w(TAG, "No portion info for ${hit.title}: ${e.message}")
            null
        }

        val scale = portion?.let { it.second / 100.0 } ?: 1.0
        val info = FoodInfo(
            name = hit.title,
            kcalPer100 = kcal100 * scale,
            proteinPer100 = (food.str("protein")?.czechNumber() ?: 0.0) * scale,
            carbsPer100 = (food.str("carbohydrate")?.czechNumber() ?: 0.0) * scale,
            fatPer100 = (food.str("fat")?.czechNumber() ?: 0.0) * scale,
            fiberPer100 = (food.str("fiber")?.czechNumber() ?: 0.0) * scale,
            saltPer100 = (food.str("salt")?.czechNumber() ?: 0.0) * scale,
            barcode = "kt:${hit.id}",
            unit = if (portion != null) "pcs" else hit.unit,
            referenceAmount = if (portion != null) 1.0 else 100.0,
            brand = hit.brand,
            quantity = portion?.first
        )
        resolved[hit.id] = info
        return info
    }

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private val BASE_UNIT_OPTION = Regex("""^\s*1?0*\s*(g|ml)\s*$""")
    }
}

/** "1 137,5" (Czech formatting, with any kind of space as thousands separator) -> 1137.5 */
private fun String.czechNumber(): Double? =
    replace(Regex("""[\s  ]"""), "").replace(',', '.').toDoubleOrNull()

/** Lowercase, accents stripped, punctuation collapsed to single spaces. */
fun String.normalizedForSearch(): String =
    Normalizer.normalize(this, Normalizer.Form.NFD)
        .replace(Regex("""\p{Mn}+"""), "")
        .lowercase()
        .replace(Regex("""[^a-z0-9]+"""), " ")
        .trim()
