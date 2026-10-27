package com.jsemalja.trackmecalories.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.json.*
import java.util.Locale

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
    val nutriScore: String? = null
)

/**
 * Repository for fetching food data from Open Food Facts.
 * Optionally backed by a [FoodDao] to cache barcode lookups offline, so repeat scans
 * don't always hit the network.
 */
class FoodRepository(private val dao: FoodDao? = null) {
    private val TAG = "FoodRepository"

    // Lightweight in-memory cache for repeated identical searches within this process's
    // lifetime. Persistent caching only applies to individual (barcode-keyed) products.
    private val searchCache = object : LinkedHashMap<String, List<FoodInfo>>() {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<FoodInfo>>) =
            size > 40
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
        }
    }

    /**
     * Product information for [barcode], stale-while-revalidate: a cached product is
     * returned at once (offline, no wait), and if it was cached more than
     * [STALE_AFTER_MS] ago [onStale] is called so the caller can refresh it in the
     * background with [fetchProductInfo] (recipes and labels change). Without a cache hit
     * it's fetched from Open Food Facts.
     * @return FoodInfo if found, null otherwise.
     */
    suspend fun getProductInfo(barcode: String, onStale: () -> Unit = {}): FoodInfo? {
        dao?.getCachedProduct(barcode)?.let {
            Log.d(TAG, "Cache hit for barcode: $barcode")
            if (System.currentTimeMillis() - it.cachedAt > STALE_AFTER_MS) onStale()
            return it.toFoodInfo()
        }
        return fetchProductInfo(barcode)
    }

    /**
     * Fetches a product from the Open Food Facts API v2 and caches it. Asks the Czech
     * instance first (same database, but names come back in Czech where the product has
     * them), then the world one if that fails.
     */
    suspend fun fetchProductInfo(barcode: String): FoodInfo? {
        Log.d(TAG, "Fetching product info for barcode: $barcode")
        for (host in OFF_PRODUCT_HOSTS) {
            try {
                val response: String = client.get("https://$host/api/v2/product/$barcode.json").body()
                val json = Json.parseToJsonElement(response).jsonObject

                if (json["status"]?.jsonPrimitive?.intOrNull != 1) {
                    // Both instances share one database: "not found" on one is final.
                    Log.w(TAG, "Product not found for barcode: $barcode")
                    return null
                }

                val product = json["product"]?.jsonObject ?: return null
                val info = parseProduct(product, barcode, requireNutrition = false, langs = PREFERRED_LANGS) ?: return null
                info.toCache()?.let { dao?.upsertCachedProduct(it) }
                return info
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching product info from $host: ${e.message}", e)
            }
        }
        return null
    }

    /**
     * Full-text product search via Open Food Facts' search service (search-a-licious).
     *
     * The older `/api/v2/search` endpoint silently ignores `search_terms` and returns the
     * globally most-scanned products for any query, so it can't be used for text search.
     *
     * Throws on network failure so callers can tell "offline" apart from "no matches".
     */
    suspend fun searchProducts(query: String): List<FoodInfo> = supervisorScope {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@supervisorScope emptyList()

        // Czech products first (a filtered query), then the global one so anything else can
        // still be found. The CZ query also runs without the last, maybe half-typed, word —
        // search-a-licious has no prefix matching — and the caller re-filters by prefix.
        val requests = SearchMatcher.onlineVariants(trimmed).map { variant ->
            async { searchVariant(variant, czechOnly = true) }
        } + async { searchVariant(trimmed, czechOnly = false) }

        val outcomes = requests.map { runCatching { it.await() } }
        // Only report a failure if every request failed (offline), not if one was slow.
        if (outcomes.all { it.isFailure }) throw outcomes.first().exceptionOrNull()!!
        val results = outcomes.mapNotNull { it.getOrNull() }.flatten()
            .distinctBy { it.barcode ?: "${it.name}|${it.brand}" }

        results.forEach { info -> info.toCache()?.let { dao?.upsertCachedProduct(it) } }
        results
    }

    private suspend fun searchVariant(query: String, czechOnly: Boolean): List<FoodInfo> {
        val key = "${if (czechOnly) "cz" else "all"}|${query.lowercase()}"
        synchronized(searchCache) { searchCache[key] }?.let { return it }

        val langs = PREFERRED_LANGS
        val response: String = client.get("https://search.openfoodfacts.org/search") {
            parameter("q", if (czechOnly) "$query countries_tags:\"en:czech-republic\"" else query)
            parameter("page_size", if (czechOnly) 40 else 30)
            parameter("langs", langs.joinToString(","))
            parameter(
                "fields",
                (listOf("code", "product_name", "generic_name", "brands", "quantity", "nutriments", "countries_tags", "nutriscore_grade") +
                    langs.flatMap { listOf("product_name_$it", "generic_name_$it") }).joinToString(",")
            )
        }.body()

        val hits = Json.parseToJsonElement(response).jsonObject["hits"]?.jsonArray ?: return emptyList()
        val results = hits.mapNotNull { hit ->
            val obj = hit.jsonObject
            parseProduct(obj, obj.string("code"), requireNutrition = true, langs = langs)
        }
        Log.d(TAG, "Search '$query' (cz=$czechOnly): ${hits.size} hits, ${results.size} usable")
        synchronized(searchCache) { searchCache[key] = results }
        return results
    }

    /**
     * Maps an Open Food Facts product object to [FoodInfo]. Returns null when there's no
     * usable name, or (if [requireNutrition]) no energy value — logging those would just
     * record a misleading 0 kcal entry.
     */
    private fun parseProduct(
        product: JsonObject,
        barcode: String?,
        requireNutrition: Boolean,
        langs: List<String> = PREFERRED_LANGS
    ): FoodInfo? {
        val name = (langs.map { "product_name_$it" } + "product_name" +
            langs.map { "generic_name_$it" } + "generic_name")
            .firstNotNullOfOrNull { product.string(it) }
            ?: return null

        val nutrients = product["nutriments"] as? JsonObject
        val kcal = nutrients?.number("energy-kcal_100g")
            ?: nutrients?.number("energy-kj_100g")?.div(4.184)
            ?: nutrients?.number("energy_100g")?.div(4.184) // OFF stores generic energy in kJ
        if (requireNutrition && kcal == null) return null

        return FoodInfo(
            name = name,
            kcalPer100 = kcal ?: 0.0,
            // OFF's key is the plural "proteins_100g".
            proteinPer100 = nutrients?.number("proteins_100g") ?: 0.0,
            carbsPer100 = nutrients?.number("carbohydrates_100g") ?: 0.0,
            fatPer100 = nutrients?.number("fat_100g") ?: 0.0,
            fiberPer100 = nutrients?.number("fiber_100g") ?: 0.0,
            saltPer100 = nutrients?.number("salt_100g") ?: 0.0,
            barcode = barcode,
            brand = product.brands(),
            quantity = product.string("quantity"),
            sugarPer100 = nutrients?.number("sugars_100g"),
            saturatedFatPer100 = nutrients?.number("saturated-fat_100g"),
            nutriScore = product.string("nutriscore_grade")?.lowercase()?.takeIf { it.length == 1 && it[0] in 'a'..'e' },
            isCzech = (product["countries_tags"] as? JsonArray)
                ?.any { (it as? JsonPrimitive)?.contentOrNull == "en:czech-republic" } == true
        )
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.doubleOrNull

    // `brands` is a comma-separated string on the product API but an array on the search API.
    private fun JsonObject.brands(): String? {
        val first = when (val raw = this["brands"]) {
            is JsonArray -> raw.firstNotNullOfOrNull { (it as? JsonPrimitive)?.contentOrNull }
            is JsonPrimitive -> raw.contentOrNull?.split(',')?.firstOrNull()
            else -> null
        }
        return first?.trim()?.takeIf { it.isNotEmpty() }
    }

    private companion object {
        // Czech instance first; world as a fallback if it's unreachable.
        val OFF_PRODUCT_HOSTS = listOf("cz.openfoodfacts.org", "world.openfoodfacts.org")

        // Czech names first so "Jogurt bílý" isn't shown as its English/German label.
        val PREFERRED_LANGS = listOf("cs", Locale.getDefault().language.ifBlank { "en" }, "en").distinct()

        // A cached product older than this is still shown, then re-fetched in the background.
        const val STALE_AFTER_MS = 30L * 24 * 60 * 60 * 1000
    }
}
