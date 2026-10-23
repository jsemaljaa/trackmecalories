package com.jsemalja.trackmecalories.data

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
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
    val quantity: String? = null
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
            size > 20
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
     * Fetches product information from the Open Food Facts API v2, preferring a local
     * cache hit over the network.
     * @param barcode The UPC/EAN code of the product.
     * @return FoodInfo if found, null otherwise.
     */
    suspend fun getProductInfo(barcode: String): FoodInfo? {
        dao?.getCachedProduct(barcode)?.let {
            Log.d(TAG, "Cache hit for barcode: $barcode")
            return it.toFoodInfo()
        }
        Log.d(TAG, "Fetching product info for barcode: $barcode")
        return try {
            val response: String = client.get("https://world.openfoodfacts.org/api/v2/product/$barcode.json").body()
            val json = Json.parseToJsonElement(response).jsonObject

            if (json["status"]?.jsonPrimitive?.intOrNull != 1) {
                Log.w(TAG, "Product not found for barcode: $barcode")
                return null
            }

            val product = json["product"]?.jsonObject ?: return null
            val info = parseProduct(product, barcode, requireNutrition = false) ?: return null
            info.toCache()?.let { dao?.upsertCachedProduct(it) }
            info
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching product info: ${e.message}", e)
            null
        }
    }

    /**
     * Full-text product search via Open Food Facts' search service (search-a-licious).
     *
     * The older `/api/v2/search` endpoint silently ignores `search_terms` and returns the
     * globally most-scanned products for any query, so it can't be used for text search.
     *
     * Throws on network failure so callers can tell "offline" apart from "no matches".
     */
    suspend fun searchProducts(query: String): List<FoodInfo> {
        val trimmed = query.trim()
        val key = trimmed.lowercase()
        if (key.length < 2) return emptyList()
        searchCache[key]?.let { return it }

        val deviceLang = Locale.getDefault().language.ifBlank { "en" }
        val langs = listOf(deviceLang, "en").distinct()

        val response: String = client.get("https://search.openfoodfacts.org/search") {
            parameter("q", trimmed)
            parameter("page_size", 40)
            parameter("langs", langs.joinToString(","))
            parameter(
                "fields",
                (listOf("code", "product_name", "generic_name", "brands", "quantity", "nutriments") +
                    langs.flatMap { listOf("product_name_$it", "generic_name_$it") }).joinToString(",")
            )
        }.body()

        val hits = Json.parseToJsonElement(response).jsonObject["hits"]?.jsonArray ?: return emptyList()
        val results = hits.mapNotNull { hit ->
            val obj = hit.jsonObject
            parseProduct(obj, obj.string("code"), requireNutrition = true, langs = langs)
        }.distinctBy { it.barcode ?: "${it.name}|${it.brand}" }

        Log.d(TAG, "Search '$trimmed': ${hits.size} hits, ${results.size} usable")
        searchCache[key] = results
        results.forEach { info -> info.toCache()?.let { dao?.upsertCachedProduct(it) } }
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
        langs: List<String> = listOf(Locale.getDefault().language, "en")
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
            quantity = product.string("quantity")
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
}
