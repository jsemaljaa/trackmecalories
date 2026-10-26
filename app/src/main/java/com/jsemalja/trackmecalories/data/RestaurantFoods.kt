package com.jsemalja.trackmecalories.data

import android.content.res.AssetManager
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.*

/**
 * Restaurant menus bundled with the app (JSON files in assets/restaurants): McDonald's ČR from the
 * official nutrition tables (tools/scrape_mcdonalds_cz.py) and other Czech chains from
 * Kalorické tabulky (tools/scrape_kaloricke_chains.py). Searching them is offline, instant,
 * accent-insensitive and works on half-typed words.
 */
class RestaurantMenu(private val assets: AssetManager) {
    // [name] is the item alone ("Big Mac"); [food]'s name also carries the chain for the log.
    private class Item(val food: CustomFood, val name: String, val chain: String, val aliases: String, val detail: String?)

    // Parsed on first search, off the main thread (the caller runs on Dispatchers.Default).
    private val items: List<Item> by lazy { load() }

    fun search(query: String): List<SearchHit> {
        val tokens = SearchMatcher.tokens(query)
        if (tokens.isEmpty()) return emptyList()
        return items.mapNotNull { item ->
            val score = SearchMatcher.score(tokens, item.name, item.chain, item.aliases) ?: return@mapNotNull null
            SearchHit(
                key = item.food.barcode!!,
                name = item.name,
                brand = item.chain,
                detail = item.detail,
                kcal = item.food.kcalPer100,
                protein = item.food.proteinPer100,
                carbs = item.food.carbsPer100,
                fat = item.food.fatPer100,
                unit = item.food.unit,
                source = SearchSource.MENU,
                // Curated menus are what people searching a chain want first.
                score = score + 0.9,
                food = item.food,
                isCzech = true
            )
        }
    }

    private fun load(): List<Item> = try {
        assets.list("restaurants").orEmpty().filter { it.endsWith(".json") }.flatMap { file ->
            val root = assets.open("restaurants/$file").bufferedReader().use { Json.parseToJsonElement(it.readText()) }.jsonObject
            val chain = root.getValue("chain").jsonPrimitive.content
            val aliases = CHAIN_ALIASES[chain].orEmpty()
            root.getValue("items").jsonArray.map { element ->
                val obj = element.jsonObject
                val name = obj.getValue("name").jsonPrimitive.content
                val ktId = obj["ktId"]?.jsonPrimitive?.contentOrNull
                val unit = obj["unit"]?.jsonPrimitive?.contentOrNull ?: "pcs"
                val grams = obj["portionGrams"]?.jsonPrimitive?.doubleOrNull
                val portion = obj["portion"]?.jsonPrimitive?.contentOrNull
                val food = CustomFood(
                    // Pseudo-barcode: lets a logged item be recognised (and not duplicated) in
                    // saved foods without its own column. KT items share the live search's
                    // "kt:" key, so the same item from either path is one saved food.
                    barcode = ktId?.let { "kt:$it" } ?: "menu:${chain.normalizedForSearch()}:${name.normalizedForSearch()}".replace(' ', '-'),
                    name = "$name · $chain",
                    kcalPer100 = obj.num("kcal"),
                    proteinPer100 = obj.num("protein"),
                    carbsPer100 = obj.num("carbs"),
                    fatPer100 = obj.num("fat"),
                    fiberPer100 = obj.num("fiber"),
                    saltPer100 = obj.num("salt"),
                    unit = unit,
                    referenceAmount = obj["referenceAmount"]?.jsonPrimitive?.doubleOrNull ?: if (unit == "pcs") 1.0 else 100.0,
                    // McDonald's lists the weight; KT chains name the serving, "balení (400 g)".
                    pieceGrams = if (unit == "pcs") grams ?: quantityGrams(portion) else null
                )
                Item(food, name, chain, aliases, portion ?: grams?.let { "portion ${it.toInt()} g" })
            }
        }
    } catch (e: Exception) {
        Log.e("RestaurantMenu", "Couldn't load bundled menus", e)
        emptyList()
    }

    private fun JsonObject.num(key: String): Double = this[key]?.jsonPrimitive?.doubleOrNull ?: 0.0

    companion object {
        private val CHAIN_ALIASES = mapOf(
            "McDonald's" to "mcdonalds mcd mekac mekdonalds",
            "Burger King" to "bk burgerking",
            "Domino's" to "dominos",
            "Albert Fresh Bistro" to "albert"
        )
    }
}

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
    val saturatedFat: Double?
)

/**
 * Live lookups against kaloricketabulky.cz, the largest Czech food database (Czech brands,
 * supermarket products, restaurant meals).
 */
class KalorickeTabulkyRepository {
    private val TAG = "KalorickeTabulky"
    private val base = "https://www.kaloricketabulky.cz"

    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 12_000
            connectTimeoutMillis = 8_000
        }
    }

    private val searchCache = object : LinkedHashMap<String, List<KtHit>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<KtHit>>) = size > 40
    }
    private val macrosCache = HashMap<String, KtMacros>()
    private val resolved = HashMap<String, FoodInfo>()
    // Be polite to KT when prefetching macros for a page of results.
    private val detailPermits = Semaphore(4)

    /**
     * Searches every variant of [query] (see [SearchMatcher.onlineVariants]) in parallel and
     * returns the distinct hits, in KT's own popularity order. Throws on network failure so
     * callers can tell "offline" apart from "no matches".
     */
    suspend fun search(query: String): List<KtHit> = supervisorScope {
        val outcomes = SearchMatcher.onlineVariants(query)
            .map { variant -> async { searchVariant(variant) } }
            .map { runCatching { it.await() } }
        // Only a failure if every variant failed (offline), not just one.
        if (outcomes.all { it.isFailure }) throw outcomes.first().exceptionOrNull()!!
        outcomes.mapNotNull { it.getOrNull() }
            .flatten()
            .distinctBy { it.id } // ids are LazyColumn keys; duplicates would crash the list
    }

    private suspend fun searchVariant(query: String): List<KtHit> {
        val key = query.lowercase()
        synchronized(searchCache) { searchCache[key] }?.let { return it }

        val response: String = client.get("$base/autocomplete/foodstuff-activity-meal") {
            parameter("query", query)
            // Default is ~10; more candidates survive the local relevance filter.
            parameter("limit", 50)
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
        }
        synchronized(searchCache) { searchCache[key] = hits }
        return hits
    }

    /** Per-100 macros for [hit], cached. Used to show P/C/F in the result list. */
    suspend fun macros(hit: KtHit): KtMacros {
        synchronized(macrosCache) { macrosCache[hit.id] }?.let { return it }
        return detailPermits.withPermit {
            val detail = Json.parseToJsonElement(
                client.get("$base/foodstuff/detail/${hit.id}/100/${hit.unit}") { parameter("format", "json") }.body<String>()
            ).jsonObject
            val food = detail.getValue("foodstuff").jsonObject
            val energy = food.str("energy")?.czechNumber() ?: hit.kcalPer100
            val macros = KtMacros(
                kcal = if (detail.str("energyUnit").equals("kj", ignoreCase = true)) energy / 4.184 else energy,
                protein = food.str("protein")?.czechNumber() ?: 0.0,
                carbs = food.str("carbohydrate")?.czechNumber() ?: 0.0,
                fat = food.str("fat")?.czechNumber() ?: 0.0,
                fiber = food.str("fiber")?.czechNumber() ?: 0.0,
                salt = food.str("salt")?.czechNumber() ?: 0.0,
                sugar = food.str("sugar")?.czechNumber(),
                saturatedFat = food.str("saturatedFattyAcid")?.czechNumber()
            )
            synchronized(macrosCache) { macrosCache[hit.id] = macros }
            macros
        }
    }

    /**
     * Full food for [hit]. If it has a natural serving (e.g. "kus (215 g)" for a Big Mac,
     * "porce (216 g)" for a pizza slice), it's returned as a per-piece food, which is how
     * restaurant meals are actually logged; otherwise per 100 g/ml.
     */
    suspend fun resolve(hit: KtHit): FoodInfo {
        synchronized(resolved) { resolved[hit.id] }?.let { return it }

        val (macros, portion) = coroutineScope {
            val macros = async { macros(hit) }
            val portion = async { portion(hit) }
            macros.await() to portion.await()
        }

        val scale = portion?.let { it.second / 100.0 } ?: 1.0
        val info = FoodInfo(
            name = hit.title,
            kcalPer100 = macros.kcal * scale,
            proteinPer100 = macros.protein * scale,
            carbsPer100 = macros.carbs * scale,
            fatPer100 = macros.fat * scale,
            fiberPer100 = macros.fiber * scale,
            saltPer100 = macros.salt * scale,
            sugarPer100 = macros.sugar?.times(scale),
            saturatedFatPer100 = macros.saturatedFat?.times(scale),
            barcode = "kt:${hit.id}",
            unit = if (portion != null) "pcs" else hit.unit,
            referenceAmount = if (portion != null) 1.0 else 100.0,
            brand = hit.brand,
            quantity = portion?.first
        )
        synchronized(resolved) { resolved[hit.id] = info }
        return info
    }

    private suspend fun portion(hit: KtHit): Pair<String, Double>? = try {
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

    private fun JsonObject.str(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    companion object {
        private val BASE_UNIT_OPTION = Regex("""^\s*1?0*\s*(g|ml)\s*$""")
    }
}

/** "1 137,5" (Czech formatting, with any kind of space as thousands separator) -> 1137.5 */
private fun String.czechNumber(): Double? =
    replace(Regex("""[\s  ]"""), "").replace(',', '.').toDoubleOrNull()
